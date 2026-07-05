package com.gymtracker.view.workout;

import com.gymtracker.dto.workout.WorkoutSessionSummaryDTO;
import com.gymtracker.enums.Role;
import com.gymtracker.enums.WorkoutStatus;
import com.gymtracker.security.CustomUserDetails;
import com.gymtracker.service.FatigueService;
import com.gymtracker.service.WorkoutSessionService;
import com.gymtracker.ui.component.EmptyState;
import com.gymtracker.ui.component.LoadingSpinner;
import com.gymtracker.ui.component.NotificationBadge;
import com.gymtracker.ui.component.Notifications;
import com.gymtracker.ui.component.Toolbar;
import com.gymtracker.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Read-only workout history. Athletes see their own full history with
 * client-side search/filter. Coaches and Nutritionists have no "list
 * everything I can see" option available on WorkoutSessionService, so they
 * search by date range or mesocycle - the service itself already scopes
 * what each role may read.
 */
@Slf4j
@Route(value = "workouts/history", layout = MainLayout.class)
@PageTitle("Workout History - GymTracker")
public class WorkoutHistoryView extends VerticalLayout implements BeforeEnterObserver {

    private final WorkoutSessionService workoutSessionService;
    private final WorkoutDetailsDialog detailsDialog;
    private final VerticalLayout contentLayout = new VerticalLayout();
    private final Grid<WorkoutSessionSummaryDTO> grid = new Grid<>(WorkoutSessionSummaryDTO.class, false);

    private final DatePicker startDateField = new DatePicker("Start Date");
    private final DatePicker endDateField = new DatePicker("End Date");
    private final TextField mesocycleIdField = new TextField("Mesocycle ID");
    private final ComboBox<WorkoutStatus> statusFilter = new ComboBox<>("Status");

    private Role currentRole;
    private String currentAthleteId;
    private List<WorkoutSessionSummaryDTO> loadedSessions = List.of();

