package com.gymtracker.exception;

/**
 * Exception thrown when workout validation fails.
 */
public class WorkoutValidationException extends ValidationException {

    public WorkoutValidationException(String message) {
        super(message);
    }
}
