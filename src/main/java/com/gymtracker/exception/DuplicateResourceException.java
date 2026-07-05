package com.gymtracker.exception;

/**
 * Thrown when attempting to create duplicated data.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
