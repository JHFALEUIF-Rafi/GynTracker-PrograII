package com.gymtracker.dto.nutrition;

import com.gymtracker.enums.NutritionGoal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for creating or updating nutrition plans.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NutritionPlanRequestDTO {

    @NotNull
    private String athleteId;

    @NotNull
    private String nutritionistId;

    @NotNull
    private NutritionGoal goal;

    @NotNull
    @Positive
    private Integer calories;

    @NotNull
    @PositiveOrZero
    private Double protein;

    @NotNull
    @PositiveOrZero
    private Double carbohydrates;

    @NotNull
    @PositiveOrZero
    private Double fat;

    @Size(max = 500)
    private String observations;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    private Boolean active;
}
