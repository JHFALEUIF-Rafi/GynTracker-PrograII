package com.gymtracker.view.nutrition;

import com.gymtracker.dto.nutrition.NutritionPlanSummaryDTO;
import com.gymtracker.enums.Role;
import com.gymtracker.security.CustomUserDetails;
import com.gymtracker.service.NutritionPlanService;
import com.gymtracker.ui.component.EmptyState;
import com.gymtracker.ui.component.LoadingSpinner;
import com.gymtracker.ui.component.Notifications;
import com.gymtracker.ui.component.Toolbar;
import com.gymtracker.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Read-only nutrition plan history. Athletes see their own full history
 * automatically. Coaches and Nutritionists have no "list everything I can
 * see" option available on NutritionPlanService, so they look up a specific
 * athlete's history by id - the service itself already scopes what each role
 * may read.
 */
@Slf4j
@Route(value = "nutrition/history", layout = MainLayout.class)
@PageTitle("Nutrition History - GymTracker")
public class NutritionHistoryView extends VerticalLayout implements BeforeEnterObserver {

    private final NutritionPlanService nutritionPlanService;
    private final NutritionPlanGrid planGrid = new NutritionPlanGrid();
    private final NutritionPlanDetailsDialog detailsDialog = new NutritionPlanDetailsDialog();
    private final VerticalLayout contentLayout = new VerticalLayout();
    private final TextField athleteIdField = new TextField("Athlete ID");

    private Role currentRole;
    private String currentUserId;

    public NutritionHistoryView(NutritionPlanService nutritionPlanService) {
        this.nutritionPlanService = nutritionPlanService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        setSizeFull();

        planGrid.setWriteAccess(false);
        planGrid.setOnViewDetails(this::openDetails);

        add(new Toolbar("Nutrition History"), contentLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        currentRole = getAuthenticatedRole();
        currentUserId = getAuthenticatedUserId();

        contentLayout.removeAll();
        if (currentRole == Role.ATHLETE) {
            loadHistory(currentUserId);
        } else {
            contentLayout.add(buildSearchBar(), new EmptyState(VaadinIcon.SEARCH, "Search Required", "Enter an athlete ID to view their nutrition history."));
        }
    }

    private HorizontalLayout buildSearchBar() {
        athleteIdField.setWidth("260px");
        Button searchButton = new Button("Search", VaadinIcon.SEARCH.create(), event -> {
            if (athleteIdField.getValue() == null || athleteIdField.getValue().isBlank()) {
                showError("Athlete ID is required.");
                return;
            }
            loadHistory(athleteIdField.getValue());
        });
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout bar = new HorizontalLayout(athleteIdField, searchButton);
        bar.setAlignItems(Alignment.END);
        bar.getStyle().set("flex-wrap", "wrap");
        return bar;
    }

    private void loadHistory(String athleteId) {
        contentLayout.removeAll();
        if (currentRole != Role.ATHLETE) {
            contentLayout.add(buildSearchBar());
        }
        contentLayout.add(new LoadingSpinner());

        try {
            var history = nutritionPlanService.getNutritionHistory(athleteId);
            contentLayout.removeAll();
            if (currentRole != Role.ATHLETE) {
                contentLayout.add(buildSearchBar());
            }

            if (history.isEmpty()) {
                contentLayout.add(new EmptyState(VaadinIcon.FLASK, "No Nutrition History", "No nutrition plans found for this athlete."));
                return;
            }

            planGrid.setItems(history);
            contentLayout.setSizeFull();
            contentLayout.add(planGrid);
            contentLayout.setFlexGrow(1, planGrid);
        } catch (Exception e) {
            log.error("Error loading nutrition history for athleteId={}", athleteId, e);
            contentLayout.removeAll();
            if (currentRole != Role.ATHLETE) {
                contentLayout.add(buildSearchBar());
            }
            contentLayout.add(new EmptyState(VaadinIcon.WARNING, "Something Went Wrong", "Could not load nutrition history. Please try again."));
        }
    }

    private void openDetails(NutritionPlanSummaryDTO plan) {
        try {
            detailsDialog.showDetails(nutritionPlanService.getNutritionPlanById(plan.getId()));
        } catch (Exception e) {
            log.error("Error loading nutrition plan details for id={}", plan.getId(), e);
            showError("Could not load nutrition plan details.");
        }
    }

    private void showError(String message) {
        Notifications.error(message);
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
                .map(authority -> {
                    String value = authority.getAuthority();
                    if (value.contains("ATHLETE")) {
                        return Role.ATHLETE;
                    }
                    if (value.contains("COACH")) {
                        return Role.COACH;
                    }
                    if (value.contains("NUTRITIONIST")) {
                        return Role.NUTRITIONIST;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
