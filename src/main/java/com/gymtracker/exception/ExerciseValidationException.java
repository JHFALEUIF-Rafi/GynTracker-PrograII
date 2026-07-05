package com.gymtracker.exception;

/**
 * Exception thrown when exercise validation fails.
 */
public class ExerciseValidationException extends ValidationException {

    public ExerciseValidationException(String message) {
        super(message);
    }
}
