package com.gymtracker.view.mesocycle;

import com.gymtracker.dto.mesocycle.MesocycleWorkoutDayDTO;
import com.gymtracker.dto.mesocycle.MesocycleWorkoutExerciseDTO;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import java.util.ArrayList;
import java.util.List;

/**
 * Editor for a mesocycle's weekly training plan: training days containing
 * exercises, with drag-and-drop support (Vaadin's native DnD, {@code
 * com.vaadin.flow.component.dnd}) to move an exercise from one day to
 * another. Exercises are identified by id, entered manually, since this
 * component only depends on mesocycle data - it does not consume
 * ExerciseService.
 */
public class WeeklyPlannerComponent extends VerticalLayout {

    private final VerticalLayout daysContainer = new VerticalLayout();
    private final List<DayCard> dayCards = new ArrayList<>();
    private ExerciseRow draggedRow;
    private DayCard draggedRowOrigin;

    public WeeklyPlannerComponent() {
        setPadding(false);
        setSpacing(true);

        daysContainer.setPadding(false);
        daysContainer.setSpacing(true);

        Button addDayButton = new Button("Add Training Day", VaadinIcon.PLUS.create(), event -> addDay("Day " + (dayCards.size() + 1)));
        addDayButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        add(daysContainer, addDayButton);
    }

    public void setValue(List<MesocycleWorkoutDayDTO> days) {
        daysContainer.removeAll();
        dayCards.clear();
        if (days != null) {
            days.forEach(day -> addDay(day.getDayName(), day.getExercises()));
        }
    }

    public List<MesocycleWorkoutDayDTO> getValue() {
        return dayCards.stream()
                .map(DayCard::toDTO)
                .toList();
    }

    public boolean isValid() {
        return !dayCards.isEmpty() && dayCards.stream().allMatch(DayCard::hasExercises);
    }

    private void addDay(String dayName) {
        addDay(dayName, List.of());
    }

    private void addDay(String dayName, List<MesocycleWorkoutExerciseDTO> exercises) {
        DayCard card = new DayCard(dayName);
        exercises.forEach(card::addExerciseRow);
        dayCards.add(card);
        daysContainer.add(card);
    }

    /**
     * One training day: a drop target for exercise rows dragged from other
     * days, plus a form to add new exercise rows.
     */
    private final class DayCard extends VerticalLayout {

        private final TextField dayNameField;
        private final VerticalLayout rowsContainer = new VerticalLayout();

        private DayCard(String dayName) {
            setClassName("planner-day-card");
            setPadding(true);
            setSpacing(true);
            getStyle().set("border", "1px solid var(--lumo-contrast-20pct)").set("border-radius", "8px");

            dayNameField = new TextField();
            dayNameField.setValue(dayName);
            dayNameField.setWidth("200px");

            Button removeDayButton = new Button(VaadinIcon.TRASH.create(), event -> {
                dayCards.remove(this);
                daysContainer.remove(this);
            });
            removeDayButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
            removeDayButton.setAriaLabel("Remove training day");

            HorizontalLayout header = new HorizontalLayout(new H4("Training Day"), dayNameField, removeDayButton);
            header.setAlignItems(Alignment.CENTER);

            rowsContainer.setPadding(false);
            rowsContainer.setSpacing(false);

            DropTarget<VerticalLayout> dropTarget = DropTarget.create(rowsContainer);
            dropTarget.addDropListener(event -> {
                if (draggedRow != null && draggedRowOrigin != this) {
                    draggedRowOrigin.rowsContainer.remove(draggedRow);
                    rowsContainer.add(draggedRow);
                }
            });

            Button addExerciseButton = new Button("Add Exercise", VaadinIcon.PLUS.create(),
                    event -> addExerciseRow(null));
            addExerciseButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

            add(header, rowsContainer, addExerciseButton);
        }

        private void addExerciseRow(MesocycleWorkoutExerciseDTO existing) {
            ExerciseRow row = new ExerciseRow(this, existing);
            rowsContainer.add(row);
        }

        private boolean hasExercises() {
            return rowsContainer.getComponentCount() > 0;
        }

        private MesocycleWorkoutDayDTO toDTO() {
            List<MesocycleWorkoutExerciseDTO> exercises = rowsContainer.getChildren()
                    .filter(ExerciseRow.class::isInstance)
                    .map(ExerciseRow.class::cast)
                    .map(ExerciseRow::toDTO)
                    .toList();
            return MesocycleWorkoutDayDTO.builder()
                    .dayName(dayNameField.getValue())
                    .exercises(exercises)
                    .build();
        }
    }

    /**
     * One planned exercise entry within a day. Draggable so it can be
     * reassigned to a different training day.
     */
    private final class ExerciseRow extends HorizontalLayout {

        private final TextField exerciseIdField = new TextField();
        private final IntegerField setsField = new IntegerField();
        private final IntegerField repetitionsField = new IntegerField();
        private final NumberField targetWeightField = new NumberField();
        private final IntegerField targetRpeField = new IntegerField();

        private ExerciseRow(DayCard owner, MesocycleWorkoutExerciseDTO existing) {
            setClassName("planner-exercise-row");
            setSpacing(true);
            setAlignItems(Alignment.BASELINE);
            getStyle().set("padding", "4px 0").set("flex-wrap", "wrap");

            exerciseIdField.setPlaceholder("Exercise ID");
            exerciseIdField.setWidth("160px");
            exerciseIdField.setAriaLabel("Exercise ID");
            setsField.setPlaceholder("Sets");
            setsField.setWidth("80px");
            setsField.setAriaLabel("Sets");
            repetitionsField.setPlaceholder("Reps");
            repetitionsField.setWidth("80px");
            repetitionsField.setAriaLabel("Repetitions");
            targetWeightField.setPlaceholder("Weight (kg)");
            targetWeightField.setWidth("110px");
            targetWeightField.setAriaLabel("Target weight in kilograms");
            targetRpeField.setPlaceholder("RPE");
            targetRpeField.setWidth("80px");
            targetRpeField.setAriaLabel("Target RPE");
            targetRpeField.setMin(1);
            targetRpeField.setMax(10);

            if (existing != null) {
                exerciseIdField.setValue(existing.getExerciseId() != null ? existing.getExerciseId() : "");
                setsField.setValue(existing.getSets());
                repetitionsField.setValue(existing.getRepetitions());
                targetWeightField.setValue(existing.getTargetWeight());
                targetRpeField.setValue(existing.getTargetRpe());
            }

            Button removeButton = new Button(VaadinIcon.CLOSE_SMALL.create(), event -> owner.rowsContainer.remove(this));
            removeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            removeButton.setAriaLabel("Remove exercise");

            add(exerciseIdField, setsField, repetitionsField, targetWeightField, targetRpeField, removeButton);

            DragSource<HorizontalLayout> dragSource = DragSource.create(this);
            dragSource.setDraggable(true);
            dragSource.addDragStartListener(event -> {
                draggedRow = this;
                draggedRowOrigin = owner;
            });
            dragSource.addDragEndListener(event -> {
                draggedRow = null;
                draggedRowOrigin = null;
            });
        }

        private MesocycleWorkoutExerciseDTO toDTO() {
            return MesocycleWorkoutExerciseDTO.builder()
                    .exerciseId(exerciseIdField.getValue())
                    .sets(setsField.getValue())
                    .repetitions(repetitionsField.getValue())
                    .targetWeight(targetWeightField.getValue())
                    .targetRpe(targetRpeField.getValue())
                    .build();
        }
    }
}
