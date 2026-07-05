package com.gymtracker.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.gymtracker.dto.workout.WorkoutExerciseDTO;
import com.gymtracker.dto.workout.WorkoutSessionRequestDTO;
import com.gymtracker.dto.workout.WorkoutSessionResponseDTO;
import com.gymtracker.dto.workout.WorkoutSessionSummaryDTO;
import com.gymtracker.dto.workout.WorkoutSetDTO;
import com.gymtracker.entity.Mesocycle;
import com.gymtracker.entity.Session;
import com.gymtracker.entity.User;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.BusinessRuleException;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.mapper.ObjectIdMapperImpl;
import com.gymtracker.mapper.WorkoutSessionMapper;
import com.gymtracker.mapper.WorkoutSessionMapperImpl;
import com.gymtracker.repository.MesocycleRepository;
import com.gymtracker.repository.NutritionPlanRepository;
import com.gymtracker.repository.SessionRepository;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.security.AuthenticatedUserProvider;
import com.gymtracker.service.AlertService;
import com.gymtracker.service.AthleteAssignmentService;
import com.gymtracker.service.FatigueService;
import com.gymtracker.service.OneRepMaxService;
import com.gymtracker.validation.WorkoutValidator;
import jakarta.validation.Validation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
 * Unit tests for WorkoutSessionServiceImpl, using the real (generated)
 * WorkoutSessionMapper and WorkoutValidator against mocked repositories and
 * collaborating services.
 */
