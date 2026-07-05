package com.gymtracker.exception;

/**
 * Exception thrown when athlete validation fails.
 */
public class AthleteValidationException extends ValidationException {

    public AthleteValidationException(String message) {
        super(message);
    }
}
