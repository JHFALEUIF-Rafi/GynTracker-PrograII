package com.gymtracker.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymtracker.dto.nutrition.NutritionPlanRequestDTO;
import com.gymtracker.enums.NutritionGoal;
import com.gymtracker.exception.NutritionPlanValidationException;
import jakarta.validation.Validation;
import java.time.LocalDate;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for NutritionPlanValidator using a real Bean Validation Validator.
 */
class NutritionPlanValidatorTest {

    private final NutritionPlanValidator validator =
            new NutritionPlanValidator(Validation.buildDefaultValidatorFactory().getValidator());

    private NutritionPlanRequestDTO.NutritionPlanRequestDTOBuilder validRequestBuilder() {
        return NutritionPlanRequestDTO.builder()
                .athleteId(new ObjectId().toHexString())
                .nutritionistId(new ObjectId().toHexString())
                .goal(NutritionGoal.CUTTING)
                .calories(2200)
                .protein(160.0)
                .carbohydrates(200.0)
                .fat(60.0)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .active(true);
    }

    @Test
    void validateCreateAcceptsValidRequest() {
        assertThatCode(() -> validator.validateCreate(validRequestBuilder().build())).doesNotThrowAnyException();
    }

    @Test
    void validateCreateRejectsNullRequest() {
        assertThatThrownBy(() -> validator.validateCreate(null)).isInstanceOf(NutritionPlanValidationException.class);
    }

    @Test
    void validateCreateRejectsEndDateBeforeStartDate() {
        NutritionPlanRequestDTO requestDTO = validRequestBuilder()
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().minusDays(1))
                .build();

        assertThatThrownBy(() -> validator.validateCreate(requestDTO)).isInstanceOf(NutritionPlanValidationException.class);
    }

    @Test
    void validateCreateRejectsNonPositiveCalories() {
        NutritionPlanRequestDTO requestDTO = validRequestBuilder().calories(0).build();

        assertThatThrownBy(() -> validator.validateCreate(requestDTO)).isInstanceOf(NutritionPlanValidationException.class);
    }

    @Test
    void validateCreateRejectsNegativeProtein() {
        NutritionPlanRequestDTO requestDTO = validRequestBuilder().protein(-1.0).build();

        assertThatThrownBy(() -> validator.validateCreate(requestDTO)).isInstanceOf(NutritionPlanValidationException.class);
    }

    @Test
    void validateAssignmentRejectsBlankIds() {
        assertThatThrownBy(() -> validator.validateAssignment("", "")).isInstanceOf(NutritionPlanValidationException.class);
    }

    @Test
    void validateSingleActivePlanRejectsWhenAnotherActiveExists() {
        assertThatThrownBy(() -> validator.validateSingleActivePlan(true)).isInstanceOf(NutritionPlanValidationException.class);
    }

    @Test
    void validateDeleteRejectsBlankId() {
        assertThatThrownBy(() -> validator.validateDelete(" ")).isInstanceOf(NutritionPlanValidationException.class);
    }
}
