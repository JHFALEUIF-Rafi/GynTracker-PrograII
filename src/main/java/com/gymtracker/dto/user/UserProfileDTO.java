package com.gymtracker.dto.user;

import com.gymtracker.enums.Role;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Generic profile representation shared by every role (Athlete-specific
 * biometric fields are handled separately by AthleteService).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {

    private String id;
    private Role role;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
}
