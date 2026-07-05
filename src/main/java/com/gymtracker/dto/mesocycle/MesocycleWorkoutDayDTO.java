package com.gymtracker.dto.mesocycle;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Planned training day DTO for mesocycle requests and responses.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesocycleWorkoutDayDTO {

    @NotBlank
    private String dayName;

    @NotEmpty
    @Valid
    private List<MesocycleWorkoutExerciseDTO> exercises;
}
