package com.gymtracker.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.gymtracker.dto.athlete.AthleteDetailDTO;
import com.gymtracker.dto.athlete.AthleteRequestDTO;
import com.gymtracker.dto.athlete.AthleteResponseDTO;
import com.gymtracker.dto.athlete.AthleteSummaryDTO;
import com.gymtracker.dto.mesocycle.MesocycleResponseDTO;
import com.gymtracker.dto.mesocycle.MesocycleSummaryDTO;
import com.gymtracker.dto.nutrition.NutritionPlanResponseDTO;
import com.gymtracker.dto.nutrition.NutritionPlanSummaryDTO;
import com.gymtracker.dto.workout.WorkoutSessionSummaryDTO;
import com.gymtracker.entity.User;
import com.gymtracker.enums.ActivityLevel;
import com.gymtracker.enums.Gender;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.DuplicateResourceException;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.mapper.AthleteMapper;
import com.gymtracker.mapper.AthleteMapperImpl;
import com.gymtracker.mapper.ObjectIdMapperImpl;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.security.AuthenticatedUserProvider;
import com.gymtracker.service.FatigueService;
import com.gymtracker.service.MesocycleService;
import com.gymtracker.service.NutritionPlanService;
import com.gymtracker.service.OneRepMaxService;
import com.gymtracker.service.WorkoutSessionService;
import com.gymtracker.validation.AthleteValidator;
import jakarta.validation.Validation;
import java.time.LocalDate;
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
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for AthleteServiceImpl, using the real (generated) AthleteMapper
 * and AthleteValidator against mocked repository/service collaborators.
 */
