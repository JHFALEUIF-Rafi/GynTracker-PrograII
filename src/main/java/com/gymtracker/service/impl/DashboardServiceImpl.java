package com.gymtracker.service.impl;

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
import com.gymtracker.enums.Role;
import com.gymtracker.exception.ResourceNotFoundException;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.security.AuthenticatedUserProvider;
import com.gymtracker.service.AlertService;
import com.gymtracker.service.DashboardService;
import com.gymtracker.service.FatigueService;
import com.gymtracker.service.MesocycleService;
import com.gymtracker.service.NutritionPlanService;
import com.gymtracker.service.OneRepMaxService;
import com.gymtracker.service.WorkoutSessionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Business implementation for read-only dashboard aggregation by user role.
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardServiceImpl.class);
    private static final String DASHBOARD_CACHE = "dashboards";
    private static final String TYPE_NUTRITION_PLAN_EXPIRED = "NUTRITION_PLAN_EXPIRED";

    private final WorkoutSessionService workoutSessionService;
    private final OneRepMaxService oneRepMaxService;
    private final FatigueService fatigueService;
    private final AlertService alertService;
    private final MesocycleService mesocycleService;
    private final NutritionPlanService nutritionPlanService;
    private final UserRepository userRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public DashboardServiceImpl(
            WorkoutSessionService workoutSessionService,
            OneRepMaxService oneRepMaxService,
            FatigueService fatigueService,
            AlertService alertService,
            MesocycleService mesocycleService,
            NutritionPlanService nutritionPlanService,
            UserRepository userRepository,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.workoutSessionService = workoutSessionService;
        this.oneRepMaxService = oneRepMaxService;
        this.fatigueService = fatigueService;
        this.alertService = alertService;
        this.mesocycleService = mesocycleService;
        this.nutritionPlanService = nutritionPlanService;
        this.userRepository = userRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @Override
    @Cacheable(value = DASHBOARD_CACHE, key = "'ATHLETE:' + #athleteId")
    public DashboardDTO getAthleteDashboard(String athleteId) {
        User currentUser = getAuthenticatedUser();
        validateIdentifier(athleteId, "Athlete id is required.");
        ensureOwnRoleDashboard(currentUser, Role.ATHLETE, athleteId);

        DashboardDTO dashboard = DashboardDTO.builder()
                .userId(athleteId)
                .role(Role.ATHLETE.name())
                .generatedAt(LocalDateTime.now())
                .build();

        List<WorkoutSessionSummaryDTO> sessions = workoutSessionService.getWorkoutSessionsByAthlete(athleteId);
        List<AlertDTO> alerts = alertService.getAlertsByAthlete(athleteId);
        dashboard.setCompletedSessions(sessions.size());
        dashboard.setLastWorkoutDate(extractLastWorkoutDate(sessions));
        dashboard.setTrainingVolume(sumTrainingVolume(sessions));
        dashboard.setWeeklyTrainingVolume(sumWeeklyTrainingVolume(sessions));
        dashboard.setActiveAlerts((int) alerts.stream().filter(alert -> alert.getStatus() == AlertStatus.ACTIVE).count());

        dashboard.setEstimatedOneRepMax(resolveCurrentOneRepMax(athleteId, sessions));
        dashboard.setCurrentFatigueLevel(fatigueService.getCurrentFatigueLevel(athleteId).name());
        dashboard.setRecoveryScore(fatigueService.calculateRecoveryScore(athleteId));

        MesocycleResponseDTO activeMesocycle = mesocycleService.getActiveMesocycle(athleteId);
        dashboard.setCurrentMesocycle(activeMesocycle != null ? activeMesocycle.getName() : null);

        NutritionPlanResponseDTO activePlan = nutritionPlanService.getActiveNutritionPlan(athleteId);
        dashboard.setCurrentActiveNutritionPlan(activePlan != null ? activePlan.getGoal().name() : null);

        User athlete = findUserById(athleteId);
        dashboard.setCurrentWeight(athlete.getWeight());

        LOGGER.info("Dashboard generated role=ATHLETE userId={}", athleteId);
        return dashboard;
    }

    @Override
    @Cacheable(value = DASHBOARD_CACHE, key = "'COACH:' + #coachId")
    public DashboardDTO getCoachDashboard(String coachId) {
        User currentUser = getAuthenticatedUser();
        validateIdentifier(coachId, "Coach id is required.");
        ensureOwnRoleDashboard(currentUser, Role.COACH, coachId);

        List<MesocycleSummaryDTO> mesocycles = mesocycleService.getMesocyclesByCoach(coachId);
        Set<String> athleteIds = new HashSet<>();
        for (MesocycleSummaryDTO mesocycle : mesocycles) {
            if (mesocycle.getAthleteId() != null && !mesocycle.getAthleteId().isBlank()) {
                athleteIds.add(mesocycle.getAthleteId());
            }
        }

        int activeMesocycles = (int) mesocycles.stream()
                .filter(mesocycle -> mesocycle.getStatus() == MesocycleStatus.ACTIVE)
                .count();

        int athletesWithHighFatigue = 0;
        int weeklySessions = 0;
        double trendAccumulator = 0.0d;
        int trendCount = 0;
        LocalDate weekStart = LocalDate.now().minusDays(6);
        for (String athleteId : athleteIds) {
            FatigueLevel fatigueLevel = fatigueService.getCurrentFatigueLevel(athleteId);
            if (fatigueLevel == FatigueLevel.HIGH || fatigueLevel == FatigueLevel.CRITICAL) {
                athletesWithHighFatigue++;
            }

            List<WorkoutSessionSummaryDTO> athleteSessions = workoutSessionService.getWorkoutSessionsByAthlete(athleteId);
            weeklySessions += athleteSessions.stream()
                    .filter(session -> session.getDate() != null && !session.getDate().isBefore(weekStart))
                    .count();

            Double athleteTrend = calculateAthletePerformanceTrend(athleteSessions);
            if (athleteTrend != null) {
                trendAccumulator += athleteTrend;
                trendCount++;
            }
        }

        List<AlertDTO> coachAlerts = alertService.getAlertsByCoach(coachId);
        int pendingAlerts = (int) coachAlerts.stream()
                .filter(alert -> alert.getStatus() == AlertStatus.ACTIVE || alert.getStatus() == AlertStatus.ACKNOWLEDGED)
                .count();

        DashboardDTO dashboard = DashboardDTO.builder()
                .userId(coachId)
                .role(Role.COACH.name())
                .assignedAthletes(athleteIds.size())
                .activeMesocycles(activeMesocycles)
                .athletesWithHighFatigue(athletesWithHighFatigue)
                .pendingAlerts(pendingAlerts)
                .weeklySessions(weeklySessions)
                .performanceTrend(trendCount == 0 ? 0.0d : round(trendAccumulator / trendCount))
                .generatedAt(LocalDateTime.now())
                .build();

        LOGGER.info("Dashboard generated role=COACH userId={}", coachId);
        return dashboard;
    }

    @Override
    @Cacheable(value = DASHBOARD_CACHE, key = "'NUTRITIONIST:' + #nutritionistId")
    public DashboardDTO getNutritionistDashboard(String nutritionistId) {
        User currentUser = getAuthenticatedUser();
        validateIdentifier(nutritionistId, "Nutritionist id is required.");
        ensureOwnRoleDashboard(currentUser, Role.NUTRITIONIST, nutritionistId);

        List<NutritionPlanSummaryDTO> plans = nutritionPlanService.getNutritionPlansByNutritionist(nutritionistId);
        Set<String> athleteIds = new HashSet<>();
        int activePlans = 0;
        int expiredPlans = 0;
        for (NutritionPlanSummaryDTO plan : plans) {
            athleteIds.add(plan.getAthleteId());
            if (Boolean.TRUE.equals(plan.getActive())) {
                activePlans++;
            }
            if (plan.getEndDate() != null && plan.getEndDate().isBefore(LocalDate.now())) {
                expiredPlans++;
            }
        }

        int nutritionAlerts = 0;
        for (String athleteId : athleteIds) {
            nutritionAlerts += alertService.getAlertsByAthlete(athleteId).stream()
                    .filter(alert -> TYPE_NUTRITION_PLAN_EXPIRED.equals(alert.getType()))
                    .count();
        }

        DashboardDTO dashboard = DashboardDTO.builder()
                .userId(nutritionistId)
                .role(Role.NUTRITIONIST.name())
                .assignedAthletes(athleteIds.size())
                .activeNutritionPlans(activePlans)
                .expiredPlans(expiredPlans)
                .nutritionAlerts(nutritionAlerts)
                .generatedAt(LocalDateTime.now())
                .build();

        LOGGER.info("Dashboard generated role=NUTRITIONIST userId={}", nutritionistId);
        return dashboard;
    }

    @Override
    @CacheEvict(value = DASHBOARD_CACHE, allEntries = true)
    public DashboardDTO refreshDashboard(String userId) {
        validateIdentifier(userId, "User id is required.");
        User currentUser = getAuthenticatedUser();
        if (!Objects.equals(currentUser.getId().toHexString(), userId)) {
            throw new UnauthorizedOperationException("Users may only refresh their own dashboard.");
        }

        LOGGER.info("Dashboard refreshed userId={}", userId);
        return getDashboardSummary(userId);
    }

    @Override
    @Cacheable(value = DASHBOARD_CACHE, key = "'SUMMARY:' + #userId")
    public DashboardDTO getDashboardSummary(String userId) {
        validateIdentifier(userId, "User id is required.");
        User currentUser = getAuthenticatedUser();
        if (!Objects.equals(currentUser.getId().toHexString(), userId)) {
            throw new UnauthorizedOperationException("Users may only access their own dashboard summary.");
        }

        return switch (currentUser.getRole()) {
            case ATHLETE -> getAthleteDashboard(userId);
            case COACH -> getCoachDashboard(userId);
            case NUTRITIONIST -> getNutritionistDashboard(userId);
        };
    }

    private Double resolveCurrentOneRepMax(String athleteId, List<WorkoutSessionSummaryDTO> sessions) {
        boolean hasValidOneRepMax = sessions.stream()
                .anyMatch(session -> session.getEstimatedOneRepMax() != null && session.getEstimatedOneRepMax() > 0);
        if (!hasValidOneRepMax) {
            return 0.0d;
        }
        return oneRepMaxService.getCurrentEstimatedOneRepMax(athleteId);
    }

    private LocalDate extractLastWorkoutDate(List<WorkoutSessionSummaryDTO> sessions) {
        return sessions.stream()
                .map(WorkoutSessionSummaryDTO::getDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    private Double sumTrainingVolume(List<WorkoutSessionSummaryDTO> sessions) {
        return round(sessions.stream()
                .map(WorkoutSessionSummaryDTO::getTotalVolume)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum());
    }

    private Double sumWeeklyTrainingVolume(List<WorkoutSessionSummaryDTO> sessions) {
        LocalDate weekStart = LocalDate.now().minusDays(6);
        return round(sessions.stream()
                .filter(session -> session.getDate() != null && !session.getDate().isBefore(weekStart))
                .map(WorkoutSessionSummaryDTO::getTotalVolume)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum());
    }

    private Double calculateAthletePerformanceTrend(List<WorkoutSessionSummaryDTO> sessions) {
        List<WorkoutSessionSummaryDTO> withOneRepMax = sessions.stream()
                .filter(session -> session.getEstimatedOneRepMax() != null && session.getEstimatedOneRepMax() > 0)
                .sorted((left, right) -> left.getDate().compareTo(right.getDate()))
                .toList();
        if (withOneRepMax.size() < 2) {
            return null;
        }
        double first = withOneRepMax.get(0).getEstimatedOneRepMax();
        double latest = withOneRepMax.get(withOneRepMax.size() - 1).getEstimatedOneRepMax();
        if (first == 0) {
            return null;
        }
        return ((latest - first) / first) * 100.0d;
    }

    private User getAuthenticatedUser() {
        return authenticatedUserProvider.getAuthenticatedUser();
    }

    private User findUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private void ensureOwnRoleDashboard(User currentUser, Role role, String requestedUserId) {
        if (currentUser.getRole() != role) {
            throw new UnauthorizedOperationException("User role is not allowed to access this dashboard.");
        }
        if (!Objects.equals(currentUser.getId().toHexString(), requestedUserId)) {
            throw new UnauthorizedOperationException("Users may only access their own dashboard.");
        }
    }

    private void validateIdentifier(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }
}
