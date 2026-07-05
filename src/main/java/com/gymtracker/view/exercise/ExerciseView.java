package com.gymtracker.view.exercise;

import com.gymtracker.dto.exercise.ExerciseRequestDTO;
import com.gymtracker.dto.exercise.ExerciseSummaryDTO;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.service.ExerciseService;
import com.gymtracker.ui.component.ConfirmDialog;
import com.gymtracker.ui.component.EmptyState;
import com.gymtracker.ui.component.LoadingSpinner;
import com.gymtracker.ui.component.Notifications;
import com.gymtracker.ui.component.Toolbar;
import com.gymtracker.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Exercise catalog screen. Coaches have full CRUD (create, update,
 * deactivate); Athletes and Nutritionists have read-only access. No business
 * logic lives here - every operation is delegated to {@link ExerciseService}.
 */
@Slf4j
@Route(value = "exercises", layout = MainLayout.class)
@PageTitle("Exercises - GymTracker")
public class ExerciseView extends VerticalLayout implements BeforeEnterObserver {

    private final ExerciseService exerciseService;
    private final VerticalLayout contentLayout = new VerticalLayout();
    private final ExerciseFilterBar filterBar = new ExerciseFilterBar();
    private final ExerciseGrid exerciseGrid = new ExerciseGrid();
    private final ExerciseDetailsDialog detailsDialog = new ExerciseDetailsDialog();
    private final Toolbar toolbar = new Toolbar("Exercises");

    private boolean writeAccess;
    private List<ExerciseSummaryDTO> allExercises = List.of();

    public ExerciseView(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        setSizeFull();

        filterBar.setOnFilterChange(this::applyFilter);
        exerciseGrid.setOnViewDetails(this::openDetails);
        exerciseGrid.setOnEdit(this::openEditForm);
        exerciseGrid.setOnDeactivate(this::confirmDeactivate);

        add(toolbar, contentLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        writeAccess = getAuthenticatedRole() == Role.COACH;
        exerciseGrid.setWriteAccess(writeAccess);
        setupToolbarActions();
        loadExercises();
    }

    private void setupToolbarActions() {
        if (!writeAccess) {
            return;
        }
        Button newExerciseButton = new Button("New Exercise", VaadinIcon.PLUS.create(), event -> openCreateForm());
        newExerciseButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        toolbar.addAction(newExerciseButton);
    }

    private void loadExercises() {
        contentLayout.removeAll();
        contentLayout.add(new LoadingSpinner());

        try {
            allExercises = exerciseService.getAllExercises();
            contentLayout.removeAll();

            if (allExercises.isEmpty()) {
                contentLayout.add(new EmptyState(VaadinIcon.HAMMER, "No Exercises", "The exercise catalog is empty."));
                return;
            }

            exerciseGrid.setItems(allExercises);
            contentLayout.setSizeFull();
            contentLayout.add(filterBar, exerciseGrid);
            contentLayout.setFlexGrow(1, exerciseGrid);
        } catch (Exception e) {
            log.error("Error loading exercise catalog", e);
            contentLayout.removeAll();
            contentLayout.add(new EmptyState(VaadinIcon.WARNING, "Something Went Wrong", "Could not load the exercise catalog. Please try again."));
        }
    }

    private void applyFilter() {
        String searchTerm = filterBar.getSearchTerm();
        String normalizedSearch = searchTerm != null ? searchTerm.trim().toLowerCase(Locale.ROOT) : "";

        exerciseGrid.setFilter(exercise -> matchesSearch(exercise, normalizedSearch)
                && (filterBar.getType() == null || exercise.getExerciseType() == filterBar.getType())
                && (filterBar.getDifficulty() == null || exercise.getDifficulty() == filterBar.getDifficulty())
                && (filterBar.getEquipment() == null || exercise.getEquipment() == filterBar.getEquipment())
                && (filterBar.getStatus() == null || exercise.getStatus() == filterBar.getStatus()));
    }

    private boolean matchesSearch(ExerciseSummaryDTO exercise, String normalizedSearch) {
        return normalizedSearch.isBlank()
                || (exercise.getName() != null && exercise.getName().toLowerCase(Locale.ROOT).contains(normalizedSearch));
    }

    private void openDetails(ExerciseSummaryDTO exercise) {
        try {
            detailsDialog.showDetails(exerciseService.getExerciseById(exercise.getId()));
        } catch (Exception e) {
            log.error("Error loading exercise details for id={}", exercise.getId(), e);
            showError("Could not load exercise details.");
        }
    }

    private void openCreateForm() {
        ExerciseForm form = new ExerciseForm();
        form.setNewExercise();
        openFormDialog("New Exercise", form, () -> {
            ExerciseRequestDTO requestDTO = form.getValue();
            exerciseService.createExercise(requestDTO);
            showSuccess("Exercise created successfully.");
        });
    }

    private void openEditForm(ExerciseSummaryDTO exercise) {
        try {
            ExerciseForm form = new ExerciseForm();
            form.setExercise(exerciseService.getExerciseById(exercise.getId()));
            openFormDialog("Edit Exercise", form, () -> {
                ExerciseRequestDTO requestDTO = form.getValue();
                exerciseService.updateExercise(exercise.getId(), requestDTO);
                showSuccess("Exercise updated successfully.");
            });
        } catch (Exception e) {
            log.error("Error loading exercise for edit, id={}", exercise.getId(), e);
            showError("Could not load exercise for editing.");
        }
    }

    private void openFormDialog(String title, ExerciseForm form, Runnable onSave) {
        Dialog dialog = new Dialog();
        dialog.setClassName("app-dialog");
        dialog.setHeaderTitle(title);
        dialog.setWidth("640px");
        dialog.setMaxWidth("95vw");
        dialog.add(form);

        Button saveButton = new Button("Save", VaadinIcon.CHECK.create(), event -> {
            if (!form.isValid()) {
                showError("Please review the highlighted fields before saving.");
                return;
            }
            try {
                onSave.run();
                dialog.close();
                loadExercises();
            } catch (ValidationException e) {
                showError(e.getMessage());
            } catch (Exception e) {
                log.error("Error saving exercise", e);
                showError("Could not save the exercise. Please try again.");
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", VaadinIcon.CLOSE.create(), event -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(new HorizontalLayout(cancelButton, saveButton));
        dialog.open();
    }

    private void confirmDeactivate(ExerciseSummaryDTO exercise) {
        ConfirmDialog confirmDialog = new ConfirmDialog("Deactivate Exercise",
                "Deactivate \"" + exercise.getName() + "\"? It will no longer be available for new mesocycles.");
        confirmDialog.setOnConfirm(() -> {
            try {
                exerciseService.deactivateExercise(exercise.getId());
                showSuccess("Exercise deactivated successfully.");
                loadExercises();
            } catch (Exception e) {
                log.error("Error deactivating exercise id={}", exercise.getId(), e);
                showError("Could not deactivate the exercise. Please try again.");
            }
        });
        confirmDialog.open();
    }

    private Role getAuthenticatedRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getAuthorities().stream()
                .map(authority -> mapAuthorityToRole(authority.getAuthority()))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Role mapAuthorityToRole(String authority) {
        if (authority.contains("ATHLETE")) {
            return Role.ATHLETE;
        }
        if (authority.contains("COACH")) {
            return Role.COACH;
        }
        if (authority.contains("NUTRITIONIST")) {
            return Role.NUTRITIONIST;
        }
        return null;
    }

    private void showSuccess(String message) {
        Notifications.success(message);
    }

    private void showError(String message) {
        Notifications.error(message);
    }
}
