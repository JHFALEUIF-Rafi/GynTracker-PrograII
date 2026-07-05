package com.gymtracker.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.gymtracker.dto.mesocycle.MesocycleDetailDTO;
import com.gymtracker.dto.mesocycle.MesocycleRequestDTO;
import com.gymtracker.dto.mesocycle.MesocycleResponseDTO;
import com.gymtracker.dto.mesocycle.MesocycleSummaryDTO;
import com.gymtracker.dto.mesocycle.MesocycleWorkoutDayDTO;
import com.gymtracker.dto.mesocycle.MesocycleWorkoutExerciseDTO;
import com.gymtracker.entity.Exercise;
import com.gymtracker.entity.Mesocycle;
import com.gymtracker.entity.User;
import com.gymtracker.enums.ExerciseStatus;
import com.gymtracker.enums.MesocycleStatus;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.BusinessRuleException;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.mapper.MesocycleMapper;
import com.gymtracker.mapper.MesocycleMapperImpl;
import com.gymtracker.mapper.ObjectIdMapperImpl;
import com.gymtracker.repository.ExerciseRepository;
import com.gymtracker.repository.MesocycleRepository;
import com.gymtracker.repository.NutritionPlanRepository;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.security.AuthenticatedUserProvider;
import com.gymtracker.service.AthleteAssignmentService;
import com.gymtracker.validation.MesocycleValidator;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for MesocycleServiceImpl, using the real (generated)
 * MesocycleMapper and MesocycleValidator against mocked repositories.
 */
@ExtendWith(MockitoExtension.class)
class MesocycleServiceImplTest {

    @Mock
    private MesocycleRepository mesocycleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ExerciseRepository exerciseRepository;
    @Mock
    private NutritionPlanRepository nutritionPlanRepository;
    @Mock
    private MongoOperations mongoTemplate;

    private final MesocycleMapper mesocycleMapper = new MesocycleMapperImpl();
    private final MesocycleValidator mesocycleValidator =
            new MesocycleValidator(Validation.buildDefaultValidatorFactory().getValidator());

