package com.gymtracker.exception;

/**
 * Thrown when a business rule is violated.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
