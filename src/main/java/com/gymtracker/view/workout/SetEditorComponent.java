package com.gymtracker.view.workout;

import com.gymtracker.dto.workout.WorkoutSetDTO;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import java.util.List;

/**
 * Editor for the sets of one workout exercise: weight, repetitions, RPE and
 * rest time. Rest time is a UI-only pacing aid - WorkoutSetDTO has no such
 * field, so it is never sent to the backend. Edits write directly into the
 * mutable WorkoutSetDTO instances supplied by the caller.
 */
public class SetEditorComponent extends VerticalLayout {

    private final List<WorkoutSetDTO> sets;
    private final VerticalLayout rowsContainer = new VerticalLayout();

    public SetEditorComponent(List<WorkoutSetDTO> sets) {
        this.sets = sets;

        setPadding(false);
        setSpacing(false);

        rowsContainer.setPadding(false);
        rowsContainer.setSpacing(false);
        sets.forEach(this::renderRow);

        Button addSetButton = new Button("Add Set", VaadinIcon.PLUS.create(), event -> addSet());
        addSetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        add(rowsContainer, addSetButton);
    }

    private void addSet() {
        WorkoutSetDTO set = WorkoutSetDTO.builder().build();
        sets.add(set);
        renderRow(set);
    }

    private void renderRow(WorkoutSetDTO set) {
        NumberField weightField = new NumberField();
        weightField.setPlaceholder("Weight (kg)");
        weightField.setAriaLabel("Weight in kilograms");
        weightField.setWidth("120px");
        weightField.setMin(0.1);
        weightField.setValue(set.getWeight());
        weightField.addValueChangeListener(event -> set.setWeight(event.getValue()));

        IntegerField repetitionsField = new IntegerField();
        repetitionsField.setPlaceholder("Reps");
        repetitionsField.setAriaLabel("Repetitions");
        repetitionsField.setWidth("90px");
        repetitionsField.setMin(1);
        repetitionsField.setValue(set.getRepetitions());
        repetitionsField.addValueChangeListener(event -> set.setRepetitions(event.getValue()));

        IntegerField rpeField = new IntegerField();
        rpeField.setPlaceholder("RPE");
        rpeField.setAriaLabel("RPE");
        rpeField.setWidth("90px");
        rpeField.setMin(1);
        rpeField.setMax(10);
        rpeField.setValue(set.getRpe());
        rpeField.addValueChangeListener(event -> set.setRpe(event.getValue()));

        IntegerField restTimeField = new IntegerField();
        restTimeField.setPlaceholder("Rest (sec)");
        restTimeField.setAriaLabel("Rest time in seconds");
        restTimeField.setWidth("110px");
        restTimeField.setMin(0);

        HorizontalLayout row = new HorizontalLayout(weightField, repetitionsField, rpeField, restTimeField);
        row.setAlignItems(Alignment.BASELINE);
        row.setSpacing(true);
        row.getStyle().set("flex-wrap", "wrap");

        Button removeButton = new Button(VaadinIcon.CLOSE_SMALL.create(), event -> {
            sets.remove(set);
            rowsContainer.remove(row);
        });
        removeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        removeButton.setAriaLabel("Remove set");
        row.add(removeButton);

        rowsContainer.add(row);
    }
}
