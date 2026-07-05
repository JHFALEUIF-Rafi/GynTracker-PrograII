package com.gymtracker.validation;

import com.gymtracker.dto.nutrition.NutritionPlanRequestDTO;
import com.gymtracker.exception.NutritionPlanValidationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

/**
 * Centralized validation for nutrition plan data.
 */
@Component
public class NutritionPlanValidator extends BaseValidator {

    public NutritionPlanValidator(Validator validator) {
        super(validator);
    }

    public void validateCreate(NutritionPlanRequestDTO requestDTO) {
        validateNutritionPlanRequest(requestDTO);
    }

    public void validateUpdate(NutritionPlanRequestDTO requestDTO) {
        validateNutritionPlanRequest(requestDTO);
    }

    public void validateDelete(String nutritionPlanId) {
        requireCondition(hasText(nutritionPlanId), "Nutrition plan id is required.",
                NutritionPlanValidationException::new);
    }

    public void validateAssignment(String athleteId, String nutritionistId) {
        requireCondition(hasText(athleteId), "Athlete id is required.", NutritionPlanValidationException::new);
        requireCondition(hasText(nutritionistId), "Nutritionist id is required.", NutritionPlanValidationException::new);
    }

    public void validateSingleActivePlan(boolean hasAnotherActivePlan) {
        requireCondition(!hasAnotherActivePlan, "Athlete already has an active nutrition plan.",
                NutritionPlanValidationException::new);
    }

    private void validateNutritionPlanRequest(NutritionPlanRequestDTO requestDTO) {
        requireCondition(requestDTO != null, "Nutrition plan request is required.", NutritionPlanValidationException::new);
        validateBean(requestDTO, NutritionPlanValidationException::new);

        requireCondition(!requestDTO.getEndDate().isBefore(requestDTO.getStartDate()),
                "Nutrition plan end date cannot be before start date.", NutritionPlanValidationException::new);
        requireCondition(requestDTO.getCalories() > 0, "Calories must be greater than zero.",
                NutritionPlanValidationException::new);
        requireCondition(requestDTO.getProtein() >= 0, "Protein must be greater than or equal to zero.",
                NutritionPlanValidationException::new);
        requireCondition(requestDTO.getCarbohydrates() >= 0,
                "Carbohydrates must be greater than or equal to zero.", NutritionPlanValidationException::new);
        requireCondition(requestDTO.getFat() >= 0, "Fat must be greater than or equal to zero.",
                NutritionPlanValidationException::new);
    }
}
