package com.gymtracker.validation;

import com.gymtracker.dto.athlete.AthleteRequestDTO;
import com.gymtracker.exception.AthleteValidationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

/**
 * Centralized validation for athlete data.
 */
@Component
public class AthleteValidator extends BaseValidator {

    public AthleteValidator(Validator validator) {
        super(validator);
    }

    public void validateCreate(AthleteRequestDTO requestDTO) {
        validateAthleteRequest(requestDTO);
    }

    public void validateUpdate(AthleteRequestDTO requestDTO) {
        validateAthleteRequest(requestDTO);
    }

    public void validateDelete(String athleteId) {
        requireCondition(hasText(athleteId), "Athlete id is required.", AthleteValidationException::new);
    }

    public void validateEmailUniqueness(boolean emailAlreadyExists) {
        requireCondition(!emailAlreadyExists, "Athlete email already exists.", AthleteValidationException::new);
    }

    private void validateAthleteRequest(AthleteRequestDTO requestDTO) {
        requireCondition(requestDTO != null, "Athlete request is required.", AthleteValidationException::new);
        validateBean(requestDTO, AthleteValidationException::new);

        requireCondition(requestDTO.getAge() >= 14 && requestDTO.getAge() <= 100,
                "Athlete age must be between 14 and 100.", AthleteValidationException::new);
        requireCondition(requestDTO.getWeight() > 0, "Athlete weight must be greater than zero.",
                AthleteValidationException::new);
        requireCondition(requestDTO.getHeight() > 0, "Athlete height must be greater than zero.",
                AthleteValidationException::new);
    }
}
