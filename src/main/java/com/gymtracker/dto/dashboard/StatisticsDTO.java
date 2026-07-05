package com.gymtracker.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for statistics and KPI values.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsDTO {

    private Double weeklyTrainingVolume;
    private Double monthlyTrainingVolume;
    private Double averageRpe;
    private Double averageWorkoutDuration;
    private Double averageWeeklySessions;
    private Double estimatedStrengthProgress;
    private Double bodyWeightEvolution;
    private Integer completedMesocycles;
    private Integer completedNutritionPlans;
}
