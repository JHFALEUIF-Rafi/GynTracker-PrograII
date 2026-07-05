package com.gymtracker.validation;

import com.gymtracker.dto.alert.AlertDTO;
import com.gymtracker.exception.AlertValidationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

/**
 * Centralized validation for alert data.
 */
@Component
public class AlertValidator extends BaseValidator {

    public AlertValidator(Validator validator) {
        super(validator);
    }

    public void validateCreate(AlertDTO alertDTO) {
        validateAlert(alertDTO);
    }

    public void validateUpdate(AlertDTO alertDTO) {
        validateAlert(alertDTO);
    }

    public void validateDelete(String alertId) {
        requireCondition(hasText(alertId), "Alert id is required.", AlertValidationException::new);
    }

    private void validateAlert(AlertDTO alertDTO) {
        requireCondition(alertDTO != null, "Alert payload is required.", AlertValidationException::new);
        requireCondition(alertDTO.getStatus() != null, "Alert status is required.", AlertValidationException::new);
        requireCondition(hasText(alertDTO.getAthleteId()), "Alert athlete id is required.", AlertValidationException::new);
        requireCondition(hasText(alertDTO.getCoachId()), "Alert coach id is required.", AlertValidationException::new);
        requireCondition(hasText(alertDTO.getMessage()), "Alert reason is required.", AlertValidationException::new);
    }
}
