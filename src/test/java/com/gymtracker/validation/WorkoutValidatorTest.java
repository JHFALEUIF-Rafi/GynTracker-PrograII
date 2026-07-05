package com.gymtracker.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymtracker.dto.workout.WorkoutExerciseDTO;
import com.gymtracker.dto.workout.WorkoutSessionRequestDTO;
import com.gymtracker.dto.workout.WorkoutSetDTO;
import com.gymtracker.exception.WorkoutValidationException;
import jakarta.validation.Validation;
import java.time.LocalDate;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for WorkoutValidator using a real Bean Validation Validator.
 */
class WorkoutValidatorTest {

    private final WorkoutValidator validator = new WorkoutValidator(Validation.buildDefaultValidatorFactory().getValidator());

    private WorkoutSetDTO validSet() {
        return WorkoutSetDTO.builder().weight(60.0).repetitions(8).rpe(7).build();
    }

    private WorkoutSessionRequestDTO.WorkoutSessionRequestDTOBuilder validRequestBuilder() {
        WorkoutExerciseDTO exercise = WorkoutExerciseDTO.builder()
                .exerciseId(new ObjectId().toHexString()).sets(List.of(validSet())).build();
        return WorkoutSessionRequestDTO.builder()
                .athleteId(new ObjectId().toHexString())
                .mesocycleId(new ObjectId().toHexString())
                .date(LocalDate.now())
                .durationMinutes(60)
                .completedExercises(List.of(exercise));
    }

    @Test
    void validateCreateAcceptsValidRequest() {
        assertThatCode(() -> validator.validateCreate(validRequestBuilder().build())).doesNotThrowAnyException();
    }

    @Test
    void validateCreateRejectsNullRequest() {
        assertThatThrownBy(() -> validator.validateCreate(null)).isInstanceOf(WorkoutValidationException.class);
    }

    @Test
    void validateCreateRejectsZeroDuration() {
        WorkoutSessionRequestDTO requestDTO = validRequestBuilder().durationMinutes(0).build();

        assertThatThrownBy(() -> validator.validateCreate(requestDTO)).isInstanceOf(WorkoutValidationException.class);
    }

    @Test
    void validateCreateRejectsNoExercises() {
        WorkoutSessionRequestDTO requestDTO = validRequestBuilder().completedExercises(List.of()).build();

        assertThatThrownBy(() -> validator.validateCreate(requestDTO)).isInstanceOf(WorkoutValidationException.class);
    }

    @Test
    void validateCreateRejectsSetWithZeroWeight() {
        WorkoutSetDTO invalidSet = WorkoutSetDTO.builder().weight(0.0).repetitions(8).rpe(7).build();
        WorkoutExerciseDTO exercise = WorkoutExerciseDTO.builder()
                .exerciseId(new ObjectId().toHexString()).sets(List.of(invalidSet)).build();
        WorkoutSessionRequestDTO requestDTO = validRequestBuilder().completedExercises(List.of(exercise)).build();

        assertThatThrownBy(() -> validator.validateCreate(requestDTO)).isInstanceOf(WorkoutValidationException.class);
    }

    @Test
    void validateCreateRejectsRpeOutOfRange() {
        WorkoutSetDTO invalidSet = WorkoutSetDTO.builder().weight(60.0).repetitions(8).rpe(11).build();
        WorkoutExerciseDTO exercise = WorkoutExerciseDTO.builder()
                .exerciseId(new ObjectId().toHexString()).sets(List.of(invalidSet)).build();
        WorkoutSessionRequestDTO requestDTO = validRequestBuilder().completedExercises(List.of(exercise)).build();

        assertThatThrownBy(() -> validator.validateCreate(requestDTO)).isInstanceOf(WorkoutValidationException.class);
    }

    @Test
    void validateDeleteRejectsBlankId() {
        assertThatThrownBy(() -> validator.validateDelete("")).isInstanceOf(WorkoutValidationException.class);
    }
}
