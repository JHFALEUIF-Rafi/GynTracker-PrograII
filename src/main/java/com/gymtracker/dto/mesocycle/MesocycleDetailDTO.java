package com.gymtracker.dto.mesocycle;

import com.gymtracker.enums.MesocycleStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Detailed mesocycle representation for detail screens.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesocycleDetailDTO {

    private String id;
    private String coachId;
    private String coachName;
    private String athleteId;
    private String athleteName;
    private String name;
    private Integer durationWeeks;
    private Integer targetRpe;
    private String notes;
    private MesocycleStatus status;
    private List<MesocycleWorkoutDayDTO> days;
    private LocalDateTime createdAt;
}
