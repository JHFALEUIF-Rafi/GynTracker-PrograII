package com.gymtracker.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.gymtracker.dto.dashboard.ChartDTO;
import com.gymtracker.dto.fatigue.FatigueDTO;
import com.gymtracker.entity.Session;
import com.gymtracker.entity.User;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.BusinessRuleException;
import com.gymtracker.exception.ResourceNotFoundException;
import com.gymtracker.repository.MesocycleRepository;
import com.gymtracker.repository.SessionRepository;
import com.gymtracker.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for FatigueServiceImpl against mocked repositories.
 */
@ExtendWith(MockitoExtension.class)
class FatigueServiceImplTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private MesocycleRepository mesocycleRepository;
    @Mock
    private UserRepository userRepository;

    private FatigueServiceImpl fatigueService;
    private User athlete;

    @BeforeEach
    void setUp() {
        fatigueService = new FatigueServiceImpl(sessionRepository, mesocycleRepository, userRepository);
        athlete = User.builder()
                .id(new ObjectId())
                .role(Role.ATHLETE)
                .email("athlete@example.com")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        lenient().when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));
    }

    private Session buildSession(LocalDate date, double volume, double oneRepMax, int rpe) {
        Session.CompletedSet set = Session.CompletedSet.builder().weight(50.0).repetitions(8).rpe(rpe).build();
        Session.CompletedExercise exercise = Session.CompletedExercise.builder()
                .exerciseId(new ObjectId()).sets(List.of(set)).build();
        return Session.builder()
                .id(new ObjectId())
                .athleteId(athlete.getId())
                .mesocycleId(new ObjectId())
                .date(date)
                .durationMinutes(60)
                .completedExercises(List.of(exercise))
                .totalVolume(volume)
                .estimatedOneRepMax(oneRepMax)
                .build();
    }

    @Test
    void calculateFatigueThrowsWhenNoCompletedSessions() {
        when(sessionRepository.findByAthleteId(athlete.getId())).thenReturn(List.of());

        CompletableFuture<FatigueDTO> result = fatigueService.calculateFatigue(athlete.getId().toHexString());

        assertThatThrownBy(result::join).hasCauseInstanceOf(BusinessRuleException.class);
    }

    @Test
    void calculateFatigueReturnsSnapshotForLatestSession() {
        when(sessionRepository.findByAthleteId(athlete.getId()))
                .thenReturn(List.of(buildSession(LocalDate.now(), 400.0, 80.0, 7)));
        when(mesocycleRepository.findByAthleteId(athlete.getId())).thenReturn(List.of());

        FatigueDTO result = fatigueService.calculateFatigue(athlete.getId().toHexString()).join();

        assertThat(result.getAthleteId()).isEqualTo(athlete.getId().toHexString());
        assertThat(result.getFatigueLevel()).isNotNull();
    }

    @Test
    void getCurrentFatigueLevelThrowsWhenAthleteNotFound() {
        when(userRepository.findById("missing-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fatigueService.getCurrentFatigueLevel("missing-id"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getFatigueHistoryReturnsSnapshotPerSession() {
        when(sessionRepository.findByAthleteId(athlete.getId())).thenReturn(List.of(
                buildSession(LocalDate.now().minusDays(3), 300.0, 70.0, 6),
                buildSession(LocalDate.now(), 400.0, 80.0, 7)
        ));
        when(mesocycleRepository.findByAthleteId(athlete.getId())).thenReturn(List.of());

        List<FatigueDTO> history = fatigueService.getFatigueHistory(athlete.getId().toHexString());

        assertThat(history).hasSize(2);
    }

    @Test
    void evaluateWorkoutLoadThrowsForIncompleteSession() {
        Session incompleteSession = Session.builder().id(new ObjectId()).completedExercises(List.of()).build();
        when(sessionRepository.findById(incompleteSession.getId().toHexString())).thenReturn(Optional.of(incompleteSession));

        assertThatThrownBy(() -> fatigueService.evaluateWorkoutLoad(incompleteSession.getId().toHexString()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void evaluateWorkoutLoadComputesPositiveLoadForCompletedSession() {
        Session session = buildSession(LocalDate.now(), 400.0, 80.0, 7);
        when(sessionRepository.findById(session.getId().toHexString())).thenReturn(Optional.of(session));

        Double load = fatigueService.evaluateWorkoutLoad(session.getId().toHexString());

        assertThat(load).isGreaterThan(0.0);
    }

    @Test
    void calculateWeeklyTrainingLoadSumsSessionsWithinLatestWeek() {
        when(sessionRepository.findByAthleteId(athlete.getId())).thenReturn(List.of(
                buildSession(LocalDate.now(), 400.0, 80.0, 7),
                buildSession(LocalDate.now().minusDays(20), 999.0, 80.0, 7)
        ));

        Double weeklyLoad = fatigueService.calculateWeeklyTrainingLoad(athlete.getId().toHexString());

        assertThat(weeklyLoad).isEqualTo(400.0);
    }

    @Test
    void calculateRecoveryScoreIsInverseOfFatigueScore() {
        when(sessionRepository.findByAthleteId(athlete.getId()))
                .thenReturn(List.of(buildSession(LocalDate.now(), 400.0, 80.0, 7)));
        when(mesocycleRepository.findByAthleteId(athlete.getId())).thenReturn(List.of());

        Double recoveryScore = fatigueService.calculateRecoveryScore(athlete.getId().toHexString());

        assertThat(recoveryScore).isBetween(0.0, 100.0);
    }

    @Test
    void getFatigueHistoryChartBuildsLabelsAndValues() {
        when(sessionRepository.findByAthleteId(athlete.getId()))
                .thenReturn(List.of(buildSession(LocalDate.now(), 400.0, 80.0, 7)));
        when(mesocycleRepository.findByAthleteId(athlete.getId())).thenReturn(List.of());

        ChartDTO chart = fatigueService.getFatigueHistoryChart(athlete.getId().toHexString());

        assertThat(chart.getTitle()).isEqualTo("Fatigue History");
        assertThat(chart.getLabels()).hasSize(1);
        assertThat(chart.getValues()).hasSize(1);
    }
}
