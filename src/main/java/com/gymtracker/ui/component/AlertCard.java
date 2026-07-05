package com.gymtracker.ui.component;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Reusable component for displaying an alert or notification card.
 */
public class AlertCard extends VerticalLayout {

    private final Icon icon;
    private final Span typeLabel;
    private final Span messageLabel;
    private final Span statusLabel;

    public AlertCard(String type, String message, String status) {
        this.setClassName("alert-card");
        this.getElement().setAttribute("role", "listitem");
        this.setPadding(true);
        this.setSpacing(true);

        icon = VaadinIcon.BELL.create();
        icon.setClassName("alert-icon");

        typeLabel = new Span(type);
        typeLabel.setClassName("alert-type");

        messageLabel = new Span(message);
        messageLabel.setClassName("alert-message");

        statusLabel = new Span(status);
        statusLabel.setClassName("alert-status");

        HorizontalLayout typeArea = new HorizontalLayout(icon, typeLabel);
        typeArea.setAlignItems(Alignment.CENTER);
        typeArea.setSpacing(true);

        HorizontalLayout header = new HorizontalLayout(typeArea, statusLabel);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        add(header, messageLabel);
    }

    public void setType(String type) {
        typeLabel.setText(type);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }
}
