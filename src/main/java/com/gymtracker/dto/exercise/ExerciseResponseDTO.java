package com.gymtracker.dto.exercise;

import com.gymtracker.enums.Difficulty;
import com.gymtracker.enums.Equipment;
import com.gymtracker.enums.ExerciseStatus;
import com.gymtracker.enums.ExerciseType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response payload for exercise operations.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseResponseDTO {

    private String id;
    private String name;
    private String primaryMuscle;
    private List<String> secondaryMuscles;
    private ExerciseType exerciseType;
    private String description;
    private Difficulty difficulty;
    private Equipment equipment;
    private ExerciseStatus status;
}
