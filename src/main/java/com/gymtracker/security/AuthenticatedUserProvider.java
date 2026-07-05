package com.gymtracker.security;

import com.gymtracker.entity.User;
import com.gymtracker.exception.ResourceNotFoundException;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Resolves the authenticated {@link User} entity from Spring Security's
 * context. Every service implementation previously duplicated this exact
 * logic (retrieve the {@code Authentication}, extract the principal's email,
 * look up the {@link User} by email); this centralizes it in one place.
 */
@Component
public class AuthenticatedUserProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticatedUserProvider.class);

    private final UserRepository userRepository;

    public AuthenticatedUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns the fully-loaded {@link User} entity for the currently
     * authenticated principal.
     *
     * @throws UnauthorizedOperationException if there is no authenticated principal
     * @throws ResourceNotFoundException if the principal's email has no matching user
     */
    public User getAuthenticatedUser() {
        String email = extractEmail(requireAuthentication());
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));
    }

    /**
     * Returns the current {@link Authentication}, requiring that it exists
     * and is authenticated. Exposed separately from {@link #getAuthenticatedUser()}
     * for callers that need other details off the {@code Authentication}
     * (e.g. granted authorities) without an extra user lookup.
     */
    public Authentication requireAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            LOGGER.warn("Unauthorized access attempt without authenticated principal.");
            throw new UnauthorizedOperationException("Authentication is required.");
        }
        return authentication;
    }

    /**
     * Extracts the username (email) from an authenticated principal.
     */
    public String extractEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String principalValue && !"anonymousUser".equals(principalValue)) {
            return principalValue;
        }
        LOGGER.warn("Unauthorized access with unsupported principal type.");
        throw new UnauthorizedOperationException("Authenticated principal is invalid.");
    }
}
