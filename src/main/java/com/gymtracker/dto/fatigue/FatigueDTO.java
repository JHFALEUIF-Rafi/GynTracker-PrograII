package com.gymtracker.dto.fatigue;

import com.gymtracker.enums.FatigueLevel;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO representing one fatigue snapshot for an athlete.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FatigueDTO {

    private String athleteId;
    private String sessionId;
    private LocalDate date;
    private Double fatigueScore;
    private FatigueLevel fatigueLevel;
    private Double recoveryScore;
    private Double weeklyTrainingLoad;
}
