package com.gymtracker.dto.athlete;

import com.gymtracker.enums.ActivityLevel;
import com.gymtracker.enums.Gender;
import com.gymtracker.enums.Role;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response payload for athlete profile operations.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AthleteResponseDTO {

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
}
