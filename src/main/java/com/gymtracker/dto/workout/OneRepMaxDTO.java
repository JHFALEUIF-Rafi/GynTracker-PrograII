package com.gymtracker.dto.workout;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO representing a one-rep-max estimation for one athlete and one exercise.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OneRepMaxDTO {

    private String athleteId;
    private String exerciseId;
    private String exerciseName;
    private String sessionId;
    private LocalDate date;
    private Double estimatedOneRepMax;
    private String formula;
}
