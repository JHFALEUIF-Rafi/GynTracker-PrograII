package com.gymtracker.view.exercise;

import com.gymtracker.dto.exercise.ExerciseDetailDTO;
import com.gymtracker.dto.exercise.ExerciseRequestDTO;
import com.gymtracker.enums.Difficulty;
import com.gymtracker.enums.Equipment;
import com.gymtracker.enums.ExerciseType;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Presentational exercise create/edit form. Validation is sourced from the
 * Bean Validation constraints already declared on {@link ExerciseRequestDTO},
 * so no rule is duplicated here. Status is intentionally not editable through
 * this form: exercises start ACTIVE and are only ever deactivated through the
 * dedicated action, never reactivated from a raw field.
 */
public class ExerciseForm extends FormLayout {

    private final TextField nameField = new TextField("Name");
    private final TextField primaryMuscleField = new TextField("Primary Muscle");
    private final MultiSelectComboBox<String> secondaryMusclesField = new MultiSelectComboBox<>("Secondary Muscles");
    private final ComboBox<ExerciseType> exerciseTypeField = new ComboBox<>("Type");
    private final ComboBox<Difficulty> difficultyField = new ComboBox<>("Difficulty");
    private final ComboBox<Equipment> equipmentField = new ComboBox<>("Equipment");
    private final TextArea descriptionField = new TextArea("Description");

    private final BeanValidationBinder<ExerciseRequestDTO> binder = new BeanValidationBinder<>(ExerciseRequestDTO.class);
    private ExerciseRequestDTO workingCopy;

    public ExerciseForm() {
        setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep("500px", 2)
        );

        secondaryMusclesField.setAllowCustomValue(true);
        secondaryMusclesField.addCustomValueSetListener(event -> {
            Set<String> updated = new TreeSet<>(secondaryMusclesField.getValue());
            updated.add(event.getDetail());
            secondaryMusclesField.setValue(updated);
        });

        exerciseTypeField.setItems(ExerciseType.values());
        difficultyField.setItems(Difficulty.values());
        equipmentField.setItems(Equipment.values());

        descriptionField.setMaxLength(500);
        setColspan(descriptionField, 2);
        setColspan(secondaryMusclesField, 2);

        binder.forField(nameField).asRequired("Name is required.").bind("name");
        binder.forField(primaryMuscleField).asRequired("Primary muscle is required.").bind("primaryMuscle");
        binder.forField(secondaryMusclesField)
                .asRequired("At least one secondary muscle is required.")
                .bind(dto -> dto.getSecondaryMuscles() != null ? new java.util.LinkedHashSet<>(dto.getSecondaryMuscles()) : Set.of(),
                        (dto, value) -> dto.setSecondaryMuscles(new ArrayList<>(value)));
        binder.forField(exerciseTypeField).asRequired("Type is required.").bind("exerciseType");
        binder.forField(difficultyField).bind("difficulty");
        binder.forField(equipmentField).bind("equipment");
        binder.forField(descriptionField).asRequired("Description is required.").bind("description");

        add(nameField, primaryMuscleField, secondaryMusclesField, exerciseTypeField,
                difficultyField, equipmentField, descriptionField);
    }

    public void setNewExercise() {
        workingCopy = ExerciseRequestDTO.builder().secondaryMuscles(new ArrayList<>()).build();
        binder.setBean(workingCopy);
    }

    public void setExercise(ExerciseDetailDTO detail) {
        List<String> secondaryMuscles = detail.getSecondaryMuscles() != null
                ? new ArrayList<>(detail.getSecondaryMuscles()) : new ArrayList<>();

        workingCopy = ExerciseRequestDTO.builder()
                .name(detail.getName())
                .primaryMuscle(detail.getPrimaryMuscle())
                .secondaryMuscles(secondaryMuscles)
                .exerciseType(detail.getExerciseType())
                .description(detail.getDescription())
                .difficulty(detail.getDifficulty())
                .equipment(detail.getEquipment())
                .build();
        binder.setBean(workingCopy);
    }

    public boolean isValid() {
        return binder.isValid();
    }

    public ExerciseRequestDTO getValue() {
        return workingCopy;
    }
}
