package com.gymtracker.exception;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Standard error response structure used across exception handling.
 */
@Getter
@Builder
@AllArgsConstructor
public class ErrorResponseDTO {

    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final List<String> details;
}
