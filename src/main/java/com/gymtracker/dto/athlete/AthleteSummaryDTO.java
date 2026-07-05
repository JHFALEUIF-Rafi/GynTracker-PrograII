package com.gymtracker.dto.athlete;

import com.gymtracker.enums.ActivityLevel;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lightweight athlete representation for list views.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AthleteSummaryDTO {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private ActivityLevel activityLevel;
    private Integer age;
    private Double weight;
    private Double height;
    private Boolean enabled;
    private String currentMesocycleName;
    private String currentCoachId;
    private String currentCoachName;
    private LocalDate lastWorkoutDate;
}
