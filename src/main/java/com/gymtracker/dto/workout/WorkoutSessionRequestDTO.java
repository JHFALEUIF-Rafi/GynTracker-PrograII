package com.gymtracker.dto.workout;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for registering workout sessions.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutSessionRequestDTO {

    @NotNull
    private String athleteId;

    @NotNull
    private String mesocycleId;

    @NotNull
    private LocalDate date;

    @NotNull
    @Positive
    private Integer durationMinutes;

    @NotEmpty
    @Valid
    private List<WorkoutExerciseDTO> completedExercises;
}
