package com.gymtracker.dto.alert;

import com.gymtracker.enums.AlertStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Detailed DTO for alert data.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDTO {

    private String id;
    private String athleteId;
    private String athleteName;
    private String coachId;
    private String coachName;
    private String type;
    private String message;
    private AlertStatus status;
    private LocalDateTime generatedAt;
    private LocalDateTime reviewedAt;
}
