package com.gymtracker.exception;

/**
 * Thrown when an operation cannot be completed due to current system state.
 */
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message) {
        super(message);
    }
}
