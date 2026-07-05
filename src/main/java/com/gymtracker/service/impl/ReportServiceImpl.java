package com.gymtracker.service.impl;

import com.gymtracker.dto.dashboard.ChartDTO;
import com.gymtracker.dto.dashboard.DashboardDTO;
import com.gymtracker.dto.dashboard.ProgressDTO;
import com.gymtracker.dto.dashboard.ReportDTO;
import com.gymtracker.dto.dashboard.StatisticsDTO;
import com.gymtracker.entity.Mesocycle;
import com.gymtracker.entity.NutritionPlan;
import com.gymtracker.entity.Session;
import com.gymtracker.entity.User;
import com.gymtracker.enums.ExportFormat;
import com.gymtracker.enums.MesocycleStatus;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.BusinessRuleException;
import com.gymtracker.exception.ResourceNotFoundException;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.repository.MesocycleRepository;
import com.gymtracker.repository.NutritionPlanRepository;
import com.gymtracker.repository.SessionRepository;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.security.AuthenticatedUserProvider;
import com.gymtracker.service.AthleteAssignmentService;
import com.gymtracker.service.DashboardService;
import com.gymtracker.service.ReportService;
import com.gymtracker.service.StatisticsService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Business implementation for report generation and export.
 */
@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportServiceImpl.class);

    private static final String TYPE_ATHLETE = "ATHLETE_REPORT";
    private static final String TYPE_COACH = "COACH_REPORT";
    private static final String TYPE_NUTRITION = "NUTRITION_REPORT";
    private static final String TYPE_PROGRESS = "PROGRESS_REPORT";
    private static final String TYPE_WORKOUT_HISTORY = "WORKOUT_HISTORY_REPORT";
    private static final String TYPE_MESOCYCLE = "MESOCYCLE_REPORT";

    private final SessionRepository workoutSessionRepository;
    private final NutritionPlanRepository nutritionPlanRepository;
    private final MesocycleRepository mesocycleRepository;
    private final StatisticsService statisticsService;
    private final DashboardService dashboardService;
    private final UserRepository userRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final AthleteAssignmentService athleteAssignmentService;

    public ReportServiceImpl(
            SessionRepository workoutSessionRepository,
            NutritionPlanRepository nutritionPlanRepository,
            MesocycleRepository mesocycleRepository,
            StatisticsService statisticsService,
            DashboardService dashboardService,
            UserRepository userRepository,
            AuthenticatedUserProvider authenticatedUserProvider,
            AthleteAssignmentService athleteAssignmentService
    ) {
        this.workoutSessionRepository = workoutSessionRepository;
        this.nutritionPlanRepository = nutritionPlanRepository;
        this.mesocycleRepository = mesocycleRepository;
        this.statisticsService = statisticsService;
        this.dashboardService = dashboardService;
        this.userRepository = userRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.athleteAssignmentService = athleteAssignmentService;
    }

    @Override
    public ReportDTO generateAthleteReport(String athleteId) {
        validateIdentifier(athleteId, "Athlete id is required.");
        User currentUser = getAuthenticatedUser();
        User athlete = getAthleteById(athleteId);
        ensureCanGenerateAthleteScopedReport(currentUser, athlete.getId());

        DashboardDTO dashboard = resolveAthleteDashboard(currentUser, athleteId);
        StatisticsDTO statistics = statisticsService.getAthleteStatistics(athleteId);
        ChartDTO oneRepMaxProgress = statisticsService.getOneRepMaxChart(athleteId);

        Map<String, Object> sections = new LinkedHashMap<>();
        sections.put("personalInformation", toPersonalInformation(athlete));
        sections.put("currentMesocycle", resolveCurrentMesocycleName(athlete.getId()));
        sections.put("nutritionPlan", resolveCurrentNutritionSummary(athlete.getId()));
        sections.put("workoutSummary", resolveWorkoutSummary(athlete.getId()));
        sections.put("fatigueAnalysis", resolveFatigueAnalysis(dashboard));
        sections.put("activeAlerts", dashboard != null ? dashboard.getActiveAlerts() : 0);

        ReportDTO report = baseReport(TYPE_ATHLETE, "Athlete Report", athleteId, currentUser)
                .dashboard(dashboard)
                .statistics(statistics)
                .charts(List.of(oneRepMaxProgress))
                .sections(sections)
                .build();
        LOGGER.info("Report generated type={} targetUserId={}", TYPE_ATHLETE, athleteId);
        return report;
    }

    @Override
    public ReportDTO generateCoachReport(String coachId) {
        validateIdentifier(coachId, "Coach id is required.");
        User currentUser = getAuthenticatedUser();
        User coach = getCoachById(coachId);
        ensureCoachSelf(currentUser, coach.getId());

        DashboardDTO dashboard = dashboardService.getCoachDashboard(coachId);
        StatisticsDTO statistics = statisticsService.getCoachStatistics(coachId);

        Map<String, Object> sections = new LinkedHashMap<>();
        sections.put("assignedAthletes", dashboard.getAssignedAthletes());
        sections.put("activeMesocycles", dashboard.getActiveMesocycles());
        sections.put("weeklyProgress", statistics.getEstimatedStrengthProgress());
        sections.put("highFatigueAthletes", dashboard.getAthletesWithHighFatigue());
        sections.put("pendingAlerts", dashboard.getPendingAlerts());

        ReportDTO report = baseReport(TYPE_COACH, "Coach Report", coachId, currentUser)
                .dashboard(dashboard)
                .statistics(statistics)
                .sections(sections)
                .build();
        LOGGER.info("Report generated type={} targetUserId={}", TYPE_COACH, coachId);
        return report;
    }

    @Override
    public ReportDTO generateNutritionReport(String athleteId) {
        validateIdentifier(athleteId, "Athlete id is required.");
        User currentUser = getAuthenticatedUser();
        User athlete = getAthleteById(athleteId);
        ensureCanGenerateNutritionReport(currentUser, athlete.getId());

        List<NutritionPlan> plans = nutritionPlanRepository.findByAthleteId(athlete.getId()).stream()
                .sorted(Comparator.comparing(NutritionPlan::getCreatedAt).reversed())
                .toList();
        if (plans.isEmpty()) {
            throw new ResourceNotFoundException("Nutrition plans not found for athlete: " + athleteId);
        }

        Map<String, Object> sections = new LinkedHashMap<>();
        sections.put("nutritionPlans", plans.size());
        sections.put("calories", round(plans.stream().mapToInt(NutritionPlan::getCalories).average().orElse(0.0d)));
        sections.put("protein", round(plans.stream().mapToDouble(NutritionPlan::getProtein).average().orElse(0.0d)));
        sections.put("carbohydrates", round(plans.stream().mapToDouble(NutritionPlan::getCarbohydrates).average().orElse(0.0d)));
        sections.put("fat", round(plans.stream().mapToDouble(NutritionPlan::getFat).average().orElse(0.0d)));
        sections.put("complianceHistory", buildNutritionComplianceHistory(plans));

        ReportDTO report = baseReport(TYPE_NUTRITION, "Nutrition Report", athleteId, currentUser)
                .sections(sections)
                .build();
        LOGGER.info("Report generated type={} targetUserId={}", TYPE_NUTRITION, athleteId);
        return report;
    }

    @Override
    public ReportDTO generateProgressReport(String athleteId) {
        validateIdentifier(athleteId, "Athlete id is required.");
        User currentUser = getAuthenticatedUser();
        User athlete = getAthleteById(athleteId);
        ensureCanGenerateAthleteScopedReport(currentUser, athlete.getId());

        StatisticsDTO statistics = statisticsService.getAthleteStatistics(athleteId);
        ChartDTO volumeChart = statisticsService.getWorkoutVolumeChart(athleteId);
        ChartDTO oneRepMaxChart = statisticsService.getOneRepMaxChart(athleteId);
        ChartDTO fatigueChart = statisticsService.getFatigueChart(athleteId);
        ProgressDTO progress = buildProgressDto(statistics, fatigueChart);

        Map<String, Object> sections = new LinkedHashMap<>();
        sections.put("strengthProgress", progress.getStrengthProgress());
        sections.put("volumeProgress", progress.getVolumeProgress());
        sections.put("workoutConsistency", progress.getWorkoutConsistency());
        sections.put("fatigueTrend", progress.getFatigueTrend());
        sections.put("trainingFrequency", progress.getTrainingFrequency());

        ReportDTO report = baseReport(TYPE_PROGRESS, "Progress Report", athleteId, currentUser)
                .statistics(statistics)
                .progress(progress)
                .charts(List.of(volumeChart, oneRepMaxChart, fatigueChart))
                .sections(sections)
                .build();
        LOGGER.info("Report generated type={} targetUserId={}", TYPE_PROGRESS, athleteId);
        return report;
    }

    @Override
    public ReportDTO generateWorkoutHistoryReport(String athleteId) {
        validateIdentifier(athleteId, "Athlete id is required.");
        User currentUser = getAuthenticatedUser();
        User athlete = getAthleteById(athleteId);
        ensureCanGenerateAthleteScopedReport(currentUser, athlete.getId());

        List<Session> sessions = workoutSessionRepository.findByAthleteId(athlete.getId()).stream()
                .filter(this::isCompletedSession)
                .sorted(Comparator.comparing(Session::getDate).reversed())
                .toList();
        if (sessions.isEmpty()) {
            throw new ResourceNotFoundException("Workout sessions not found for athlete: " + athleteId);
        }

        Map<String, Object> sections = new LinkedHashMap<>();
        sections.put("workoutHistory", buildWorkoutHistoryRows(sessions));
        sections.put("totalSessions", sessions.size());
        sections.put("totalVolume", round(sessions.stream().mapToDouble(Session::getTotalVolume).sum()));

        ReportDTO report = baseReport(TYPE_WORKOUT_HISTORY, "Workout History Report", athleteId, currentUser)
                .sections(sections)
                .build();
        LOGGER.info("Report generated type={} targetUserId={}", TYPE_WORKOUT_HISTORY, athleteId);
        return report;
    }

    @Override
    public ReportDTO generateMesocycleReport(String mesocycleId) {
        validateIdentifier(mesocycleId, "Mesocycle id is required.");
        User currentUser = getAuthenticatedUser();
        Mesocycle mesocycle = mesocycleRepository.findById(mesocycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Mesocycle not found with id: " + mesocycleId));
        ensureCanGenerateMesocycleReport(currentUser, mesocycle);

        User athlete = userRepository.findById(mesocycle.getAthleteId().toHexString())
                .orElseThrow(() -> new ResourceNotFoundException("Athlete not found for mesocycle."));
        User coach = userRepository.findById(mesocycle.getCoachId().toHexString())
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found for mesocycle."));

        List<Session> sessions = workoutSessionRepository.findByMesocycleId(mesocycle.getId()).stream()
                .filter(this::isCompletedSession)
                .sorted(Comparator.comparing(Session::getDate))
                .toList();

        Map<String, Object> sections = new LinkedHashMap<>();
        sections.put("mesocycleName", mesocycle.getName());
        sections.put("status", mesocycle.getStatus().name());
        sections.put("durationWeeks", mesocycle.getDurationWeeks());
        sections.put("coach", coach.getFirstName() + " " + coach.getLastName());
        sections.put("athlete", athlete.getFirstName() + " " + athlete.getLastName());
        sections.put("plannedDays", mesocycle.getDays().size());
        sections.put("plannedExercises", countPlannedExercises(mesocycle));
        sections.put("completedSessions", sessions.size());
        sections.put("completedVolume", round(sessions.stream().mapToDouble(Session::getTotalVolume).sum()));

        ReportDTO report = baseReport(TYPE_MESOCYCLE, "Mesocycle Report", mesocycle.getAthleteId().toHexString(), currentUser)
                .sections(sections)
                .build();
        LOGGER.info("Report generated type={} mesocycleId={}", TYPE_MESOCYCLE, mesocycleId);
        return report;
    }

    @Override
    @Async
    public CompletableFuture<byte[]> exportReport(ReportDTO report, ExportFormat format) {
        validateReportForExport(report, format);
        User currentUser = getAuthenticatedUser();
        ensureCanExport(currentUser, report);
        LOGGER.info("Async execution started for report export type={} format={}", report.getReportType(), format);

        try {
            byte[] content = switch (format) {
                case PDF -> buildPdfExport(report);
                case XLSX -> buildXlsxExport(report);
            };
            LOGGER.info("Export completed type={} format={}", report.getReportType(), format);
            return CompletableFuture.completedFuture(content);
        } catch (RuntimeException exception) {
            LOGGER.warn("Export failed type={} format={} error={}",
                    report.getReportType(), format, exception.getMessage());
            return CompletableFuture.failedFuture(exception);
        }
    }

    private static class ReportBuilder {
        private final ReportDTO.ReportDTOBuilder builder;

        ReportBuilder(String type, String title, String targetUserId, User currentUser) {
            this.builder = ReportDTO.builder()
                    .reportType(type)
                    .title(title)
                    .requestedForUserId(targetUserId)
                    .generatedByUserId(currentUser.getId().toHexString())
                    .generatedByRole(currentUser.getRole().name())
                    .generatedAt(LocalDateTime.now());
        }

        ReportBuilder dashboard(DashboardDTO dashboard) {
            builder.dashboard(dashboard);
            return this;
        }

        ReportBuilder statistics(StatisticsDTO statistics) {
            builder.statistics(statistics);
            return this;
        }

        ReportBuilder progress(ProgressDTO progress) {
            builder.progress(progress);
            return this;
        }

        ReportBuilder charts(List<ChartDTO> charts) {
            builder.charts(charts);
            return this;
        }

        ReportBuilder sections(Map<String, Object> sections) {
            builder.sections(sections);
            return this;
        }

        ReportDTO build() {
            return builder.build();
        }
    }

    private ReportBuilder baseReport(String type, String title, String targetUserId, User currentUser) {
        return new ReportBuilder(type, title, targetUserId, currentUser);
    }

    private DashboardDTO resolveAthleteDashboard(User currentUser, String athleteId) {
        if (currentUser.getRole() == Role.ATHLETE && Objects.equals(currentUser.getId().toHexString(), athleteId)) {
            return dashboardService.getAthleteDashboard(athleteId);
        }
        List<Session> sessions = workoutSessionRepository.findByAthleteId(toObjectId(athleteId));
        return DashboardDTO.builder()
                .userId(athleteId)
                .role(Role.ATHLETE.name())
                .completedSessions((int) sessions.stream().filter(this::isCompletedSession).count())
                .trainingVolume(round(sessions.stream().mapToDouble(session -> safeDouble(session.getTotalVolume())).sum()))
                .activeAlerts(0)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private Map<String, Object> toPersonalInformation(User athlete) {
        Map<String, Object> personalInfo = new LinkedHashMap<>();
        personalInfo.put("id", athlete.getId().toHexString());
        personalInfo.put("name", athlete.getFirstName() + " " + athlete.getLastName());
        personalInfo.put("email", athlete.getEmail());
        personalInfo.put("age", athlete.getAge());
        personalInfo.put("weight", athlete.getWeight());
        personalInfo.put("height", athlete.getHeight());
        return personalInfo;
    }

    private String resolveCurrentMesocycleName(ObjectId athleteId) {
        return mesocycleRepository.findByAthleteId(athleteId).stream()
                .filter(mesocycle -> mesocycle.getStatus() == MesocycleStatus.ACTIVE)
                .max(Comparator.comparing(Mesocycle::getCreatedAt))
                .map(Mesocycle::getName)
                .orElse("N/A");
    }

    private String resolveCurrentNutritionSummary(ObjectId athleteId) {
        return nutritionPlanRepository.findByAthleteId(athleteId).stream()
                .filter(plan -> Boolean.TRUE.equals(plan.getActive()))
                .max(Comparator.comparing(NutritionPlan::getCreatedAt))
                .map(plan -> plan.getGoal().name() + " / " + plan.getCalories() + " kcal")
                .orElse("N/A");
    }

    private Map<String, Object> resolveWorkoutSummary(ObjectId athleteId) {
        List<Session> sessions = workoutSessionRepository.findByAthleteId(athleteId).stream()
                .filter(this::isCompletedSession)
                .toList();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("completedSessions", sessions.size());
        summary.put("totalVolume", round(sessions.stream().mapToDouble(Session::getTotalVolume).sum()));
        summary.put("latestWorkoutDate", sessions.stream()
                .map(Session::getDate)
                .max(LocalDate::compareTo)
                .orElse(null));
        return summary;
    }

    private Map<String, Object> resolveFatigueAnalysis(DashboardDTO dashboard) {
        Map<String, Object> fatigue = new LinkedHashMap<>();
        fatigue.put("fatigueLevel", dashboard != null ? dashboard.getCurrentFatigueLevel() : "N/A");
        fatigue.put("recoveryScore", dashboard != null && dashboard.getRecoveryScore() != null
                ? dashboard.getRecoveryScore() : 0.0d);
        return fatigue;
    }

    private List<Map<String, Object>> buildNutritionComplianceHistory(List<NutritionPlan> plans) {
        LocalDate today = LocalDate.now();
        return plans.stream()
                .sorted(Comparator.comparing(NutritionPlan::getStartDate))
                .map(plan -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("startDate", plan.getStartDate());
                    row.put("endDate", plan.getEndDate());
                    row.put("goal", plan.getGoal().name());
                    row.put("active", plan.getActive());
                    row.put("compliant", !plan.getEndDate().isBefore(today));
                    return row;
                })
                .toList();
    }

    private ProgressDTO buildProgressDto(StatisticsDTO statistics, ChartDTO fatigueChart) {
        return ProgressDTO.builder()
                .strengthProgress(statistics.getEstimatedStrengthProgress())
                .volumeProgress(calculateVolumeProgress(statistics))
                .workoutConsistency(round((safeDouble(statistics.getAverageWeeklySessions()) / 7.0d) * 100.0d))
                .fatigueTrend(calculateChartTrend(fatigueChart))
                .weightTrend(statistics.getBodyWeightEvolution())
                .trainingFrequency(statistics.getAverageWeeklySessions())
                .nutritionAdherence(0.0d)
                .build();
    }

    private List<Map<String, Object>> buildWorkoutHistoryRows(List<Session> sessions) {
        return sessions.stream()
                .map(session -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("date", session.getDate());
                    row.put("durationMinutes", session.getDurationMinutes());
                    row.put("volume", round(session.getTotalVolume()));
                    row.put("estimatedOneRepMax", round(session.getEstimatedOneRepMax()));
                    row.put("completedExercises", session.getCompletedExercises().size());
                    return row;
                })
                .toList();
    }

    private int countPlannedExercises(Mesocycle mesocycle) {
        return mesocycle.getDays().stream()
                .mapToInt(day -> day.getExercises().size())
                .sum();
    }

    private void ensureCanGenerateAthleteScopedReport(User currentUser, ObjectId athleteId) {
        if (currentUser.getRole() == Role.NUTRITIONIST) {
            throw new UnauthorizedOperationException("Nutritionists may only generate nutrition reports.");
        }
        if (currentUser.getRole() == Role.ATHLETE) {
            if (!Objects.equals(currentUser.getId(), athleteId)) {
                throw new UnauthorizedOperationException("Athletes may only generate their own reports.");
            }
            return;
        }
        if (currentUser.getRole() == Role.COACH && !athleteAssignmentService.isAthleteAssignedToCoach(currentUser.getId(), athleteId)) {
            throw new UnauthorizedOperationException("Coaches may only generate reports for assigned athletes.");
        }
    }

    private void ensureCanGenerateNutritionReport(User currentUser, ObjectId athleteId) {
        if (currentUser.getRole() == Role.ATHLETE) {
            if (!Objects.equals(currentUser.getId(), athleteId)) {
                throw new UnauthorizedOperationException("Athletes may only generate their own reports.");
            }
            return;
        }
        if (currentUser.getRole() == Role.COACH) {
            if (!athleteAssignmentService.isAthleteAssignedToCoach(currentUser.getId(), athleteId)) {
                throw new UnauthorizedOperationException("Coaches may only generate reports for assigned athletes.");
            }
            return;
        }
        if (currentUser.getRole() == Role.NUTRITIONIST && !athleteAssignmentService.isAthleteAssignedToNutritionist(currentUser.getId(), athleteId)) {
            throw new UnauthorizedOperationException("Nutritionists may only generate nutrition reports for assigned athletes.");
        }
    }

    private void ensureCanGenerateMesocycleReport(User currentUser, Mesocycle mesocycle) {
        if (currentUser.getRole() == Role.NUTRITIONIST) {
            throw new UnauthorizedOperationException("Nutritionists cannot generate mesocycle reports.");
        }
        if (currentUser.getRole() == Role.ATHLETE && !Objects.equals(currentUser.getId(), mesocycle.getAthleteId())) {
            throw new UnauthorizedOperationException("Athletes may only generate reports for their own mesocycles.");
        }
        if (currentUser.getRole() == Role.COACH && !Objects.equals(currentUser.getId(), mesocycle.getCoachId())) {
            throw new UnauthorizedOperationException("Coaches may only generate their own mesocycle reports.");
        }
    }

    private void ensureCoachSelf(User currentUser, ObjectId coachId) {
        if (currentUser.getRole() != Role.COACH || !Objects.equals(currentUser.getId(), coachId)) {
            throw new UnauthorizedOperationException("Only authenticated coaches can generate coach reports.");
        }
    }

    private void ensureCanExport(User currentUser, ReportDTO report) {
        ObjectId targetAthleteId = toObjectId(report.getRequestedForUserId());
        if (currentUser.getRole() == Role.ATHLETE) {
            if (!Objects.equals(currentUser.getId(), targetAthleteId)) {
                throw new UnauthorizedOperationException("Athletes may export only their own reports.");
            }
            return;
        }
        if (currentUser.getRole() == Role.COACH) {
            if (TYPE_COACH.equals(report.getReportType())) {
                if (!Objects.equals(currentUser.getId().toHexString(), report.getRequestedForUserId())) {
                    throw new UnauthorizedOperationException("Coaches may export only their own coach reports.");
                }
                return;
            }
            if (!athleteAssignmentService.isAthleteAssignedToCoach(currentUser.getId(), targetAthleteId)) {
                throw new UnauthorizedOperationException("Coaches may export reports for assigned athletes only.");
            }
            return;
        }
        if (currentUser.getRole() == Role.NUTRITIONIST) {
            if (!TYPE_NUTRITION.equals(report.getReportType())) {
                throw new UnauthorizedOperationException("Nutritionists may export nutrition reports only.");
            }
            if (!athleteAssignmentService.isAthleteAssignedToNutritionist(currentUser.getId(), targetAthleteId)) {
                throw new UnauthorizedOperationException("Nutritionists may export only assigned nutrition reports.");
            }
            return;
        }
        throw new UnauthorizedOperationException("Role not authorized for report export.");
    }

    private byte[] buildPdfExport(ReportDTO report) {
        List<String> lines = flattenReportLines(report);
        String content = buildPdfContent(lines);
        StringBuilder builder = new StringBuilder();
        List<Integer> offsets = new ArrayList<>();
        builder.append("%PDF-1.4\n");
        appendPdfObject(builder, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");
        appendPdfObject(builder, offsets, 2, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
        appendPdfObject(builder, offsets, 3, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                + "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>");
        appendPdfObject(builder, offsets, 4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        offsets.add(byteLength(builder));
        builder.append("5 0 obj\n<< /Length ").append(content.getBytes(StandardCharsets.UTF_8).length)
                .append(" >>\nstream\n")
                .append(content)
                .append("\nendstream\nendobj\n");
        int xrefOffset = byteLength(builder);
        builder.append("xref\n0 6\n")
                .append("0000000000 65535 f \n");
        for (int offset : offsets) {
            builder.append(String.format("%010d 00000 n %n", offset));
        }
        builder.append("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n")
                .append(xrefOffset)
                .append("\n%%EOF");
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * PDF byte offsets (used in the xref table) must be measured in UTF-8
     * bytes, not chars - {@link StringBuilder#length()} would silently
     * produce a corrupt xref table for any report content containing
     * multi-byte characters (accented letters, etc.).
     */
    private int byteLength(StringBuilder builder) {
        return builder.toString().getBytes(StandardCharsets.UTF_8).length;
    }

    private byte[] buildXlsxExport(ReportDTO report) {
        List<String[]> rows = flattenReportRows(report);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            writeZipEntry(zipOutputStream, "[Content_Types].xml", contentTypesXml());
            writeZipEntry(zipOutputStream, "_rels/.rels", relsXml());
            writeZipEntry(zipOutputStream, "xl/workbook.xml", workbookXml());
            writeZipEntry(zipOutputStream, "xl/_rels/workbook.xml.rels", workbookRelsXml());
            writeZipEntry(zipOutputStream, "xl/worksheets/sheet1.xml", sheetXml(rows));
            zipOutputStream.finish();
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new BusinessRuleException("Failed to generate XLSX export.");
        }
    }

    private List<String> flattenReportLines(ReportDTO report) {
        List<String> lines = new ArrayList<>();
        lines.add("Title: " + report.getTitle());
        lines.add("Type: " + report.getReportType());
        lines.add("Generated At: " + report.getGeneratedAt());
        if (report.getSections() != null) {
            report.getSections().forEach((key, value) -> lines.add(key + ": " + value));
        }
        return lines;
    }

    private List<String[]> flattenReportRows(ReportDTO report) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Field", "Value"});
        rows.add(new String[]{"Title", report.getTitle()});
        rows.add(new String[]{"Type", report.getReportType()});
        rows.add(new String[]{"Generated At", String.valueOf(report.getGeneratedAt())});
        if (report.getSections() != null) {
            report.getSections().forEach((key, value) -> rows.add(new String[]{key, String.valueOf(value)}));
        }
        return rows;
    }

    private String buildPdfContent(List<String> lines) {
        StringBuilder content = new StringBuilder("BT /F1 11 Tf 40 800 Td ");
        boolean first = true;
        for (String line : lines) {
            if (!first) {
                content.append("T* ");
            }
            content.append("(").append(escapePdfText(line)).append(") Tj ");
            first = false;
        }
        content.append("ET");
        return content.toString();
    }

    private void appendPdfObject(StringBuilder builder, List<Integer> offsets, int number, String body) {
        offsets.add(byteLength(builder));
        builder.append(number).append(" 0 obj\n")
                .append(body)
                .append("\nendobj\n");
    }

    private void writeZipEntry(ZipOutputStream zipOutputStream, String name, String content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(name));
        zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }

    private String contentTypesXml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                </Types>
                """;
    }

    private String relsXml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """;
    }

    private String workbookXml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets>
                    <sheet name="Report" sheetId="1" r:id="rId1"/>
                  </sheets>
                </workbook>
                """;
    }

    private String workbookRelsXml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                </Relationships>
                """;
    }

    private String sheetXml(List<String[]> rows) {
        StringBuilder builder = new StringBuilder();
        builder.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                """);
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 1;
            builder.append("<row r=\"").append(rowNumber).append("\">")
                    .append(inlineCell("A", rowNumber, rows.get(i)[0]))
                    .append(inlineCell("B", rowNumber, rows.get(i)[1]))
                    .append("</row>");
        }
        builder.append("""
                  </sheetData>
                </worksheet>
                """);
        return builder.toString();
    }

    private String inlineCell(String column, int rowNumber, String value) {
        return "<c r=\"" + column + rowNumber + "\" t=\"inlineStr\"><is><t>"
                + escapeXml(value)
                + "</t></is></c>";
    }

    private String escapePdfText(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private double calculateVolumeProgress(StatisticsDTO statistics) {
        double weekly = safeDouble(statistics.getWeeklyTrainingVolume());
        double monthly = safeDouble(statistics.getMonthlyTrainingVolume());
        if (weekly == 0) {
            return 0.0d;
        }
        return round(((monthly - (weekly * 4.0d)) / (weekly * 4.0d)) * 100.0d);
    }

    private double calculateChartTrend(ChartDTO chart) {
        if (chart == null || chart.getValues() == null || chart.getValues().size() < 2) {
            return 0.0d;
        }
        double first = safeDouble(chart.getValues().get(0));
        double last = safeDouble(chart.getValues().get(chart.getValues().size() - 1));
        if (first == 0.0d) {
            return 0.0d;
        }
        return round(((last - first) / first) * 100.0d);
    }

    private boolean isCompletedSession(Session session) {
        return session != null
                && session.getCompletedExercises() != null
                && !session.getCompletedExercises().isEmpty();
    }

    private User getAthleteById(String athleteId) {
        User athlete = userRepository.findById(athleteId)
                .orElseThrow(() -> new ResourceNotFoundException("Athlete not found with id: " + athleteId));
        if (athlete.getRole() != Role.ATHLETE) {
            throw new BusinessRuleException("Referenced user is not an athlete.");
        }
        return athlete;
    }

    private User getCoachById(String coachId) {
        User coach = userRepository.findById(coachId)
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found with id: " + coachId));
        if (coach.getRole() != Role.COACH) {
            throw new BusinessRuleException("Referenced user is not a coach.");
        }
        return coach;
    }

    private User getAuthenticatedUser() {
        return authenticatedUserProvider.getAuthenticatedUser();
    }

    private void validateReportForExport(ReportDTO report, ExportFormat format) {
        if (report == null) {
            throw new ValidationException("Report is required.");
        }
        if (format == null) {
            throw new ValidationException("Export format is required.");
        }
        validateIdentifier(report.getReportType(), "Report type is required.");
        validateIdentifier(report.getRequestedForUserId(), "Report target user id is required.");
    }

    private void validateIdentifier(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
    }

    private ObjectId toObjectId(String value) {
        try {
            return new ObjectId(value);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("Invalid identifier format: " + value);
        }
    }

    private Double safeDouble(Double value) {
        return value != null ? value : 0.0d;
    }

    private double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }
}
