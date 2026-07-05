package com.gymtracker.exception;

/**
 * Exception thrown when alert validation fails.
 */
public class AlertValidationException extends ValidationException {

    public AlertValidationException(String message) {
        super(message);
    }
}
