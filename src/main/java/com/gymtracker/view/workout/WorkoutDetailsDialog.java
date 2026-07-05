package com.gymtracker.view.workout;

import com.gymtracker.dto.workout.WorkoutExerciseDTO;
import com.gymtracker.dto.workout.WorkoutSessionDetailDTO;
import com.gymtracker.dto.workout.WorkoutSetDTO;
import com.gymtracker.service.FatigueService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.extern.slf4j.Slf4j;

/**
 * Read-only dialog with the full picture of a completed workout session:
 * duration, total volume, average RPE, estimated 1RM, current fatigue level
 * (best-effort, via FatigueService) and the exercise/set breakdown. Purely
 * presentational; the owning view supplies the already-fetched DTO.
 */
@Slf4j
public class WorkoutDetailsDialog extends Dialog {

    private final FatigueService fatigueService;
    private final VerticalLayout content = new VerticalLayout();

    public WorkoutDetailsDialog(FatigueService fatigueService) {
        this.fatigueService = fatigueService;

        setClassName("app-dialog");
        setWidth("600px");
        setMaxWidth("95vw");
        setHeaderTitle("Workout Details");

        Button closeButton = new Button(VaadinIcon.CLOSE.create(), event -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        closeButton.setAriaLabel("Close");
        getHeader().add(closeButton);

        content.setPadding(false);
        content.setSpacing(true);
        add(content);
    }

    public void showDetails(WorkoutSessionDetailDTO detail) {
        content.removeAll();

        content.add(
                infoRow("Date", String.valueOf(detail.getDate())),
                infoRow("Status", detail.getStatus() != null ? detail.getStatus().name() : "-"),
                infoRow("Duration", detail.getDurationMinutes() + " min"),
                infoRow("Total Volume", formatNumber(detail.getTotalVolume()) + " kg"),
                infoRow("Average RPE", formatNumber(detail.getFatigueScore())),
                infoRow("Estimated 1RM", detail.getEstimatedOneRepMax() != null
                        ? formatNumber(detail.getEstimatedOneRepMax()) + " kg" : "Not available"),
                infoRow("Fatigue Level", resolveFatigueLevel(detail.getAthleteId())),
                buildExercisesSection(detail)
        );
        open();
    }

    private String resolveFatigueLevel(String athleteId) {
        try {
            return fatigueService.getCurrentFatigueLevel(athleteId).name();
        } catch (Exception e) {
            log.warn("Fatigue level not available for athleteId={}", athleteId);
            return "Not available";
        }
    }

    private VerticalLayout buildExercisesSection(WorkoutSessionDetailDTO detail) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.add(new H4("Exercises"));

        if (detail.getCompletedExercises() == null || detail.getCompletedExercises().isEmpty()) {
            section.add(new Span("No exercises recorded."));
            return section;
        }

        for (WorkoutExerciseDTO exercise : detail.getCompletedExercises()) {
            Span exerciseTitle = new Span("Exercise " + exercise.getExerciseId());
            exerciseTitle.getStyle().set("font-weight", "600");
            section.add(exerciseTitle);

            for (WorkoutSetDTO set : exercise.getSets()) {
                Span setLine = new Span(String.format("%.1f kg x %d reps (RPE %d)",
                        set.getWeight(), set.getRepetitions(), set.getRpe()));
                setLine.getStyle().set("color", "var(--lumo-secondary-text-color)").set("display", "block");
                section.add(setLine);
            }
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

    private String formatNumber(Double value) {
        return value != null ? String.format("%.1f", value) : "-";
    }
}
