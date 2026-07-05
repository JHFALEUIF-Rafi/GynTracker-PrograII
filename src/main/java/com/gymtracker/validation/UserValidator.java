package com.gymtracker.validation;

import com.gymtracker.dto.user.ChangePasswordRequestDTO;
import com.gymtracker.dto.user.UserProfileUpdateDTO;
import com.gymtracker.exception.ValidationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

/**
 * Centralized validation for generic user profile and password data.
 */
@Component
public class UserValidator extends BaseValidator {

    public UserValidator(Validator validator) {
        super(validator);
    }

    public void validateProfileUpdate(UserProfileUpdateDTO requestDTO) {
        requireCondition(requestDTO != null, "Profile update request is required.", ValidationException::new);
        validateBean(requestDTO, ValidationException::new);
    }

    public void validatePasswordChange(ChangePasswordRequestDTO requestDTO) {
        requireCondition(requestDTO != null, "Password change request is required.", ValidationException::new);
        validateBean(requestDTO, ValidationException::new);
        requireCondition(requestDTO.getNewPassword().equals(requestDTO.getConfirmNewPassword()),
                "New password and confirmation do not match.", ValidationException::new);
        requireCondition(!requestDTO.getNewPassword().equals(requestDTO.getCurrentPassword()),
                "New password must be different from the current password.", ValidationException::new);
    }
}
