package com.gymtracker.exception;

/**
 * Thrown when business validation fails.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
