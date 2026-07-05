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
 * Detailed nutrition plan representation for detail screens.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NutritionPlanDetailDTO {

    private String id;
    private String athleteId;
    private String athleteName;
    private String nutritionistId;
    private String nutritionistName;
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
