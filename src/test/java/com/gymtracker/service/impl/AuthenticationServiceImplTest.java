package com.gymtracker.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymtracker.dto.auth.AuthenticatedUserDTO;
import com.gymtracker.entity.User;
import com.gymtracker.enums.Role;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.security.CustomUserDetails;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for AuthenticationServiceImpl. Vaadin's current request/response
 * (needed to persist the SecurityContext into the HTTP session) are stubbed
 * via CurrentInstance, since there is no real HTTP request in a plain unit
 * test.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    private AuthenticationServiceImpl authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationServiceImpl(authenticationManager, userRepository);

        // VaadinServletRequest/Response are concrete wrapper classes; real
        // instances are built around mocked plain servlet interfaces instead
        // of mocking the Vaadin wrapper classes themselves (their complex
        // wrapper hierarchy is not instrumentable by Mockito on every JDK).
        lenient().when(httpServletRequest.getSession(org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(mock(HttpSession.class));
        VaadinServletRequest vaadinServletRequest = new VaadinServletRequest(httpServletRequest, null);
        VaadinServletResponse vaadinServletResponse = new VaadinServletResponse(httpServletResponse, null);

        CurrentInstance.set(VaadinRequest.class, vaadinServletRequest);
        CurrentInstance.set(VaadinResponse.class, vaadinServletResponse);
    }

    @AfterEach
    void tearDown() {
        CurrentInstance.clearAll();
        SecurityContextHolder.clearContext();
    }

    private User buildUser() {
        return User.builder()
                .id(new ObjectId())
                .role(Role.ATHLETE)
                .email("athlete@example.com")
                .password("encoded-password")
                .firstName("Jane")
                .lastName("Doe")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void authenticateReturnsAuthenticatedUserAndRecordsLastLogin() {
        User user = buildUser();
        CustomUserDetails principal = new CustomUserDetails(user);
        Authentication authenticationResult = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authenticationResult);
        when(userRepository.findById(user.getId().toHexString())).thenReturn(Optional.of(user));

        AuthenticatedUserDTO result = authenticationService.authenticate("athlete@example.com", "password");

        assertThat(result.getEmail()).isEqualTo("athlete@example.com");
        assertThat(result.getRole()).isEqualTo(Role.ATHLETE);
        assertThat(result.getUserId()).isEqualTo(user.getId().toHexString());
        verify(userRepository).save(user);
        assertThat(user.getLastLoginAt()).isNotNull();
    }

    @Test
    void authenticateRotatesSessionIdToPreventSessionFixation() {
        User user = buildUser();
        CustomUserDetails principal = new CustomUserDetails(user);
        Authentication authenticationResult = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authenticationResult);
        when(userRepository.findById(user.getId().toHexString())).thenReturn(Optional.of(user));

        authenticationService.authenticate("athlete@example.com", "password");

        verify(httpServletRequest).changeSessionId();
    }

    @Test
    void authenticateSetsSecurityContext() {
        User user = buildUser();
        CustomUserDetails principal = new CustomUserDetails(user);
        Authentication authenticationResult = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authenticationResult);
        when(userRepository.findById(user.getId().toHexString())).thenReturn(Optional.of(user));

        authenticationService.authenticate("athlete@example.com", "password");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authenticationResult);
    }

    @Test
    void authenticatePropagatesAuthenticationException() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authenticationService.authenticate("athlete@example.com", "wrong-password"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void authenticateDoesNotFailWhenUserRecordMissingForLastLoginUpdate() {
        User user = buildUser();
        CustomUserDetails principal = new CustomUserDetails(user);
        Authentication authenticationResult = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authenticationResult);
        when(userRepository.findById(user.getId().toHexString())).thenReturn(Optional.empty());

        AuthenticatedUserDTO result = authenticationService.authenticate("athlete@example.com", "password");

        assertThat(result.getEmail()).isEqualTo("athlete@example.com");
    }
}
