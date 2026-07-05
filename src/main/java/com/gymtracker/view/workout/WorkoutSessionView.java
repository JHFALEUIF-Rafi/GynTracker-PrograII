package com.gymtracker.view.workout;

import com.gymtracker.dto.workout.WorkoutSessionRequestDTO;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.service.WorkoutSessionService;
import com.gymtracker.ui.component.ConfirmDialog;
import com.gymtracker.ui.component.Notifications;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import lombok.extern.slf4j.Slf4j;

/**
 * In-progress workout editor: shown by {@link WorkoutView} while
 * {@link ActiveWorkoutDraft} is active. Lets the athlete assign exercises and
 * sets, then finish (persisting through WorkoutSessionService) or cancel
 * (discarding the draft - nothing was ever persisted).
 */
@Slf4j
public class WorkoutSessionView extends VerticalLayout {

    private final WorkoutSessionService workoutSessionService;
    private final ActiveWorkoutDraft draft;
    private final Runnable onFinished;
    private final Runnable onCancelled;
    private final TextField mesocycleIdField = new TextField("Mesocycle ID");
    private final IntegerField durationField = new IntegerField("Duration (minutes)");

    public WorkoutSessionView(WorkoutSessionService workoutSessionService, ActiveWorkoutDraft draft,
                               Runnable onFinished, Runnable onCancelled) {
        this.workoutSessionService = workoutSessionService;
        this.draft = draft;
        this.onFinished = onFinished;
        this.onCancelled = onCancelled;

        setPadding(false);
        setSpacing(true);

        H3 title = new H3("Workout in Progress");
        Span elapsed = new Span("Elapsed: " + draft.getElapsedMinutes() + " min");
        elapsed.getStyle().set("color", "var(--lumo-secondary-text-color)");

        mesocycleIdField.setValue(draft.getMesocycleId() != null ? draft.getMesocycleId() : "");
        mesocycleIdField.setWidth("260px");
        mesocycleIdField.addValueChangeListener(event -> draft.setMesocycleId(event.getValue()));

        durationField.setMin(1);
        durationField.setValue(Math.max(draft.getElapsedMinutes(), 1));
        durationField.setWidth("200px");

        WorkoutExerciseEditor exerciseEditor = new WorkoutExerciseEditor(draft.getExercises());

        Button finishButton = new Button("Finish Workout", VaadinIcon.CHECK_CIRCLE.create(), event -> attemptFinish(exerciseEditor));
        finishButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Button cancelButton = new Button("Cancel Workout", VaadinIcon.CLOSE.create(), event -> confirmCancel());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

        HorizontalLayout actions = new HorizontalLayout(cancelButton, finishButton);
        actions.getStyle().set("flex-wrap", "wrap");

        HorizontalLayout headerRow = new HorizontalLayout(title, elapsed);
        headerRow.setAlignItems(Alignment.CENTER);
        headerRow.getStyle().set("flex-wrap", "wrap");

        HorizontalLayout fieldsRow = new HorizontalLayout(mesocycleIdField, durationField);
        fieldsRow.getStyle().set("flex-wrap", "wrap");

        add(headerRow, fieldsRow, exerciseEditor, actions);
    }

    private void attemptFinish(WorkoutExerciseEditor exerciseEditor) {
        if (mesocycleIdField.getValue() == null || mesocycleIdField.getValue().isBlank()) {
            showError("Mesocycle ID is required.");
            return;
        }
        if (durationField.getValue() == null || durationField.getValue() <= 0) {
            showError("Duration must be greater than zero.");
            return;
        }
        if (!exerciseEditor.isValid()) {
            showError("Add at least one exercise with valid sets (weight, reps and RPE 1-10) before finishing.");
            return;
        }

        try {
            WorkoutSessionRequestDTO requestDTO = WorkoutSessionRequestDTO.builder()
                    .athleteId(draft.getAthleteId())
                    .mesocycleId(draft.getMesocycleId())
                    .date(draft.getDate())
                    .durationMinutes(durationField.getValue())
                    .completedExercises(draft.getExercises())
                    .build();

            workoutSessionService.createWorkoutSession(requestDTO);
            draft.finish();
            showSuccess("Workout completed successfully.");
            onFinished.run();
        } catch (ValidationException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            log.error("Error finishing workout session", e);
            showError("Could not save the workout. Please try again.");
        }
    }

    private void confirmCancel() {
        ConfirmDialog confirmDialog = new ConfirmDialog("Cancel Workout",
                "Discard this workout? Nothing recorded so far will be saved.");
        confirmDialog.setOnConfirm(() -> {
            draft.cancel();
            onCancelled.run();
        });
        confirmDialog.open();
    }

    private void showSuccess(String message) {
        Notifications.success(message);
    }

    private void showError(String message) {
        Notifications.error(message);
    }
}
