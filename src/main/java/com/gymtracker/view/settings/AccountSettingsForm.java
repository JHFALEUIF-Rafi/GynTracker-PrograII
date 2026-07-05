package com.gymtracker.view.settings;

import com.gymtracker.dto.user.UserProfileDTO;
import com.gymtracker.security.SecurityConstants;
import com.gymtracker.ui.component.NotificationBadge;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Read-only account overview (role, status, last login) plus Logout. Purely
 * presentational; the owning view supplies the already-fetched profile.
 */
public class AccountSettingsForm extends VerticalLayout {

    public AccountSettingsForm() {
        setPadding(false);
        setSpacing(false);
    }

    public void setProfile(UserProfileDTO profile) {
        removeAll();

        boolean active = Boolean.TRUE.equals(profile.getEnabled());
        add(infoRow("Role", profile.getRole() != null ? profile.getRole().name() : "-"),
                infoRowWithBadge("Account Status", active ? "Active" : "Inactive",
                        active ? NotificationBadge.BadgeType.SUCCESS : NotificationBadge.BadgeType.NEUTRAL),
                infoRow("Last Login", profile.getLastLoginAt() != null ? profile.getLastLoginAt().toString() : "Never"));

        Button logoutButton = new Button("Logout", VaadinIcon.SIGN_OUT.create(), event -> logout());
        logoutButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        logoutButton.getStyle().set("margin-top", "12px");
        add(logoutButton);
    }

    private void logout() {
        SecurityContextHolder.clearContext();
        VaadinSession.getCurrent().getSession().invalidate();
        UI.getCurrent().getPage().setLocation(SecurityConstants.LOGIN_ROUTE);
    }

    private HorizontalLayout infoRow(String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setJustifyContentMode(JustifyContentMode.BETWEEN);

        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");

        row.add(labelSpan, new Span(value));
        return row;
    }

    private HorizontalLayout infoRowWithBadge(String label, String value, NotificationBadge.BadgeType type) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setJustifyContentMode(JustifyContentMode.BETWEEN);

        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");

        row.add(labelSpan, new NotificationBadge(value, type));
        return row;
    }
}
