package com.gymtracker.service.impl;

import com.gymtracker.dto.workout.WorkoutSessionDetailDTO;
import com.gymtracker.dto.workout.WorkoutSessionRequestDTO;
import com.gymtracker.dto.workout.WorkoutSessionResponseDTO;
import com.gymtracker.dto.workout.WorkoutSessionSummaryDTO;
import com.gymtracker.entity.Mesocycle;
import com.gymtracker.entity.Session;
import com.gymtracker.entity.User;
import com.gymtracker.enums.Role;
import com.gymtracker.enums.WorkoutStatus;
import com.gymtracker.exception.BusinessRuleException;
import com.gymtracker.exception.ResourceNotFoundException;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.mapper.WorkoutSessionMapper;
import com.gymtracker.repository.MesocycleRepository;
import com.gymtracker.repository.SessionRepository;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.security.AuthenticatedUserProvider;
import com.gymtracker.service.AlertService;
import com.gymtracker.service.AthleteAssignmentService;
import com.gymtracker.service.FatigueService;
import com.gymtracker.service.OneRepMaxService;
import com.gymtracker.service.WorkoutSessionService;
import com.gymtracker.validation.WorkoutValidator;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/**
 * Business implementation for workout session registration and history.
 */
@Service
public class WorkoutSessionServiceImpl implements WorkoutSessionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkoutSessionServiceImpl.class);

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final MesocycleRepository mesocycleRepository;
    private final WorkoutSessionMapper workoutSessionMapper;
    private final WorkoutValidator workoutValidator;
    private final OneRepMaxService oneRepMaxService;
    private final FatigueService fatigueService;
    private final AlertService alertService;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final AthleteAssignmentService athleteAssignmentService;

    public WorkoutSessionServiceImpl(
            SessionRepository sessionRepository,
            UserRepository userRepository,
            MesocycleRepository mesocycleRepository,
            WorkoutSessionMapper workoutSessionMapper,
            WorkoutValidator workoutValidator,
            OneRepMaxService oneRepMaxService,
            FatigueService fatigueService,
            AlertService alertService,
            AuthenticatedUserProvider authenticatedUserProvider,
            AthleteAssignmentService athleteAssignmentService
    ) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.mesocycleRepository = mesocycleRepository;
        this.workoutSessionMapper = workoutSessionMapper;
        this.workoutValidator = workoutValidator;
        this.oneRepMaxService = oneRepMaxService;
        this.fatigueService = fatigueService;
        this.alertService = alertService;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.athleteAssignmentService = athleteAssignmentService;
    }

    @Override
    @CacheEvict(value = "dashboards", allEntries = true)
    public WorkoutSessionResponseDTO createWorkoutSession(WorkoutSessionRequestDTO requestDTO) {
        User currentUser = getAuthenticatedUser();
        ensureAthleteCanRegister(currentUser, requestDTO != null ? requestDTO.getAthleteId() : null);

        try {
            workoutValidator.validateCreate(requestDTO);
        } catch (ValidationException exception) {
            LOGGER.warn("Workout validation failure on create by email={}: {}", currentUser.getEmail(), exception.getMessage());
            throw exception;
        }

        User athlete = getAthleteById(requestDTO.getAthleteId());
        Mesocycle mesocycle = findMesocycleById(requestDTO.getMesocycleId());
        ensureMesocycleBelongsToAthlete(mesocycle, athlete);
        ensureExercisesBelongToMesocycle(requestDTO, mesocycle);

        Session session = workoutSessionMapper.toEntity(requestDTO);
        session.setAthleteId(athlete.getId());
        session.setMesocycleId(mesocycle.getId());
        session.setTotalVolume(calculateTotalVolume(requestDTO));
        session.setEstimatedOneRepMax(calculateEstimatedOneRepMax(requestDTO));

        Session savedSession = sessionRepository.save(session);
        oneRepMaxService.calculateOneRepMax(athlete.getId().toHexString());
        fatigueService.calculateFatigue(athlete.getId().toHexString());
        alertService.generateFatigueAlert(athlete.getId().toHexString());
        alertService.generateMissedWorkoutAlert(athlete.getId().toHexString());
        alertService.generateNutritionPlanExpiredAlert(athlete.getId().toHexString());
        alertService.generateMesocycleCompletedAlert(athlete.getId().toHexString());
        alertService.generatePerformanceDropAlert(athlete.getId().toHexString());
        LOGGER.info("Workout session created id={}, athleteId={}, mesocycleId={}",
                savedSession.getId(), savedSession.getAthleteId(), savedSession.getMesocycleId());
        return toResponseWithDerivedFields(savedSession);
    }

    @Override
    public WorkoutSessionDetailDTO getWorkoutSessionById(String sessionId) {
        workoutValidator.validateDelete(sessionId);
        User currentUser = getAuthenticatedUser();

        Session session = findSessionById(sessionId);
        ensureReadPermission(currentUser, session.getAthleteId());
        return toDetailWithDerivedFields(session);
    }

    @Override
    public List<WorkoutSessionSummaryDTO> getWorkoutSessionsByAthlete(String athleteId) {
        validateIdentifier(athleteId, "Athlete id is required.");
        User currentUser = getAuthenticatedUser();
        User athlete = getAthleteById(athleteId);
        ensureReadPermission(currentUser, athlete.getId());

        return sessionRepository.findByAthleteId(athlete.getId()).stream()
                .sorted(Comparator.comparing(Session::getDate).reversed())
                .map(this::toSummaryWithDerivedFields)
                .toList();
    }

    @Override
    public List<WorkoutSessionSummaryDTO> getWorkoutSessionsByMesocycle(String mesocycleId) {
        validateIdentifier(mesocycleId, "Mesocycle id is required.");
        User currentUser = getAuthenticatedUser();

        Mesocycle mesocycle = findMesocycleById(mesocycleId);
        ensureReadPermission(currentUser, mesocycle.getAthleteId());

        return sessionRepository.findByMesocycleId(mesocycle.getId()).stream()
                .filter(session -> canReadSession(currentUser, session))
                .sorted(Comparator.comparing(Session::getDate).reversed())
                .map(this::toSummaryWithDerivedFields)
                .toList();
    }

    @Override
    public List<WorkoutSessionSummaryDTO> getWorkoutSessionsByDateRange(LocalDate startDate, LocalDate endDate) {
        User currentUser = getAuthenticatedUser();
        validateDateRange(startDate, endDate);

        return sessionRepository.findByDateBetween(startDate, endDate).stream()
                .filter(session -> canReadSession(currentUser, session))
                .sorted(Comparator.comparing(Session::getDate).reversed())
                .map(this::toSummaryWithDerivedFields)
                .toList();
    }

    private Session findSessionById(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found with id: " + sessionId));
    }

    private Mesocycle findMesocycleById(String mesocycleId) {
        return mesocycleRepository.findById(mesocycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Mesocycle not found with id: " + mesocycleId));
    }

    private User getAthleteById(String athleteId) {
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

    private void ensureAthleteCanRegister(User currentUser, String athleteId) {
        if (currentUser.getRole() != Role.ATHLETE) {
            LOGGER.warn("Unauthorized workout creation attempt by email={}", currentUser.getEmail());
            throw new UnauthorizedOperationException("Only athletes may register workout sessions.");
        }
        if (!Objects.equals(currentUser.getId().toHexString(), athleteId)) {
            LOGGER.warn("Unauthorized workout creation for different athlete by email={}", currentUser.getEmail());
            throw new UnauthorizedOperationException("Athletes may only register their own workout sessions.");
        }
    }

    /**
     * A coach may only read workout sessions for athletes actually assigned
     * to them (has a mesocycle for that athlete); a nutritionist may only
     * read sessions for athletes assigned to them (has a nutrition plan for
     * that athlete) - matching the assignment check already enforced by
     * ReportServiceImpl. Broad "any coach/nutritionist can read" access was
     * a broken-access-control gap, not an intended design.
     */
    private void ensureReadPermission(User currentUser, ObjectId athleteId) {
        if (!canReadPermission(currentUser, athleteId)) {
            LOGGER.warn("Unauthorized workout read attempt by email={}", currentUser.getEmail());
            throw new UnauthorizedOperationException("User does not have permission to view these workout sessions.");
        }
    }

    private boolean canReadPermission(User currentUser, ObjectId athleteId) {
        return switch (currentUser.getRole()) {
            case ATHLETE -> Objects.equals(currentUser.getId(), athleteId);
            case COACH -> athleteAssignmentService.isAthleteAssignedToCoach(currentUser.getId(), athleteId);
            case NUTRITIONIST -> athleteAssignmentService.isAthleteAssignedToNutritionist(currentUser.getId(), athleteId);
        };
    }

    private boolean canReadSession(User currentUser, Session session) {
        return canReadPermission(currentUser, session.getAthleteId());
    }

    private void ensureMesocycleBelongsToAthlete(Mesocycle mesocycle, User athlete) {
        if (!Objects.equals(mesocycle.getAthleteId(), athlete.getId())) {
            throw new BusinessRuleException("Mesocycle is not assigned to the given athlete.");
        }
    }

    private void ensureExercisesBelongToMesocycle(WorkoutSessionRequestDTO requestDTO, Mesocycle mesocycle) {
        List<ObjectId> mesocycleExerciseIds = mesocycle.getDays().stream()
                .flatMap(day -> day.getExercises().stream())
                .map(Mesocycle.WorkoutExercise::getExerciseId)
                .distinct()
                .toList();

        requestDTO.getCompletedExercises().forEach(exercise -> {
            ObjectId exerciseId = toObjectId(exercise.getExerciseId(), "Completed exercise id is invalid.");
            if (!mesocycleExerciseIds.contains(exerciseId)) {
                throw new BusinessRuleException("Completed exercise is not part of the assigned mesocycle: "
                        + exercise.getExerciseId());
            }
        });
    }

    private void validateIdentifier(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start date and end date are required.");
        }
        if (endDate.isBefore(startDate)) {
            throw new ValidationException("End date must be greater than or equal to start date.");
        }
    }

    private ObjectId toObjectId(String value, String message) {
        try {
            return new ObjectId(value);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(message);
        }
    }

    private Double calculateTotalVolume(WorkoutSessionRequestDTO requestDTO) {
        return requestDTO.getCompletedExercises().stream()
                .flatMap(exercise -> exercise.getSets().stream())
                .mapToDouble(set -> set.getWeight() * set.getRepetitions())
                .sum();
    }

    private Double calculateEstimatedOneRepMax(WorkoutSessionRequestDTO requestDTO) {
        return requestDTO.getCompletedExercises().stream()
                .flatMap(exercise -> exercise.getSets().stream())
                .mapToDouble(set -> oneRepMaxService.estimateOneRepMax(set.getWeight(), set.getRepetitions()))
                .max()
                .orElse(0.0d);
    }

    private Double calculateFatigueScore(Session session) {
        return session.getCompletedExercises().stream()
                .flatMap(exercise -> exercise.getSets().stream())
                .mapToDouble(Session.CompletedSet::getRpe)
                .average()
                .orElse(0.0d);
    }

    private WorkoutSessionResponseDTO toResponseWithDerivedFields(Session session) {
        WorkoutSessionResponseDTO responseDTO = workoutSessionMapper.toResponseDTO(session);
        responseDTO.setStatus(WorkoutStatus.COMPLETED);
        responseDTO.setFatigueScore(calculateFatigueScore(session));
        return responseDTO;
    }

    private WorkoutSessionDetailDTO toDetailWithDerivedFields(Session session) {
        WorkoutSessionDetailDTO detailDTO = workoutSessionMapper.toDetailDTO(session);
        detailDTO.setStatus(WorkoutStatus.COMPLETED);
        detailDTO.setFatigueScore(calculateFatigueScore(session));
        return detailDTO;
    }

    private WorkoutSessionSummaryDTO toSummaryWithDerivedFields(Session session) {
        WorkoutSessionSummaryDTO summaryDTO = workoutSessionMapper.toSummaryDTO(session);
        summaryDTO.setStatus(WorkoutStatus.COMPLETED);
        return summaryDTO;
    }
}
