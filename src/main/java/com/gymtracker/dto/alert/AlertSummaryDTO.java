package com.gymtracker.dto.alert;

import com.gymtracker.enums.AlertStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lightweight DTO for alert list screens.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertSummaryDTO {

    private String id;
    private String athleteId;
    private String coachId;
    private AlertStatus status;
    private String message;
    private LocalDateTime generatedAt;
}
