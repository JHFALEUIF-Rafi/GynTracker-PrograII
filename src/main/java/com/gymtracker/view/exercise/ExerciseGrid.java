package com.gymtracker.view.exercise;

import com.gymtracker.dto.exercise.ExerciseSummaryDTO;
import com.gymtracker.enums.ExerciseStatus;
import com.gymtracker.ui.component.NotificationBadge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.function.SerializablePredicate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Presentational grid listing the exercise catalog. Filtering predicates and
 * row actions are supplied by the owning view; edit/deactivate actions are
 * only rendered when write access is enabled (Coach role).
 */
public class ExerciseGrid extends VerticalLayout {

    private final Grid<ExerciseSummaryDTO> grid = new Grid<>(ExerciseSummaryDTO.class, false);
    private Consumer<ExerciseSummaryDTO> onViewDetails;
    private Consumer<ExerciseSummaryDTO> onEdit;
    private Consumer<ExerciseSummaryDTO> onDeactivate;
    private boolean writeAccess;

    public ExerciseGrid() {
        setPadding(false);
        setSpacing(false);
        setSizeFull();

        grid.setSizeFull();
        grid.getElement().setAttribute("aria-label", "Exercises");
        grid.addColumn(ExerciseSummaryDTO::getName).setHeader("Name").setAutoWidth(true).setSortable(true);
        grid.addColumn(ExerciseSummaryDTO::getPrimaryMuscle).setHeader("Muscle Group").setAutoWidth(true).setSortable(true);
        grid.addColumn(exercise -> exercise.getExerciseType() != null ? exercise.getExerciseType().name() : "-")
                .setHeader("Type").setAutoWidth(true);
        grid.addColumn(exercise -> exercise.getDifficulty() != null ? exercise.getDifficulty().name() : "-")
                .setHeader("Difficulty").setAutoWidth(true);
        grid.addColumn(exercise -> exercise.getEquipment() != null ? exercise.getEquipment().name() : "-")
                .setHeader("Equipment").setAutoWidth(true);
        grid.addComponentColumn(this::renderStatusBadge).setHeader("Status").setAutoWidth(true);
        grid.addComponentColumn(this::renderActions).setHeader("").setAutoWidth(true).setFlexGrow(0);

        grid.setItems(new ArrayList<>());
        add(grid);
    }

    public void setItems(List<ExerciseSummaryDTO> exercises) {
        grid.setItems(exercises);
    }

    public void setFilter(SerializablePredicate<ExerciseSummaryDTO> filter) {
        ((ListDataProvider<ExerciseSummaryDTO>) grid.getDataProvider()).setFilter(filter);
    }

    public void setWriteAccess(boolean writeAccess) {
        this.writeAccess = writeAccess;
        grid.getDataProvider().refreshAll();
    }

    public void setOnViewDetails(Consumer<ExerciseSummaryDTO> onViewDetails) {
        this.onViewDetails = onViewDetails;
    }

    public void setOnEdit(Consumer<ExerciseSummaryDTO> onEdit) {
        this.onEdit = onEdit;
    }

    public void setOnDeactivate(Consumer<ExerciseSummaryDTO> onDeactivate) {
        this.onDeactivate = onDeactivate;
    }

    private NotificationBadge renderStatusBadge(ExerciseSummaryDTO exercise) {
        boolean active = exercise.getStatus() == ExerciseStatus.ACTIVE;
        return new NotificationBadge(active ? "Active" : "Inactive",
                active ? NotificationBadge.BadgeType.SUCCESS : NotificationBadge.BadgeType.NEUTRAL);
    }

    private HorizontalLayout renderActions(ExerciseSummaryDTO exercise) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(false);

        Button viewButton = new Button(VaadinIcon.EYE.create());
        viewButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        viewButton.setAriaLabel("View details");
        viewButton.addClickListener(event -> {
            if (onViewDetails != null) {
                onViewDetails.accept(exercise);
            }
        });
        actions.add(viewButton);

        if (writeAccess) {
            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            editButton.setAriaLabel("Edit exercise");
            editButton.addClickListener(event -> {
                if (onEdit != null) {
                    onEdit.accept(exercise);
                }
            });
            actions.add(editButton);

            if (exercise.getStatus() == ExerciseStatus.ACTIVE) {
                Button deactivateButton = new Button(VaadinIcon.BAN.create());
                deactivateButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
                deactivateButton.setAriaLabel("Deactivate exercise");
                deactivateButton.addClickListener(event -> {
                    if (onDeactivate != null) {
                        onDeactivate.accept(exercise);
                    }
                });
                actions.add(deactivateButton);
            }
        }

        return actions;
    }
}
