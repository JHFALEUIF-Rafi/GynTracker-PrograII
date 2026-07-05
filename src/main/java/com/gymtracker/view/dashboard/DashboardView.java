package com.gymtracker.view.dashboard;

import com.gymtracker.dto.dashboard.DashboardDTO;
import com.gymtracker.dto.user.UserProfileDTO;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.ResourceNotFoundException;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.service.DashboardService;
import com.gymtracker.service.UserService;
import com.gymtracker.ui.component.EmptyState;
import com.gymtracker.ui.component.LoadingSpinner;
import com.gymtracker.ui.component.Notifications;
import com.gymtracker.ui.component.StatCard;
import com.gymtracker.ui.component.Toolbar;
import com.gymtracker.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;

/**
 * Main dashboard view for authenticated users.
 * Displays role-specific dashboards (Athlete, Coach, Nutritionist).
 * Routes to /dashboard and uses MainLayout.
 */
@Slf4j
@Route(value = "/dashboard", layout = MainLayout.class)
@PageTitle("Dashboard - GymTracker")
public class DashboardView extends VerticalLayout {

    private final DashboardService dashboardService;
    private final UserService userService;
    private final VerticalLayout contentLayout;
    private LoadingSpinner loadingSpinner;

    public DashboardView(DashboardService dashboardService, UserService userService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
        this.contentLayout = new VerticalLayout();

        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(contentLayout);
        loadDashboard();
    }

    private void loadDashboard() {
        try {
            UserProfileDTO currentUser = userService.getCurrentUserProfile();
            String userId = currentUser.getId();
            Role userRole = currentUser.getRole();

            contentLayout.removeAll();
            displayLoadingSpinner();

            DashboardDTO dashboard = switch (userRole) {
                case ATHLETE -> dashboardService.getAthleteDashboard(userId);
                case COACH -> dashboardService.getCoachDashboard(userId);
                case NUTRITIONIST -> dashboardService.getNutritionistDashboard(userId);
            };

            contentLayout.removeAll();

            if (dashboard == null) {
                displayEmptyState();
            } else {
                displayDashboard(dashboard, userRole);
            }

            log.info("Dashboard loaded successfully for user={} role={}", userId, userRole);
        } catch (UnauthorizedOperationException e) {
            log.warn("Unauthorized dashboard access: {}", e.getMessage());
            displayErrorNotification("Unauthorized: " + e.getMessage());
            displayEmptyState();
        } catch (ResourceNotFoundException e) {
            log.warn("Resource not found: {}", e.getMessage());
            displayErrorNotification("Resource not found: " + e.getMessage());
            displayEmptyState();
        } catch (Exception e) {
            log.error("Error loading dashboard", e);
            displayErrorNotification("Error loading dashboard. Please try again.");
            displayEmptyState();
        }
    }

    private void displayLoadingSpinner() {
        loadingSpinner = new LoadingSpinner();
        contentLayout.add(loadingSpinner);
    }

    private void displayEmptyState() {
        EmptyState emptyState = new EmptyState(
            "No Data Available",
            "Dashboard data is not available at the moment."
        );
        contentLayout.add(emptyState);
    }

    private void displayDashboard(DashboardDTO dashboard, Role role) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setWidthFull();
        mainLayout.setSpacing(true);
        mainLayout.setPadding(true);

        // Header with refresh button
        displayHeader(mainLayout);

        switch (role) {
            case ATHLETE -> displayAthleteDashboard(mainLayout, dashboard);
            case COACH -> displayCoachDashboard(mainLayout, dashboard);
            case NUTRITIONIST -> displayNutritionistDashboard(mainLayout, dashboard);
        }