@ExtendWith(MockitoExtension.class)
class WorkoutSessionServiceImplTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MesocycleRepository mesocycleRepository;
    @Mock
    private NutritionPlanRepository nutritionPlanRepository;
    @Mock
    private OneRepMaxService oneRepMaxService;
    @Mock
    private FatigueService fatigueService;
    @Mock
    private AlertService alertService;

    private final WorkoutSessionMapper workoutSessionMapper = new WorkoutSessionMapperImpl();
    private final WorkoutValidator workoutValidator =
            new WorkoutValidator(Validation.buildDefaultValidatorFactory().getValidator());

    private WorkoutSessionServiceImpl workoutSessionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(workoutSessionMapper, "objectIdMapper", new ObjectIdMapperImpl());
        AuthenticatedUserProvider authenticatedUserProvider = new AuthenticatedUserProvider(userRepository);
        AthleteAssignmentService athleteAssignmentService =
                new AthleteAssignmentServiceImpl(mesocycleRepository, nutritionPlanRepository);
        workoutSessionService = new WorkoutSessionServiceImpl(sessionRepository, userRepository, mesocycleRepository,
                workoutSessionMapper, workoutValidator, oneRepMaxService, fatigueService, alertService,
                authenticatedUserProvider, athleteAssignmentService);
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
                .password("encoded")
                .firstName("First")
                .lastName("Last")
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

    private Mesocycle buildMesocycle(User athlete, ObjectId exerciseId) {
        Mesocycle.WorkoutExercise exercise = Mesocycle.WorkoutExercise.builder()
                .exerciseId(exerciseId).sets(3).repetitions(10).targetWeight(50.0).targetRPE(7).build();
        Mesocycle.WorkoutDay day = Mesocycle.WorkoutDay.builder().dayName("Day 1").exercises(List.of(exercise)).build();
        return Mesocycle.builder()
                .id(new ObjectId())
                .athleteId(athlete.getId())
                .coachId(new ObjectId())
                .days(List.of(day))
                .build();
    }

    private Session buildSession(ObjectId athleteId, ObjectId mesocycleId) {
        Session.CompletedSet set = Session.CompletedSet.builder().weight(60.0).repetitions(8).rpe(7).build();
        Session.CompletedExercise exercise = Session.CompletedExercise.builder()
                .exerciseId(new ObjectId()).sets(List.of(set)).build();
        return Session.builder()
                .id(new ObjectId())
                .athleteId(athleteId)
                .mesocycleId(mesocycleId)
                .date(LocalDate.now())
                .durationMinutes(60)
                .completedExercises(List.of(exercise))
                .totalVolume(480.0)
                .estimatedOneRepMax(75.0)
                .build();
    }

    @Test
    void createWorkoutSessionSucceedsForOwnAthlete() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(athlete);

        ObjectId exerciseId = new ObjectId();
        Mesocycle mesocycle = buildMesocycle(athlete, exerciseId);
        WorkoutSetDTO set = WorkoutSetDTO.builder().weight(60.0).repetitions(8).rpe(7).build();
        WorkoutExerciseDTO exerciseDTO = WorkoutExerciseDTO.builder().exerciseId(exerciseId.toHexString()).sets(List.of(set)).build();
        WorkoutSessionRequestDTO requestDTO = WorkoutSessionRequestDTO.builder()
                .athleteId(athlete.getId().toHexString())
                .mesocycleId(mesocycle.getId().toHexString())
                .date(LocalDate.now())
                .durationMinutes(60)
                .completedExercises(List.of(exerciseDTO))
                .build();

        when(mesocycleRepository.findById(mesocycle.getId().toHexString())).thenReturn(Optional.of(mesocycle));
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));
        when(oneRepMaxService.estimateOneRepMax(60.0, 8)).thenReturn(75.0);
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(oneRepMaxService.calculateOneRepMax(anyString())).thenReturn(CompletableFuture.completedFuture(List.of()));
        lenient().when(fatigueService.calculateFatigue(anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        lenient().when(alertService.generateFatigueAlert(anyString())).thenReturn(CompletableFuture.completedFuture(null));
        lenient().when(alertService.generateMissedWorkoutAlert(anyString())).thenReturn(CompletableFuture.completedFuture(null));
        lenient().when(alertService.generateNutritionPlanExpiredAlert(anyString())).thenReturn(CompletableFuture.completedFuture(null));
        lenient().when(alertService.generateMesocycleCompletedAlert(anyString())).thenReturn(CompletableFuture.completedFuture(null));
        lenient().when(alertService.generatePerformanceDropAlert(anyString())).thenReturn(CompletableFuture.completedFuture(null));

        WorkoutSessionResponseDTO response = workoutSessionService.createWorkoutSession(requestDTO);

        assertThat(response.getTotalVolume()).isEqualTo(480.0);
        assertThat(response.getStatus().name()).isEqualTo("COMPLETED");
    }

    @Test
    void createWorkoutSessionRejectsNonAthleteCaller() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        authenticateAs(coach);

        WorkoutSessionRequestDTO requestDTO = WorkoutSessionRequestDTO.builder()
                .athleteId(coach.getId().toHexString())
                .mesocycleId(new ObjectId().toHexString())
                .date(LocalDate.now())
                .durationMinutes(60)
                .completedExercises(List.of())
                .build();

        assertThatThrownBy(() -> workoutSessionService.createWorkoutSession(requestDTO))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void createWorkoutSessionRejectsRegisteringForAnotherAthlete() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        User otherAthlete = buildUser(Role.ATHLETE, "other-athlete@example.com");
        authenticateAs(athlete);

        WorkoutSessionRequestDTO requestDTO = WorkoutSessionRequestDTO.builder()
                .athleteId(otherAthlete.getId().toHexString())
                .mesocycleId(new ObjectId().toHexString())
                .date(LocalDate.now())
                .durationMinutes(60)
                .completedExercises(List.of())
                .build();

        assertThatThrownBy(() -> workoutSessionService.createWorkoutSession(requestDTO))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void createWorkoutSessionRejectsExerciseNotInMesocycle() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(athlete);

        Mesocycle mesocycle = buildMesocycle(athlete, new ObjectId());
        WorkoutSetDTO set = WorkoutSetDTO.builder().weight(60.0).repetitions(8).rpe(7).build();
        WorkoutExerciseDTO exerciseDTO = WorkoutExerciseDTO.builder()
                .exerciseId(new ObjectId().toHexString()).sets(List.of(set)).build();
        WorkoutSessionRequestDTO requestDTO = WorkoutSessionRequestDTO.builder()
                .athleteId(athlete.getId().toHexString())
                .mesocycleId(mesocycle.getId().toHexString())
                .date(LocalDate.now())
                .durationMinutes(60)
                .completedExercises(List.of(exerciseDTO))
                .build();

        when(mesocycleRepository.findById(mesocycle.getId().toHexString())).thenReturn(Optional.of(mesocycle));
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));

        assertThatThrownBy(() -> workoutSessionService.createWorkoutSession(requestDTO))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void getWorkoutSessionByIdRejectsAthleteViewingAnotherAthletesSession() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        User otherAthlete = buildUser(Role.ATHLETE, "other-athlete@example.com");
        authenticateAs(otherAthlete);

        Session session = buildSession(athlete.getId(), new ObjectId());
        when(sessionRepository.findById(session.getId().toHexString())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> workoutSessionService.getWorkoutSessionById(session.getId().toHexString()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void getWorkoutSessionsByAthleteReturnsSortedSummaries() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(athlete);
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));
        when(sessionRepository.findByAthleteId(athlete.getId()))
                .thenReturn(List.of(buildSession(athlete.getId(), new ObjectId())));

        List<WorkoutSessionSummaryDTO> results = workoutSessionService.getWorkoutSessionsByAthlete(athlete.getId().toHexString());

        assertThat(results).hasSize(1);
    }

    @Test
    void getWorkoutSessionsByDateRangeRejectsInvertedRange() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        authenticateAs(coach);

        LocalDate start = LocalDate.now();
        LocalDate end = start.minusDays(1);

        assertThatThrownBy(() -> workoutSessionService.getWorkoutSessionsByDateRange(start, end))
                .isInstanceOf(ValidationException.class);
    }
}
