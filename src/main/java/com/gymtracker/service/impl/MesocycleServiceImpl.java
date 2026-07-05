package com.gymtracker.service.impl;

import com.gymtracker.dto.mesocycle.MesocycleDetailDTO;
import com.gymtracker.dto.mesocycle.MesocycleRequestDTO;
import com.gymtracker.dto.mesocycle.MesocycleResponseDTO;
import com.gymtracker.dto.mesocycle.MesocycleSummaryDTO;
import com.gymtracker.entity.Exercise;
import com.gymtracker.entity.Mesocycle;
import com.gymtracker.entity.User;
import com.gymtracker.enums.ExerciseStatus;
import com.gymtracker.enums.MesocycleStatus;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.BusinessRuleException;
import com.gymtracker.exception.DuplicateResourceException;
import com.gymtracker.exception.ResourceNotFoundException;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.mapper.MesocycleMapper;
import com.gymtracker.repository.ExerciseRepository;
import com.gymtracker.repository.MesocycleRepository;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.security.AuthenticatedUserProvider;
import com.gymtracker.service.AthleteAssignmentService;
import com.gymtracker.service.MesocycleService;
import com.gymtracker.validation.MesocycleValidator;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

/**
 * Business implementation for mesocycle lifecycle management.
 */
@Service
public class MesocycleServiceImpl implements MesocycleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MesocycleServiceImpl.class);

    private final MesocycleRepository mesocycleRepository;
    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final MesocycleMapper mesocycleMapper;
    private final MesocycleValidator mesocycleValidator;
    private final MongoOperations mongoTemplate;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final AthleteAssignmentService athleteAssignmentService;

    public MesocycleServiceImpl(
            MesocycleRepository mesocycleRepository,
            UserRepository userRepository,
            ExerciseRepository exerciseRepository,
            MesocycleMapper mesocycleMapper,
            MesocycleValidator mesocycleValidator,
            MongoOperations mongoTemplate,
            AuthenticatedUserProvider authenticatedUserProvider,
            AthleteAssignmentService athleteAssignmentService
    ) {
        this.mesocycleRepository = mesocycleRepository;
        this.userRepository = userRepository;
        this.exerciseRepository = exerciseRepository;
        this.mesocycleMapper = mesocycleMapper;
        this.mesocycleValidator = mesocycleValidator;
        this.mongoTemplate = mongoTemplate;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.athleteAssignmentService = athleteAssignmentService;
    }

    @Override
    @CacheEvict(value = "dashboards", allEntries = true)
    public MesocycleResponseDTO createMesocycle(MesocycleRequestDTO requestDTO) {
        User currentUser = getAuthenticatedUser();
        ensureCoachCanWrite(currentUser);
        validateMesocycleRequestForCreate(requestDTO, currentUser);

        User coach = getCoachById(requestDTO.getCoachId());
        User athlete = getAthleteById(requestDTO.getAthleteId());
        ensureAuthenticatedCoachMatchesRequest(currentUser, coach.getId());
        validateReferencedExercisesAreActive(requestDTO);

        boolean duplicate = mesocycleRepository.findByCoachId(coach.getId()).stream()
                .anyMatch(mesocycle -> Objects.equals(mesocycle.getAthleteId(), athlete.getId())
                        && equalsIgnoreCase(mesocycle.getName(), requestDTO.getName())
                        && mesocycle.getStatus() != MesocycleStatus.ARCHIVED);
        if (duplicate) {
            throw new DuplicateResourceException("A mesocycle with the same name already exists for this athlete.");
        }

        if (requestDTO.getStatus() == MesocycleStatus.ACTIVE) {
            archiveOtherActiveMesocycles(athlete.getId(), null);
        }

        Mesocycle mesocycle = mesocycleMapper.toEntity(requestDTO);
        mesocycle.setCoachId(coach.getId());
        mesocycle.setAthleteId(athlete.getId());
        mesocycle.setCreatedAt(LocalDateTime.now());

        Mesocycle savedMesocycle = mesocycleRepository.save(mesocycle);
        LOGGER.info("Mesocycle created id={}, coachId={}, athleteId={}",
                savedMesocycle.getId(), savedMesocycle.getCoachId(), savedMesocycle.getAthleteId());
        return mesocycleMapper.toResponseDTO(savedMesocycle);
    }

    @Override
    @CacheEvict(value = "dashboards", allEntries = true)
    public MesocycleResponseDTO updateMesocycle(String mesocycleId, MesocycleRequestDTO requestDTO) {
        User currentUser = getAuthenticatedUser();
        ensureCoachCanWrite(currentUser);
        validateMesocycleRequestForUpdate(mesocycleId, requestDTO, currentUser);

        Mesocycle existingMesocycle = findMesocycleById(mesocycleId);
        ensureCoachOwnsMesocycle(currentUser, existingMesocycle);

        if (existingMesocycle.getStatus() == MesocycleStatus.ARCHIVED) {
            throw new BusinessRuleException("Archived mesocycles cannot be modified.");
        }

        User coach = getCoachById(requestDTO.getCoachId());
        User athlete = getAthleteById(requestDTO.getAthleteId());
        ensureAuthenticatedCoachMatchesRequest(currentUser, coach.getId());
        validateReferencedExercisesAreActive(requestDTO);

        if (requestDTO.getStatus() == MesocycleStatus.ACTIVE) {
            archiveOtherActiveMesocycles(athlete.getId(), existingMesocycle.getId());
        }

        mesocycleMapper.updateEntityFromRequest(requestDTO, existingMesocycle);
        existingMesocycle.setCoachId(coach.getId());
        existingMesocycle.setAthleteId(athlete.getId());

        Mesocycle savedMesocycle = mesocycleRepository.save(existingMesocycle);
        LOGGER.info("Mesocycle updated id={}, coachId={}, athleteId={}",
                savedMesocycle.getId(), savedMesocycle.getCoachId(), savedMesocycle.getAthleteId());
        return mesocycleMapper.toResponseDTO(savedMesocycle);
    }

    @Override
    @CacheEvict(value = "dashboards", allEntries = true)
    public MesocycleResponseDTO activateMesocycle(String mesocycleId) {
        User currentUser = getAuthenticatedUser();
        ensureCoachCanWrite(currentUser);
        mesocycleValidator.validateDelete(mesocycleId);

        Mesocycle mesocycle = findMesocycleById(mesocycleId);
        ensureCoachOwnsMesocycle(currentUser, mesocycle);

        if (mesocycle.getStatus() == MesocycleStatus.ARCHIVED) {
            throw new BusinessRuleException("Archived mesocycles cannot be activated.");
        }

        archiveOtherActiveMesocycles(mesocycle.getAthleteId(), mesocycle.getId());
        mesocycle.setStatus(MesocycleStatus.ACTIVE);
        Mesocycle savedMesocycle = mesocycleRepository.save(mesocycle);
        LOGGER.info("Mesocycle activated id={}, athleteId={}", savedMesocycle.getId(), savedMesocycle.getAthleteId());
        return mesocycleMapper.toResponseDTO(savedMesocycle);
    }

    @Override
    @CacheEvict(value = "dashboards", allEntries = true)
    public MesocycleResponseDTO archiveMesocycle(String mesocycleId) {
        User currentUser = getAuthenticatedUser();
        ensureCoachCanWrite(currentUser);
        mesocycleValidator.validateDelete(mesocycleId);

        Mesocycle mesocycle = findMesocycleById(mesocycleId);
        ensureCoachOwnsMesocycle(currentUser, mesocycle);

        if (mesocycle.getStatus() == MesocycleStatus.ARCHIVED) {
            throw new BusinessRuleException("Mesocycle is already archived.");
        }

        mesocycle.setStatus(MesocycleStatus.ARCHIVED);
        Mesocycle savedMesocycle = mesocycleRepository.save(mesocycle);
        LOGGER.info("Mesocycle archived id={}, athleteId={}", savedMesocycle.getId(), savedMesocycle.getAthleteId());
        return mesocycleMapper.toResponseDTO(savedMesocycle);
    }

    @Override
    public MesocycleDetailDTO getMesocycleById(String mesocycleId) {
        mesocycleValidator.validateDelete(mesocycleId);
        User currentUser = getAuthenticatedUser();
        Mesocycle mesocycle = findMesocycleById(mesocycleId);
        ensureReadPermission(currentUser, mesocycle);

        MesocycleDetailDTO detail = mesocycleMapper.toDetailDTO(mesocycle);
        detail.setCoachName(resolveUserFullName(mesocycle.getCoachId()));
        detail.setAthleteName(resolveUserFullName(mesocycle.getAthleteId()));
        return detail;
    }

    @Override
    public MesocycleResponseDTO getActiveMesocycle(String athleteId) {
        validateIdentifier(athleteId, "Athlete id is required.");
        User currentUser = getAuthenticatedUser();
        User athlete = getAthleteById(athleteId);

        Mesocycle activeMesocycle = mesocycleRepository.findByAthleteId(athlete.getId()).stream()
                .filter(mesocycle -> mesocycle.getStatus() == MesocycleStatus.ACTIVE)
                .max(Comparator.comparing(Mesocycle::getCreatedAt))
                .orElseThrow(() -> new ResourceNotFoundException("Active mesocycle not found for athlete: " + athleteId));

        ensureReadPermission(currentUser, activeMesocycle);
        return mesocycleMapper.toResponseDTO(activeMesocycle);
    }

    @Override
    public List<MesocycleSummaryDTO> getMesocyclesByCoach(String coachId) {
        validateIdentifier(coachId, "Coach id is required.");
        User currentUser = getAuthenticatedUser();
        User coach = getCoachById(coachId);
        ensureReadPermissionByCoach(currentUser, coach.getId());

        List<Mesocycle> mesocycles = mesocycleRepository.findByCoachId(coach.getId()).stream()
                .sorted(Comparator.comparing(Mesocycle::getCreatedAt).reversed())
                .toList();
        return toEnrichedSummaries(mesocycles);
    }

    @Override
    public List<MesocycleSummaryDTO> getMesocyclesByAthlete(String athleteId) {
        validateIdentifier(athleteId, "Athlete id is required.");
        User currentUser = getAuthenticatedUser();
        User athlete = getAthleteById(athleteId);

        List<Mesocycle> mesocycles = mesocycleRepository.findByAthleteId(athlete.getId()).stream()
                .filter(mesocycle -> canReadMesocycle(currentUser, mesocycle))
                .sorted(Comparator.comparing(Mesocycle::getCreatedAt).reversed())
                .toList();
        return toEnrichedSummaries(mesocycles);
    }

    @Override
    public List<MesocycleSummaryDTO> searchMesocycles(String keyword) {
        User currentUser = getAuthenticatedUser();
        validateIdentifier(keyword, "Search keyword is required.");
        Pattern keywordPattern = Pattern.compile(Pattern.quote(keyword.trim()), Pattern.CASE_INSENSITIVE);

        Criteria keywordCriteria = new Criteria().orOperator(
                Criteria.where("name").regex(keywordPattern),
                Criteria.where("notes").regex(keywordPattern),
                Criteria.where("status").regex(keywordPattern),
                Criteria.where("coachIdStr").regex(keywordPattern),
                Criteria.where("athleteIdStr").regex(keywordPattern)
        );

        Criteria scopeCriteria = switch (currentUser.getRole()) {
            case ATHLETE -> Criteria.where("athleteId").is(currentUser.getId());
            case COACH -> Criteria.where("coachId").is(currentUser.getId());
            case NUTRITIONIST -> Criteria.where("athleteId")
                    .in(athleteAssignmentService.assignedAthleteIdsForNutritionist(currentUser.getId()));
        };
        Criteria criteria = new Criteria().andOperator(keywordCriteria, scopeCriteria);

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.addFields()
                        .addField("coachIdStr").withValue(ConvertOperators.ToString.toString("$coachId"))
                        .addField("athleteIdStr").withValue(ConvertOperators.ToString.toString("$athleteId"))
                        .build(),
                Aggregation.match(criteria)
        );

        List<Mesocycle> mesocycles = mongoTemplate.aggregate(aggregation, "mesocycles", Mesocycle.class)
                .getMappedResults().stream()
                .sorted(Comparator.comparing(Mesocycle::getCreatedAt).reversed())
                .toList();
        return toEnrichedSummaries(mesocycles);
    }

    @Override
    public List<MesocycleSummaryDTO> getMesocyclesForCurrentUser() {
        User currentUser = getAuthenticatedUser();
        String currentUserId = currentUser.getId().toHexString();

        return switch (currentUser.getRole()) {
            case ATHLETE -> getMesocyclesByAthlete(currentUserId);
            case COACH -> getMesocyclesByCoach(currentUserId);
            case NUTRITIONIST -> {
                Set<ObjectId> assignedAthleteIds =
                        athleteAssignmentService.assignedAthleteIdsForNutritionist(currentUser.getId());
                List<Mesocycle> mesocycles = assignedAthleteIds.isEmpty()
                        ? List.of()
                        : mesocycleRepository.findAll().stream()
                        .filter(mesocycle -> assignedAthleteIds.contains(mesocycle.getAthleteId()))
                        .sorted(Comparator.comparing(Mesocycle::getCreatedAt).reversed())
                        .toList();
                yield toEnrichedSummaries(mesocycles);
            }
        };
    }

    /**
     * Builds summaries for a list of mesocycles, resolving every referenced
     * coach/athlete name with a single batched {@code findAllById} call
     * instead of two lookups per mesocycle.
     */
    private List<MesocycleSummaryDTO> toEnrichedSummaries(List<Mesocycle> mesocycles) {
        Set<String> userIds = new HashSet<>();
        for (Mesocycle mesocycle : mesocycles) {
            if (mesocycle.getCoachId() != null) {
                userIds.add(mesocycle.getCoachId().toHexString());
            }
            if (mesocycle.getAthleteId() != null) {
                userIds.add(mesocycle.getAthleteId().toHexString());
            }
        }
        Map<String, String> namesById = resolveUserFullNames(userIds);

        return mesocycles.stream()
                .map(mesocycle -> {
                    MesocycleSummaryDTO summary = mesocycleMapper.toSummaryDTO(mesocycle);
                    summary.setCoachName(mesocycle.getCoachId() != null ? namesById.get(mesocycle.getCoachId().toHexString()) : null);
                    summary.setAthleteName(mesocycle.getAthleteId() != null ? namesById.get(mesocycle.getAthleteId().toHexString()) : null);
                    return summary;
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

    private String resolveUserFullName(ObjectId userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId.toHexString())
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .orElse(null);
    }

    @Override
    @CacheEvict(value = "dashboards", allEntries = true)
    public MesocycleResponseDTO duplicateMesocycle(String mesocycleId) {
        User currentUser = getAuthenticatedUser();
        ensureCoachCanWrite(currentUser);
        mesocycleValidator.validateDelete(mesocycleId);

        Mesocycle original = findMesocycleById(mesocycleId);
        ensureCoachOwnsMesocycle(currentUser, original);

        Mesocycle duplicate = Mesocycle.builder()
                .coachId(original.getCoachId())
                .athleteId(original.getAthleteId())
                .name(original.getName() + " (Copy)")
                .durationWeeks(original.getDurationWeeks())
                .targetRPE(original.getTargetRPE())
                .notes(original.getNotes())
                .status(MesocycleStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .days(copyWorkoutDays(original.getDays()))
                .build();

        Mesocycle savedMesocycle = mesocycleRepository.save(duplicate);
        LOGGER.info("Mesocycle duplicated originalId={}, duplicatedId={}", original.getId(), savedMesocycle.getId());
        return mesocycleMapper.toResponseDTO(savedMesocycle);
    }

    private void validateMesocycleRequestForCreate(MesocycleRequestDTO requestDTO, User currentUser) {
        try {
            mesocycleValidator.validateCreate(requestDTO);
            mesocycleValidator.validateAssignment(requestDTO.getCoachId(), requestDTO.getAthleteId());
        } catch (ValidationException exception) {
            LOGGER.warn("Mesocycle validation failure on create by email={}: {}", currentUser.getEmail(), exception.getMessage());
            throw exception;
        }
    }

    private void validateMesocycleRequestForUpdate(String mesocycleId, MesocycleRequestDTO requestDTO, User currentUser) {
        try {
            mesocycleValidator.validateDelete(mesocycleId);
            mesocycleValidator.validateUpdate(requestDTO);
            mesocycleValidator.validateAssignment(requestDTO.getCoachId(), requestDTO.getAthleteId());
        } catch (ValidationException exception) {
            LOGGER.warn("Mesocycle validation failure on update by email={}: {}", currentUser.getEmail(), exception.getMessage());
            throw exception;
        }
    }

    private void validateReferencedExercisesAreActive(MesocycleRequestDTO requestDTO) {
        requestDTO.getDays().forEach(day -> day.getExercises().forEach(requestExercise -> {
            Exercise exercise = exerciseRepository.findById(requestExercise.getExerciseId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Exercise not found with id: " + requestExercise.getExerciseId()));
            if (exercise.getStatus() != ExerciseStatus.ACTIVE) {
                throw new BusinessRuleException("Referenced exercise must be ACTIVE. Exercise id: " + requestExercise.getExerciseId());
            }
        }));
    }

    private void archiveOtherActiveMesocycles(ObjectId athleteId, ObjectId exceptMesocycleId) {
        List<Mesocycle> activeMesocycles = mesocycleRepository.findByAthleteId(athleteId).stream()
                .filter(mesocycle -> mesocycle.getStatus() == MesocycleStatus.ACTIVE)
                .toList();

        for (Mesocycle activeMesocycle : activeMesocycles) {
            if (exceptMesocycleId == null || !Objects.equals(activeMesocycle.getId(), exceptMesocycleId)) {
                activeMesocycle.setStatus(MesocycleStatus.ARCHIVED);
                mesocycleRepository.save(activeMesocycle);
            }
        }
    }

    private Mesocycle findMesocycleById(String mesocycleId) {
        return mesocycleRepository.findById(mesocycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Mesocycle not found with id: " + mesocycleId));
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

    private void ensureCoachCanWrite(User currentUser) {
        if (currentUser.getRole() != Role.COACH) {
            LOGGER.warn("Unauthorized mesocycle write attempt by email={}", currentUser.getEmail());
            throw new UnauthorizedOperationException("Only coaches may create or modify mesocycles.");
        }
    }

    private void ensureCoachOwnsMesocycle(User currentUser, Mesocycle mesocycle) {
        if (!Objects.equals(currentUser.getId(), mesocycle.getCoachId())) {
            LOGGER.warn("Unauthorized mesocycle write attempt by email={}, mesocycleId={}",
                    currentUser.getEmail(), mesocycle.getId());
            throw new UnauthorizedOperationException("Coaches may only modify their own mesocycles.");
        }
    }

    private void ensureReadPermission(User currentUser, Mesocycle mesocycle) {
        if (!canReadMesocycle(currentUser, mesocycle)) {
            LOGGER.warn("Unauthorized mesocycle read attempt by email={}, mesocycleId={}",
                    currentUser.getEmail(), mesocycle.getId());
            throw new UnauthorizedOperationException("User does not have permission to view this mesocycle.");
        }
    }

    private void ensureReadPermissionByCoach(User currentUser, ObjectId coachId) {
        if (currentUser.getRole() == Role.ATHLETE) {
            LOGGER.warn("Unauthorized mesocycle coach-list attempt by athlete email={}", currentUser.getEmail());
            throw new UnauthorizedOperationException("Athletes cannot list mesocycles by coach.");
        }
        if (currentUser.getRole() == Role.COACH && !Objects.equals(currentUser.getId(), coachId)) {
            LOGGER.warn("Unauthorized mesocycle coach-list attempt by coach email={}", currentUser.getEmail());
            throw new UnauthorizedOperationException("Coaches can only list their own mesocycles.");
        }
        if (currentUser.getRole() == Role.NUTRITIONIST) {
            LOGGER.warn("Unauthorized mesocycle coach-list attempt by nutritionist email={}", currentUser.getEmail());
            throw new UnauthorizedOperationException("Nutritionists cannot list mesocycles by coach.");
        }
    }

    private void ensureAuthenticatedCoachMatchesRequest(User currentUser, ObjectId requestCoachId) {
        if (!Objects.equals(currentUser.getId(), requestCoachId)) {
            LOGGER.warn("Unauthorized coach mismatch. Authenticated email={}, requestCoachId={}",
                    currentUser.getEmail(), requestCoachId);
            throw new UnauthorizedOperationException("Coaches may only create or update their own mesocycles.");
        }
    }

    /**
     * A coach may only read mesocycles they own; a nutritionist may only read
     * mesocycles for athletes actually assigned to them (has a nutrition plan
     * for that athlete) - matching the assignment check already enforced by
     * ReportServiceImpl. Broad "any coach/nutritionist can read" access was a
     * broken-access-control gap, not an intended design.
     */
    private boolean canReadMesocycle(User user, Mesocycle mesocycle) {
        return switch (user.getRole()) {
            case ATHLETE -> Objects.equals(user.getId(), mesocycle.getAthleteId());
            case COACH -> Objects.equals(user.getId(), mesocycle.getCoachId());
            case NUTRITIONIST -> athleteAssignmentService.isAthleteAssignedToNutritionist(
                    user.getId(), mesocycle.getAthleteId());
        };
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private List<Mesocycle.WorkoutDay> copyWorkoutDays(List<Mesocycle.WorkoutDay> sourceDays) {
        return sourceDays.stream()
                .map(day -> Mesocycle.WorkoutDay.builder()
                        .dayName(day.getDayName())
                        .exercises(day.getExercises().stream()
                                .map(exercise -> Mesocycle.WorkoutExercise.builder()
                                        .exerciseId(exercise.getExerciseId())
                                        .sets(exercise.getSets())
                                        .repetitions(exercise.getRepetitions())
                                        .targetWeight(exercise.getTargetWeight())
                                        .targetRPE(exercise.getTargetRPE())
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    private void validateIdentifier(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
    }
}
