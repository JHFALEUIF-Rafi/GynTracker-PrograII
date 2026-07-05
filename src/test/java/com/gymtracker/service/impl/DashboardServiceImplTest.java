package com.gymtracker.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.gymtracker.dto.alert.AlertDTO;
import com.gymtracker.dto.dashboard.DashboardDTO;
import com.gymtracker.dto.mesocycle.MesocycleResponseDTO;
import com.gymtracker.dto.mesocycle.MesocycleSummaryDTO;
import com.gymtracker.dto.nutrition.NutritionPlanResponseDTO;
import com.gymtracker.dto.nutrition.NutritionPlanSummaryDTO;
import com.gymtracker.dto.workout.WorkoutSessionSummaryDTO;
import com.gymtracker.entity.User;
import com.gymtracker.enums.AlertStatus;
import com.gymtracker.enums.FatigueLevel;
import com.gymtracker.enums.MesocycleStatus;
import com.gymtracker.enums.NutritionGoal;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.security.AuthenticatedUserProvider;
import com.gymtracker.service.AlertService;
import com.gymtracker.service.FatigueService;
import com.gymtracker.service.MesocycleService;
import com.gymtracker.service.NutritionPlanService;
import com.gymtracker.service.OneRepMaxService;
import com.gymtracker.service.WorkoutSessionService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for DashboardServiceImpl. {@code @Cacheable}/{@code @CacheEvict}
 * have no effect outside a Spring context, so caching behavior is not
 * exercised here (methods run directly).
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private WorkoutSessionService workoutSessionService;
    @Mock
    private OneRepMaxService oneRepMaxService;
    @Mock
    private FatigueService fatigueService;
    @Mock
    private AlertService alertService;
    @Mock
    private MesocycleService mesocycleService;
    @Mock
    private NutritionPlanService nutritionPlanService;
    @Mock
    private UserRepository userRepository;

    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        AuthenticatedUserProvider authenticatedUserProvider = new AuthenticatedUserProvider(userRepository);
        dashboardService = new DashboardServiceImpl(workoutSessionService, oneRepMaxService, fatigueService,
                alertService, mesocycleService, nutritionPlanService, userRepository, authenticatedUserProvider);
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
                .weight(70.0)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void authenticateAs(User user) {
        TestingAuthenticationToken token = new TestingAuthenticationToken(user.getEmail(), null);
        token.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(token);
        lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    void getAthleteDashboardReturnsAggregatedData() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(athlete);
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));

        when(workoutSessionService.getWorkoutSessionsByAthlete(athlete.getId().toHexString())).thenReturn(List.of());
        when(alertService.getAlertsByAthlete(athlete.getId().toHexString())).thenReturn(List.of(
                AlertDTO.builder().status(AlertStatus.ACTIVE).build()));
        when(fatigueService.getCurrentFatigueLevel(athlete.getId().toHexString())).thenReturn(FatigueLevel.LOW);
        when(fatigueService.calculateRecoveryScore(athlete.getId().toHexString())).thenReturn(90.0);
        when(mesocycleService.getActiveMesocycle(athlete.getId().toHexString())).thenReturn(null);
        when(nutritionPlanService.getActiveNutritionPlan(athlete.getId().toHexString())).thenReturn(null);

        DashboardDTO dashboard = dashboardService.getAthleteDashboard(athlete.getId().toHexString());

        assertThat(dashboard.getRole()).isEqualTo("ATHLETE");
        assertThat(dashboard.getActiveAlerts()).isEqualTo(1);
        assertThat(dashboard.getCurrentFatigueLevel()).isEqualTo("LOW");
    }

    @Test
    void getAthleteDashboardRejectsWrongRole() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        authenticateAs(coach);

        assertThatThrownBy(() -> dashboardService.getAthleteDashboard(coach.getId().toHexString()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void getAthleteDashboardRejectsDifferentAthleteId() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(athlete);

        assertThatThrownBy(() -> dashboardService.getAthleteDashboard(new ObjectId().toHexString()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void getCoachDashboardAggregatesAssignedAthletes() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        authenticateAs(coach);

        MesocycleSummaryDTO mesocycleSummary = MesocycleSummaryDTO.builder()
                .athleteId(new ObjectId().toHexString()).status(MesocycleStatus.ACTIVE).build();
        when(mesocycleService.getMesocyclesByCoach(coach.getId().toHexString())).thenReturn(List.of(mesocycleSummary));
        when(fatigueService.getCurrentFatigueLevel(mesocycleSummary.getAthleteId())).thenReturn(FatigueLevel.HIGH);
        when(workoutSessionService.getWorkoutSessionsByAthlete(mesocycleSummary.getAthleteId())).thenReturn(List.of());
        when(alertService.getAlertsByCoach(coach.getId().toHexString())).thenReturn(List.of());

        DashboardDTO dashboard = dashboardService.getCoachDashboard(coach.getId().toHexString());

        assertThat(dashboard.getAssignedAthletes()).isEqualTo(1);
        assertThat(dashboard.getActiveMesocycles()).isEqualTo(1);
        assertThat(dashboard.getAthletesWithHighFatigue()).isEqualTo(1);
    }

    @Test
    void getNutritionistDashboardAggregatesActivePlans() {
        User nutritionist = buildUser(Role.NUTRITIONIST, "nutritionist@example.com");
        authenticateAs(nutritionist);

        NutritionPlanSummaryDTO plan = NutritionPlanSummaryDTO.builder()
                .athleteId(new ObjectId().toHexString()).active(true).goal(NutritionGoal.CUTTING).build();
        when(nutritionPlanService.getNutritionPlansByNutritionist(nutritionist.getId().toHexString()))
                .thenReturn(List.of(plan));
        when(alertService.getAlertsByAthlete(plan.getAthleteId())).thenReturn(List.of());

        DashboardDTO dashboard = dashboardService.getNutritionistDashboard(nutritionist.getId().toHexString());

        assertThat(dashboard.getAssignedAthletes()).isEqualTo(1);
        assertThat(dashboard.getActiveNutritionPlans()).isEqualTo(1);
    }

    @Test
    void refreshDashboardRejectsDifferentUser() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(athlete);

        assertThatThrownBy(() -> dashboardService.refreshDashboard(new ObjectId().toHexString()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void getDashboardSummaryDispatchesToAthleteDashboard() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(athlete);
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));
        when(workoutSessionService.getWorkoutSessionsByAthlete(athlete.getId().toHexString())).thenReturn(List.of());
        when(alertService.getAlertsByAthlete(athlete.getId().toHexString())).thenReturn(List.of());
        when(fatigueService.getCurrentFatigueLevel(athlete.getId().toHexString())).thenReturn(FatigueLevel.LOW);
        when(fatigueService.calculateRecoveryScore(athlete.getId().toHexString())).thenReturn(90.0);
        when(mesocycleService.getActiveMesocycle(athlete.getId().toHexString())).thenReturn(null);
        when(nutritionPlanService.getActiveNutritionPlan(athlete.getId().toHexString())).thenReturn(null);

        DashboardDTO dashboard = dashboardService.getDashboardSummary(athlete.getId().toHexString());

        assertThat(dashboard.getRole()).isEqualTo("ATHLETE");
    }
}
