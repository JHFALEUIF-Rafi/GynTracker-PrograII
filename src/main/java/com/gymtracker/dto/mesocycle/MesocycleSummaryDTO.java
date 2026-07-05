package com.gymtracker.dto.mesocycle;

import com.gymtracker.enums.MesocycleStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lightweight mesocycle representation for list views.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesocycleSummaryDTO {

    private String id;
    private String name;
    private String coachId;
    private String coachName;
    private String athleteId;
    private String athleteName;
    private Integer durationWeeks;
    private MesocycleStatus status;
    private LocalDateTime createdAt;
}
