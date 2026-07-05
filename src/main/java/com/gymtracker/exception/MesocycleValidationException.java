package com.gymtracker.exception;

/**
 * Exception thrown when mesocycle validation fails.
 */
public class MesocycleValidationException extends ValidationException {

    public MesocycleValidationException(String message) {
        super(message);
    }
}
