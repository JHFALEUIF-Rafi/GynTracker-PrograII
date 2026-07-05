package com.gymtracker.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.gymtracker.dto.alert.AlertDTO;
import com.gymtracker.entity.Alert;
import com.gymtracker.entity.Mesocycle;
import com.gymtracker.entity.User;
import com.gymtracker.enums.AlertStatus;
import com.gymtracker.enums.FatigueLevel;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.BusinessRuleException;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.mapper.AlertMapper;
import com.gymtracker.mapper.AlertMapperImpl;
import com.gymtracker.mapper.ObjectIdMapperImpl;
import com.gymtracker.repository.AlertRepository;
import com.gymtracker.repository.MesocycleRepository;
import com.gymtracker.repository.NutritionPlanRepository;
import com.gymtracker.repository.SessionRepository;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.security.AuthenticatedUserProvider;
import com.gymtracker.service.AthleteAssignmentService;
import com.gymtracker.service.FatigueService;
import com.gymtracker.service.OneRepMaxService;
import com.gymtracker.validation.AlertValidator;
import jakarta.validation.Validation;
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
 * Unit tests for AlertServiceImpl, using the real (generated) AlertMapper and
 * AlertValidator against mocked repositories and collaborating services.
 * {@code @Async} has no effect outside a Spring context, so generate* methods
 * execute synchronously here.
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceImplTest {

    @Mock
    private AlertRepository alertRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private MesocycleRepository mesocycleRepository;
    @Mock
    private NutritionPlanRepository nutritionPlanRepository;
    @Mock
    private FatigueService fatigueService;
    @Mock
    private OneRepMaxService oneRepMaxService;

    private final AlertMapper alertMapper = new AlertMapperImpl();
    private final AlertValidator alertValidator = new AlertValidator(Validation.buildDefaultValidatorFactory().getValidator());

    private AlertServiceImpl alertService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(alertMapper, "objectIdMapper", new ObjectIdMapperImpl());
        AuthenticatedUserProvider authenticatedUserProvider = new AuthenticatedUserProvider(userRepository);
        AthleteAssignmentService athleteAssignmentService =
                new AthleteAssignmentServiceImpl(mesocycleRepository, nutritionPlanRepository);
        alertService = new AlertServiceImpl(alertRepository, userRepository, sessionRepository, mesocycleRepository,
                nutritionPlanRepository, fatigueService, oneRepMaxService, alertMapper, alertValidator,
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

    private Alert buildAlert(ObjectId athleteId, ObjectId coachId, AlertStatus status) {
        return Alert.builder()
                .id(new ObjectId())
                .athleteId(athleteId)
                .coachId(coachId)
                .type("HIGH_FATIGUE")
                .message("Message")
                .status(status)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void generateFatigueAlertCreatesCriticalAlertAndAssignsCoachFromLatestMesocycle() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        User coach = buildUser(Role.COACH, "coach@example.com");
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));
        when(fatigueService.getCurrentFatigueLevel(athlete.getId().toHexString())).thenReturn(FatigueLevel.CRITICAL);
        Mesocycle mesocycle = Mesocycle.builder().coachId(coach.getId()).createdAt(LocalDateTime.now()).build();
        when(mesocycleRepository.findByAthleteId(athlete.getId())).thenReturn(List.of(mesocycle));
        when(alertRepository.findByAthleteIdAndTypeAndStatus(athlete.getId(), "CRITICAL_FATIGUE", AlertStatus.ACTIVE))
                .thenReturn(List.of());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompletableFuture<AlertDTO> result = alertService.generateFatigueAlert(athlete.getId().toHexString());

        assertThat(result.join().getType()).isEqualTo("CRITICAL_FATIGUE");
        assertThat(result.join().getCoachId()).isEqualTo(coach.getId().toHexString());
    }

    @Test
    void generateFatigueAlertSkipsDuplicateActiveAlert() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));
        when(fatigueService.getCurrentFatigueLevel(athlete.getId().toHexString())).thenReturn(FatigueLevel.HIGH);
        lenient().when(mesocycleRepository.findByAthleteId(athlete.getId()))
                .thenReturn(List.of(Mesocycle.builder().coachId(new ObjectId()).createdAt(LocalDateTime.now()).build()));
        when(alertRepository.findByAthleteIdAndTypeAndStatus(athlete.getId(), "HIGH_FATIGUE", AlertStatus.ACTIVE))
                .thenReturn(List.of(buildAlert(athlete.getId(), new ObjectId(), AlertStatus.ACTIVE)));

        CompletableFuture<AlertDTO> result = alertService.generateFatigueAlert(athlete.getId().toHexString());

        assertThat(result.join()).isNull();
    }

    @Test
    void getAlertsByAthleteRejectsOtherAthlete() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        User otherAthlete = buildUser(Role.ATHLETE, "other-athlete@example.com");
        authenticateAs(otherAthlete);
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));

        assertThatThrownBy(() -> alertService.getAlertsByAthlete(athlete.getId().toHexString()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void getAlertsByAthleteFiltersToNutritionRelatedForNutritionist() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        User nutritionist = buildUser(Role.NUTRITIONIST, "nutritionist@example.com");
        authenticateAs(nutritionist);
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));

        com.gymtracker.entity.NutritionPlan assignedPlan = com.gymtracker.entity.NutritionPlan.builder()
                .nutritionistId(nutritionist.getId()).athleteId(athlete.getId()).build();
        when(nutritionPlanRepository.findByNutritionistId(nutritionist.getId())).thenReturn(List.of(assignedPlan));

        Alert nutritionAlert = buildAlert(athlete.getId(), new ObjectId(), AlertStatus.ACTIVE);
        nutritionAlert.setType("NUTRITION_PLAN_EXPIRED");
        Alert fatigueAlert = buildAlert(athlete.getId(), new ObjectId(), AlertStatus.ACTIVE);
        fatigueAlert.setType("HIGH_FATIGUE");
        when(alertRepository.findByAthleteId(athlete.getId())).thenReturn(List.of(nutritionAlert, fatigueAlert));

        List<AlertDTO> results = alertService.getAlertsByAthlete(athlete.getId().toHexString());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getType()).isEqualTo("NUTRITION_PLAN_EXPIRED");
    }

    @Test
    void acknowledgeAlertSucceedsForOwningCoach() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        authenticateAs(coach);
        Alert alert = buildAlert(new ObjectId(), coach.getId(), AlertStatus.ACTIVE);
        when(alertRepository.findById(alert.getId().toHexString())).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlertDTO result = alertService.acknowledgeAlert(alert.getId().toHexString());

        assertThat(result.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
    }

    @Test
    void acknowledgeAlertRejectsNonOwningCoach() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        authenticateAs(coach);
        Alert alert = buildAlert(new ObjectId(), new ObjectId(), AlertStatus.ACTIVE);
        when(alertRepository.findById(alert.getId().toHexString())).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> alertService.acknowledgeAlert(alert.getId().toHexString()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void acknowledgeAlertRejectsNonActiveAlert() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        authenticateAs(coach);
        Alert alert = buildAlert(new ObjectId(), coach.getId(), AlertStatus.RESOLVED);
        when(alertRepository.findById(alert.getId().toHexString())).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> alertService.acknowledgeAlert(alert.getId().toHexString()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void resolveAlertSucceedsForAcknowledgedAlert() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        authenticateAs(coach);
        Alert alert = buildAlert(new ObjectId(), coach.getId(), AlertStatus.ACKNOWLEDGED);
        when(alertRepository.findById(alert.getId().toHexString())).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlertDTO result = alertService.resolveAlert(alert.getId().toHexString());

        assertThat(result.getStatus()).isEqualTo(AlertStatus.RESOLVED);
    }

    @Test
    void resolveAlertRejectsActiveAlert() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        authenticateAs(coach);
        Alert alert = buildAlert(new ObjectId(), coach.getId(), AlertStatus.ACTIVE);
        when(alertRepository.findById(alert.getId().toHexString())).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> alertService.resolveAlert(alert.getId().toHexString()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void deleteResolvedAlertsAlwaysThrows() {
        assertThatThrownBy(() -> alertService.deleteResolvedAlerts())
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void getAlertsForCurrentUserReturnsNutritionRelatedAlertsForNutritionist() {
        User nutritionist = buildUser(Role.NUTRITIONIST, "nutritionist@example.com");
        authenticateAs(nutritionist);
        ObjectId athleteId = new ObjectId();
        Alert nutritionAlert = buildAlert(athleteId, new ObjectId(), AlertStatus.ACTIVE);
        nutritionAlert.setType("NUTRITION_PLAN_EXPIRED");
        when(alertRepository.findByTypeIn(any())).thenReturn(List.of(nutritionAlert));

        com.gymtracker.entity.NutritionPlan assignedPlan = com.gymtracker.entity.NutritionPlan.builder()
                .nutritionistId(nutritionist.getId()).athleteId(athleteId).build();
        when(nutritionPlanRepository.findByNutritionistId(nutritionist.getId())).thenReturn(List.of(assignedPlan));

        List<AlertDTO> results = alertService.getAlertsForCurrentUser();

        assertThat(results).hasSize(1);
    }
}
