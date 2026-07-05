package com.gymtracker.view.mesocycle;

import com.gymtracker.dto.mesocycle.MesocycleDetailDTO;
import com.gymtracker.dto.mesocycle.MesocycleWorkoutDayDTO;
import com.gymtracker.dto.mesocycle.MesocycleWorkoutExerciseDTO;
import com.gymtracker.ui.component.NotificationBadge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Read-only dialog with the full picture of a mesocycle: identification,
 * schedule and the full weekly plan. Purely presentational; the owning view
 * supplies the already-fetched DTO.
 */
public class MesocycleDetailsDialog extends Dialog {

    private final VerticalLayout content = new VerticalLayout();

    public MesocycleDetailsDialog() {
        setClassName("app-dialog");
        setWidth("640px");
        setMaxWidth("95vw");
        setHeaderTitle("Mesocycle Details");

        Button closeButton = new Button(VaadinIcon.CLOSE.create(), event -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        closeButton.setAriaLabel("Close");
        getHeader().add(closeButton);

        content.setPadding(false);
        content.setSpacing(true);
        add(content);
    }

    public void showDetails(MesocycleDetailDTO detail) {
        content.removeAll();

        HorizontalLayout nameAndStatus = new HorizontalLayout();
        nameAndStatus.setWidthFull();
        nameAndStatus.setJustifyContentMode(JustifyContentMode.BETWEEN);
        nameAndStatus.add(new Span(detail.getName()), new NotificationBadge(detail.getStatus().name(),
                detail.getStatus().name().equals("ACTIVE") ? NotificationBadge.BadgeType.SUCCESS : NotificationBadge.BadgeType.NEUTRAL));

        content.add(nameAndStatus,
                infoRow("Athlete", valueOrDash(detail.getAthleteName())),
                infoRow("Coach", valueOrDash(detail.getCoachName())),
                infoRow("Duration", detail.getDurationWeeks() + " weeks"),
                infoRow("Target RPE", String.valueOf(detail.getTargetRpe())),
                infoRow("Notes", valueOrDash(detail.getNotes())),
                buildPlanSection(detail)
        );
        open();
    }

    private VerticalLayout buildPlanSection(MesocycleDetailDTO detail) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.add(new H4("Weekly Plan"));

        if (detail.getDays() == null || detail.getDays().isEmpty()) {
            section.add(new Span("No training days defined."));
            return section;
        }

        for (MesocycleWorkoutDayDTO day : detail.getDays()) {
            VerticalLayout dayLayout = new VerticalLayout();
            dayLayout.setPadding(false);
            dayLayout.setSpacing(false);
            dayLayout.getStyle().set("margin-bottom", "8px");

            Span dayTitle = new Span(day.getDayName());
            dayTitle.getStyle().set("font-weight", "600");
            dayLayout.add(dayTitle);

            for (MesocycleWorkoutExerciseDTO exercise : day.getExercises()) {
                Span exerciseLine = new Span(String.format("Exercise %s - %d x %d @ %.1f kg (RPE %d)",
                        exercise.getExerciseId(), exercise.getSets(), exercise.getRepetitions(),
                        exercise.getTargetWeight(), exercise.getTargetRpe()));
                exerciseLine.getStyle().set("color", "var(--lumo-secondary-text-color)").set("display", "block");
                dayLayout.add(exerciseLine);
            }
            section.add(dayLayout);
        }
        return section;
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
