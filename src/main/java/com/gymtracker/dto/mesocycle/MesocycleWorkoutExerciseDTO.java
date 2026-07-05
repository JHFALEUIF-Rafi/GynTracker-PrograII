package com.gymtracker.dto.mesocycle;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Planned exercise DTO within a mesocycle day.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesocycleWorkoutExerciseDTO {

    @NotNull
    private String exerciseId;

    @NotNull
    @Positive
    private Integer sets;

    @NotNull
    @Positive
    private Integer repetitions;

    @NotNull
    @PositiveOrZero
    private Double targetWeight;

    @NotNull
    @Min(1)
    @Max(10)
    private Integer targetRpe;
}
