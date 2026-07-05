package com.gymtracker.view.nutrition;

import com.gymtracker.dto.nutrition.NutritionPlanRequestDTO;
import com.gymtracker.dto.nutrition.NutritionPlanSummaryDTO;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.security.CustomUserDetails;
import com.gymtracker.service.NutritionPlanService;
import com.gymtracker.ui.component.ConfirmDialog;
import com.gymtracker.ui.component.EmptyState;
import com.gymtracker.ui.component.LoadingSpinner;
import com.gymtracker.ui.component.Notifications;
import com.gymtracker.ui.component.SearchBar;
import com.gymtracker.ui.component.Toolbar;
import com.gymtracker.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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
 * Nutrition plan catalog screen. Nutritionists have full access to the plans
 * they created (create, edit, deactivate); Coaches and Athletes have
 * read-only access. No business logic lives here - every operation is
 * delegated to {@link NutritionPlanService}.
 */
@Slf4j
@Route(value = "nutrition", layout = MainLayout.class)
@PageTitle("Nutrition Plans - GymTracker")
public class NutritionView extends VerticalLayout implements BeforeEnterObserver {

    private static final String STATUS_ALL = "All";
    private static final String STATUS_ACTIVE = "Active";
    private static final String STATUS_INACTIVE = "Inactive";

    private final NutritionPlanService nutritionPlanService;
    private final VerticalLayout contentLayout = new VerticalLayout();
    private final SearchBar searchField = new SearchBar("Search by athlete");
    private final ComboBox<String> statusFilter = new ComboBox<>("Status");
    private final NutritionPlanGrid planGrid = new NutritionPlanGrid();
    private final NutritionPlanDetailsDialog detailsDialog = new NutritionPlanDetailsDialog();
    private final Toolbar toolbar = new Toolbar("Nutrition Plans");

    private boolean writeAccess;
    private String currentUserId;
    private List<NutritionPlanSummaryDTO> allPlans = List.of();

