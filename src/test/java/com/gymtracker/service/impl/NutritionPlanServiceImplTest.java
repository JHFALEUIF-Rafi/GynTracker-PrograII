package com.gymtracker.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.gymtracker.dto.nutrition.NutritionPlanRequestDTO;
import com.gymtracker.dto.nutrition.NutritionPlanResponseDTO;
import com.gymtracker.dto.nutrition.NutritionPlanSummaryDTO;
import com.gymtracker.entity.NutritionPlan;
import com.gymtracker.entity.User;
import com.gymtracker.enums.NutritionGoal;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.BusinessRuleException;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.mapper.NutritionPlanMapper;
import com.gymtracker.mapper.NutritionPlanMapperImpl;
import com.gymtracker.mapper.ObjectIdMapperImpl;
import com.gymtracker.repository.MesocycleRepository;
import com.gymtracker.repository.NutritionPlanRepository;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.security.AuthenticatedUserProvider;
import com.gymtracker.service.AthleteAssignmentService;
import com.gymtracker.validation.NutritionPlanValidator;
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
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for NutritionPlanServiceImpl, using the real (generated)
 * NutritionPlanMapper and NutritionPlanValidator against mocked repositories.
 */
@ExtendWith(MockitoExtension.class)
class NutritionPlanServiceImplTest {

    @Mock
    private NutritionPlanRepository nutritionPlanRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MesocycleRepository mesocycleRepository;
    @Mock
    private MongoOperations mongoTemplate;

    private final NutritionPlanMapper nutritionPlanMapper = new NutritionPlanMapperImpl();
    private final NutritionPlanValidator nutritionPlanValidator =
            new NutritionPlanValidator(Validation.buildDefaultValidatorFactory().getValidator());

    private NutritionPlanServiceImpl nutritionPlanService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(nutritionPlanMapper, "objectIdMapper", new ObjectIdMapperImpl());
        AuthenticatedUserProvider authenticatedUserProvider = new AuthenticatedUserProvider(userRepository);
        AthleteAssignmentService athleteAssignmentService =
                new AthleteAssignmentServiceImpl(mesocycleRepository, nutritionPlanRepository);
        nutritionPlanService = new NutritionPlanServiceImpl(
                nutritionPlanRepository, userRepository, nutritionPlanMapper,
                nutritionPlanValidator, mongoTemplate, authenticatedUserProvider, athleteAssignmentService);
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

    private NutritionPlanRequestDTO buildRequest(User athlete, User nutritionist) {
        return NutritionPlanRequestDTO.builder()
                .athleteId(athlete.getId().toHexString())
                .nutritionistId(nutritionist.getId().toHexString())
                .goal(NutritionGoal.CUTTING)
                .calories(2200)
                .protein(160.0)
                .carbohydrates(200.0)
                .fat(60.0)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .active(true)
                .build();
    }

    private NutritionPlan buildPlan(User athlete, User nutritionist, boolean active) {
        return NutritionPlan.builder()
                .id(new ObjectId())
                .athleteId(athlete.getId())
                .nutritionistId(nutritionist.getId())
                .goal(NutritionGoal.CUTTING)
                .calories(2200)
                .protein(160.0)
                .carbohydrates(200.0)
                .fat(60.0)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .active(active)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createNutritionPlanSucceedsForOwnNutritionist() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        User nutritionist = buildUser(Role.NUTRITIONIST, "nutritionist@example.com");
        authenticateAs(nutritionist);

        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));
        when(userRepository.findById(nutritionist.getId().toHexString())).thenReturn(Optional.of(nutritionist));
        when(nutritionPlanRepository.findByAthleteId(athlete.getId())).thenReturn(List.of());
        when(nutritionPlanRepository.save(any(NutritionPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NutritionPlanResponseDTO response = nutritionPlanService.createNutritionPlan(buildRequest(athlete, nutritionist));

        assertThat(response.getCalories()).isEqualTo(2200);
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void createNutritionPlanRejectsNonNutritionistCaller() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(athlete);

        assertThatThrownBy(() -> nutritionPlanService.createNutritionPlan(buildRequest(athlete, athlete)))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void deactivateNutritionPlanRejectsAlreadyInactivePlan() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        User nutritionist = buildUser(Role.NUTRITIONIST, "nutritionist@example.com");
        authenticateAs(nutritionist);

        NutritionPlan plan = buildPlan(athlete, nutritionist, false);
        when(nutritionPlanRepository.findById(plan.getId().toHexString())).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> nutritionPlanService.deactivateNutritionPlan(plan.getId().toHexString()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void deactivateNutritionPlanSucceeds() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        User nutritionist = buildUser(Role.NUTRITIONIST, "nutritionist@example.com");
        authenticateAs(nutritionist);

        NutritionPlan plan = buildPlan(athlete, nutritionist, true);
        when(nutritionPlanRepository.findById(plan.getId().toHexString())).thenReturn(Optional.of(plan));
        when(nutritionPlanRepository.save(any(NutritionPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NutritionPlanResponseDTO response = nutritionPlanService.deactivateNutritionPlan(plan.getId().toHexString());

        assertThat(response.getActive()).isFalse();
    }

    @Test
    void getNutritionPlanByIdRejectsAthleteViewingAnotherAthletesPlan() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        User otherAthlete = buildUser(Role.ATHLETE, "other-athlete@example.com");
        User nutritionist = buildUser(Role.NUTRITIONIST, "nutritionist@example.com");
        authenticateAs(otherAthlete);

        NutritionPlan plan = buildPlan(athlete, nutritionist, true);
        when(nutritionPlanRepository.findById(plan.getId().toHexString())).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> nutritionPlanService.getNutritionPlanById(plan.getId().toHexString()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void getNutritionPlansByNutritionistRejectsOtherNutritionist() {
        User nutritionist = buildUser(Role.NUTRITIONIST, "nutritionist@example.com");
        User otherNutritionist = buildUser(Role.NUTRITIONIST, "other-nutritionist@example.com");
        authenticateAs(nutritionist);
        when(userRepository.findById(otherNutritionist.getId().toHexString())).thenReturn(Optional.of(otherNutritionist));

        assertThatThrownBy(() -> nutritionPlanService.getNutritionPlansByNutritionist(otherNutritionist.getId().toHexString()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void getNutritionPlansForCurrentUserReturnsEmptyForCoachWithNoAssignedAthletes() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        authenticateAs(coach);
        when(mesocycleRepository.findByCoachId(coach.getId())).thenReturn(List.of());

        List<NutritionPlanSummaryDTO> results = nutritionPlanService.getNutritionPlansForCurrentUser();

        assertThat(results).isEmpty();
    }

    @Test
    void searchNutritionPlansRejectsBlankKeyword() {
        User nutritionist = buildUser(Role.NUTRITIONIST, "nutritionist@example.com");
        authenticateAs(nutritionist);

        assertThatThrownBy(() -> nutritionPlanService.searchNutritionPlans(" "))
                .isInstanceOf(com.gymtracker.exception.ValidationException.class);
    }
}
