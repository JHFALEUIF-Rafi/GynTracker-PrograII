package com.gymtracker.service.impl;

import com.gymtracker.dto.exercise.ExerciseDetailDTO;
import com.gymtracker.dto.exercise.ExerciseRequestDTO;
import com.gymtracker.dto.exercise.ExerciseResponseDTO;
import com.gymtracker.dto.exercise.ExerciseSummaryDTO;
import com.gymtracker.entity.Exercise;
import com.gymtracker.entity.Mesocycle;
import com.gymtracker.enums.Difficulty;
import com.gymtracker.enums.Equipment;
import com.gymtracker.enums.ExerciseStatus;
import com.gymtracker.enums.ExerciseType;
import com.gymtracker.enums.MesocycleStatus;
import com.gymtracker.exception.BusinessRuleException;
import com.gymtracker.exception.DuplicateResourceException;
import com.gymtracker.exception.ResourceNotFoundException;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.mapper.ExerciseMapper;
import com.gymtracker.repository.ExerciseRepository;
import com.gymtracker.repository.MesocycleRepository;
import com.gymtracker.security.AuthenticatedUserProvider;
import com.gymtracker.service.ExerciseService;
import com.gymtracker.validation.ExerciseValidator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Business implementation for exercise catalog management.
 */
@Service
public class ExerciseServiceImpl implements ExerciseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExerciseServiceImpl.class);
    private static final String ROLE_COACH = "ROLE_COACH";

    private final ExerciseRepository exerciseRepository;
    private final MesocycleRepository mesocycleRepository;
    private final ExerciseMapper exerciseMapper;
    private final ExerciseValidator exerciseValidator;
    private final MongoOperations mongoTemplate;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public ExerciseServiceImpl(
            ExerciseRepository exerciseRepository,
            MesocycleRepository mesocycleRepository,
            ExerciseMapper exerciseMapper,
            ExerciseValidator exerciseValidator,
            MongoOperations mongoTemplate,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.exerciseRepository = exerciseRepository;
        this.mesocycleRepository = mesocycleRepository;
        this.exerciseMapper = exerciseMapper;
        this.exerciseValidator = exerciseValidator;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public ExerciseResponseDTO createExercise(ExerciseRequestDTO requestDTO) {
        AuthenticatedUser currentUser = getAuthenticatedUser();
        ensureCoachWritePermission(currentUser);

        try {
            exerciseValidator.validateCreate(requestDTO);
        } catch (ValidationException exception) {
            LOGGER.warn("Exercise validation failure on create by email={}: {}", currentUser.email(), exception.getMessage());
            throw exception;
        }

        boolean duplicatedNameOnCreate = existsByNameIgnoringCase(requestDTO.getName(), null);
        try {
            exerciseValidator.validateUniqueName(duplicatedNameOnCreate);
        } catch (ValidationException exception) {
            throw new DuplicateResourceException(exception.getMessage());
        }

        Exercise exercise = exerciseMapper.toEntity(requestDTO);
        exercise.setStatus(requestDTO.getStatus() != null ? requestDTO.getStatus() : ExerciseStatus.ACTIVE);
        exercise.setCreatedAt(LocalDateTime.now());
        exercise.setUpdatedAt(LocalDateTime.now());

        Exercise savedExercise = exerciseRepository.save(exercise);
        LOGGER.info("Exercise created id={}, name={}", savedExercise.getId(), savedExercise.getName());
        return exerciseMapper.toResponseDTO(savedExercise);
    }

    @Override
    public ExerciseResponseDTO updateExercise(String exerciseId, ExerciseRequestDTO requestDTO) {
        AuthenticatedUser currentUser = getAuthenticatedUser();
        ensureCoachWritePermission(currentUser);
        exerciseValidator.validateDelete(exerciseId);

        try {
            exerciseValidator.validateUpdate(requestDTO);
        } catch (ValidationException exception) {
            LOGGER.warn("Exercise validation failure on update by email={}: {}", currentUser.email(), exception.getMessage());
            throw exception;
        }

        Exercise existingExercise = findExerciseById(exerciseId);
        boolean duplicatedNameOnUpdate = existsByNameIgnoringCase(requestDTO.getName(), existingExercise.getId());
        try {
            exerciseValidator.validateUniqueName(duplicatedNameOnUpdate);
        } catch (ValidationException exception) {
            throw new DuplicateResourceException(exception.getMessage());
        }

        exerciseMapper.updateEntityFromRequest(requestDTO, existingExercise);
        if (requestDTO.getStatus() == null) {
            existingExercise.setStatus(existingExercise.getStatus() != null ? existingExercise.getStatus() : ExerciseStatus.ACTIVE);
        }
        existingExercise.setUpdatedAt(LocalDateTime.now());

        Exercise savedExercise = exerciseRepository.save(existingExercise);
        LOGGER.info("Exercise updated id={}, name={}", savedExercise.getId(), savedExercise.getName());
        return exerciseMapper.toResponseDTO(savedExercise);
    }

    @Override
    public ExerciseResponseDTO deactivateExercise(String exerciseId) {
        AuthenticatedUser currentUser = getAuthenticatedUser();
        ensureCoachWritePermission(currentUser);
        exerciseValidator.validateDelete(exerciseId);

        Exercise exercise = findExerciseById(exerciseId);
        if (exercise.getStatus() == ExerciseStatus.INACTIVE) {
            throw new BusinessRuleException("Exercise is already inactive.");
        }

        if (isUsedByActiveMesocycle(exercise.getId())) {
            throw new BusinessRuleException("Exercise cannot be deactivated while used by an active mesocycle.");
        }

        exercise.setStatus(ExerciseStatus.INACTIVE);
        exercise.setUpdatedAt(LocalDateTime.now());
        Exercise savedExercise = exerciseRepository.save(exercise);

        LOGGER.info("Exercise deactivated id={}, name={}", savedExercise.getId(), savedExercise.getName());
        return exerciseMapper.toResponseDTO(savedExercise);
    }

    @Override
    public ExerciseDetailDTO getExerciseById(String exerciseId) {
        getAuthenticatedUser();
        exerciseValidator.validateDelete(exerciseId);
        return exerciseMapper.toDetailDTO(findExerciseById(exerciseId));
    }

    @Override
    public List<ExerciseSummaryDTO> getAllExercises() {
        getAuthenticatedUser();
        return exerciseRepository.findAll().stream()
                .map(exerciseMapper::toSummaryDTO)
                .toList();
    }

    @Override
    public List<ExerciseSummaryDTO> searchExercises(String keyword) {
        getAuthenticatedUser();
        validateIdentifier(keyword, "Search keyword is required.");
        Pattern keywordPattern = Pattern.compile(Pattern.quote(keyword.trim()), Pattern.CASE_INSENSITIVE);

        Criteria keywordCriteria = new Criteria().orOperator(
                Criteria.where("name").regex(keywordPattern),
                Criteria.where("primaryMuscle").regex(keywordPattern),
                Criteria.where("description").regex(keywordPattern),
                Criteria.where("exerciseType").regex(keywordPattern),
                Criteria.where("difficulty").regex(keywordPattern),
                Criteria.where("equipment").regex(keywordPattern),
                Criteria.where("status").regex(keywordPattern)
        );

        return mongoTemplate.find(new Query(keywordCriteria), Exercise.class).stream()
                .map(exerciseMapper::toSummaryDTO)
                .toList();
    }

    @Override
    public List<ExerciseSummaryDTO> filterExercises(
            ExerciseType type,
            Difficulty difficulty,
            Equipment equipment,
            ExerciseStatus status
    ) {
        getAuthenticatedUser();

        List<Criteria> filters = new ArrayList<>();
        if (type != null) {
            filters.add(Criteria.where("exerciseType").is(type));
        }
        if (difficulty != null) {
            filters.add(Criteria.where("difficulty").is(difficulty));
        }
        if (equipment != null) {
            filters.add(Criteria.where("equipment").is(equipment));
        }
        if (status != null) {
            filters.add(Criteria.where("status").is(status));
        }

        Query query = filters.isEmpty() ? new Query() : new Query(new Criteria().andOperator(filters));
        return mongoTemplate.find(query, Exercise.class).stream()
                .map(exerciseMapper::toSummaryDTO)
                .toList();
    }

    @Override
    public boolean existsByName(String name) {
        getAuthenticatedUser();
        validateIdentifier(name, "Exercise name is required.");
        return existsByNameIgnoringCase(name, null);
    }

    private Exercise findExerciseById(String exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise not found with id: " + exerciseId));
    }

    private boolean existsByNameIgnoringCase(String name, ObjectId ignoreId) {
        Pattern exactNamePattern = Pattern.compile("^" + Pattern.quote(name.trim()) + "$", Pattern.CASE_INSENSITIVE);
        Criteria criteria = Criteria.where("name").regex(exactNamePattern);
        if (ignoreId != null) {
            criteria = criteria.and("id").ne(ignoreId);
        }
        return mongoTemplate.exists(new Query(criteria), Exercise.class);
    }

    private boolean isUsedByActiveMesocycle(ObjectId exerciseId) {
        return mesocycleRepository.findByStatus(MesocycleStatus.ACTIVE).stream()
                .flatMap(mesocycle -> mesocycle.getDays().stream())
                .flatMap(day -> day.getExercises().stream())
                .map(Mesocycle.WorkoutExercise::getExerciseId)
                .anyMatch(referencedExerciseId -> Objects.equals(referencedExerciseId, exerciseId));
    }

    private AuthenticatedUser getAuthenticatedUser() {
        Authentication authentication = authenticatedUserProvider.requireAuthentication();
        String email = authenticatedUserProvider.extractEmail(authentication);
        boolean isCoach = authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLE_COACH.equals(authority.getAuthority()));
        return new AuthenticatedUser(email, isCoach);
    }

    private void ensureCoachWritePermission(AuthenticatedUser currentUser) {
        if (!currentUser.coach()) {
            LOGGER.warn("Unauthorized exercise write attempt by email={}", currentUser.email());
            throw new UnauthorizedOperationException("Only coaches may create, update or deactivate exercises.");
        }
    }

    private void validateIdentifier(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
    }

    private record AuthenticatedUser(String email, boolean coach) {
    }
}
