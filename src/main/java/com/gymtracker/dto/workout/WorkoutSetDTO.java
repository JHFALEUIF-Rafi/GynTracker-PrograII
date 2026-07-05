package com.gymtracker.dto.workout;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO representing one workout set.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutSetDTO {

    private Double weight;
    private Integer repetitions;
    private Integer rpe;
    private LocalDateTime completionTime;
}