    public NutritionView(NutritionPlanService nutritionPlanService) {
        this.nutritionPlanService = nutritionPlanService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        setSizeFull();

        statusFilter.setItems(STATUS_ALL, STATUS_ACTIVE, STATUS_INACTIVE);
        statusFilter.setValue(STATUS_ALL);
        searchField.setWidth("260px");
        statusFilter.setWidth("150px");

        searchField.addValueChangeListener(event -> applyFilter());
        statusFilter.addValueChangeListener(event -> applyFilter());
        planGrid.setOnViewDetails(this::openDetails);
        planGrid.setOnEdit(this::openEditForm);
        planGrid.setOnDeactivate(this::confirmDeactivate);

        add(toolbar, contentLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        currentUserId = getAuthenticatedUserId();
        writeAccess = getAuthenticatedRole() == Role.NUTRITIONIST;
        planGrid.setWriteAccess(writeAccess);
        setupToolbarActions();
        loadPlans();
    }

    private void setupToolbarActions() {
        if (!writeAccess) {
            return;
        }
        Button newPlanButton = new Button("New Nutrition Plan", VaadinIcon.PLUS.create(), event -> openCreateForm());
        newPlanButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        toolbar.addAction(newPlanButton);
    }

    private void loadPlans() {
        contentLayout.removeAll();
        contentLayout.add(new LoadingSpinner());

        try {
            allPlans = nutritionPlanService.getNutritionPlansForCurrentUser();
            contentLayout.removeAll();

            if (allPlans.isEmpty()) {
                contentLayout.add(new EmptyState(VaadinIcon.FLASK, "No Nutrition Plans", "There are no nutrition plans to display yet."));
                return;
            }

            planGrid.setItems(allPlans);
            HorizontalLayout filterBar = new HorizontalLayout(searchField, statusFilter);
            filterBar.setAlignItems(Alignment.END);
            filterBar.setWidthFull();
            filterBar.getStyle().set("flex-wrap", "wrap");

            contentLayout.setSizeFull();
            contentLayout.add(filterBar, planGrid);
            contentLayout.setFlexGrow(1, planGrid);
        } catch (Exception e) {
            log.error("Error loading nutrition plans", e);
            contentLayout.removeAll();
            contentLayout.add(new EmptyState(VaadinIcon.WARNING, "Something Went Wrong", "Could not load the nutrition plan list. Please try again."));
        }
    }

    private void applyFilter() {
        String normalizedSearch = searchField.getValue() != null ? searchField.getValue().trim().toLowerCase(Locale.ROOT) : "";
        String status = statusFilter.getValue();

        planGrid.setFilter(plan -> matchesSearch(plan, normalizedSearch) && matchesStatus(plan, status));
    }

    private boolean matchesSearch(NutritionPlanSummaryDTO plan, String normalizedSearch) {
        return normalizedSearch.isBlank()
                || (plan.getAthleteName() != null && plan.getAthleteName().toLowerCase(Locale.ROOT).contains(normalizedSearch));
    }

    private boolean matchesStatus(NutritionPlanSummaryDTO plan, String status) {
        if (status == null || STATUS_ALL.equals(status)) {
            return true;
        }
        boolean expectedActive = STATUS_ACTIVE.equals(status);
        return Boolean.TRUE.equals(plan.getActive()) == expectedActive;
    }

    private void openDetails(NutritionPlanSummaryDTO plan) {
        try {
            detailsDialog.showDetails(nutritionPlanService.getNutritionPlanById(plan.getId()));
        } catch (Exception e) {
            log.error("Error loading nutrition plan details for id={}", plan.getId(), e);
            showError("Could not load nutrition plan details.");
        }
    }

    private void openCreateForm() {
        NutritionPlanForm form = new NutritionPlanForm();
        form.setNewPlan();
        form.setNutritionistId(currentUserId);
        openFormDialog("New Nutrition Plan", form, () -> {
            NutritionPlanRequestDTO requestDTO = form.getValue();
            nutritionPlanService.createNutritionPlan(requestDTO);
            showSuccess("Nutrition plan created successfully.");
        });
    }

    private void openEditForm(NutritionPlanSummaryDTO plan) {
        try {
            NutritionPlanForm form = new NutritionPlanForm();
            form.setPlan(nutritionPlanService.getNutritionPlanById(plan.getId()));
            form.setNutritionistId(currentUserId);
            openFormDialog("Edit Nutrition Plan", form, () -> {
                NutritionPlanRequestDTO requestDTO = form.getValue();
                nutritionPlanService.updateNutritionPlan(plan.getId(), requestDTO);
                showSuccess("Nutrition plan updated successfully.");
            });
        } catch (Exception e) {
            log.error("Error loading nutrition plan for edit, id={}", plan.getId(), e);
            showError("Could not load nutrition plan for editing.");
        }
    }

    private void openFormDialog(String title, NutritionPlanForm form, Runnable onSave) {
        Dialog dialog = new Dialog();
        dialog.setClassName("app-dialog");
        dialog.setHeaderTitle(title);
        dialog.setWidth("680px");
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
                loadPlans();
            } catch (ValidationException e) {
                showError(e.getMessage());
            } catch (Exception e) {
                log.error("Error saving nutrition plan", e);
                showError("Could not save the nutrition plan. Please try again.");
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", VaadinIcon.CLOSE.create(), event -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(new HorizontalLayout(cancelButton, saveButton));
        dialog.open();
    }

    private void confirmDeactivate(NutritionPlanSummaryDTO plan) {
        ConfirmDialog confirmDialog = new ConfirmDialog("Deactivate Nutrition Plan",
                "Deactivate this nutrition plan for \"" + plan.getAthleteName() + "\"?");
        confirmDialog.setOnConfirm(() -> {
            try {
                nutritionPlanService.deactivateNutritionPlan(plan.getId());
                showSuccess("Nutrition plan deactivated successfully.");
                loadPlans();
            } catch (Exception e) {
                log.error("Error deactivating nutrition plan id={}", plan.getId(), e);
                showError("Could not deactivate the nutrition plan. Please try again.");
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
