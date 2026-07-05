package com.gymtracker.service;

import com.gymtracker.dto.dashboard.DashboardDTO;

/**
 * Service contract for role-based dashboard data.
 */
public interface DashboardService {

    DashboardDTO getAthleteDashboard(String athleteId);

    DashboardDTO getCoachDashboard(String coachId);

    DashboardDTO getNutritionistDashboard(String nutritionistId);

    DashboardDTO refreshDashboard(String userId);

    DashboardDTO getDashboardSummary(String userId);
}
