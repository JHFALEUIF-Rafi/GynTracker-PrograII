package com.gymtracker.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.gymtracker.dto.dashboard.ChartDTO;
import com.gymtracker.dto.dashboard.StatisticsDTO;
import com.gymtracker.entity.Session;
import com.gymtracker.entity.User;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.repository.MesocycleRepository;
import com.gymtracker.repository.NutritionPlanRepository;
import com.gymtracker.repository.SessionRepository;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.security.CustomUserDetails;
import com.gymtracker.service.AthleteAssignmentService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for StatisticsServiceImpl against mocked repositories.
 */
@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private MesocycleRepository mesocycleRepository;
    @Mock
    private NutritionPlanRepository nutritionPlanRepository;
    @Mock
    private UserRepository userRepository;

    private StatisticsServiceImpl statisticsService;

    @BeforeEach
    void setUp() {
        AthleteAssignmentService athleteAssignmentService =
                new AthleteAssignmentServiceImpl(mesocycleRepository, nutritionPlanRepository);
        statisticsService = new StatisticsServiceImpl(sessionRepository, mesocycleRepository, nutritionPlanRepository,
                userRepository, null, athleteAssignmentService);
        ReflectionTestUtils.setField(statisticsService, "self", statisticsService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User buildUser(Role role, String email) {
        return User.builder()
                .id(new ObjectId())
                .role(role)
                .email(email)
                .weight(75.0)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void authenticateAs(User user) {
        // StatisticsServiceImpl.ensureCanAccessStatistics casts the principal
        // directly to UserDetails, so a plain String principal (as used in
        // other tests via TestingAuthenticationToken) will not work here.
        CustomUserDetails principal = new CustomUserDetails(user);
        TestingAuthenticationToken token = new TestingAuthenticationToken(principal, null);
        token.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(token);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    private Session buildSession(LocalDate date, double volume, int durationMinutes, double oneRepMax) {
        return Session.builder()
                .id(new ObjectId())
                .date(date)
                .durationMinutes(durationMinutes)
                .totalVolume(volume)
                .estimatedOneRepMax(oneRepMax)
                .completedExercises(List.of())
                .build();
    }

    @Test
    void getAthleteStatisticsAllowsAthleteToViewOwnStatistics() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(athlete);
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));
        when(sessionRepository.findByAthleteId(athlete.getId()))
                .thenReturn(List.of(buildSession(LocalDate.now(), 400.0, 60, 80.0)));
        when(mesocycleRepository.findByAthleteId(athlete.getId())).thenReturn(List.of());
        when(nutritionPlanRepository.findByAthleteId(athlete.getId())).thenReturn(List.of());

        StatisticsDTO stats = statisticsService.getAthleteStatistics(athlete.getId().toHexString());

        assertThat(stats.getWeeklyTrainingVolume()).isEqualTo(400.0);
        assertThat(stats.getBodyWeightEvolution()).isEqualTo(75.0);
    }

    @Test
    void getAthleteStatisticsRejectsUnrelatedAthlete() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        User otherAthlete = buildUser(Role.ATHLETE, "other-athlete@example.com");
        authenticateAs(otherAthlete);
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));

        assertThatThrownBy(() -> statisticsService.getAthleteStatistics(athlete.getId().toHexString()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void getAthleteStatisticsAllowsCoachToViewAssignedAthlete() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(coach);
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));
        com.gymtracker.entity.Mesocycle assignedMesocycle = com.gymtracker.entity.Mesocycle.builder()
                .coachId(coach.getId()).athleteId(athlete.getId()).build();
        when(mesocycleRepository.findByCoachId(coach.getId())).thenReturn(List.of(assignedMesocycle));
        when(sessionRepository.findByAthleteId(athlete.getId())).thenReturn(List.of());
        when(mesocycleRepository.findByAthleteId(athlete.getId())).thenReturn(List.of());
        when(nutritionPlanRepository.findByAthleteId(athlete.getId())).thenReturn(List.of());

        StatisticsDTO stats = statisticsService.getAthleteStatistics(athlete.getId().toHexString());

        assertThat(stats).isNotNull();
    }

    @Test
    void getAthleteStatisticsRejectsCoachNotAssignedToAthlete() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(coach);
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));
        when(mesocycleRepository.findByCoachId(coach.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> statisticsService.getAthleteStatistics(athlete.getId().toHexString()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void getCoachStatisticsAggregatesAssignedAthleteWorkouts() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        authenticateAs(coach);
        when(userRepository.findById(coach.getId().toHexString())).thenReturn(Optional.of(coach));

        com.gymtracker.entity.Mesocycle mesocycle = com.gymtracker.entity.Mesocycle.builder()
                .coachId(coach.getId()).athleteId(new ObjectId())
                .status(com.gymtracker.enums.MesocycleStatus.COMPLETED).build();
        when(mesocycleRepository.findByCoachId(coach.getId())).thenReturn(List.of(mesocycle));
        when(sessionRepository.findByAthleteIdIn(Set.of(mesocycle.getAthleteId())))
                .thenReturn(List.of(buildSession(LocalDate.now(), 300.0, 45, 70.0)));

        StatisticsDTO stats = statisticsService.getCoachStatistics(coach.getId().toHexString());

        assertThat(stats.getCompletedMesocycles()).isEqualTo(1);
    }

    @Test
    void getNutritionistStatisticsCountsCompletedPlans() {
        User nutritionist = buildUser(Role.NUTRITIONIST, "nutritionist@example.com");
        authenticateAs(nutritionist);
        when(userRepository.findById(nutritionist.getId().toHexString())).thenReturn(Optional.of(nutritionist));

        com.gymtracker.entity.NutritionPlan plan = com.gymtracker.entity.NutritionPlan.builder()
                .athleteId(new ObjectId()).endDate(LocalDate.now().minusDays(1)).build();
        when(nutritionPlanRepository.findByNutritionistId(nutritionist.getId())).thenReturn(List.of(plan));

        StatisticsDTO stats = statisticsService.getNutritionistStatistics(nutritionist.getId().toHexString());

        assertThat(stats.getCompletedNutritionPlans()).isEqualTo(1);
    }

    @Test
    void getWorkoutVolumeChartBuildsMonthlyLabels() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(athlete);
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));
        when(sessionRepository.findByAthleteId(athlete.getId())).thenReturn(List.of());

        ChartDTO chart = statisticsService.getWorkoutVolumeChart(athlete.getId().toHexString());

        assertThat(chart.getTitle()).isEqualTo("Monthly Training Volume");
        assertThat(chart.getLabels()).hasSize(13);
    }

    @Test
    void getOneRepMaxChartBuildsLabelsFromSessions() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(athlete);
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));
        when(sessionRepository.findByAthleteId(athlete.getId()))
                .thenReturn(List.of(buildSession(LocalDate.now(), 300.0, 45, 70.0)));

        ChartDTO chart = statisticsService.getOneRepMaxChart(athlete.getId().toHexString());

        assertThat(chart.getLabels()).hasSize(1);
        assertThat(chart.getValues()).containsExactly(70.0);
    }

    @Test
    void getFatigueChartBuildsSevenDayPlaceholderSeries() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(athlete);
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));

        ChartDTO chart = statisticsService.getFatigueChart(athlete.getId().toHexString());

        assertThat(chart.getLabels()).hasSize(7);
    }
}
