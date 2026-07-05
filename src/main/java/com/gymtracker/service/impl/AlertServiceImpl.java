package com.gymtracker.service.impl;

import com.gymtracker.dto.alert.AlertDTO;
import com.gymtracker.entity.Alert;
import com.gymtracker.entity.Mesocycle;
import com.gymtracker.entity.NutritionPlan;
import com.gymtracker.entity.Session;
import com.gymtracker.entity.User;
import com.gymtracker.enums.AlertStatus;
import com.gymtracker.enums.FatigueLevel;
import com.gymtracker.enums.MesocycleStatus;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.BusinessRuleException;
import com.gymtracker.exception.ResourceNotFoundException;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.mapper.AlertMapper;
import com.gymtracker.repository.AlertRepository;
import com.gymtracker.repository.MesocycleRepository;
import com.gymtracker.repository.NutritionPlanRepository;
import com.gymtracker.repository.SessionRepository;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.security.AuthenticatedUserProvider;
import com.gymtracker.service.AlertService;
import com.gymtracker.service.AthleteAssignmentService;
import com.gymtracker.service.FatigueService;
import com.gymtracker.service.OneRepMaxService;
import com.gymtracker.validation.AlertValidator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Business implementation for automatic alert generation and lifecycle management.
 */
@Service
public class AlertServiceImpl implements AlertService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertServiceImpl.class);

    private static final String TYPE_HIGH_FATIGUE = "HIGH_FATIGUE";
    private static final String TYPE_CRITICAL_FATIGUE = "CRITICAL_FATIGUE";
    private static final String TYPE_MISSED_WORKOUT = "MISSED_WORKOUT";
    private static final String TYPE_NUTRITION_PLAN_EXPIRED = "NUTRITION_PLAN_EXPIRED";
    private static final String TYPE_MESOCYCLE_COMPLETED = "MESOCYCLE_COMPLETED";
    private static final String TYPE_PERFORMANCE_DROP = "PERFORMANCE_DROP";
    private static final String TYPE_RECOVERY_RECOMMENDED = "RECOVERY_RECOMMENDED";
    private static final Set<String> NUTRITION_RELATED_TYPES = Set.of(TYPE_NUTRITION_PLAN_EXPIRED);

    private static final long MISSED_WORKOUT_DAYS_THRESHOLD = 3;
    private static final double PERFORMANCE_DROP_THRESHOLD_PERCENT = -5.0d;

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final SessionRepository workoutSessionRepository;
    private final MesocycleRepository mesocycleRepository;
    private final NutritionPlanRepository nutritionPlanRepository;
    private final FatigueService fatigueService;
    private final OneRepMaxService oneRepMaxService;
    private final AlertMapper alertMapper;
    private final AlertValidator alertValidator;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final AthleteAssignmentService athleteAssignmentService;

    public AlertServiceImpl(
            AlertRepository alertRepository,
            UserRepository userRepository,
            SessionRepository workoutSessionRepository,
            MesocycleRepository mesocycleRepository,
            NutritionPlanRepository nutritionPlanRepository,
            FatigueService fatigueService,
            OneRepMaxService oneRepMaxService,
            AlertMapper alertMapper,
            AlertValidator alertValidator,
            AuthenticatedUserProvider authenticatedUserProvider,
            AthleteAssignmentService athleteAssignmentService
    ) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.workoutSessionRepository = workoutSessionRepository;
        this.mesocycleRepository = mesocycleRepository;
        this.nutritionPlanRepository = nutritionPlanRepository;
        this.fatigueService = fatigueService;
        this.oneRepMaxService = oneRepMaxService;
        this.alertMapper = alertMapper;
        this.alertValidator = alertValidator;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.athleteAssignmentService = athleteAssignmentService;
    }

    @Override
    @Async
    @CacheEvict(value = "dashboards", allEntries = true, beforeInvocation = true)
    public CompletableFuture<AlertDTO> generateFatigueAlert(String athleteId) {
        try {
            User athlete = getAthleteById(athleteId);
            FatigueLevel fatigueLevel = fatigueService.getCurrentFatigueLevel(athleteId);

            if (fatigueLevel == FatigueLevel.CRITICAL) {
                AlertDTO alert = createAlertIfNotDuplicateActive(
                        athlete,
                        TYPE_CRITICAL_FATIGUE,
                        "Critical fatigue detected. Immediate recovery is recommended."
                );
                return CompletableFuture.completedFuture(alert);
            }

            if (fatigueLevel == FatigueLevel.HIGH) {
                AlertDTO alert = createAlertIfNotDuplicateActive(
                        athlete,
                        TYPE_HIGH_FATIGUE,
                        "High fatigue detected. Training load should be reviewed."
                );
                return CompletableFuture.completedFuture(alert);
            }

            return CompletableFuture.completedFuture(
                    createAlertIfNotDuplicateActive(
                            athlete,
                            TYPE_RECOVERY_RECOMMENDED,
                            "Recovery block recommended based on recent fatigue trend."
                    )
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("Alert generation failure type={} athleteId={}: {}",
                    TYPE_HIGH_FATIGUE, athleteId, exception.getMessage());
            return CompletableFuture.failedFuture(exception);
        }
    }

    @Override
    @Async
    @CacheEvict(value = "dashboards", allEntries = true, beforeInvocation = true)
    public CompletableFuture<AlertDTO> generateMissedWorkoutAlert(String athleteId) {
        try {
            User athlete = getAthleteById(athleteId);
            LocalDate lastWorkoutDate = workoutSessionRepository.findByAthleteId(athlete.getId()).stream()
                    .filter(this::isCompletedSession)
                    .map(Session::getDate)
                    .max(LocalDate::compareTo)
                    .orElse(null);

            if (lastWorkoutDate == null) {
                return CompletableFuture.completedFuture(
                        createAlertIfNotDuplicateActive(
                                athlete,
                                TYPE_MISSED_WORKOUT,
                                "No completed workouts registered recently."
                        )
                );
            }

            long daysWithoutWorkout = ChronoUnit.DAYS.between(lastWorkoutDate, LocalDate.now());
            if (daysWithoutWorkout >= MISSED_WORKOUT_DAYS_THRESHOLD) {
                return CompletableFuture.completedFuture(
                        createAlertIfNotDuplicateActive(
                                athlete,
                                TYPE_MISSED_WORKOUT,
                                "Athlete missed workouts for " + daysWithoutWorkout + " days."
                        )
                );
            }

            return CompletableFuture.completedFuture(null);
        } catch (RuntimeException exception) {
            LOGGER.warn("Alert generation failure type={} athleteId={}: {}",
                    TYPE_MISSED_WORKOUT, athleteId, exception.getMessage());
            return CompletableFuture.failedFuture(exception);
        }
    }

    @Override
    @Async
    @CacheEvict(value = "dashboards", allEntries = true, beforeInvocation = true)
    public CompletableFuture<AlertDTO> generateNutritionPlanExpiredAlert(String athleteId) {
        try {
            User athlete = getAthleteById(athleteId);
            NutritionPlan latestPlan = nutritionPlanRepository.findByAthleteId(athlete.getId()).stream()
                    .max(Comparator.comparing(NutritionPlan::getEndDate))
                    .orElse(null);

            if (latestPlan == null || latestPlan.getEndDate() == null) {
                return CompletableFuture.completedFuture(null);
            }

            if (latestPlan.getEndDate().isBefore(LocalDate.now())) {
                return CompletableFuture.completedFuture(
                        createAlertIfNotDuplicateActive(
                                athlete,
                                TYPE_NUTRITION_PLAN_EXPIRED,
                                "Nutrition plan expired on " + latestPlan.getEndDate() + "."
                        )
                );
            }
            return CompletableFuture.completedFuture(null);
        } catch (RuntimeException exception) {
            LOGGER.warn("Alert generation failure type={} athleteId={}: {}",
                    TYPE_NUTRITION_PLAN_EXPIRED, athleteId, exception.getMessage());
            return CompletableFuture.failedFuture(exception);
        }
    }

    @Override
    @Async
    @CacheEvict(value = "dashboards", allEntries = true, beforeInvocation = true)
    public CompletableFuture<AlertDTO> generateMesocycleCompletedAlert(String athleteId) {
        try {
            User athlete = getAthleteById(athleteId);
            Mesocycle latestMesocycle = mesocycleRepository.findByAthleteId(athlete.getId()).stream()
                    .max(Comparator.comparing(Mesocycle::getCreatedAt))
                    .orElse(null);

            if (latestMesocycle == null || latestMesocycle.getCreatedAt() == null || latestMesocycle.getDurationWeeks() == null) {
                return CompletableFuture.completedFuture(null);
            }

            LocalDate expectedEndDate = latestMesocycle.getCreatedAt().toLocalDate().plusWeeks(latestMesocycle.getDurationWeeks());
            if (LocalDate.now().isBefore(expectedEndDate)) {
                return CompletableFuture.completedFuture(null);
            }

            if (latestMesocycle.getStatus() == MesocycleStatus.COMPLETED || latestMesocycle.getStatus() == MesocycleStatus.ARCHIVED) {
                return CompletableFuture.completedFuture(
                        createAlertIfNotDuplicateActive(
                                athlete,
                                TYPE_MESOCYCLE_COMPLETED,
                                "Mesocycle completed. Coach review is required."
                        )
                );
            }

            return CompletableFuture.completedFuture(null);
        } catch (RuntimeException exception) {
            LOGGER.warn("Alert generation failure type={} athleteId={}: {}",
                    TYPE_MESOCYCLE_COMPLETED, athleteId, exception.getMessage());
            return CompletableFuture.failedFuture(exception);
        }
    }

    @Override
    @Async
    @CacheEvict(value = "dashboards", allEntries = true, beforeInvocation = true)
    public CompletableFuture<AlertDTO> generatePerformanceDropAlert(String athleteId) {
        try {
            User athlete = getAthleteById(athleteId);
            List<Session> sessions = workoutSessionRepository.findByAthleteId(athlete.getId()).stream()
                    .filter(this::isCompletedSession)
                    .sorted(Comparator.comparing(Session::getDate))
                    .toList();
            if (sessions.size() < 2) {
                return CompletableFuture.completedFuture(null);
            }

            double first = sessions.get(sessions.size() - 2).getEstimatedOneRepMax();
            double latest = oneRepMaxService.getCurrentEstimatedOneRepMax(athleteId);
            if (first <= 0) {
                return CompletableFuture.completedFuture(null);
            }

            double percentChange = ((latest - first) / first) * 100.0d;
            if (percentChange <= PERFORMANCE_DROP_THRESHOLD_PERCENT) {
                return CompletableFuture.completedFuture(
                        createAlertIfNotDuplicateActive(
                                athlete,
                                TYPE_PERFORMANCE_DROP,
                                "Performance drop detected (" + round(percentChange) + "%)."
                        )
                );
            }
            return CompletableFuture.completedFuture(null);
        } catch (RuntimeException exception) {
            LOGGER.warn("Alert generation failure type={} athleteId={}: {}",
                    TYPE_PERFORMANCE_DROP, athleteId, exception.getMessage());
            return CompletableFuture.failedFuture(exception);
        }
    }

    @Override
    public List<AlertDTO> getAlertsByAthlete(String athleteId) {
        validateIdentifier(athleteId, "Athlete id is required.");
        User currentUser = getAuthenticatedUser();
        User athlete = getAthleteById(athleteId);

        if (currentUser.getRole() == Role.ATHLETE && !Objects.equals(currentUser.getId(), athlete.getId())) {
            throw new UnauthorizedOperationException("Athletes may only view their own alerts.");
        }
        if (currentUser.getRole() == Role.COACH
                && !athleteAssignmentService.isAthleteAssignedToCoach(currentUser.getId(), athlete.getId())) {
            throw new UnauthorizedOperationException("Coaches may only view alerts for their assigned athletes.");
        }
        if (currentUser.getRole() == Role.NUTRITIONIST
                && !athleteAssignmentService.isAthleteAssignedToNutritionist(currentUser.getId(), athlete.getId())) {
            throw new UnauthorizedOperationException("Nutritionists may only view alerts for their assigned athletes.");
        }

        List<Alert> alerts = alertRepository.findByAthleteId(athlete.getId()).stream()
                .filter(alert -> currentUser.getRole() != Role.NUTRITIONIST || NUTRITION_RELATED_TYPES.contains(alert.getType()))
                .sorted(Comparator.comparing(Alert::getGeneratedAt).reversed())
                .toList();
        return toEnrichedDTOs(alerts);
    }

    @Override
    public List<AlertDTO> getAlertsByCoach(String coachId) {
        validateIdentifier(coachId, "Coach id is required.");
        User currentUser = getAuthenticatedUser();
        User coach = getCoachById(coachId);

        if (currentUser.getRole() == Role.COACH && !Objects.equals(currentUser.getId(), coach.getId())) {
            throw new UnauthorizedOperationException("Coaches may only view their own alerts.");
        }
        if (currentUser.getRole() == Role.ATHLETE) {
            throw new UnauthorizedOperationException("Athletes cannot view coach alerts.");
        }

        List<Alert> alerts = alertRepository.findByCoachId(coach.getId()).stream()
                .filter(alert -> currentUser.getRole() != Role.NUTRITIONIST
                        || (NUTRITION_RELATED_TYPES.contains(alert.getType())
                        && athleteAssignmentService.isAthleteAssignedToNutritionist(currentUser.getId(), alert.getAthleteId())))
                .sorted(Comparator.comparing(Alert::getGeneratedAt).reversed())
                .toList();
        return toEnrichedDTOs(alerts);
    }

    @Override
    public List<AlertDTO> getAlertsForCurrentUser() {
        User currentUser = getAuthenticatedUser();
        String currentUserId = currentUser.getId().toHexString();

        return switch (currentUser.getRole()) {
            case ATHLETE -> getAlertsByAthlete(currentUserId);
            case COACH -> getAlertsByCoach(currentUserId);
            case NUTRITIONIST -> {
                Set<ObjectId> assignedAthleteIds =
                        athleteAssignmentService.assignedAthleteIdsForNutritionist(currentUser.getId());
                List<Alert> alerts = assignedAthleteIds.isEmpty()
                        ? List.of()
                        : alertRepository.findByTypeIn(NUTRITION_RELATED_TYPES).stream()
                        .filter(alert -> assignedAthleteIds.contains(alert.getAthleteId()))
                        .sorted(Comparator.comparing(Alert::getGeneratedAt).reversed())
                        .toList();
                yield toEnrichedDTOs(alerts);
            }
        };
    }

    /**
     * Builds DTOs for a list of alerts, resolving every referenced
     * athlete/coach name with a single batched {@code findAllById} call
     * instead of two lookups per alert.
     */
    private List<AlertDTO> toEnrichedDTOs(List<Alert> alerts) {
        Set<String> userIds = new HashSet<>();
        for (Alert alert : alerts) {
            if (alert.getAthleteId() != null) {
                userIds.add(alert.getAthleteId().toHexString());
            }
            if (alert.getCoachId() != null) {
                userIds.add(alert.getCoachId().toHexString());
            }
        }
        Map<String, String> namesById = resolveUserFullNames(userIds);

        return alerts.stream()
                .map(alert -> {
                    AlertDTO dto = alertMapper.toResponseDTO(alert);
                    dto.setAthleteName(alert.getAthleteId() != null ? namesById.get(alert.getAthleteId().toHexString()) : null);
                    dto.setCoachName(alert.getCoachId() != null ? namesById.get(alert.getCoachId().toHexString()) : null);
                    return dto;
                })
                .toList();
    }

    private Map<String, String> resolveUserFullNames(Collection<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(user -> user.getId().toHexString(),
                        user -> user.getFirstName() + " " + user.getLastName()));
    }

    @Override
    @CacheEvict(value = "dashboards", allEntries = true)
    public AlertDTO acknowledgeAlert(String alertId) {
        User currentUser = getAuthenticatedUser();
        ensureCoachCanManageAlerts(currentUser);
        alertValidator.validateDelete(alertId);

        Alert alert = findAlertById(alertId);
        if (alert.getStatus() != AlertStatus.ACTIVE) {
            throw new BusinessRuleException("Only ACTIVE alerts can be acknowledged.");
        }
        if (!Objects.equals(alert.getCoachId(), currentUser.getId())) {
            throw new UnauthorizedOperationException("Coaches may only acknowledge their own alerts.");
        }

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setReviewedAt(LocalDateTime.now());
        Alert savedAlert = alertRepository.save(alert);
        LOGGER.info("Alert acknowledged id={}, type={}, coachId={}",
                savedAlert.getId(), savedAlert.getType(), savedAlert.getCoachId());
        return alertMapper.toResponseDTO(savedAlert);
    }

    @Override
    @CacheEvict(value = "dashboards", allEntries = true)
    public AlertDTO resolveAlert(String alertId) {
        User currentUser = getAuthenticatedUser();
        ensureCoachCanManageAlerts(currentUser);
        alertValidator.validateDelete(alertId);

        Alert alert = findAlertById(alertId);
        if (alert.getStatus() != AlertStatus.ACKNOWLEDGED) {
            throw new BusinessRuleException("Only ACKNOWLEDGED alerts can be resolved.");
        }
        if (!Objects.equals(alert.getCoachId(), currentUser.getId())) {
            throw new UnauthorizedOperationException("Coaches may only resolve their own alerts.");
        }

        alert.setStatus(AlertStatus.RESOLVED);
        alert.setReviewedAt(LocalDateTime.now());
        Alert savedAlert = alertRepository.save(alert);
        LOGGER.info("Alert resolved id={}, type={}, coachId={}",
                savedAlert.getId(), savedAlert.getType(), savedAlert.getCoachId());
        return alertMapper.toResponseDTO(savedAlert);
    }

    @Override
    public int deleteResolvedAlerts() {
        throw new BusinessRuleException("Resolved alerts must remain in history and cannot be deleted.");
    }

    private AlertDTO createAlertIfNotDuplicateActive(User athlete, String type, String message) {
        List<Alert> existingActiveAlerts = alertRepository.findByAthleteIdAndTypeAndStatus(
                athlete.getId(),
                type,
                AlertStatus.ACTIVE
        );
        if (!existingActiveAlerts.isEmpty()) {
            LOGGER.info("Duplicate alert prevented athleteId={}, type={}", athlete.getId(), type);
            return null;
        }

        ObjectId coachId = resolveCoachIdForAthlete(athlete.getId());
        AlertDTO draftAlert = AlertDTO.builder()
                .athleteId(athlete.getId().toHexString())
                .coachId(coachId.toHexString())
                .type(type)
                .message(message)
                .status(AlertStatus.ACTIVE)
                .generatedAt(LocalDateTime.now())
                .build();
        try {
            alertValidator.validateCreate(draftAlert);
        } catch (ValidationException exception) {
            LOGGER.warn("Alert generation failure type={} athleteId={}: {}", type, athlete.getId(), exception.getMessage());
            throw exception;
        }

        Alert alertEntity = alertMapper.toEntity(draftAlert);
        alertEntity.setAthleteId(athlete.getId());
        alertEntity.setCoachId(coachId);
        alertEntity.setGeneratedAt(LocalDateTime.now());
        alertEntity.setStatus(AlertStatus.ACTIVE);

        Alert savedAlert = alertRepository.save(alertEntity);
        LOGGER.info("Alert generated id={}, athleteId={}, coachId={}, type={}",
                savedAlert.getId(), savedAlert.getAthleteId(), savedAlert.getCoachId(), savedAlert.getType());
        return alertMapper.toResponseDTO(savedAlert);
    }

    private ObjectId resolveCoachIdForAthlete(ObjectId athleteId) {
        Mesocycle latestMesocycle = mesocycleRepository.findByAthleteId(athleteId).stream()
                .max(Comparator.comparing(Mesocycle::getCreatedAt))
                .orElseThrow(() -> new ResourceNotFoundException("No coach assignment found for athlete alerts."));
        return latestMesocycle.getCoachId();
    }

    private boolean isCompletedSession(Session session) {
        return session != null
                && session.getCompletedExercises() != null
                && !session.getCompletedExercises().isEmpty();
    }

    private Alert findAlertById(String alertId) {
        return alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + alertId));
    }

    private User getCoachById(String coachId) {
        User coach = userRepository.findById(coachId)
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found with id: " + coachId));
        if (coach.getRole() != Role.COACH) {
            throw new BusinessRuleException("Referenced user is not a coach.");
        }
        return coach;
    }

    private User getAthleteById(String athleteId) {
        validateIdentifier(athleteId, "Athlete id is required.");
        User athlete = userRepository.findById(athleteId)
                .orElseThrow(() -> new ResourceNotFoundException("Athlete not found with id: " + athleteId));
        if (athlete.getRole() != Role.ATHLETE) {
            throw new BusinessRuleException("Referenced user is not an athlete.");
        }
        return athlete;
    }

    private User getAuthenticatedUser() {
        return authenticatedUserProvider.getAuthenticatedUser();
    }

    private void ensureCoachCanManageAlerts(User currentUser) {
        if (currentUser.getRole() != Role.COACH) {
            throw new UnauthorizedOperationException("Only coaches may acknowledge or resolve alerts.");
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