@ExtendWith(MockitoExtension.class)
class AthleteServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MesocycleService mesocycleService;
    @Mock
    private NutritionPlanService nutritionPlanService;
    @Mock
    private WorkoutSessionService workoutSessionService;
    @Mock
    private FatigueService fatigueService;
    @Mock
    private OneRepMaxService oneRepMaxService;

    private final AthleteMapper athleteMapper = new AthleteMapperImpl();
    private final AthleteValidator athleteValidator =
            new AthleteValidator(Validation.buildDefaultValidatorFactory().getValidator());

    private AthleteServiceImpl athleteService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(athleteMapper, "objectIdMapper", new ObjectIdMapperImpl());
        AuthenticatedUserProvider authenticatedUserProvider = new AuthenticatedUserProvider(userRepository);
        athleteService = new AthleteServiceImpl(userRepository, athleteMapper, athleteValidator,
                mesocycleService, nutritionPlanService, workoutSessionService, fatigueService, oneRepMaxService,
                authenticatedUserProvider);
        lenient().when(workoutSessionService.getWorkoutSessionsByAthlete(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User buildAthlete(String email) {
        return User.builder()
                .id(new ObjectId())
                .role(Role.ATHLETE)
                .email(email)
                .password("encoded")
                .firstName("Jane")
                .lastName("Doe")
                .age(25)
                .gender(Gender.FEMALE)
                .weight(60.0)
                .height(165.0)
                .activityLevel(ActivityLevel.MODERATE)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private User buildCoach(String email) {
        return User.builder()
                .id(new ObjectId())
                .role(Role.COACH)
                .email(email)
                .password("encoded")
                .firstName("Coach")
                .lastName("Smith")
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
    void getCurrentAthleteReturnsProfileForAuthenticatedAthlete() {
        User athlete = buildAthlete("athlete@example.com");
        authenticateAs(athlete);

        AthleteDetailDTO detail = athleteService.getCurrentAthlete();

        assertThat(detail.getEmail()).isEqualTo("athlete@example.com");
        assertThat(detail.getFirstName()).isEqualTo("Jane");
    }

    @Test
    void getCurrentAthleteRejectsNonAthleteCaller() {
        User coach = buildCoach("coach@example.com");
        authenticateAs(coach);

        assertThatThrownBy(() -> athleteService.getCurrentAthlete())
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void getAthleteByIdAllowsAthleteToViewOwnProfile() {
        User athlete = buildAthlete("athlete@example.com");
        authenticateAs(athlete);
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));

        AthleteDetailDTO detail = athleteService.getAthleteById(athlete.getId().toHexString());

        assertThat(detail.getId()).isEqualTo(athlete.getId().toHexString());
    }

    @Test
    void getAthleteByIdRejectsAthleteViewingAnotherAthlete() {
        User athlete = buildAthlete("athlete@example.com");
        User otherAthlete = buildAthlete("other@example.com");
        authenticateAs(athlete);
        when(userRepository.findById(otherAthlete.getId().toHexString())).thenReturn(Optional.of(otherAthlete));

        assertThatThrownBy(() -> athleteService.getAthleteById(otherAthlete.getId().toHexString()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void getAthleteByIdEnrichesDetailWithPerformanceData() {
        User coach = buildCoach("coach@example.com");
        User athlete = buildAthlete("athlete@example.com");
        authenticateAs(coach);
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));
        when(userRepository.findById(coach.getId().toHexString())).thenReturn(Optional.of(coach));

        MesocycleSummaryDTO assignedMesocycle = MesocycleSummaryDTO.builder()
                .athleteId(athlete.getId().toHexString()).build();
        when(mesocycleService.getMesocyclesByCoach(coach.getId().toHexString())).thenReturn(List.of(assignedMesocycle));

        MesocycleResponseDTO activeMesocycle = MesocycleResponseDTO.builder()
                .name("Hypertrophy Block").coachId(coach.getId().toHexString()).build();
        when(mesocycleService.getActiveMesocycle(athlete.getId().toHexString())).thenReturn(activeMesocycle);
        when(nutritionPlanService.getActiveNutritionPlan(athlete.getId().toHexString())).thenReturn(null);
        when(fatigueService.getCurrentFatigueLevel(athlete.getId().toHexString()))
                .thenReturn(com.gymtracker.enums.FatigueLevel.LOW);

        AthleteDetailDTO detail = athleteService.getAthleteById(athlete.getId().toHexString());

        assertThat(detail.getCurrentMesocycleName()).isEqualTo("Hypertrophy Block");
        assertThat(detail.getAssignedCoachName()).isEqualTo("Coach Smith");
        assertThat(detail.getCurrentFatigueLevel()).isEqualTo(com.gymtracker.enums.FatigueLevel.LOW);
    }

    @Test
    void updateAthleteProfileUpdatesWeightAndHeight() {
        User athlete = buildAthlete("athlete@example.com");
        authenticateAs(athlete);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AthleteRequestDTO requestDTO = AthleteRequestDTO.builder()
                .firstName("Jane").lastName("Doe").email("athlete@example.com")
                .age(25).gender(Gender.FEMALE).weight(62.0).height(165.0).activityLevel(ActivityLevel.MODERATE)
                .build();

        AthleteResponseDTO response = athleteService.updateAthleteProfile(requestDTO);

        assertThat(response.getWeight()).isEqualTo(62.0);
    }

    @Test
    void updateAthleteProfileRejectsNonAthleteCaller() {
        User coach = buildCoach("coach@example.com");
        authenticateAs(coach);

        AthleteRequestDTO requestDTO = AthleteRequestDTO.builder()
                .firstName("A").lastName("B").email("coach@example.com")
                .age(30).gender(Gender.MALE).weight(80.0).height(180.0).activityLevel(ActivityLevel.ACTIVE)
                .build();

        assertThatThrownBy(() -> athleteService.updateAthleteProfile(requestDTO))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void updateAthleteProfileRejectsDuplicateEmail() {
        User athlete = buildAthlete("athlete@example.com");
        authenticateAs(athlete);
        when(userRepository.existsByEmail("new-email@example.com")).thenReturn(true);

        AthleteRequestDTO requestDTO = AthleteRequestDTO.builder()
                .firstName("Jane").lastName("Doe").email("new-email@example.com")
                .age(25).gender(Gender.FEMALE).weight(60.0).height(165.0).activityLevel(ActivityLevel.MODERATE)
                .build();

        assertThatThrownBy(() -> athleteService.updateAthleteProfile(requestDTO))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void getAllAthletesRejectsAthleteCaller() {
        User athlete = buildAthlete("athlete@example.com");
        authenticateAs(athlete);

        assertThatThrownBy(() -> athleteService.getAllAthletes())
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void getAllAthletesReturnsEnrichedSummariesForCoach() {
        User coach = buildCoach("coach@example.com");
        User athlete = buildAthlete("athlete@example.com");
        authenticateAs(coach);
        when(userRepository.findByRole(Role.ATHLETE)).thenReturn(List.of(athlete));
        when(mesocycleService.getActiveMesocycle(athlete.getId().toHexString())).thenReturn(null);

        List<AthleteSummaryDTO> summaries = athleteService.getAllAthletes();

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).getEmail()).isEqualTo("athlete@example.com");
    }

    @Test
    void searchAthletesRejectsBlankKeyword() {
        User coach = buildCoach("coach@example.com");
        authenticateAs(coach);

        assertThatThrownBy(() -> athleteService.searchAthletes(" "))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void existsByEmailRejectsBlankEmail() {
        assertThatThrownBy(() -> athleteService.existsByEmail(""))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void existsByEmailDelegatesToRepository() {
        when(userRepository.existsByEmail("athlete@example.com")).thenReturn(true);

        assertThat(athleteService.existsByEmail("athlete@example.com")).isTrue();
    }

    @Test
    void getAthletesForCurrentUserRejectsAthleteCaller() {
        User athlete = buildAthlete("athlete@example.com");
        authenticateAs(athlete);

        assertThatThrownBy(() -> athleteService.getAthletesForCurrentUser())
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void getAthletesForCurrentUserReturnsAssignedAthletesForCoach() {
        User coach = buildCoach("coach@example.com");
        User athlete = buildAthlete("athlete@example.com");
        authenticateAs(coach);

        MesocycleSummaryDTO summary = MesocycleSummaryDTO.builder().athleteId(athlete.getId().toHexString()).build();
        when(mesocycleService.getMesocyclesByCoach(coach.getId().toHexString())).thenReturn(List.of(summary));
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));
        when(mesocycleService.getActiveMesocycle(athlete.getId().toHexString())).thenReturn(null);

        List<AthleteSummaryDTO> result = athleteService.getAthletesForCurrentUser();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(athlete.getId().toHexString());
    }
}
