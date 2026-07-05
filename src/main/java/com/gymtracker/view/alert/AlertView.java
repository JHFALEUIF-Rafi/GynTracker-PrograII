package com.gymtracker.view.alert;

import com.gymtracker.dto.alert.AlertDTO;
import com.gymtracker.enums.AlertStatus;
import com.gymtracker.enums.Role;
import com.gymtracker.service.AlertService;
import com.gymtracker.ui.component.ConfirmDialog;
import com.gymtracker.ui.component.EmptyState;
import com.gymtracker.ui.component.LoadingSpinner;
import com.gymtracker.ui.component.NotificationBadge;
import com.gymtracker.ui.component.Notifications;
import com.gymtracker.ui.component.Toolbar;
import com.gymtracker.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Alert monitoring screen. Coaches can acknowledge and resolve their alerts;
 * Athletes see only their own alerts; Nutritionists see only
 * nutrition-related alerts. No business logic lives here - every operation
 * is delegated to {@link AlertService}, which already scopes what each role
 * may see.
 */
@Slf4j
@Route(value = "alerts", layout = MainLayout.class)
@PageTitle("Alerts - GymTracker")
public class AlertView extends VerticalLayout implements BeforeEnterObserver {

    private final AlertService alertService;
    private final VerticalLayout contentLayout = new VerticalLayout();
    private final AlertFilterBar filterBar = new AlertFilterBar();
    private final AlertGrid alertGrid = new AlertGrid();
    private final AlertDetailsDialog detailsDialog = new AlertDetailsDialog();
    private final Toolbar toolbar = new Toolbar("Alerts");

    private boolean writeAccess;
    private List<AlertDTO> allAlerts = List.of();

    public AlertView(AlertService alertService) {
        this.alertService = alertService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        setSizeFull();

        filterBar.setOnFilterChange(this::applyFilter);
        alertGrid.setOnViewDetails(detailsDialog::showDetails);
        alertGrid.setOnAcknowledge(this::acknowledgeAlert);
        alertGrid.setOnResolve(this::confirmResolve);

        Button refreshButton = new Button("Refresh", VaadinIcon.REFRESH.create(), event -> loadAlerts());
        toolbar.addAction(refreshButton);

        add(toolbar, contentLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        writeAccess = getAuthenticatedRole() == Role.COACH;
        alertGrid.setWriteAccess(writeAccess);
        loadAlerts();
    }

    private void loadAlerts() {
        contentLayout.removeAll();
        contentLayout.add(new LoadingSpinner());

        try {
            allAlerts = alertService.getAlertsForCurrentUser();
            contentLayout.removeAll();

            if (allAlerts.isEmpty()) {
                contentLayout.add(new EmptyState(VaadinIcon.BELL, "No Alerts", "There are no alerts to display right now."));
                return;
            }

            filterBar.setTypeOptions(allAlerts.stream()
                    .map(AlertDTO::getType)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            alertGrid.setItems(allAlerts);
            contentLayout.setSizeFull();
            contentLayout.add(buildHeaderRow(), filterBar, alertGrid);
            contentLayout.setFlexGrow(1, alertGrid);
        } catch (Exception e) {
            log.error("Error loading alerts", e);
            contentLayout.removeAll();
            contentLayout.add(new EmptyState(VaadinIcon.WARNING, "Something Went Wrong", "Could not load alerts. Please try again."));
        }
    }

    private NotificationBadge buildHeaderRow() {
        long activeCount = allAlerts.stream().filter(alert -> alert.getStatus() == AlertStatus.ACTIVE).count();
        return new NotificationBadge(activeCount + " active alert" + (activeCount == 1 ? "" : "s"),
                activeCount > 0 ? NotificationBadge.BadgeType.ERROR : NotificationBadge.BadgeType.SUCCESS);
    }

    private void applyFilter() {
        String normalizedSearch = filterBar.getSearchTerm() != null
                ? filterBar.getSearchTerm().trim().toLowerCase(Locale.ROOT) : "";
        String normalizedAthlete = filterBar.getAthleteName() != null
                ? filterBar.getAthleteName().trim().toLowerCase(Locale.ROOT) : "";

        alertGrid.setFilter(alert -> matchesSearch(alert, normalizedSearch)
                && (filterBar.getStatus() == null || alert.getStatus() == filterBar.getStatus())
                && (filterBar.getType() == null || filterBar.getType().equals(alert.getType()))
                && matchesAthlete(alert, normalizedAthlete)
                && matchesDate(alert, filterBar.getDate()));
    }

    private boolean matchesSearch(AlertDTO alert, String normalizedSearch) {
        return normalizedSearch.isBlank()
                || containsIgnoreCase(alert.getMessage(), normalizedSearch)
                || containsIgnoreCase(alert.getType(), normalizedSearch)
                || containsIgnoreCase(alert.getAthleteName(), normalizedSearch);
    }

    private boolean matchesAthlete(AlertDTO alert, String normalizedAthlete) {
        return normalizedAthlete.isBlank() || containsIgnoreCase(alert.getAthleteName(), normalizedAthlete);
    }

    private boolean matchesDate(AlertDTO alert, LocalDate date) {
        return date == null || (alert.getGeneratedAt() != null && alert.getGeneratedAt().toLocalDate().equals(date));
    }

    private boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private void acknowledgeAlert(AlertDTO alert) {
        try {
            alertService.acknowledgeAlert(alert.getId());
            showSuccess("Alert acknowledged.");
            loadAlerts();
        } catch (Exception e) {
            log.error("Error acknowledging alert id={}", alert.getId(), e);
            showError("Could not acknowledge the alert. Please try again.");
        }
    }

    private void confirmResolve(AlertDTO alert) {
        ConfirmDialog confirmDialog = new ConfirmDialog("Resolve Alert",
                "Mark this alert as resolved? This action cannot be undone.");
        confirmDialog.setOnConfirm(() -> {
            try {
                alertService.resolveAlert(alert.getId());
                showSuccess("Alert resolved.");
                loadAlerts();
            } catch (Exception e) {
                log.error("Error resolving alert id={}", alert.getId(), e);
                showError("Could not resolve the alert. Please try again.");
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

    private void showSuccess(String message) {
        Notifications.success(message);
    }

    private void showError(String message) {
        Notifications.error(message);
    }
}
