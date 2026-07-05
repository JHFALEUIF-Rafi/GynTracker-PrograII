package com.gymtracker.view.layout;

import com.gymtracker.security.SecurityConstants;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.VaadinSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Header component displaying logo, app name, user info and logout.
 */
@Slf4j
public class HeaderComponent extends HorizontalLayout {

    private final Span userNameSpan;
    private final Span userRoleSpan;
    private final Button logoutButton;
    private final Button notificationButton;

    public HeaderComponent() {
        setPadding(true);
        setSpacing(true);
        setWidthFull();
        setHeight("80px");
        getStyle()
                .set("background-color", "var(--lumo-primary-color)")
                .set("color", "white")
                .set("box-shadow", "0 2px 8px rgba(0, 0, 0, 0.1)");

        Div logoArea = createLogoArea();
        Div spacer = new Div();
        spacer.setWidthFull();

        userNameSpan = new Span();
        userNameSpan.getStyle()
                .set("font-weight", "600")
                .set("font-size", "14px");

        userRoleSpan = new Span();
        userRoleSpan.getStyle()
                .set("background-color", "rgba(255, 255, 255, 0.2)")
                .set("padding", "4px 12px")
                .set("border-radius", "12px")
                .set("font-size", "12px")
                .set("margin-top", "4px");

        notificationButton = createNotificationButton();
        logoutButton = createLogoutButton();

        Div userInfo = new Div(userNameSpan, userRoleSpan);
        userInfo.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "4px")
                .set("align-items", "flex-end")
                .set("min-width", "150px");

        add(logoArea, spacer, userInfo, notificationButton, logoutButton);
        setAlignItems(FlexComponent.Alignment.CENTER);
    }

    private Div createLogoArea() {
        Div logoArea = new Div();
        logoArea.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "12px")
                .set("font-size", "20px")
                .set("font-weight", "bold")
                .set("white-space", "nowrap");

        Icon hammer = new Icon(VaadinIcon.HAMMER);
        hammer.getStyle()
                .set("font-size", "28px")
                .set("color", "white");

        Span appName = new Span("GymTracker");
        logoArea.add(hammer, appName);
        return logoArea;
    }

    private Button createNotificationButton() {
        Button button = new Button(new Icon(VaadinIcon.BELL));
        button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_CONTRAST);
        button.setAriaLabel("Notifications");
        button.getStyle().set("color", "white");
        button.addClickListener(event -> {
            log.info("Notification button clicked");
        });
        return button;
    }

    private Button createLogoutButton() {
        Button button = new Button("Logout", new Icon(VaadinIcon.SIGN_OUT));
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        button.getStyle()
                .set("color", "white")
                .set("margin-left", "12px");
        button.addClickListener(event -> {
            log.info("Logout button clicked");
            SecurityContextHolder.clearContext();
            VaadinSession.getCurrent().getSession().invalidate();
            UI.getCurrent().getPage().setLocation(SecurityConstants.LOGIN_ROUTE);
        });
        return button;
    }

    public void setUserName(String firstName, String lastName) {
        userNameSpan.setText(firstName + " " + lastName);
    }

    public void setUserRole(String role) {
        userRoleSpan.setText(role);
    }
}
