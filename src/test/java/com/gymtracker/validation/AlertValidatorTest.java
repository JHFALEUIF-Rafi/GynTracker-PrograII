package com.gymtracker.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymtracker.dto.alert.AlertDTO;
import com.gymtracker.enums.AlertStatus;
import com.gymtracker.exception.AlertValidationException;
import jakarta.validation.Validation;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for AlertValidator using a real Bean Validation Validator.
 */
class AlertValidatorTest {

    private final AlertValidator validator = new AlertValidator(Validation.buildDefaultValidatorFactory().getValidator());

    private AlertDTO.AlertDTOBuilder validAlertBuilder() {
        return AlertDTO.builder()
                .athleteId(new ObjectId().toHexString())
                .coachId(new ObjectId().toHexString())
                .type("HIGH_FATIGUE")
                .message("Message")
                .status(AlertStatus.ACTIVE);
    }

    @Test
    void validateCreateAcceptsValidAlert() {
        assertThatCode(() -> validator.validateCreate(validAlertBuilder().build())).doesNotThrowAnyException();
    }

    @Test
    void validateCreateRejectsNullAlert() {
        assertThatThrownBy(() -> validator.validateCreate(null)).isInstanceOf(AlertValidationException.class);
    }

    @Test
    void validateCreateRejectsMissingStatus() {
        AlertDTO alert = validAlertBuilder().status(null).build();

        assertThatThrownBy(() -> validator.validateCreate(alert)).isInstanceOf(AlertValidationException.class);
    }

    @Test
    void validateCreateRejectsBlankAthleteId() {
        AlertDTO alert = validAlertBuilder().athleteId(" ").build();

        assertThatThrownBy(() -> validator.validateCreate(alert)).isInstanceOf(AlertValidationException.class);
    }

    @Test
    void validateCreateRejectsBlankMessage() {
        AlertDTO alert = validAlertBuilder().message("").build();

        assertThatThrownBy(() -> validator.validateCreate(alert)).isInstanceOf(AlertValidationException.class);
    }

    @Test
    void validateDeleteRejectsBlankId() {
        assertThatThrownBy(() -> validator.validateDelete("")).isInstanceOf(AlertValidationException.class);
    }
}
