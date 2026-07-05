package com.gymtracker.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymtracker.dto.user.ChangePasswordRequestDTO;
import com.gymtracker.dto.user.UserProfileUpdateDTO;
import com.gymtracker.exception.ValidationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for UserValidator using a real Bean Validation Validator.
 */
class UserValidatorTest {

    private final UserValidator validator = new UserValidator(Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void validateProfileUpdateAcceptsValidRequest() {
        UserProfileUpdateDTO requestDTO = UserProfileUpdateDTO.builder().firstName("Jane").lastName("Doe").build();

        assertThatCode(() -> validator.validateProfileUpdate(requestDTO)).doesNotThrowAnyException();
    }

    @Test
    void validateProfileUpdateRejectsNullRequest() {
        assertThatThrownBy(() -> validator.validateProfileUpdate(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void validateProfileUpdateRejectsBlankFirstName() {
        UserProfileUpdateDTO requestDTO = UserProfileUpdateDTO.builder().firstName(" ").lastName("Doe").build();

        assertThatThrownBy(() -> validator.validateProfileUpdate(requestDTO)).isInstanceOf(ValidationException.class);
    }

    private ChangePasswordRequestDTO.ChangePasswordRequestDTOBuilder validPasswordChangeBuilder() {
        return ChangePasswordRequestDTO.builder()
                .currentPassword("current-password")
                .newPassword("brand-new-password")
                .confirmNewPassword("brand-new-password");
    }

    @Test
    void validatePasswordChangeAcceptsValidRequest() {
        assertThatCode(() -> validator.validatePasswordChange(validPasswordChangeBuilder().build()))
                .doesNotThrowAnyException();
    }

    @Test
    void validatePasswordChangeRejectsShortNewPassword() {
        ChangePasswordRequestDTO requestDTO = validPasswordChangeBuilder().newPassword("short").confirmNewPassword("short").build();

        assertThatThrownBy(() -> validator.validatePasswordChange(requestDTO)).isInstanceOf(ValidationException.class);
    }

    @Test
    void validatePasswordChangeRejectsMismatchedConfirmation() {
        ChangePasswordRequestDTO requestDTO = validPasswordChangeBuilder().confirmNewPassword("different-password").build();

        assertThatThrownBy(() -> validator.validatePasswordChange(requestDTO)).isInstanceOf(ValidationException.class);
    }

    @Test
    void validatePasswordChangeRejectsSameAsCurrentPassword() {
        ChangePasswordRequestDTO requestDTO = validPasswordChangeBuilder()
                .newPassword("current-password").confirmNewPassword("current-password").build();

        assertThatThrownBy(() -> validator.validatePasswordChange(requestDTO)).isInstanceOf(ValidationException.class);
    }
}
