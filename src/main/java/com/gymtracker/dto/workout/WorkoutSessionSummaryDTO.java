package com.gymtracker.dto.workout;

import com.gymtracker.enums.WorkoutStatus;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lightweight workout session representation for history lists.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutSessionSummaryDTO {

    private String id;
    private LocalDate date;
    private Integer durationMinutes;
    private Double totalVolume;
    private Double estimatedOneRepMax;
    private WorkoutStatus status;
}