    public WorkoutHistoryView(WorkoutSessionService workoutSessionService, FatigueService fatigueService) {
        this.workoutSessionService = workoutSessionService;
        this.detailsDialog = new WorkoutDetailsDialog(fatigueService);

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        setSizeFull();

        buildGrid();
        add(new Toolbar("Workout History"), contentLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        currentRole = getAuthenticatedRole();
        currentAthleteId = getAuthenticatedUserId();
        statusFilter.setItems(WorkoutStatus.values());

        contentLayout.removeAll();
        if (currentRole == Role.ATHLETE) {
            loadOwnHistory();
        } else {
            contentLayout.add(buildSearchBar(), new EmptyState(VaadinIcon.SEARCH, "Search Required",
                    "Enter a mesocycle ID or a date range to view workout sessions."));
        }
    }

    private void loadOwnHistory() {
        contentLayout.add(new LoadingSpinner());
        try {
            loadedSessions = workoutSessionService.getWorkoutSessionsByAthlete(currentAthleteId);
            contentLayout.removeAll();
            contentLayout.add(buildSearchBar());
            displaySessions(loadedSessions);
        } catch (Exception e) {
            log.error("Error loading workout history", e);
            contentLayout.removeAll();
            contentLayout.add(new EmptyState(VaadinIcon.WARNING, "Something Went Wrong", "Could not load your workout history."));
        }
    }

    private HorizontalLayout buildSearchBar() {
        startDateField.setWidth("160px");
        endDateField.setWidth("160px");
        mesocycleIdField.setWidth("200px");
        statusFilter.setWidth("150px");

        Button searchButton = new Button("Search", VaadinIcon.SEARCH.create(), event -> search());
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        statusFilter.addValueChangeListener(event -> applyStatusFilter());

        HorizontalLayout bar = new HorizontalLayout(startDateField, endDateField, mesocycleIdField, statusFilter, searchButton);
        bar.setAlignItems(Alignment.END);
        bar.setSpacing(true);
        bar.getStyle().set("flex-wrap", "wrap");
        return bar;
    }

    private void search() {
        String mesocycleId = mesocycleIdField.getValue();
        LocalDate startDate = startDateField.getValue();
        LocalDate endDate = endDateField.getValue();

        try {
            if (mesocycleId != null && !mesocycleId.isBlank()) {
                loadedSessions = workoutSessionService.getWorkoutSessionsByMesocycle(mesocycleId);
            } else if (startDate != null && endDate != null) {
                loadedSessions = workoutSessionService.getWorkoutSessionsByDateRange(startDate, endDate);
            } else if (currentRole == Role.ATHLETE) {
                loadedSessions = workoutSessionService.getWorkoutSessionsByAthlete(currentAthleteId);
            } else {
                showError("Provide a mesocycle ID or a full date range to search.");
                return;
            }
            displaySessions(loadedSessions);
        } catch (Exception e) {
            log.error("Error searching workout history", e);
            showError("Could not search workout history. Please try again.");
        }
    }

    private void applyStatusFilter() {
        displaySessions(loadedSessions);
    }

    private void displaySessions(List<WorkoutSessionSummaryDTO> sessions) {
        WorkoutStatus status = statusFilter.getValue();
        List<WorkoutSessionSummaryDTO> filtered = sessions.stream()
                .filter(session -> status == null || session.getStatus() == status)
                .toList();

        removeGridFromContent();
        if (filtered.isEmpty()) {
            contentLayout.add(new EmptyState(VaadinIcon.CALENDAR, "No Workouts Found", "No workout sessions match your search."));
            return;
        }
        grid.setItems(filtered);
        contentLayout.setSizeFull();
        contentLayout.add(grid);
        contentLayout.setFlexGrow(1, grid);
    }

    private void removeGridFromContent() {
        contentLayout.getChildren()
                .filter(component -> component instanceof Grid<?> || component instanceof EmptyState)
                .forEach(contentLayout::remove);
    }

    private void buildGrid() {
        grid.setSizeFull();
        grid.getElement().setAttribute("aria-label", "Workout history");
        grid.addColumn(WorkoutSessionSummaryDTO::getDate).setHeader("Date").setAutoWidth(true).setSortable(true);
        grid.addColumn(session -> session.getDurationMinutes() + " min").setHeader("Duration").setAutoWidth(true);
        grid.addColumn(session -> formatNumber(session.getTotalVolume()) + " kg").setHeader("Total Volume").setAutoWidth(true);
        grid.addColumn(session -> session.getEstimatedOneRepMax() != null
                        ? formatNumber(session.getEstimatedOneRepMax()) + " kg" : "-")
                .setHeader("Estimated 1RM").setAutoWidth(true);
        grid.addComponentColumn(this::renderStatusBadge).setHeader("Status").setAutoWidth(true);
        grid.addComponentColumn(this::renderDetailsButton).setHeader("").setAutoWidth(true).setFlexGrow(0);
        grid.setItems(new ArrayList<>());
    }

    private NotificationBadge renderStatusBadge(WorkoutSessionSummaryDTO session) {
        boolean completed = session.getStatus() == WorkoutStatus.COMPLETED;
        return new NotificationBadge(session.getStatus().name(),
                completed ? NotificationBadge.BadgeType.SUCCESS : NotificationBadge.BadgeType.NEUTRAL);
    }

    private Button renderDetailsButton(WorkoutSessionSummaryDTO session) {
        Button button = new Button(VaadinIcon.EYE.create());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        button.setAriaLabel("View details");
        button.addClickListener(event -> openDetails(session));
        return button;
    }

    private void openDetails(WorkoutSessionSummaryDTO session) {
        try {
            detailsDialog.showDetails(workoutSessionService.getWorkoutSessionById(session.getId()));
        } catch (Exception e) {
            log.error("Error loading workout details for id={}", session.getId(), e);
            showError("Could not load workout details.");
        }
    }

    private String formatNumber(Double value) {
        return value != null ? String.format("%.1f", value) : "-";
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
