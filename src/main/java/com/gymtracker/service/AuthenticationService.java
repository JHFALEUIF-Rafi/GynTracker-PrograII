package com.gymtracker.service;

import com.gymtracker.dto.auth.AuthenticatedUserDTO;

/**
 * Service contract for authenticating users through Spring Security.
 */
public interface AuthenticationService {

    /**
     * Authenticates a user by email and password using the Spring Security
     * AuthenticationManager and persists the resulting security context so
     * it survives across the Vaadin session.
     *
     * @param email    the user's email, used as the Spring Security username
     * @param password the raw password to verify
     * @return identity and role information for the authenticated user
     * @throws org.springframework.security.core.AuthenticationException if credentials are invalid
     *         or the account is disabled or locked
     */
    AuthenticatedUserDTO authenticate(String email, String password);
}
