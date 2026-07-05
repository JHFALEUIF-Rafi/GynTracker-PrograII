package com.gymtracker.validation;

import com.gymtracker.dto.exercise.ExerciseRequestDTO;
import com.gymtracker.exception.ExerciseValidationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

/**
 * Centralized validation for exercise data.
 */
@Component
public class ExerciseValidator extends BaseValidator {

    public ExerciseValidator(Validator validator) {
        super(validator);
    }

    public void validateCreate(ExerciseRequestDTO requestDTO) {
        validateExerciseRequest(requestDTO);
    }

    public void validateUpdate(ExerciseRequestDTO requestDTO) {
        validateExerciseRequest(requestDTO);
    }

    public void validateDelete(String exerciseId) {
        requireCondition(hasText(exerciseId), "Exercise id is required.", ExerciseValidationException::new);
    }

    public void validateUniqueName(boolean nameAlreadyExists) {
        requireCondition(!nameAlreadyExists, "Exercise name already exists.", ExerciseValidationException::new);
    }

    private void validateExerciseRequest(ExerciseRequestDTO requestDTO) {
        requireCondition(requestDTO != null, "Exercise request is required.", ExerciseValidationException::new);
        validateBean(requestDTO, ExerciseValidationException::new);

        requireCondition(requestDTO.getExerciseType() != null, "Exercise type is required.",
                ExerciseValidationException::new);
        requireCondition(requestDTO.getDifficulty() != null, "Exercise difficulty is required.",
                ExerciseValidationException::new);
        requireCondition(requestDTO.getEquipment() != null, "Exercise equipment is required.",
                ExerciseValidationException::new);
    }
}
