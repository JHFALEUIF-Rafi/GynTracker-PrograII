package com.gymtracker.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymtracker.dto.athlete.AthleteRequestDTO;
import com.gymtracker.enums.ActivityLevel;
import com.gymtracker.enums.Gender;
import com.gymtracker.exception.AthleteValidationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for AthleteValidator using a real Bean Validation Validator.
 */
class AthleteValidatorTest {

    private final AthleteValidator validator = new AthleteValidator(Validation.buildDefaultValidatorFactory().getValidator());

    private AthleteRequestDTO.AthleteRequestDTOBuilder validRequestBuilder() {
        return AthleteRequestDTO.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("athlete@example.com")
                .age(25)
                .gender(Gender.FEMALE)
                .weight(60.0)
                .height(165.0)
                .activityLevel(ActivityLevel.MODERATE);
    }

    @Test
    void validateCreateAcceptsValidRequest() {
        assertThatCode(() -> validator.validateCreate(validRequestBuilder().build())).doesNotThrowAnyException();
    }

    @Test
    void validateCreateRejectsNullRequest() {
        assertThatThrownBy(() -> validator.validateCreate(null)).isInstanceOf(AthleteValidationException.class);
    }

    @Test
    void validateCreateRejectsAgeBelowMinimum() {
        AthleteRequestDTO requestDTO = validRequestBuilder().age(10).build();

        assertThatThrownBy(() -> validator.validateCreate(requestDTO)).isInstanceOf(AthleteValidationException.class);
    }

    @Test
    void validateCreateRejectsAgeAboveMaximum() {
        AthleteRequestDTO requestDTO = validRequestBuilder().age(150).build();

        assertThatThrownBy(() -> validator.validateCreate(requestDTO)).isInstanceOf(AthleteValidationException.class);
    }

    @Test
    void validateCreateRejectsNonPositiveWeight() {
        AthleteRequestDTO requestDTO = validRequestBuilder().weight(0.0).build();

        assertThatThrownBy(() -> validator.validateCreate(requestDTO)).isInstanceOf(AthleteValidationException.class);
    }

    @Test
    void validateCreateRejectsNonPositiveHeight() {
        AthleteRequestDTO requestDTO = validRequestBuilder().height(-1.0).build();

        assertThatThrownBy(() -> validator.validateCreate(requestDTO)).isInstanceOf(AthleteValidationException.class);
    }

    @Test
    void validateCreateRejectsBlankFirstName() {
        AthleteRequestDTO requestDTO = validRequestBuilder().firstName(" ").build();

        assertThatThrownBy(() -> validator.validateCreate(requestDTO)).isInstanceOf(AthleteValidationException.class);
    }

    @Test
    void validateCreateRejectsInvalidEmail() {
        AthleteRequestDTO requestDTO = validRequestBuilder().email("not-an-email").build();

        assertThatThrownBy(() -> validator.validateCreate(requestDTO)).isInstanceOf(AthleteValidationException.class);
    }

    @Test
    void validateDeleteRejectsBlankId() {
        assertThatThrownBy(() -> validator.validateDelete(" ")).isInstanceOf(AthleteValidationException.class);
    }

    @Test
    void validateEmailUniquenessRejectsWhenAlreadyExists() {
        assertThatThrownBy(() -> validator.validateEmailUniqueness(true)).isInstanceOf(AthleteValidationException.class);
    }

    @Test
    void validateEmailUniquenessAcceptsWhenNotExisting() {
        assertThatCode(() -> validator.validateEmailUniqueness(false)).doesNotThrowAnyException();
    }
}
