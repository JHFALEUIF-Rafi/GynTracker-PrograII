package com.gymtracker.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.gymtracker.dto.workout.OneRepMaxComparisonDTO;
import com.gymtracker.dto.workout.OneRepMaxDTO;
import com.gymtracker.entity.Exercise;
import com.gymtracker.entity.Session;
import com.gymtracker.entity.User;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.BusinessRuleException;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.repository.ExerciseRepository;
import com.gymtracker.repository.SessionRepository;
import com.gymtracker.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for OneRepMaxServiceImpl against mocked repositories.
 */
@ExtendWith(MockitoExtension.class)
class OneRepMaxServiceImplTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private ExerciseRepository exerciseRepository;
    @Mock
    private UserRepository userRepository;

    private OneRepMaxServiceImpl oneRepMaxService;
    private User athlete;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        oneRepMaxService = new OneRepMaxServiceImpl(sessionRepository, exerciseRepository, userRepository);
        athlete = User.builder()
                .id(new ObjectId())
                .role(Role.ATHLETE)
                .email("athlete@example.com")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        exercise = Exercise.builder().id(new ObjectId()).name("Bench Press").build();
        lenient().when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));
    }

    private Session buildSession(LocalDate date, ObjectId exerciseId, double weight, int reps) {
        Session.CompletedSet set = Session.CompletedSet.builder().weight(weight).repetitions(reps).rpe(7).build();
        Session.CompletedExercise completedExercise = Session.CompletedExercise.builder()
                .exerciseId(exerciseId).sets(List.of(set)).build();
        return Session.builder()
                .id(new ObjectId())
                .athleteId(athlete.getId())
                .date(date)
                .completedExercises(List.of(completedExercise))
                .totalVolume(weight * reps)
                .estimatedOneRepMax(weight * (1 + reps / 30.0))
                .build();
    }

    @Test
    void estimateOneRepMaxAppliesEpleyFormula() {
        Double result = oneRepMaxService.estimateOneRepMax(100.0, 5);

        assertThat(result).isEqualTo(100.0 * (1 + 5 / 30.0));
    }

    @Test
    void estimateOneRepMaxRejectsInvalidWeight() {
        assertThatThrownBy(() -> oneRepMaxService.estimateOneRepMax(0.0, 5))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void estimateOneRepMaxRejectsInvalidRepetitions() {
        assertThatThrownBy(() -> oneRepMaxService.estimateOneRepMax(100.0, 0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void calculateOneRepMaxReturnsBestValuePerExercise() {
        when(sessionRepository.findByAthleteId(athlete.getId())).thenReturn(List.of(
                buildSession(LocalDate.now().minusDays(1), exercise.getId(), 80.0, 5),
                buildSession(LocalDate.now(), exercise.getId(), 100.0, 5)
        ));
        when(exerciseRepository.findById(exercise.getId().toHexString())).thenReturn(Optional.of(exercise));

        List<OneRepMaxDTO> results = oneRepMaxService.calculateOneRepMax(athlete.getId().toHexString()).join();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEstimatedOneRepMax()).isEqualTo(100.0 * (1 + 5 / 30.0));
    }

    @Test
    void getCurrentEstimatedOneRepMaxThrowsWhenNoValidData() {
        when(sessionRepository.findByAthleteId(athlete.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> oneRepMaxService.getCurrentEstimatedOneRepMax(athlete.getId().toHexString()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void getOneRepMaxHistoryReturnsSortedHistoryForExercise() {
        when(sessionRepository.findByAthleteId(athlete.getId())).thenReturn(List.of(
                buildSession(LocalDate.now().minusDays(1), exercise.getId(), 80.0, 5),
                buildSession(LocalDate.now(), exercise.getId(), 100.0, 5)
        ));
        when(exerciseRepository.findById(exercise.getId().toHexString())).thenReturn(Optional.of(exercise));

        List<OneRepMaxDTO> history = oneRepMaxService.getOneRepMaxHistory(athlete.getId().toHexString(), exercise.getId().toHexString());

        assertThat(history).hasSize(2);
    }

    @Test
    void compareOneRepMaxRequiresAtLeastTwoRecords() {
        when(sessionRepository.findByAthleteId(athlete.getId()))
                .thenReturn(List.of(buildSession(LocalDate.now(), exercise.getId(), 80.0, 5)));
        when(exerciseRepository.findById(exercise.getId().toHexString())).thenReturn(Optional.of(exercise));

        assertThatThrownBy(() -> oneRepMaxService.compareOneRepMax(athlete.getId().toHexString(), exercise.getId().toHexString()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void compareOneRepMaxComputesUpwardTrend() {
        when(sessionRepository.findByAthleteId(athlete.getId())).thenReturn(List.of(
                buildSession(LocalDate.now().minusDays(1), exercise.getId(), 80.0, 5),
                buildSession(LocalDate.now(), exercise.getId(), 100.0, 5)
        ));
        when(exerciseRepository.findById(exercise.getId().toHexString())).thenReturn(Optional.of(exercise));

        OneRepMaxComparisonDTO comparison = oneRepMaxService.compareOneRepMax(
                athlete.getId().toHexString(), exercise.getId().toHexString());

        assertThat(comparison.getTrend()).isEqualTo("UP");
        assertThat(comparison.getAbsoluteChange()).isGreaterThan(0.0);
    }
}
