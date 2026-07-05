package com.gymtracker.dto.mesocycle;

import com.gymtracker.enums.MesocycleStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for creating or updating mesocycles.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesocycleRequestDTO {

    @NotNull
    private String coachId;

    @NotNull
    private String athleteId;

    @NotBlank
    private String name;

    @NotNull
    @Positive
    private Integer durationWeeks;

    @NotNull
    @Min(1)
    @Max(10)
    private Integer targetRpe;

    private String notes;

    @NotNull
    private MesocycleStatus status;

    @NotEmpty
    @Valid
    private List<MesocycleWorkoutDayDTO> days;
}
