package com.gymtracker.validation;

import com.gymtracker.dto.mesocycle.MesocycleRequestDTO;
import com.gymtracker.dto.mesocycle.MesocycleWorkoutDayDTO;
import com.gymtracker.dto.mesocycle.MesocycleWorkoutExerciseDTO;
import com.gymtracker.exception.MesocycleValidationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

/**
 * Centralized validation for mesocycle data.
 */
@Component
public class MesocycleValidator extends BaseValidator {

    public MesocycleValidator(Validator validator) {
        super(validator);
    }

    public void validateCreate(MesocycleRequestDTO requestDTO) {
        validateMesocycleRequest(requestDTO);
    }

    public void validateUpdate(MesocycleRequestDTO requestDTO) {
        validateMesocycleRequest(requestDTO);
    }

    public void validateDelete(String mesocycleId) {
        requireCondition(hasText(mesocycleId), "Mesocycle id is required.", MesocycleValidationException::new);
    }

    public void validateAssignment(String coachId, String athleteId) {
        requireCondition(hasText(coachId), "Coach id is required.", MesocycleValidationException::new);
        requireCondition(hasText(athleteId), "Athlete id is required.", MesocycleValidationException::new);
    }

    private void validateMesocycleRequest(MesocycleRequestDTO requestDTO) {
        requireCondition(requestDTO != null, "Mesocycle request is required.", MesocycleValidationException::new);
        validateBean(requestDTO, MesocycleValidationException::new);

        requireCondition(requestDTO.getDurationWeeks() > 0, "Mesocycle weeks must be greater than zero.",
                MesocycleValidationException::new);
        requireCondition(requestDTO.getDays() != null && !requestDTO.getDays().isEmpty(),
                "Mesocycle must contain at least one workout day.", MesocycleValidationException::new);
        requireCondition(hasText(requestDTO.getCoachId()), "Coach id is required.", MesocycleValidationException::new);
        requireCondition(hasText(requestDTO.getAthleteId()), "Athlete id is required.",
                MesocycleValidationException::new);

        for (MesocycleWorkoutDayDTO day : requestDTO.getDays()) {
            requireCondition(day.getExercises() != null && !day.getExercises().isEmpty(),
                    "Every workout day must include at least one exercise.", MesocycleValidationException::new);
            for (MesocycleWorkoutExerciseDTO exercise : day.getExercises()) {
                requireCondition(hasText(exercise.getExerciseId()), "Exercise id is required.",
                        MesocycleValidationException::new);
            }
        }
    }
}
