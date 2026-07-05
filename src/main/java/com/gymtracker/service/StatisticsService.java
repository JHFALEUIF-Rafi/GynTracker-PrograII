package com.gymtracker.service;

import com.gymtracker.dto.dashboard.ChartDTO;
import com.gymtracker.dto.dashboard.StatisticsDTO;

/**
 * Service contract for statistics and KPI computations.
 */
public interface StatisticsService {

    StatisticsDTO getAthleteStatistics(String athleteId);

    StatisticsDTO getCoachStatistics(String coachId);

    StatisticsDTO getNutritionistStatistics(String nutritionistId);

    ChartDTO getWorkoutVolumeChart(String athleteId);

    ChartDTO getOneRepMaxChart(String athleteId);

    ChartDTO getFatigueChart(String athleteId);
}
