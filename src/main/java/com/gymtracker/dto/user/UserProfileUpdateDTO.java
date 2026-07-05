package com.gymtracker.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for updating the generic (role-independent) profile
 * fields. Email is intentionally excluded - it is not editable here.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateDTO {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;
}
