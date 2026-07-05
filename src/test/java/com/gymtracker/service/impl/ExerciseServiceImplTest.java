package com.gymtracker.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.mapper.ExerciseMapper;
import com.gymtracker.mapper.ExerciseMapperImpl;
import com.gymtracker.mapper.ObjectIdMapperImpl;
import com.gymtracker.repository.ExerciseRepository;
import com.gymtracker.repository.MesocycleRepository;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.security.AuthenticatedUserProvider;
import com.gymtracker.validation.ExerciseValidator;
import jakarta.validation.Validation;
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
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for ExerciseServiceImpl, using the real (generated)
 * ExerciseMapper and ExerciseValidator against mocked repositories.
 */
@ExtendWith(MockitoExtension.class)
class ExerciseServiceImplTest {

    @Mock
    private ExerciseRepository exerciseRepository;
    @Mock
    private MesocycleRepository mesocycleRepository;
    @Mock
    private MongoOperations mongoTemplate;
    @Mock
    private UserRepository userRepository;

    private final ExerciseMapper exerciseMapper = new ExerciseMapperImpl();
    private final ExerciseValidator exerciseValidator =
            new ExerciseValidator(Validation.buildDefaultValidatorFactory().getValidator());

    private ExerciseServiceImpl exerciseService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(exerciseMapper, "objectIdMapper", new ObjectIdMapperImpl());
        AuthenticatedUserProvider authenticatedUserProvider = new AuthenticatedUserProvider(userRepository);
        exerciseService = new ExerciseServiceImpl(exerciseRepository, mesocycleRepository, exerciseMapper, exerciseValidator,
                mongoTemplate, authenticatedUserProvider);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsCoach() {
        TestingAuthenticationToken token = new TestingAuthenticationToken("coach@example.com", null, "ROLE_COACH");
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    private void authenticateAsAthlete() {
        TestingAuthenticationToken token = new TestingAuthenticationToken("athlete@example.com", null, "ROLE_ATHLETE");
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    private Exercise buildExercise(String name) {
        return Exercise.builder()
                .id(new ObjectId())
                .name(name)
                .primaryMuscle("Chest")
                .secondaryMuscles(List.of("Triceps"))
                .exerciseType(ExerciseType.STRENGTH)
                .difficulty(Difficulty.INTERMEDIATE)
                .equipment(Equipment.BARBELL)
                .status(ExerciseStatus.ACTIVE)
                .description("Description")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private ExerciseRequestDTO buildRequest(String name) {
        return ExerciseRequestDTO.builder()
                .name(name)
                .primaryMuscle("Chest")
                .secondaryMuscles(List.of("Triceps"))
                .exerciseType(ExerciseType.STRENGTH)
                .difficulty(Difficulty.INTERMEDIATE)
                .equipment(Equipment.BARBELL)
                .description("Description")
                .build();
    }

    @Test
    void createExerciseSucceedsForCoach() {
        authenticateAsCoach();
        when(mongoTemplate.exists(any(Query.class), eq(Exercise.class))).thenReturn(false);
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExerciseResponseDTO response = exerciseService.createExercise(buildRequest("Bench Press"));

        assertThat(response.getName()).isEqualTo("Bench Press");
        assertThat(response.getStatus()).isEqualTo(ExerciseStatus.ACTIVE);
    }

    @Test
    void createExerciseRejectsNonCoachCaller() {
        authenticateAsAthlete();

        assertThatThrownBy(() -> exerciseService.createExercise(buildRequest("Bench Press")))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void createExerciseRejectsDuplicateName() {
        authenticateAsCoach();
        when(mongoTemplate.exists(any(Query.class), eq(Exercise.class))).thenReturn(true);

        assertThatThrownBy(() -> exerciseService.createExercise(buildRequest("Bench Press")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deactivateExerciseSucceedsWhenNotUsedByActiveMesocycle() {
        authenticateAsCoach();
        Exercise exercise = buildExercise("Squat");
        when(exerciseRepository.findById(exercise.getId().toHexString())).thenReturn(Optional.of(exercise));
        when(mesocycleRepository.findByStatus(MesocycleStatus.ACTIVE)).thenReturn(List.of());
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExerciseResponseDTO response = exerciseService.deactivateExercise(exercise.getId().toHexString());

        assertThat(response.getStatus()).isEqualTo(ExerciseStatus.INACTIVE);
    }

    @Test
    void deactivateExerciseRejectsWhenUsedByActiveMesocycle() {
        authenticateAsCoach();
        Exercise exercise = buildExercise("Squat");
        when(exerciseRepository.findById(exercise.getId().toHexString())).thenReturn(Optional.of(exercise));

        Mesocycle.WorkoutExercise workoutExercise = Mesocycle.WorkoutExercise.builder()
                .exerciseId(exercise.getId()).sets(3).repetitions(10).targetWeight(50.0).targetRPE(7).build();
        Mesocycle.WorkoutDay day = Mesocycle.WorkoutDay.builder().dayName("Day 1").exercises(List.of(workoutExercise)).build();
        Mesocycle activeMesocycle = Mesocycle.builder().days(List.of(day)).build();
        when(mesocycleRepository.findByStatus(MesocycleStatus.ACTIVE)).thenReturn(List.of(activeMesocycle));

        assertThatThrownBy(() -> exerciseService.deactivateExercise(exercise.getId().toHexString()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void deactivateExerciseRejectsAlreadyInactiveExercise() {
        authenticateAsCoach();
        Exercise exercise = buildExercise("Squat");
        exercise.setStatus(ExerciseStatus.INACTIVE);
        when(exerciseRepository.findById(exercise.getId().toHexString())).thenReturn(Optional.of(exercise));

        assertThatThrownBy(() -> exerciseService.deactivateExercise(exercise.getId().toHexString()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void getAllExercisesReturnsSummaries() {
        authenticateAsAthlete();
        when(exerciseRepository.findAll()).thenReturn(List.of(buildExercise("Bench Press"), buildExercise("Squat")));

        List<ExerciseSummaryDTO> summaries = exerciseService.getAllExercises();

        assertThat(summaries).hasSize(2);
    }

    @Test
    void searchExercisesMatchesByKeyword() {
        authenticateAsAthlete();
        when(mongoTemplate.find(any(Query.class), eq(Exercise.class))).thenReturn(List.of(buildExercise("Bench Press")));

        List<ExerciseSummaryDTO> results = exerciseService.searchExercises("bench");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Bench Press");
    }

    @Test
    void filterExercisesAppliesAllCriteria() {
        authenticateAsAthlete();
        Exercise matching = buildExercise("Bench Press");
        when(mongoTemplate.find(any(Query.class), eq(Exercise.class))).thenReturn(List.of(matching));

        List<ExerciseSummaryDTO> results = exerciseService.filterExercises(
                ExerciseType.STRENGTH, Difficulty.INTERMEDIATE, Equipment.BARBELL, ExerciseStatus.ACTIVE);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Bench Press");
    }

    @Test
    void existsByNameDelegatesToRepository() {
        authenticateAsAthlete();
        when(mongoTemplate.exists(any(Query.class), eq(Exercise.class))).thenReturn(true, false);

        assertThat(exerciseService.existsByName("Bench Press")).isTrue();
        assertThat(exerciseService.existsByName("Nonexistent")).isFalse();
    }
}
