package com.gymtracker.view.exercise;

import com.gymtracker.dto.exercise.ExerciseDetailDTO;
import com.gymtracker.ui.component.NotificationBadge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.gymtracker.enums.ExerciseStatus;

/**
 * Read-only dialog with the full picture of an exercise. Purely
 * presentational; the owning view supplies the already-fetched DTO.
 */
public class ExerciseDetailsDialog extends Dialog {

    private final VerticalLayout content = new VerticalLayout();

    public ExerciseDetailsDialog() {
        setClassName("app-dialog");
        setWidth("560px");
        setMaxWidth("95vw");
        setHeaderTitle("Exercise Details");

        Button closeButton = new Button(VaadinIcon.CLOSE.create(), event -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        closeButton.setAriaLabel("Close");
        getHeader().add(closeButton);

        content.setPadding(false);
        content.setSpacing(false);
        add(content);
    }

    public void showDetails(ExerciseDetailDTO detail) {
        content.removeAll();

        HorizontalLayout nameAndStatus = new HorizontalLayout();
        nameAndStatus.setWidthFull();
        nameAndStatus.setJustifyContentMode(JustifyContentMode.BETWEEN);
        boolean active = detail.getStatus() == ExerciseStatus.ACTIVE;
        nameAndStatus.add(new Span(detail.getName()),
                new NotificationBadge(active ? "Active" : "Inactive",
                        active ? NotificationBadge.BadgeType.SUCCESS : NotificationBadge.BadgeType.NEUTRAL));

        content.add(nameAndStatus,
                infoRow("Primary Muscle", detail.getPrimaryMuscle()),
                infoRow("Secondary Muscles", detail.getSecondaryMuscles() != null && !detail.getSecondaryMuscles().isEmpty()
                        ? String.join(", ", detail.getSecondaryMuscles()) : "-"),
                infoRow("Type", detail.getExerciseType() != null ? detail.getExerciseType().name() : "-"),
                infoRow("Difficulty", detail.getDifficulty() != null ? detail.getDifficulty().name() : "-"),
                infoRow("Equipment", detail.getEquipment() != null ? detail.getEquipment().name() : "-"),
                infoRow("Description", detail.getDescription())
        );
        open();
    }

    private HorizontalLayout infoRow(String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setJustifyContentMode(JustifyContentMode.BETWEEN);

        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");

        row.add(labelSpan, new Span(value != null && !value.isBlank() ? value : "-"));
        return row;
    }
}
