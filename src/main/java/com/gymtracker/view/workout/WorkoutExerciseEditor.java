package com.gymtracker.view.workout;

import com.gymtracker.dto.workout.WorkoutExerciseDTO;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import java.util.ArrayList;
import java.util.List;

/**
 * Editor for the exercises of the in-progress workout: add/remove exercises,
 * each with its own {@link SetEditorComponent}. Exercises are identified by
 * id, entered manually, since this component depends on no exercise
 * directory (the Workout module only consumes WorkoutSessionService,
 * OneRepMaxService and FatigueService).
 */
public class WorkoutExerciseEditor extends VerticalLayout {

    private final List<WorkoutExerciseDTO> exercises;
    private final VerticalLayout exercisesContainer = new VerticalLayout();

    public WorkoutExerciseEditor(List<WorkoutExerciseDTO> exercises) {
        this.exercises = exercises;

        setPadding(false);
        setSpacing(true);

        exercisesContainer.setPadding(false);
        exercisesContainer.setSpacing(true);
        exercises.forEach(this::renderExerciseCard);

        Button addExerciseButton = new Button("Add Exercise", VaadinIcon.PLUS.create(), event -> addExercise());
        addExerciseButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        add(exercisesContainer, addExerciseButton);
    }

    private void addExercise() {
        WorkoutExerciseDTO exercise = WorkoutExerciseDTO.builder().sets(new ArrayList<>()).build();
        exercises.add(exercise);
        renderExerciseCard(exercise);
    }

    private void renderExerciseCard(WorkoutExerciseDTO exercise) {
        if (exercise.getSets() == null) {
            exercise.setSets(new ArrayList<>());
        }

        VerticalLayout card = new VerticalLayout();
        card.setClassName("planner-day-card");
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)").set("border-radius", "8px");

        TextField exerciseIdField = new TextField();
        exerciseIdField.setPlaceholder("Exercise ID");
        exerciseIdField.setAriaLabel("Exercise ID");
        exerciseIdField.setWidth("220px");
        exerciseIdField.setValue(exercise.getExerciseId() != null ? exercise.getExerciseId() : "");
        exerciseIdField.addValueChangeListener(event -> exercise.setExerciseId(event.getValue()));

        Button removeButton = new Button(VaadinIcon.TRASH.create(), event -> {
            exercises.remove(exercise);
            exercisesContainer.remove(card);
        });
        removeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
        removeButton.setAriaLabel("Remove exercise");

        HorizontalLayout header = new HorizontalLayout(new H4("Exercise"), exerciseIdField, removeButton);
        header.setAlignItems(Alignment.CENTER);
        header.getStyle().set("flex-wrap", "wrap");

        card.add(header, new SetEditorComponent(exercise.getSets()));
        exercisesContainer.add(card);
    }

    public boolean isValid() {
        return !exercises.isEmpty() && exercises.stream().allMatch(exercise ->
                exercise.getExerciseId() != null && !exercise.getExerciseId().isBlank()
                        && exercise.getSets() != null && !exercise.getSets().isEmpty()
                        && exercise.getSets().stream().allMatch(set -> set.getWeight() != null && set.getWeight() > 0
                                && set.getRepetitions() != null && set.getRepetitions() > 0
                                && set.getRpe() != null && set.getRpe() >= 1 && set.getRpe() <= 10));
    }
}
