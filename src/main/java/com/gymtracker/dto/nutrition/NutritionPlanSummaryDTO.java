package com.gymtracker.dto.nutrition;

import com.gymtracker.enums.NutritionGoal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lightweight nutrition plan representation for list views.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NutritionPlanSummaryDTO {

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
    private Boolean active;
    private LocalDate startDate;
    private LocalDate endDate;
}
