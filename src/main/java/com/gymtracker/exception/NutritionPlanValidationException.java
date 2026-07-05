package com.gymtracker.exception;

/**
 * Exception thrown when nutrition plan validation fails.
 */
public class NutritionPlanValidationException extends ValidationException {

    public NutritionPlanValidationException(String message) {
        super(message);
    }
}
