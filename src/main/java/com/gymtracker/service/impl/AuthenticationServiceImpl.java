package com.gymtracker.service.impl;

import com.gymtracker.dto.auth.AuthenticatedUserDTO;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.security.CustomUserDetails;
import com.gymtracker.service.AuthenticationService;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * Authenticates users through the Spring Security AuthenticationManager.
 * <p>
 * Vaadin server round-trips do not pass through the standard
 * UsernamePasswordAuthenticationFilter, so the resulting SecurityContext must
 * be stored into the HTTP session explicitly for the login to persist beyond
 * the current request.
 */
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthenticationServiceImpl(AuthenticationManager authenticationManager, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    @Override
    public AuthenticatedUserDTO authenticate(String email, String password) {
        Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(email, password);
        Authentication authenticationResult = authenticationManager.authenticate(authenticationRequest);

        preventSessionFixation();

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authenticationResult);
        SecurityContextHolder.setContext(securityContext);
        persistSecurityContext(securityContext);

        CustomUserDetails userDetails = (CustomUserDetails) authenticationResult.getPrincipal();
        recordLastLogin(userDetails.getUserId());
        LOGGER.info("User authenticated successfully email={} role={}", email, userDetails.getRole());

        return AuthenticatedUserDTO.builder()
                .userId(userDetails.getUserId())
                .email(userDetails.getUsername())
                .role(userDetails.getRole())
                .build();
    }

    private void recordLastLogin(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    private void persistSecurityContext(SecurityContext securityContext) {
        VaadinServletRequest request = (VaadinServletRequest) VaadinRequest.getCurrent();
        VaadinServletResponse response = (VaadinServletResponse) VaadinResponse.getCurrent();
        securityContextRepository.saveContext(securityContext, request.getHttpServletRequest(), response.getHttpServletResponse());
    }

    /**
     * Rotates the HTTP session id on successful authentication. Vaadin's
     * manual login path bypasses Spring Security's filter chain, so the
     * {@code ChangeSessionIdAuthenticationStrategy} that normally runs after
     * {@code UsernamePasswordAuthenticationFilter} never fires; without this,
     * a session id obtained/fixed before login (session fixation) would
     * remain valid - and now authenticated - after login.
     */
    private void preventSessionFixation() {
        VaadinServletRequest request = (VaadinServletRequest) VaadinRequest.getCurrent();
        HttpServletRequest httpServletRequest = request.getHttpServletRequest();
        if (httpServletRequest.getSession(false) != null) {
            httpServletRequest.changeSessionId();
        }
    }
}
