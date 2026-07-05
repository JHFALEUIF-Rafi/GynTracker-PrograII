package com.gymtracker.dto.workout;

import com.gymtracker.enums.WorkoutStatus;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response payload for workout session operations.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutSessionResponseDTO {

    private String id;
    private String athleteId;
    private String mesocycleId;
    private LocalDate date;
    private Integer durationMinutes;
    private Double totalVolume;
    private Double estimatedOneRepMax;
    private Double fatigueScore;
    private WorkoutStatus status;
    private List<WorkoutExerciseDTO> completedExercises;
}
