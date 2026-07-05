package com.gymtracker.exception;

/**
 * Thrown when a user attempts an operation without permission.
 */
public class UnauthorizedOperationException extends RuntimeException {

    public UnauthorizedOperationException(String message) {
        super(message);
    }
}
