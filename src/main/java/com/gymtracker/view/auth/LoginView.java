package com.gymtracker.view.auth;

import com.gymtracker.dto.auth.AuthenticatedUserDTO;
import com.gymtracker.security.SecurityConstants;
import com.gymtracker.service.AuthenticationService;
import com.gymtracker.ui.component.Notifications;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Public login screen. Displays {@link LoginFormComponent} and delegates
 * authentication to {@link AuthenticationService}, then redirects the user
 * to their role's landing page on success.
 * <p>
 * This view never authenticates manually: credential verification, role
 * lookup and security context handling all happen inside the service, which
 * relies exclusively on Spring Security's AuthenticationManager.
 */
@Slf4j
@Route(value = "login", layout = AuthenticationLayout.class)
@PageTitle("Login - GymTracker")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final AuthenticationService authenticationService;
    private final LoginFormComponent loginForm;

    public LoginView(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
        this.loginForm = new LoginFormComponent();

        setPadding(false);
        setSpacing(false);

        loginForm.getLoginButton().addClickListener(event -> attemptLogin());
        add(loginForm);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            event.forwardTo(SecurityConstants.DASHBOARD_ROUTE);
        }
    }

    private void attemptLogin() {
        if (!loginForm.validate()) {
            return;
        }

        String email = loginForm.getEmail();
        loginForm.setProcessing(true);

        try {
            AuthenticatedUserDTO authenticatedUser = authenticationService.authenticate(email, loginForm.getPassword());
            log.info("Login successful for email={}", email);
            navigateToRoleDashboard(authenticatedUser);
        } catch (DisabledException e) {
            log.warn("Login rejected, account disabled, email={}", email);
            showError("Your account is disabled. Please contact your coach or administrator.");
        } catch (LockedException e) {
            log.warn("Login rejected, account locked, email={}", email);
            showError("Your account is locked. Please contact your coach or administrator.");
        } catch (AuthenticationException e) {
            log.warn("Login rejected, invalid credentials, email={}", email);
            showError("Invalid email or password. Please try again.");
        } catch (Exception e) {
            log.error("Unexpected error during login for email={}", email, e);
            showError("Something went wrong. Please try again.");
        } finally {
            loginForm.setProcessing(false);
        }
    }

    private void navigateToRoleDashboard(AuthenticatedUserDTO authenticatedUser) {
        String route = switch (authenticatedUser.getRole()) {
            case ATHLETE, COACH, NUTRITIONIST -> SecurityConstants.DASHBOARD_ROUTE;
        };
        UI.getCurrent().navigate(route);
    }

    private void showError(String message) {
        Notifications.error(message);
    }
}
