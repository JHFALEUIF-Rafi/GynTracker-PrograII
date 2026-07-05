package com.gymtracker.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymtracker.entity.User;
import com.gymtracker.enums.Role;
import java.time.LocalDateTime;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

/**
 * Unit tests for the Spring Security UserDetails wrapper around User.
 */
class CustomUserDetailsTest {

    private User buildUser(Role role, boolean enabled) {
        return User.builder()
                .id(new ObjectId())
                .role(role)
                .email("user@example.com")
                .password("encoded-password")
                .firstName("Jane")
                .lastName("Doe")
                .enabled(enabled)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void exposesUsernameAsEmail() {
        CustomUserDetails userDetails = new CustomUserDetails(buildUser(Role.ATHLETE, true));

        assertThat(userDetails.getUsername()).isEqualTo("user@example.com");
    }

    @Test
    void exposesPasswordFromEntity() {
        CustomUserDetails userDetails = new CustomUserDetails(buildUser(Role.ATHLETE, true));

        assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void mapsAthleteRoleToAuthority() {
        CustomUserDetails userDetails = new CustomUserDetails(buildUser(Role.ATHLETE, true));

        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ATHLETE");
    }

    @Test
    void mapsCoachRoleToAuthority() {
        CustomUserDetails userDetails = new CustomUserDetails(buildUser(Role.COACH, true));

        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_COACH");
    }

    @Test
    void mapsNutritionistRoleToAuthority() {
        CustomUserDetails userDetails = new CustomUserDetails(buildUser(Role.NUTRITIONIST, true));

        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_NUTRITIONIST");
    }

    @Test
    void isEnabledReflectsUserEnabledFlag() {
        assertThat(new CustomUserDetails(buildUser(Role.ATHLETE, true)).isEnabled()).isTrue();
        assertThat(new CustomUserDetails(buildUser(Role.ATHLETE, false)).isEnabled()).isFalse();
    }

    @Test
    void accountFlagsAreAlwaysTrue() {
        CustomUserDetails userDetails = new CustomUserDetails(buildUser(Role.ATHLETE, true));

        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void getRoleReturnsUnderlyingRole() {
        CustomUserDetails userDetails = new CustomUserDetails(buildUser(Role.COACH, true));

        assertThat(userDetails.getRole()).isEqualTo(Role.COACH);
    }

    @Test
    void getUserIdReturnsHexStringOfEntityId() {
        User user = buildUser(Role.ATHLETE, true);
        CustomUserDetails userDetails = new CustomUserDetails(user);

        assertThat(userDetails.getUserId()).isEqualTo(user.getId().toHexString());
    }
}
