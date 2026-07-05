package com.gymtracker.dto.workout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO representing one-rep-max progression comparison.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OneRepMaxComparisonDTO {

    private String athleteId;
    private String exerciseId;
    private Double latestValue;
    private Double previousValue;
    private Double absoluteChange;
    private Double percentageChange;
    private String trend;
}
