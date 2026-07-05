package com.gymtracker.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymtracker.dto.exercise.ExerciseRequestDTO;
import com.gymtracker.enums.Difficulty;
import com.gymtracker.enums.Equipment;
import com.gymtracker.enums.ExerciseType;
import com.gymtracker.exception.ExerciseValidationException;
import jakarta.validation.Validation;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ExerciseValidator using a real Bean Validation Validator.
 */
class ExerciseValidatorTest {

    private final ExerciseValidator validator = new ExerciseValidator(Validation.buildDefaultValidatorFactory().getValidator());

    private ExerciseRequestDTO.ExerciseRequestDTOBuilder validRequestBuilder() {
        return ExerciseRequestDTO.builder()
                .name("Bench Press")
                .primaryMuscle("Chest")
                .secondaryMuscles(List.of("Triceps"))
                .exerciseType(ExerciseType.STRENGTH)
                .difficulty(Difficulty.INTERMEDIATE)
                .equipment(Equipment.BARBELL)
                .description("Description");
    }

    @Test
    void validateCreateAcceptsValidRequest() {
        assertThatCode(() -> validator.validateCreate(validRequestBuilder().build())).doesNotThrowAnyException();
    }

    @Test
    void validateCreateRejectsNullRequest() {
        assertThatThrownBy(() -> validator.validateCreate(null)).isInstanceOf(ExerciseValidationException.class);
    }

    @Test
    void validateCreateRejectsBlankName() {
        ExerciseRequestDTO requestDTO = validRequestBuilder().name(" ").build();

        assertThatThrownBy(() -> validator.validateCreate(requestDTO)).isInstanceOf(ExerciseValidationException.class);
    }

    @Test
    void validateCreateRejectsMissingExerciseType() {
        ExerciseRequestDTO requestDTO = validRequestBuilder().exerciseType(null).build();

        assertThatThrownBy(() -> validator.validateCreate(requestDTO)).isInstanceOf(ExerciseValidationException.class);
    }

    @Test
    void validateCreateRejectsEmptySecondaryMuscles() {
        ExerciseRequestDTO requestDTO = validRequestBuilder().secondaryMuscles(List.of()).build();

        assertThatThrownBy(() -> validator.validateCreate(requestDTO)).isInstanceOf(ExerciseValidationException.class);
    }

    @Test
    void validateDeleteRejectsBlankId() {
        assertThatThrownBy(() -> validator.validateDelete("")).isInstanceOf(ExerciseValidationException.class);
    }

    @Test
    void validateUniqueNameRejectsWhenAlreadyExists() {
        assertThatThrownBy(() -> validator.validateUniqueName(true)).isInstanceOf(ExerciseValidationException.class);
    }
}
