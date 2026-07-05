package com.gymtracker.view.auth;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLayout;

/**
 * Full-viewport layout that centers public authentication screens (login and,
 * in the future, other unauthenticated flows) regardless of screen size.
 * Unlike MainLayout, it has no header, sidebar or footer.
 */
public class AuthenticationLayout extends VerticalLayout implements RouterLayout {

    public AuthenticationLayout() {
        setClassName("auth-layout");
        setSizeFull();
        setPadding(true);
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
    }
}
