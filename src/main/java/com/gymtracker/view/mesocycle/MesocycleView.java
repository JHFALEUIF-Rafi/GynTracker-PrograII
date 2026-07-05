package com.gymtracker.view.mesocycle;

import com.gymtracker.dto.mesocycle.MesocycleRequestDTO;
import com.gymtracker.dto.mesocycle.MesocycleSummaryDTO;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.security.CustomUserDetails;
import com.gymtracker.service.MesocycleService;
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
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Mesocycle catalog screen. Coaches have full access to the mesocycles they
 * created (create, edit, duplicate, archive); Athletes and Nutritionists have
 * read-only access. No business logic lives here - every operation is
 * delegated to {@link MesocycleService}.
 */
@Slf4j
@Route(value = "mesocycles", layout = MainLayout.class)
@PageTitle("Mesocycles - GymTracker")
@PermitAll
public class MesocycleView extends VerticalLayout implements BeforeEnterObserver {

    private final MesocycleService mesocycleService;
    private final VerticalLayout contentLayout = new VerticalLayout();
    private final MesocycleFilterBar filterBar = new MesocycleFilterBar();
    private final MesocycleGrid mesocycleGrid = new MesocycleGrid();
    private final MesocycleDetailsDialog detailsDialog = new MesocycleDetailsDialog();
    private final Toolbar toolbar = new Toolbar("Mesocycles");

    private boolean writeAccess;
    private String currentUserId;
    private List<MesocycleSummaryDTO> allMesocycles = List.of();

