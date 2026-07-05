package com.gymtracker.view.alert;

import com.gymtracker.dto.alert.AlertDTO;
import com.gymtracker.ui.component.NotificationBadge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Read-only dialog with the full picture of an alert. Purely presentational;
 * the owning view supplies the already-loaded DTO (no extra service call is
 * needed since AlertService's list methods already return the full DTO).
 */
public class AlertDetailsDialog extends Dialog {

    private final VerticalLayout content = new VerticalLayout();

    public AlertDetailsDialog() {
        setClassName("app-dialog");
        setWidth("520px");
        setMaxWidth("95vw");
        setHeaderTitle("Alert Details");

        Button closeButton = new Button(VaadinIcon.CLOSE.create(), event -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        closeButton.setAriaLabel("Close");
        getHeader().add(closeButton);

        content.setPadding(false);
        content.setSpacing(false);
        add(content);
    }

    public void showDetails(AlertDTO alert) {
        content.removeAll();

        HorizontalLayout typeAndStatus = new HorizontalLayout();
        typeAndStatus.setWidthFull();
        typeAndStatus.setJustifyContentMode(JustifyContentMode.BETWEEN);
        typeAndStatus.add(new Span(alert.getType()), renderStatusBadge(alert));

        content.add(typeAndStatus,
                infoRow("Priority", AlertGrid.resolvePriority(alert.getType())),
                infoRow("Athlete", valueOrDash(alert.getAthleteName())),
                infoRow("Assigned Coach", valueOrDash(alert.getCoachName())),
                infoRow("Message", alert.getMessage()),
                infoRow("Created At", String.valueOf(alert.getGeneratedAt())),
                infoRow("Reviewed At", alert.getReviewedAt() != null ? alert.getReviewedAt().toString() : "-")
        );
        open();
    }

    private NotificationBadge renderStatusBadge(AlertDTO alert) {
        return switch (alert.getStatus()) {
            case ACTIVE -> new NotificationBadge("Active", NotificationBadge.BadgeType.ERROR);
            case ACKNOWLEDGED -> new NotificationBadge("Acknowledged", NotificationBadge.BadgeType.WARNING);
            case RESOLVED -> new NotificationBadge("Resolved", NotificationBadge.BadgeType.SUCCESS);
        };
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

    private String valueOrDash(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }
}
