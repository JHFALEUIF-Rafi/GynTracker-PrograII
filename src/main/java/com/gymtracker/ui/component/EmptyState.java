package com.gymtracker.ui.component;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Component displayed when no data is available.
 */
public class EmptyState extends VerticalLayout {

    private final Icon icon;
    private final H2 titleLabel;
    private final Span messageLabel;

    public EmptyState(String title, String message) {
        this(VaadinIcon.INBOX, title, message);
    }

    public EmptyState(VaadinIcon iconType, String title, String message) {
        this.setClassName("empty-state");
        this.getElement().setAttribute("role", "status");
        this.setAlignItems(Alignment.CENTER);
        this.setJustifyContentMode(JustifyContentMode.CENTER);
        this.setSizeFull();

        icon = iconType.create();
        icon.setClassName("empty-state-icon");

        titleLabel = new H2(title);
        titleLabel.setClassName("empty-state-title");

        messageLabel = new Span(message);
        messageLabel.setClassName("empty-state-message");

        add(icon, titleLabel, messageLabel);
    }
}
