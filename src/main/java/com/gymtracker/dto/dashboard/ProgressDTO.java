package com.gymtracker.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for progress indicator values.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressDTO {

    private Double strengthProgress;
    private Double volumeProgress;
    private Double workoutConsistency;
    private Double fatigueTrend;
    private Double weightTrend;
    private Double trainingFrequency;
    private Double nutritionAdherence;
}
