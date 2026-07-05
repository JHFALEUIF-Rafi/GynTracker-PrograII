package com.gymtracker.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymtracker.dto.mesocycle.MesocycleRequestDTO;
import com.gymtracker.dto.mesocycle.MesocycleWorkoutDayDTO;
import com.gymtracker.dto.mesocycle.MesocycleWorkoutExerciseDTO;
import com.gymtracker.enums.MesocycleStatus;
import com.gymtracker.exception.MesocycleValidationException;
import jakarta.validation.Validation;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for MesocycleValidator using a real Bean Validation Validator.
 */
class MesocycleValidatorTest {

    private final MesocycleValidator validator = new MesocycleValidator(Validation.buildDefaultValidatorFactory().getValidator());

    private MesocycleWorkoutDayDTO buildDay() {
        MesocycleWorkoutExerciseDTO exercise = MesocycleWorkoutExerciseDTO.builder()
                .exerciseId(new ObjectId().toHexString()).sets(3).repetitions(10).targetWeight(50.0).targetRpe(7).build();
        return MesocycleWorkoutDayDTO.builder().dayName("Day 1").exercises(List.of(exercise)).build();
    }

    private MesocycleRequestDTO.MesocycleRequestDTOBuilder validRequestBuilder() {
        return MesocycleRequestDTO.builder()
                .coachId(new ObjectId().toHexString())
                .athleteId(new ObjectId().toHexString())
                .name("Hypertrophy Block")
                .durationWeeks(8)
                .targetRpe(7)
                .status(MesocycleStatus.DRAFT)
                .days(List.of(buildDay()));
    }

    @Test
    void validateCreateAcceptsValidRequest() {
        assertThatCode(() -> validator.validateCreate(validRequestBuilder().build())).doesNotThrowAnyException();
    }

    @Test
    void validateCreateRejectsNullRequest() {
        assertThatThrownBy(() -> validator.validateCreate(null)).isInstanceOf(MesocycleValidationException.class);
    }

    @Test
    void validateCreateRejectsZeroDurationWeeks() {
        MesocycleRequestDTO requestDTO = validRequestBuilder().durationWeeks(0).build();

        assertThatThrownBy(() -> validator.validateCreate(requestDTO)).isInstanceOf(MesocycleValidationException.class);
    }

    @Test
    void validateCreateRejectsNoWorkoutDays() {
        MesocycleRequestDTO requestDTO = validRequestBuilder().days(List.of()).build();

        assertThatThrownBy(() -> validator.validateCreate(requestDTO)).isInstanceOf(MesocycleValidationException.class);
    }

    @Test
    void validateCreateRejectsDayWithNoExercises() {
        MesocycleWorkoutDayDTO emptyDay = MesocycleWorkoutDayDTO.builder().dayName("Day 1").exercises(List.of()).build();
        MesocycleRequestDTO requestDTO = validRequestBuilder().days(List.of(emptyDay)).build();

        assertThatThrownBy(() -> validator.validateCreate(requestDTO)).isInstanceOf(MesocycleValidationException.class);
    }

    @Test
    void validateCreateRejectsOutOfRangeTargetRpe() {
        MesocycleRequestDTO requestDTO = validRequestBuilder().targetRpe(11).build();

        assertThatThrownBy(() -> validator.validateCreate(requestDTO)).isInstanceOf(MesocycleValidationException.class);
    }

    @Test
    void validateAssignmentRejectsBlankIds() {
        assertThatThrownBy(() -> validator.validateAssignment(" ", " ")).isInstanceOf(MesocycleValidationException.class);
    }

    @Test
    void validateAssignmentAcceptsValidIds() {
        assertThatCode(() -> validator.validateAssignment(new ObjectId().toHexString(), new ObjectId().toHexString()))
                .doesNotThrowAnyException();
    }

    @Test
    void validateDeleteRejectsBlankId() {
        assertThatThrownBy(() -> validator.validateDelete("")).isInstanceOf(MesocycleValidationException.class);
    }
}
