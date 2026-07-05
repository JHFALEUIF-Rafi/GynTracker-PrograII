package com.gymtracker.dto.athlete;

import com.gymtracker.dto.nutrition.NutritionPlanResponseDTO;
import com.gymtracker.dto.workout.WorkoutSessionSummaryDTO;
import com.gymtracker.enums.ActivityLevel;
import com.gymtracker.enums.FatigueLevel;
import com.gymtracker.enums.Gender;
import com.gymtracker.enums.Role;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Detailed athlete representation for profile and details screens.
 * <p>
 * The performance-related fields (mesocycle, nutrition plan, latest workout,
 * fatigue and 1RM) are only populated by
 * {@link com.gymtracker.service.AthleteService#getAthleteById(String)}, since
 * an athlete's own profile view has no use for them.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AthleteDetailDTO {

    private String id;
    private Role role;
    private String firstName;
    private String lastName;
    private String email;
    private Integer age;
    private Gender gender;
    private Double weight;
    private Double height;
    private ActivityLevel activityLevel;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String currentMesocycleName;
    private String assignedCoachName;
    private String assignedNutritionistName;
    private NutritionPlanResponseDTO activeNutritionPlan;
    private WorkoutSessionSummaryDTO latestWorkout;
    private FatigueLevel currentFatigueLevel;
    private Double latestOneRepMax;
}
