package com.gymtracker.service;

import com.gymtracker.dto.dashboard.ChartDTO;
import com.gymtracker.dto.fatigue.FatigueDTO;
import com.gymtracker.enums.FatigueLevel;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service contract for athlete fatigue analysis.
 */
public interface FatigueService {

    CompletableFuture<FatigueDTO> calculateFatigue(String athleteId);

    FatigueLevel getCurrentFatigueLevel(String athleteId);

    List<FatigueDTO> getFatigueHistory(String athleteId);

    Double evaluateWorkoutLoad(String workoutSessionId);

    Double calculateWeeklyTrainingLoad(String athleteId);

    Double calculateRecoveryScore(String athleteId);

    Double calculateFatigueScore(String athleteId);

    FatigueLevel calculateFatigueLevel(String athleteId);

    ChartDTO getFatigueHistoryChart(String athleteId);
}
