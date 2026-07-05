package com.gymtracker.dto.dashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for aggregated dashboard data.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {

    private String userId;
    private String role;

    // Athlete dashboard
    private Double currentWeight;
    private String currentActiveNutritionPlan;
    private Double estimatedOneRepMax;
    private Double trainingVolume;
    private String currentMesocycle;
    private String currentFatigueLevel;
    private Double recoveryScore;
    private Double weeklyTrainingVolume;
    private Integer activeAlerts;
    private Integer completedSessions;
    private LocalDate lastWorkoutDate;

    // Coach dashboard
    private Integer assignedAthletes;
    private Integer activeMesocycles;
    private Integer athletesWithHighFatigue;
    private Integer pendingAlerts;
    private Integer weeklySessions;
    private Double performanceTrend;

    // Nutritionist dashboard
    private Integer activeNutritionPlans;
    private Integer expiredPlans;
    private Integer nutritionAlerts;

    private LocalDateTime generatedAt;
}