    private MesocycleServiceImpl mesocycleService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mesocycleMapper, "objectIdMapper", new ObjectIdMapperImpl());
        AuthenticatedUserProvider authenticatedUserProvider = new AuthenticatedUserProvider(userRepository);
        AthleteAssignmentService athleteAssignmentService =
                new AthleteAssignmentServiceImpl(mesocycleRepository, nutritionPlanRepository);
        mesocycleService = new MesocycleServiceImpl(
                mesocycleRepository, userRepository, exerciseRepository,
                mesocycleMapper, mesocycleValidator, mongoTemplate,
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

    private MesocycleRequestDTO buildRequest(User coach, User athlete, ObjectId exerciseId, MesocycleStatus status) {
        MesocycleWorkoutExerciseDTO exerciseDTO = MesocycleWorkoutExerciseDTO.builder()
                .exerciseId(exerciseId.toHexString()).sets(3).repetitions(10).targetWeight(50.0).targetRpe(7).build();
        MesocycleWorkoutDayDTO dayDTO = MesocycleWorkoutDayDTO.builder().dayName("Day 1").exercises(List.of(exerciseDTO)).build();

        return MesocycleRequestDTO.builder()
                .coachId(coach.getId().toHexString())
                .athleteId(athlete.getId().toHexString())
                .name("Hypertrophy Block")
                .durationWeeks(8)
                .targetRpe(7)
                .status(status)
                .days(List.of(dayDTO))
                .build();
    }

    private Mesocycle buildMesocycleEntity(User coach, User athlete, MesocycleStatus status) {
        Mesocycle.WorkoutExercise exercise = Mesocycle.WorkoutExercise.builder()
                .exerciseId(new ObjectId()).sets(3).repetitions(10).targetWeight(50.0).targetRPE(7).build();
        Mesocycle.WorkoutDay day = Mesocycle.WorkoutDay.builder().dayName("Day 1").exercises(List.of(exercise)).build();

        return Mesocycle.builder()
                .id(new ObjectId())
                .coachId(coach.getId())
                .athleteId(athlete.getId())
                .name("Hypertrophy Block")
                .durationWeeks(8)
                .targetRPE(7)
                .status(status)
                .createdAt(LocalDateTime.now())
                .days(List.of(day))
                .build();
    }

    @Test
    void createMesocycleSucceedsForOwnCoach() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(coach);

        Exercise exercise = Exercise.builder().id(new ObjectId()).status(ExerciseStatus.ACTIVE).build();
        MesocycleRequestDTO requestDTO = buildRequest(coach, athlete, exercise.getId(), MesocycleStatus.DRAFT);

        when(userRepository.findById(coach.getId().toHexString())).thenReturn(Optional.of(coach));
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));
        when(exerciseRepository.findById(exercise.getId().toHexString())).thenReturn(Optional.of(exercise));
        when(mesocycleRepository.findByCoachId(coach.getId())).thenReturn(List.of());
        when(mesocycleRepository.save(any(Mesocycle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MesocycleResponseDTO response = mesocycleService.createMesocycle(requestDTO);

        assertThat(response.getName()).isEqualTo("Hypertrophy Block");
    }

    @Test
    void createMesocycleRejectsNonCoachCaller() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(athlete);

        MesocycleRequestDTO requestDTO = buildRequest(athlete, athlete, new ObjectId(), MesocycleStatus.DRAFT);

        assertThatThrownBy(() -> mesocycleService.createMesocycle(requestDTO))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void createMesocycleRejectsCoachActingOnBehalfOfAnotherCoach() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        User otherCoach = buildUser(Role.COACH, "other-coach@example.com");
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(coach);

        MesocycleRequestDTO requestDTO = buildRequest(otherCoach, athlete, new ObjectId(), MesocycleStatus.DRAFT);
        lenient().when(userRepository.findById(otherCoach.getId().toHexString())).thenReturn(Optional.of(otherCoach));
        lenient().when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));

        assertThatThrownBy(() -> mesocycleService.createMesocycle(requestDTO))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void archiveMesocycleRejectsWhenAlreadyArchived() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(coach);

        Mesocycle mesocycle = buildMesocycleEntity(coach, athlete, MesocycleStatus.ARCHIVED);
        when(mesocycleRepository.findById(mesocycle.getId().toHexString())).thenReturn(Optional.of(mesocycle));

        assertThatThrownBy(() -> mesocycleService.archiveMesocycle(mesocycle.getId().toHexString()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void archiveMesocycleRejectsCoachThatDoesNotOwnIt() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        User otherCoach = buildUser(Role.COACH, "other-coach@example.com");
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(coach);

        Mesocycle mesocycle = buildMesocycleEntity(otherCoach, athlete, MesocycleStatus.ACTIVE);
        when(mesocycleRepository.findById(mesocycle.getId().toHexString())).thenReturn(Optional.of(mesocycle));

        assertThatThrownBy(() -> mesocycleService.archiveMesocycle(mesocycle.getId().toHexString()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void getMesocycleByIdRejectsAthleteViewingSomeoneElsesMesocycle() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        User otherAthlete = buildUser(Role.ATHLETE, "other-athlete@example.com");
        authenticateAs(otherAthlete);

        Mesocycle mesocycle = buildMesocycleEntity(coach, athlete, MesocycleStatus.ACTIVE);
        when(mesocycleRepository.findById(mesocycle.getId().toHexString())).thenReturn(Optional.of(mesocycle));

        assertThatThrownBy(() -> mesocycleService.getMesocycleById(mesocycle.getId().toHexString()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void getMesocycleByIdReturnsDetailWithNames() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(athlete);

        Mesocycle mesocycle = buildMesocycleEntity(coach, athlete, MesocycleStatus.ACTIVE);
        when(mesocycleRepository.findById(mesocycle.getId().toHexString())).thenReturn(Optional.of(mesocycle));
        when(userRepository.findById(coach.getId().toHexString())).thenReturn(Optional.of(coach));
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));

        MesocycleDetailDTO detail = mesocycleService.getMesocycleById(mesocycle.getId().toHexString());

        assertThat(detail.getCoachName()).isEqualTo("First Last");
        assertThat(detail.getAthleteName()).isEqualTo("First Last");
    }

    @Test
    void getMesocyclesByCoachRejectsOtherCoach() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        User otherCoach = buildUser(Role.COACH, "other-coach@example.com");
        authenticateAs(coach);
        when(userRepository.findById(otherCoach.getId().toHexString())).thenReturn(Optional.of(otherCoach));

        assertThatThrownBy(() -> mesocycleService.getMesocyclesByCoach(otherCoach.getId().toHexString()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void duplicateMesocycleCreatesDraftCopy() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(coach);

        Mesocycle original = buildMesocycleEntity(coach, athlete, MesocycleStatus.ACTIVE);
        when(mesocycleRepository.findById(original.getId().toHexString())).thenReturn(Optional.of(original));
        when(mesocycleRepository.save(any(Mesocycle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MesocycleResponseDTO duplicate = mesocycleService.duplicateMesocycle(original.getId().toHexString());

        assertThat(duplicate.getName()).isEqualTo("Hypertrophy Block (Copy)");
        assertThat(duplicate.getStatus()).isEqualTo(MesocycleStatus.DRAFT);
    }

    @Test
    void getMesocyclesForCurrentUserReturnsEmptyForNutritionistWithNoAssignedAthletes() {
        User nutritionist = buildUser(Role.NUTRITIONIST, "nutritionist@example.com");
        authenticateAs(nutritionist);
        when(nutritionPlanRepository.findByNutritionistId(nutritionist.getId())).thenReturn(List.of());

        List<MesocycleSummaryDTO> results = mesocycleService.getMesocyclesForCurrentUser();

        assertThat(results).isEmpty();
    }
}