        contentLayout.add(mainLayout);
    }

    private void displayHeader(VerticalLayout layout) {
        Toolbar toolbar = new Toolbar("Dashboard");

        Button refreshButton = new Button(
            "Refresh",
            VaadinIcon.REFRESH.create(),
            event -> loadDashboard()
        );
        refreshButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        refreshButton.setAriaLabel("Refresh dashboard data");

        toolbar.addAction(refreshButton);
        layout.add(toolbar);
    }

    private void displayAthleteDashboard(VerticalLayout layout, DashboardDTO dashboard) {
        // Welcome section
        Span welcomeMessage = new Span("Welcome back! Here's your training summary.");
        welcomeMessage.setClassName("dashboard-subtitle");
        layout.add(welcomeMessage);

        // Key metrics row
        HorizontalLayout metricsRow1 = newMetricsRow();

        metricsRow1.add(
            new StatCard(VaadinIcon.TRENDING_UP, "Weekly Training Volume", formatDouble(dashboard.getWeeklyTrainingVolume()) + " kg"),
            new StatCard(VaadinIcon.HEART, "Fatigue Level", dashboard.getCurrentFatigueLevel()),
            new StatCard(VaadinIcon.REFRESH, "Recovery Score", formatDouble(dashboard.getRecoveryScore()) + "%")
        );
        layout.add(metricsRow1);

        // Additional metrics row
        HorizontalLayout metricsRow2 = newMetricsRow();

        metricsRow2.add(
            new StatCard(VaadinIcon.CHECK_CIRCLE, "Completed Sessions", dashboard.getCompletedSessions().toString()),
            new StatCard(VaadinIcon.TROPHY, "Estimated 1RM", formatDouble(dashboard.getEstimatedOneRepMax()) + " kg"),
            new StatCard(VaadinIcon.BELL, "Active Alerts", dashboard.getActiveAlerts().toString())
        );
        layout.add(metricsRow2);

        // Current Mesocycle
        if (dashboard.getCurrentMesocycle() != null && !dashboard.getCurrentMesocycle().isBlank()) {
            H2 mesocycleTitle = new H2("Current Mesocycle");
            mesocycleTitle.setClassName("dashboard-section-title");
            Span mesocycleInfo = new Span(dashboard.getCurrentMesocycle());
            layout.add(mesocycleTitle, mesocycleInfo);
        }

        // Current Nutrition Plan
        if (dashboard.getCurrentActiveNutritionPlan() != null && !dashboard.getCurrentActiveNutritionPlan().isBlank()) {
            H2 nutritionTitle = new H2("Nutrition Plan");
            nutritionTitle.setClassName("dashboard-section-title");
            Span nutritionInfo = new Span(dashboard.getCurrentActiveNutritionPlan());
            layout.add(nutritionTitle, nutritionInfo);
        }
    }

    private void displayCoachDashboard(VerticalLayout layout, DashboardDTO dashboard) {
        // Welcome section
        Span welcomeMessage = new Span("Welcome Coach! Here's your team overview.");
        welcomeMessage.setClassName("dashboard-subtitle");
        layout.add(welcomeMessage);

        // Key metrics
        HorizontalLayout metricsRow = newMetricsRow();

        metricsRow.add(
            new StatCard(VaadinIcon.USERS, "Assigned Athletes", dashboard.getAssignedAthletes().toString()),
            new StatCard(VaadinIcon.CALENDAR, "Active Mesocycles", dashboard.getActiveMesocycles().toString()),
            new StatCard(VaadinIcon.WARNING, "Athletes with High Fatigue", dashboard.getAthletesWithHighFatigue().toString()),
            new StatCard(VaadinIcon.BELL, "Pending Alerts", dashboard.getPendingAlerts().toString())
        );
        layout.add(metricsRow);

        // Weekly performance
        HorizontalLayout performanceRow = newMetricsRow();

        performanceRow.add(
            new StatCard(VaadinIcon.CALENDAR_CLOCK, "Weekly Sessions", dashboard.getWeeklySessions().toString()),
            new StatCard(VaadinIcon.TRENDING_UP, "Performance Trend", formatDouble(dashboard.getPerformanceTrend()) + "%")
        );
        layout.add(performanceRow);

        // Quick actions
        H2 quickActionsTitle = new H2("Quick Actions");
        quickActionsTitle.setClassName("dashboard-section-title");
        layout.add(quickActionsTitle);

        HorizontalLayout actionsRow = newMetricsRow();

        Button viewAthletesBtn = new Button("View Athletes", VaadinIcon.USERS.create());
        Button createMesocycleBtn = new Button("Create Mesocycle", VaadinIcon.PLUS_CIRCLE.create());
        createMesocycleBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        actionsRow.add(viewAthletesBtn, createMesocycleBtn);
        layout.add(actionsRow);
    }

    private void displayNutritionistDashboard(VerticalLayout layout, DashboardDTO dashboard) {
        // Welcome section
        Span welcomeMessage = new Span("Welcome Nutritionist! Here's your program overview.");
        welcomeMessage.setClassName("dashboard-subtitle");
        layout.add(welcomeMessage);

        // Key metrics
        HorizontalLayout metricsRow = newMetricsRow();

        metricsRow.add(
            new StatCard(VaadinIcon.USERS, "Assigned Athletes", dashboard.getAssignedAthletes().toString()),
            new StatCard(VaadinIcon.FLASK, "Active Plans", dashboard.getActiveNutritionPlans().toString()),
            new StatCard(VaadinIcon.CLOCK, "Expiring Plans", dashboard.getExpiredPlans().toString()),
            new StatCard(VaadinIcon.BELL, "Nutrition Alerts", dashboard.getNutritionAlerts().toString())
        );
        layout.add(metricsRow);

        // Quick actions
        H2 quickActionsTitle = new H2("Quick Actions");
        quickActionsTitle.setClassName("dashboard-section-title");
        layout.add(quickActionsTitle);

        HorizontalLayout actionsRow = newMetricsRow();

        Button viewAthletesBtn = new Button("View Athletes", VaadinIcon.USERS.create());
        Button createPlanBtn = new Button("Create Nutrition Plan", VaadinIcon.PLUS_CIRCLE.create());
        createPlanBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        actionsRow.add(viewAthletesBtn, createPlanBtn);
        layout.add(actionsRow);
    }

    private HorizontalLayout newMetricsRow() {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);
        row.getStyle().set("flex-wrap", "wrap");
        return row;
    }

    private void displayErrorNotification(String message) {
        Notifications.error(message);
    }

    private String formatDouble(Double value) {
        if (value == null) {
            return "N/A";
        }
        return String.format("%.2f", value);
    }
}
