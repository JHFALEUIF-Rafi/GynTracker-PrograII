package com.gymtracker.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.gymtracker.dto.dashboard.ChartDTO;
import com.gymtracker.dto.dashboard.ReportDTO;
import com.gymtracker.dto.dashboard.StatisticsDTO;
import com.gymtracker.entity.Mesocycle;
import com.gymtracker.entity.NutritionPlan;
import com.gymtracker.entity.Session;
import com.gymtracker.entity.User;
import com.gymtracker.enums.ExportFormat;
import com.gymtracker.enums.MesocycleStatus;
import com.gymtracker.enums.NutritionGoal;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.repository.MesocycleRepository;
import com.gymtracker.repository.NutritionPlanRepository;
import com.gymtracker.repository.SessionRepository;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.security.AuthenticatedUserProvider;
import com.gymtracker.service.AthleteAssignmentService;
import com.gymtracker.service.DashboardService;
import com.gymtracker.service.StatisticsService;
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

/**
 * Unit tests for ReportServiceImpl against mocked repositories and
 * collaborating services. {@code @Async} has no effect outside a Spring
 * context, so exportReport executes synchronously here.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private NutritionPlanRepository nutritionPlanRepository;
    @Mock
    private MesocycleRepository mesocycleRepository;
    @Mock
    private StatisticsService statisticsService;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private UserRepository userRepository;

    private ReportServiceImpl reportService;

    @BeforeEach
    void setUp() {
        AuthenticatedUserProvider authenticatedUserProvider = new AuthenticatedUserProvider(userRepository);
        AthleteAssignmentService athleteAssignmentService =
                new AthleteAssignmentServiceImpl(mesocycleRepository, nutritionPlanRepository);
        reportService = new ReportServiceImpl(sessionRepository, nutritionPlanRepository, mesocycleRepository,
                statisticsService, dashboardService, userRepository, authenticatedUserProvider, athleteAssignmentService);
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
                .firstName("First")
                .lastName("Last")
                .weight(75.0)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void authenticateAs(User user) {
        TestingAuthenticationToken token = new TestingAuthenticationToken(user.getEmail(), null);
        token.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(token);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    void generateAthleteReportSucceedsForAssignedCoach() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(coach);

        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));
        Mesocycle mesocycle = Mesocycle.builder().coachId(coach.getId()).athleteId(athlete.getId()).build();
        when(mesocycleRepository.findByCoachId(coach.getId())).thenReturn(List.of(mesocycle));
        when(mesocycleRepository.findByAthleteId(athlete.getId())).thenReturn(List.of(mesocycle));
        when(sessionRepository.findByAthleteId(athlete.getId())).thenReturn(List.of());
        when(nutritionPlanRepository.findByAthleteId(athlete.getId())).thenReturn(List.of());
        when(statisticsService.getAthleteStatistics(athlete.getId().toHexString()))
                .thenReturn(StatisticsDTO.builder().build());
        when(statisticsService.getOneRepMaxChart(athlete.getId().toHexString()))
                .thenReturn(ChartDTO.builder().title("1RM").labels(List.of()).values(List.of()).build());

        ReportDTO report = reportService.generateAthleteReport(athlete.getId().toHexString());

        assertThat(report.getReportType()).isEqualTo("ATHLETE_REPORT");
        assertThat(report.getRequestedForUserId()).isEqualTo(athlete.getId().toHexString());
    }

    @Test
    void generateAthleteReportRejectsUnassignedCoach() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(coach);

        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));
        when(mesocycleRepository.findByCoachId(coach.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> reportService.generateAthleteReport(athlete.getId().toHexString()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void generateAthleteReportRejectsNutritionistCaller() {
        User nutritionist = buildUser(Role.NUTRITIONIST, "nutritionist@example.com");
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(nutritionist);
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));

        assertThatThrownBy(() -> reportService.generateAthleteReport(athlete.getId().toHexString()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void generateNutritionReportRejectsUnassignedNutritionist() {
        // A nutritionist with no plans for this athlete is, by definition,
        // not "assigned" (isAthleteAssignedToNutritionist checks
        // findByNutritionistId), so the permission check fails before the
        // "no plans" ResourceNotFoundException path could ever be reached.
        User nutritionist = buildUser(Role.NUTRITIONIST, "nutritionist@example.com");
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(nutritionist);
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));
        when(nutritionPlanRepository.findByNutritionistId(nutritionist.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> reportService.generateNutritionReport(athlete.getId().toHexString()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void generateNutritionReportSucceedsForAssignedNutritionist() {
        User nutritionist = buildUser(Role.NUTRITIONIST, "nutritionist@example.com");
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(nutritionist);
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));

        NutritionPlan plan = NutritionPlan.builder()
                .athleteId(athlete.getId()).nutritionistId(nutritionist.getId())
                .goal(NutritionGoal.CUTTING).calories(2200).protein(150.0).carbohydrates(200.0).fat(60.0)
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(30)).active(true)
                .createdAt(LocalDateTime.now())
                .build();
        when(nutritionPlanRepository.findByNutritionistId(nutritionist.getId())).thenReturn(List.of(plan));
        when(nutritionPlanRepository.findByAthleteId(athlete.getId())).thenReturn(List.of(plan));

        ReportDTO report = reportService.generateNutritionReport(athlete.getId().toHexString());

        assertThat(report.getReportType()).isEqualTo("NUTRITION_REPORT");
    }

    @Test
    void generateMesocycleReportSucceedsForOwningCoach() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        authenticateAs(coach);

        Mesocycle.WorkoutDay day = Mesocycle.WorkoutDay.builder().dayName("Day 1").exercises(List.of()).build();
        Mesocycle mesocycle = Mesocycle.builder()
                .id(new ObjectId()).coachId(coach.getId()).athleteId(athlete.getId())
                .name("Block A").status(MesocycleStatus.ACTIVE).durationWeeks(8)
                .days(List.of(day)).build();
        when(mesocycleRepository.findById(mesocycle.getId().toHexString())).thenReturn(Optional.of(mesocycle));
        when(userRepository.findById(athlete.getId().toHexString())).thenReturn(Optional.of(athlete));
        when(userRepository.findById(coach.getId().toHexString())).thenReturn(Optional.of(coach));
        when(sessionRepository.findByMesocycleId(mesocycle.getId())).thenReturn(List.of());

        ReportDTO report = reportService.generateMesocycleReport(mesocycle.getId().toHexString());

        assertThat(report.getReportType()).isEqualTo("MESOCYCLE_REPORT");
        assertThat(report.getSections()).containsEntry("mesocycleName", "Block A");
    }

    @Test
    void exportReportProducesNonEmptyPdfBytes() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        authenticateAs(coach);

        ReportDTO report = ReportDTO.builder()
                .reportType("COACH_REPORT")
                .title("Coach Report")
                .requestedForUserId(coach.getId().toHexString())
                .generatedByUserId(coach.getId().toHexString())
                .generatedByRole("COACH")
                .generatedAt(LocalDateTime.now())
                .sections(java.util.Map.of("assignedAthletes", 3))
                .build();

        byte[] bytes = reportService.exportReport(report, ExportFormat.PDF).join();

        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).startsWith("%PDF");
    }

    @Test
    void exportReportProducesNonEmptyXlsxBytes() {
        User coach = buildUser(Role.COACH, "coach@example.com");
        authenticateAs(coach);

        ReportDTO report = ReportDTO.builder()
                .reportType("COACH_REPORT")
                .title("Coach Report")
                .requestedForUserId(coach.getId().toHexString())
                .generatedByUserId(coach.getId().toHexString())
                .generatedByRole("COACH")
                .generatedAt(LocalDateTime.now())
                .sections(java.util.Map.of("assignedAthletes", 3))
                .build();

        byte[] bytes = reportService.exportReport(report, ExportFormat.XLSX).join();

        assertThat(bytes).isNotEmpty();
    }

    @Test
    void exportReportRejectsAthleteExportingSomeoneElsesReport() {
        User athlete = buildUser(Role.ATHLETE, "athlete@example.com");
        User otherAthlete = buildUser(Role.ATHLETE, "other-athlete@example.com");
        authenticateAs(athlete);

        ReportDTO report = ReportDTO.builder()
                .reportType("PROGRESS_REPORT")
                .title("Progress")
                .requestedForUserId(otherAthlete.getId().toHexString())
                .generatedByUserId(otherAthlete.getId().toHexString())
                .generatedByRole("ATHLETE")
                .generatedAt(LocalDateTime.now())
                .sections(java.util.Map.of())
                .build();

        // ensureCanExport runs before the try/catch that wraps the future, so
        // it throws synchronously from the call itself, not via the future.
        assertThatThrownBy(() -> reportService.exportReport(report, ExportFormat.PDF))
                .isInstanceOf(UnauthorizedOperationException.class);
    }
}
