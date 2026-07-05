package com.gymtracker.dto.exercise;

import com.gymtracker.enums.Difficulty;
import com.gymtracker.enums.Equipment;
import com.gymtracker.enums.ExerciseStatus;
import com.gymtracker.enums.ExerciseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for creating or updating exercises.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseRequestDTO {

    @NotBlank
    private String name;

    @NotBlank
    private String primaryMuscle;

    @NotEmpty
    private List<@NotBlank String> secondaryMuscles;

    @NotNull
    private ExerciseType exerciseType;

    @NotBlank
    private String description;

    private Difficulty difficulty;

    private Equipment equipment;

    private ExerciseStatus status;
}
