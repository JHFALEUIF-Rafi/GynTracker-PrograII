package com.gymtracker.validation;

import com.gymtracker.dto.workout.WorkoutExerciseDTO;
import com.gymtracker.dto.workout.WorkoutSessionRequestDTO;
import com.gymtracker.dto.workout.WorkoutSetDTO;
import com.gymtracker.exception.WorkoutValidationException;
import jakarta.validation.Validator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Centralized validation for workout session data.
 */
@Component
public class WorkoutValidator extends BaseValidator {

    private static final int MIN_RPE = 1;
    private static final int MAX_RPE = 10;

    public WorkoutValidator(Validator validator) {
        super(validator);
    }

    public void validateCreate(WorkoutSessionRequestDTO requestDTO) {
        validateWorkout(requestDTO);
    }

    public void validateUpdate(WorkoutSessionRequestDTO requestDTO) {
        validateWorkout(requestDTO);
    }

    public void validateDelete(String workoutSessionId) {
        requireCondition(hasText(workoutSessionId), "Workout session id is required.", WorkoutValidationException::new);
    }

    public void validateWorkout(WorkoutSessionRequestDTO requestDTO) {
        requireCondition(requestDTO != null, "Workout session request is required.", WorkoutValidationException::new);
        validateBean(requestDTO, WorkoutValidationException::new);

        requireCondition(requestDTO.getDurationMinutes() > 0, "Workout duration must be greater than zero.",
                WorkoutValidationException::new);
        requireCondition(requestDTO.getCompletedExercises() != null && !requestDTO.getCompletedExercises().isEmpty(),
                "Completed workout must contain exercises.", WorkoutValidationException::new);

        for (WorkoutExerciseDTO exercise : requestDTO.getCompletedExercises()) {
            validateExercise(exercise);
        }
    }

    private void validateExercise(WorkoutExerciseDTO exercise) {
        requireCondition(exercise != null, "Workout exercise is required.", WorkoutValidationException::new);
        requireCondition(hasText(exercise.getExerciseId()), "Workout exercise id is required.",
                WorkoutValidationException::new);
        requireCondition(exercise.getSets() != null && !exercise.getSets().isEmpty(),
                "Workout exercise must contain sets.", WorkoutValidationException::new);

        List<WorkoutSetDTO> sets = exercise.getSets();
        for (WorkoutSetDTO set : sets) {
            validateSet(set);
        }
    }

    private void validateSet(WorkoutSetDTO set) {
        requireCondition(set != null, "Workout set is required.", WorkoutValidationException::new);
        requireCondition(set.getWeight() != null && set.getWeight() > 0, "Workout set weight must be greater than zero.",
                WorkoutValidationException::new);
        requireCondition(set.getRepetitions() != null && set.getRepetitions() > 0,
                "Workout set repetitions must be greater than zero.", WorkoutValidationException::new);
        requireCondition(set.getRpe() != null && set.getRpe() >= MIN_RPE && set.getRpe() <= MAX_RPE,
                "Workout set RPE must be between 1 and 10.", WorkoutValidationException::new);
    }
}
