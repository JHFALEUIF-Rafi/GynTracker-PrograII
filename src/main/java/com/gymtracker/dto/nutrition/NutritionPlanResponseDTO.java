package com.gymtracker.dto.nutrition;

import com.gymtracker.enums.NutritionGoal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response payload for nutrition plan operations.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NutritionPlanResponseDTO {

    private String id;
    private String athleteId;
    private String nutritionistId;
    private NutritionGoal goal;
    private Integer calories;
    private Double protein;
    private Double carbohydrates;
    private Double fat;
    private String observations;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean active;
    private LocalDateTime createdAt;
}
