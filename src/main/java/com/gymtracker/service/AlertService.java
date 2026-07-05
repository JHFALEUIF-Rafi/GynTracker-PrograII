package com.gymtracker.service;

import com.gymtracker.dto.alert.AlertDTO;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service contract for fatigue alert operations.
 */
public interface AlertService {

    CompletableFuture<AlertDTO> generateFatigueAlert(String athleteId);

    CompletableFuture<AlertDTO> generateMissedWorkoutAlert(String athleteId);

    CompletableFuture<AlertDTO> generateNutritionPlanExpiredAlert(String athleteId);

    CompletableFuture<AlertDTO> generateMesocycleCompletedAlert(String athleteId);

    CompletableFuture<AlertDTO> generatePerformanceDropAlert(String athleteId);

    List<AlertDTO> getAlertsByAthlete(String athleteId);

    List<AlertDTO> getAlertsByCoach(String coachId);

    AlertDTO acknowledgeAlert(String alertId);

    AlertDTO resolveAlert(String alertId);

    int deleteResolvedAlerts();

    /**
     * Returns the alerts visible to the authenticated caller: an Athlete sees
     * their own, a Coach sees the ones assigned to them, and a Nutritionist
     * sees only nutrition-related alerts across all athletes.
     */
    List<AlertDTO> getAlertsForCurrentUser();
}
