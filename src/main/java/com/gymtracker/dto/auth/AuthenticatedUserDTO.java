package com.gymtracker.dto.auth;

import com.gymtracker.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO representing a user that has just been authenticated by
 * {@link com.gymtracker.service.AuthenticationService}.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedUserDTO {

    private String userId;
    private String email;
    private Role role;
}
