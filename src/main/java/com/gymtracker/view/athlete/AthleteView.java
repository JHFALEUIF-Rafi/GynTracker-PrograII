package com.gymtracker.view.athlete;

import com.gymtracker.dto.athlete.AthleteSummaryDTO;
import com.gymtracker.enums.Role;
import com.gymtracker.security.SecurityConstants;
import com.gymtracker.service.AthleteService;
import com.gymtracker.ui.component.EmptyState;
import com.gymtracker.ui.component.LoadingSpinner;
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Athlete list screen for Coaches and Nutritionists. Displays the athletes
 * assigned to the authenticated caller, with search and filtering, and opens
 * {@link AthleteDetailsDialog} for a full read-only view of a selected
 * athlete. Athletes are forwarded to their own profile instead, since they
 * may not browse other athletes.
 */
@Slf4j
@Route(value = "athletes", layout = MainLayout.class)
@PageTitle("Athletes - GymTracker")
public class AthleteView extends VerticalLayout implements BeforeEnterObserver {

    private final AthleteService athleteService;
    private final VerticalLayout contentLayout = new VerticalLayout();
    private final AthleteFilterBar filterBar = new AthleteFilterBar();
    private final AthleteGrid athleteGrid = new AthleteGrid();
    private final AthleteDetailsDialog detailsDialog = new AthleteDetailsDialog();

    private List<AthleteSummaryDTO> allAthletes = List.of();

    public AthleteView(AthleteService athleteService) {
        this.athleteService = athleteService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        setSizeFull();

        Toolbar toolbar = new Toolbar("Athletes");
        Button refreshButton = new Button("Refresh", VaadinIcon.REFRESH.create(), event -> loadAthletes());
        toolbar.addAction(refreshButton);

        filterBar.setOnFilterChange(this::applyFilter);
        athleteGrid.setOnViewDetails(this::openDetails);

        add(toolbar, contentLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (getAuthenticatedRole() == Role.ATHLETE) {
            event.forwardTo(SecurityConstants.ATHLETE_PROFILE_ROUTE);
            return;
        }
        loadAthletes();
    }

    private void loadAthletes() {
        contentLayout.removeAll();
        contentLayout.add(new LoadingSpinner());

        try {
            allAthletes = athleteService.getAthletesForCurrentUser();
            contentLayout.removeAll();

            if (allAthletes.isEmpty()) {
                contentLayout.add(new EmptyState(VaadinIcon.USERS, "No Athletes Assigned", "You have no athletes assigned yet."));
                return;
            }

            athleteGrid.setItems(allAthletes);
            contentLayout.setSizeFull();
            contentLayout.add(filterBar, athleteGrid);
            contentLayout.setFlexGrow(1, athleteGrid);
        } catch (Exception e) {
            log.error("Error loading athlete list", e);
            contentLayout.removeAll();
            contentLayout.add(new EmptyState(VaadinIcon.WARNING, "Something Went Wrong", "Could not load the athlete list. Please try again."));
        }
    }

    private void applyFilter() {
        String searchTerm = filterBar.getSearchTerm();
        String normalizedSearch = searchTerm != null ? searchTerm.trim().toLowerCase(Locale.ROOT) : "";
        String status = filterBar.getStatus();
        Integer minAge = filterBar.getMinAge();
        Integer maxAge = filterBar.getMaxAge();

        athleteGrid.setFilter(athlete -> matchesSearch(athlete, normalizedSearch)
                && matchesStatus(athlete, status)
                && matchesAgeRange(athlete, minAge, maxAge));
    }

    private boolean matchesSearch(AthleteSummaryDTO athlete, String normalizedSearch) {
        if (normalizedSearch.isBlank()) {
            return true;
        }
        String fullName = (athlete.getFirstName() + " " + athlete.getLastName()).toLowerCase(Locale.ROOT);
        return fullName.contains(normalizedSearch)
                || containsIgnoreCase(athlete.getEmail(), normalizedSearch)
                || containsIgnoreCase(athlete.getCurrentCoachName(), normalizedSearch);
    }

    private boolean matchesStatus(AthleteSummaryDTO athlete, String status) {
        if (status == null || AthleteFilterBar.STATUS_ALL.equals(status)) {
            return true;
        }
        boolean expectedActive = AthleteFilterBar.STATUS_ACTIVE.equals(status);
        return Boolean.TRUE.equals(athlete.getEnabled()) == expectedActive;
    }

    private boolean matchesAgeRange(AthleteSummaryDTO athlete, Integer minAge, Integer maxAge) {
        if (athlete.getAge() == null) {
            return minAge == null && maxAge == null;
        }
        if (minAge != null && athlete.getAge() < minAge) {
            return false;
        }
        return maxAge == null || athlete.getAge() <= maxAge;
    }

    private boolean containsIgnoreCase(String value, String normalizedSearch) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedSearch);
    }

    private void openDetails(AthleteSummaryDTO athlete) {
        try {
            detailsDialog.showDetails(athleteService.getAthleteById(athlete.getId()));
        } catch (Exception e) {
            log.error("Error loading athlete details for id={}", athlete.getId(), e);
            Notifications.error("Could not load athlete details. Please try again.");
        }
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
}
