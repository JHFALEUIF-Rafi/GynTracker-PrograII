package com.gymtracker.view.layout;

import com.gymtracker.enums.Role;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLayout;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Main application layout wrapping all authenticated pages.
 * Provides header, navigation, and footer.
 * Uses AppLayout for responsive design.
 */
@Slf4j
public class MainLayout extends AppLayout implements RouterLayout {

    private final HeaderComponent header;
    private final NavigationMenu navigationMenu;
    private final FooterComponent footer;

    public MainLayout() {
        log.info("Initializing MainLayout");

        header = new HeaderComponent();
        navigationMenu = new NavigationMenu();
        footer = new FooterComponent();

        setupLayout();
        initializeUser();
    }

    private void setupLayout() {
        DrawerToggle toggle = new DrawerToggle();
        addToNavbar(toggle, header);
        addToDrawer(navigationMenu);
    }

    private void initializeUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()) {
                String userEmail = authentication.getName();
                Role userRole = extractRoleFromAuthentication(authentication);

                header.setUserName(userEmail.split("@")[0], "");
                header.setUserRole(userRole.toString());
                navigationMenu.setupMenuForRole(userRole);

                log.info("MainLayout initialized for user: {} with role: {}", userEmail, userRole);
            } else {
                log.warn("MainLayout: No authenticated user found");
            }
        } catch (Exception e) {
            log.error("Error initializing MainLayout user info", e);
        }
    }

    private Role extractRoleFromAuthentication(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(auth -> {
                    String authority = auth.getAuthority();
                    if (authority.contains("ATHLETE")) return Role.ATHLETE;
                    if (authority.contains("COACH")) return Role.COACH;
                    if (authority.contains("NUTRITIONIST")) return Role.NUTRITIONIST;
                    return Role.ATHLETE;
                })
                .findFirst()
                .orElse(Role.ATHLETE);
    }
}
