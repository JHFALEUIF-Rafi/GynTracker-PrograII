package com.gymtracker.dto.exercise;

import com.gymtracker.enums.Difficulty;
import com.gymtracker.enums.Equipment;
import com.gymtracker.enums.ExerciseStatus;
import com.gymtracker.enums.ExerciseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lightweight exercise representation for catalog lists.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseSummaryDTO {

    private String id;
    private String name;
    private String primaryMuscle;
    private ExerciseType exerciseType;
    private Difficulty difficulty;
    private Equipment equipment;
    private ExerciseStatus status;
}
