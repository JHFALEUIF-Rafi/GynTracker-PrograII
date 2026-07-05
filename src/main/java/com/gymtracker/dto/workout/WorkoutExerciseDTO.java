package com.gymtracker.dto.workout;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO representing one exercise within a workout session.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutExerciseDTO {

    private String exerciseId;
    private Integer executionOrder;
    private Double targetWeight;
    private Integer targetRepetitions;
    private Integer targetRpe;
    private List<WorkoutSetDTO> sets;
}
