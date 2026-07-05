package com.gymtracker.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Common support for bean validation and reusable validation helpers.
 */
public abstract class BaseValidator {

    private final Validator validator;

    protected BaseValidator(Validator validator) {
        this.validator = validator;
    }

    protected <T> void validateBean(T target, Function<String, ? extends RuntimeException> exceptionFactory) {
        Set<ConstraintViolation<T>> violations = validator.validate(target);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw exceptionFactory.apply(message);
        }
    }

    protected void requireCondition(boolean condition, String message, Function<String, ? extends RuntimeException> exFactory) {
        if (!condition) {
            throw exFactory.apply(message);
        }
    }

    protected boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
