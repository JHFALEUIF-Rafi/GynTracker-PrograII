package com.gymtracker.security;

import com.gymtracker.entity.User;
import com.gymtracker.enums.Role;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security UserDetails wrapper around the User entity.
 */
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(mapRoleToAuthority(user.getRole())));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(user.getEnabled());
    }

    public Role getRole() {
        return user.getRole();
    }

    public String getUserId() {
        return user.getId().toHexString();
    }

    private String mapRoleToAuthority(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("User role is required for authority mapping.");
        }
        return switch (role) {
            case ATHLETE -> SecurityConstants.ROLE_ATHLETE;
            case COACH -> SecurityConstants.ROLE_COACH;
            case NUTRITIONIST -> SecurityConstants.ROLE_NUTRITIONIST;
        };
    }
}
