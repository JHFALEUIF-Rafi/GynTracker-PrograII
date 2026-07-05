package com.gymtracker.service;

import com.gymtracker.dto.dashboard.ReportDTO;
import com.gymtracker.enums.ExportFormat;
import java.util.concurrent.CompletableFuture;

/**
 * Service contract for report generation data.
 */
public interface ReportService {

    ReportDTO generateAthleteReport(String athleteId);

    ReportDTO generateCoachReport(String coachId);

    ReportDTO generateNutritionReport(String athleteId);

    ReportDTO generateProgressReport(String athleteId);

    ReportDTO generateWorkoutHistoryReport(String athleteId);

    ReportDTO generateMesocycleReport(String mesocycleId);

    CompletableFuture<byte[]> exportReport(ReportDTO report, ExportFormat format);
}