    public MesocycleView(MesocycleService mesocycleService) {
        this.mesocycleService = mesocycleService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        setSizeFull();

        filterBar.setOnFilterChange(this::applyFilter);
        mesocycleGrid.setOnViewDetails(this::openDetails);
        mesocycleGrid.setOnEdit(this::openEditForm);
        mesocycleGrid.setOnDuplicate(this::duplicateMesocycle);
        mesocycleGrid.setOnArchive(this::confirmArchive);

        add(toolbar, contentLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        currentUserId = getAuthenticatedUserId();
        writeAccess = getAuthenticatedRole() == Role.COACH;
        mesocycleGrid.setWriteAccess(writeAccess);
        setupToolbarActions();
        loadMesocycles();
    }

    private void setupToolbarActions() {
        if (!writeAccess) {
            return;
        }
        Button newMesocycleButton = new Button("New Mesocycle", VaadinIcon.PLUS.create(), event -> openCreateForm());
        newMesocycleButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        toolbar.addAction(newMesocycleButton);
    }

    private void loadMesocycles() {
        contentLayout.removeAll();
        contentLayout.add(new LoadingSpinner());

        try {
            allMesocycles = mesocycleService.getMesocyclesForCurrentUser();
            contentLayout.removeAll();

            if (allMesocycles.isEmpty()) {
                contentLayout.add(new EmptyState(VaadinIcon.CALENDAR, "No Mesocycles", "There are no mesocycles to display yet."));
                return;
            }

            filterBar.setCoachOptions(allMesocycles.stream()
                    .map(MesocycleSummaryDTO::getCoachName)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            mesocycleGrid.setItems(allMesocycles);
            contentLayout.setSizeFull();
            contentLayout.add(filterBar, mesocycleGrid);
            contentLayout.setFlexGrow(1, mesocycleGrid);
        } catch (Exception e) {
            log.error("Error loading mesocycles", e);
            contentLayout.removeAll();
            contentLayout.add(new EmptyState(VaadinIcon.WARNING, "Something Went Wrong", "Could not load the mesocycle list. Please try again."));
        }
    }

    private void applyFilter() {
        String searchTerm = filterBar.getSearchTerm();
        String normalizedSearch = searchTerm != null ? searchTerm.trim().toLowerCase(Locale.ROOT) : "";

        mesocycleGrid.setFilter(mesocycle -> matchesSearch(mesocycle, normalizedSearch)
                && (filterBar.getStatus() == null || mesocycle.getStatus() == filterBar.getStatus())
                && (filterBar.getCoachName() == null || filterBar.getCoachName().equals(mesocycle.getCoachName())));
    }

    private boolean matchesSearch(MesocycleSummaryDTO mesocycle, String normalizedSearch) {
        return normalizedSearch.isBlank()
                || (mesocycle.getName() != null && mesocycle.getName().toLowerCase(Locale.ROOT).contains(normalizedSearch));
    }

    private void openDetails(MesocycleSummaryDTO mesocycle) {
        try {
            detailsDialog.showDetails(mesocycleService.getMesocycleById(mesocycle.getId()));
        } catch (Exception e) {
            log.error("Error loading mesocycle details for id={}", mesocycle.getId(), e);
            showError("Could not load mesocycle details.");
        }
    }

    private void openCreateForm() {
        MesocycleForm form = new MesocycleForm();
        form.setNewMesocycle();
        form.setCoachId(currentUserId);
        openFormDialog("New Mesocycle", form, () -> {
            MesocycleRequestDTO requestDTO = form.getValue();
            mesocycleService.createMesocycle(requestDTO);
            showSuccess("Mesocycle created successfully.");
        });
    }

    private void openEditForm(MesocycleSummaryDTO mesocycle) {
        try {
            MesocycleForm form = new MesocycleForm();
            form.setMesocycle(mesocycleService.getMesocycleById(mesocycle.getId()));
            form.setCoachId(currentUserId);
            openFormDialog("Edit Mesocycle", form, () -> {
                MesocycleRequestDTO requestDTO = form.getValue();
                mesocycleService.updateMesocycle(mesocycle.getId(), requestDTO);
                showSuccess("Mesocycle updated successfully.");
            });
        } catch (Exception e) {
            log.error("Error loading mesocycle for edit, id={}", mesocycle.getId(), e);
            showError("Could not load mesocycle for editing.");
        }
    }

    private void openFormDialog(String title, MesocycleForm form, Runnable onSave) {
        Dialog dialog = new Dialog();
        dialog.setClassName("app-dialog");
        dialog.setHeaderTitle(title);
        dialog.setWidth("720px");
        dialog.setMaxWidth("95vw");
        dialog.add(form);

        Button saveButton = new Button("Save", VaadinIcon.CHECK.create(), event -> {
            if (!form.isValid()) {
                showError("Please review the highlighted fields and ensure every training day has at least one exercise.");
                return;
            }
            try {
                onSave.run();
                dialog.close();
                loadMesocycles();
            } catch (ValidationException e) {
                showError(e.getMessage());
            } catch (Exception e) {
                log.error("Error saving mesocycle", e);
                showError("Could not save the mesocycle. Please try again.");
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", VaadinIcon.CLOSE.create(), event -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(new HorizontalLayout(cancelButton, saveButton));
        dialog.open();
    }

    private void duplicateMesocycle(MesocycleSummaryDTO mesocycle) {
        try {
            mesocycleService.duplicateMesocycle(mesocycle.getId());
            showSuccess("Mesocycle duplicated successfully.");
            loadMesocycles();
        } catch (Exception e) {
            log.error("Error duplicating mesocycle id={}", mesocycle.getId(), e);
            showError("Could not duplicate the mesocycle. Please try again.");
        }
    }

    private void confirmArchive(MesocycleSummaryDTO mesocycle) {
        ConfirmDialog confirmDialog = new ConfirmDialog("Archive Mesocycle",
                "Archive \"" + mesocycle.getName() + "\"? It can no longer be edited or activated afterwards.");
        confirmDialog.setOnConfirm(() -> {
            try {
                mesocycleService.archiveMesocycle(mesocycle.getId());
                showSuccess("Mesocycle archived successfully.");
                loadMesocycles();
            } catch (Exception e) {
                log.error("Error archiving mesocycle id={}", mesocycle.getId(), e);
                showError("Could not archive the mesocycle. Please try again.");
            }
        });
        confirmDialog.open();
    }

    private String getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        return null;
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
