package com.gymtracker.view.statistics;

import com.gymtracker.dto.dashboard.ChartDTO;
import com.gymtracker.dto.dashboard.StatisticsDTO;
import com.gymtracker.enums.Role;
import com.gymtracker.security.CustomUserDetails;
import com.gymtracker.service.StatisticsService;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Statistics screen. Athletes see their own statistics and charts.
 * Nutritionists see only the nutrition-related figures (no training charts).
 * Coaches see their own aggregate roster statistics and may drill into one
 * assigned athlete's charts by id, since StatisticsService has no "list my
 * athletes" method - that lookup belongs to AthleteService, which this
 * module does not consume. No business logic lives here - every computation
 * is delegated to {@link StatisticsService}.
 */
@Slf4j
@Route(value = "statistics", layout = MainLayout.class)
@PageTitle("Statistics - GymTracker")
public class StatisticsView extends VerticalLayout implements BeforeEnterObserver {

    private final StatisticsService statisticsService;
    private final VerticalLayout contentLayout = new VerticalLayout();
    private final StatisticsFilterBar filterBar = new StatisticsFilterBar();
    private final StatisticsSummaryCards summaryCards = new StatisticsSummaryCards();
    private final StatisticsCharts charts = new StatisticsCharts();
    private final ExerciseStatisticsView exerciseStatistics = new ExerciseStatisticsView();

    private Role currentRole;
    private String currentUserId;
    private ChartDTO volumeChart;
    private ChartDTO oneRepMaxChart;
    private ChartDTO fatigueChart;

    public StatisticsView(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        setSizeFull();

        filterBar.setOnFilterChange(this::applyDateRangeFilter);
        add(new Toolbar("Statistics"), contentLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        currentRole = getAuthenticatedRole();
        currentUserId = getAuthenticatedUserId();
        loadStatistics();
    }

    private void loadStatistics() {
        contentLayout.removeAll();
        contentLayout.add(new LoadingSpinner());

        try {
            switch (currentRole) {
                case ATHLETE -> renderAthleteStatistics(currentUserId);
                case COACH -> renderCoachStatistics();
                case NUTRITIONIST -> renderNutritionistStatistics();
                default -> {
                }
            }
        } catch (Exception e) {
            log.error("Error loading statistics", e);
            contentLayout.removeAll();
            contentLayout.add(new EmptyState(VaadinIcon.WARNING, "Something Went Wrong", "Could not load statistics. Please try again."));
        }
    }

    private void renderAthleteStatistics(String athleteId) {
        StatisticsDTO stats = statisticsService.getAthleteStatistics(athleteId);
        volumeChart = statisticsService.getWorkoutVolumeChart(athleteId);
        oneRepMaxChart = statisticsService.getOneRepMaxChart(athleteId);
        fatigueChart = statisticsService.getFatigueChart(athleteId);

        summaryCards.showFullStatistics(stats);
        applyDateRangeFilter();

        contentLayout.removeAll();
        contentLayout.add(summaryCards, filterBar, charts, exerciseStatistics);
    }

    private void renderCoachStatistics() {
        StatisticsDTO stats = statisticsService.getCoachStatistics(currentUserId);
        summaryCards.showFullStatistics(stats);

        contentLayout.removeAll();
        contentLayout.add(summaryCards, buildAthleteDrillDownBar());
    }

    private void renderNutritionistStatistics() {
        StatisticsDTO stats = statisticsService.getNutritionistStatistics(currentUserId);
        summaryCards.showNutritionStatistics(stats);

        contentLayout.removeAll();
        contentLayout.add(summaryCards);
    }

    private HorizontalLayout buildAthleteDrillDownBar() {
        TextField athleteIdField = new TextField("Athlete ID");
        athleteIdField.setWidth("260px");

        Button viewButton = new Button("View Athlete Charts", VaadinIcon.SEARCH.create(), event -> {
            if (athleteIdField.getValue() == null || athleteIdField.getValue().isBlank()) {
                showError("Athlete ID is required.");
                return;
            }
            try {
                volumeChart = statisticsService.getWorkoutVolumeChart(athleteIdField.getValue());
                oneRepMaxChart = statisticsService.getOneRepMaxChart(athleteIdField.getValue());
                fatigueChart = statisticsService.getFatigueChart(athleteIdField.getValue());
                applyDateRangeFilter();

                if (!contentLayout.getChildren().anyMatch(component -> component == charts)) {
                    contentLayout.add(filterBar, charts, exerciseStatistics);
                }
            } catch (Exception e) {
                log.error("Error loading charts for athleteId={}", athleteIdField.getValue(), e);
                showError("Could not load statistics for that athlete.");
            }
        });
        viewButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout bar = new HorizontalLayout(athleteIdField, viewButton);
        bar.setAlignItems(Alignment.END);
        bar.getStyle().set("flex-wrap", "wrap");
        return bar;
    }

    private void applyDateRangeFilter() {
        LocalDate startDate = filterBar.getStartDate();
        LocalDate endDate = filterBar.getEndDate();

        charts.showCharts(
                filterByDateRange(volumeChart, startDate, endDate),
                filterByDateRange(oneRepMaxChart, startDate, endDate),
                filterByDateRange(fatigueChart, startDate, endDate)
        );
    }

    private ChartDTO filterByDateRange(ChartDTO chart, LocalDate startDate, LocalDate endDate) {
        if (chart == null || (startDate == null && endDate == null) || chart.getLabels() == null) {
            return chart;
        }

        List<String> filteredLabels = new ArrayList<>();
        List<Double> filteredValues = new ArrayList<>();
        List<String> labels = chart.getLabels();
        List<Double> values = chart.getValues();

        for (int i = 0; i < labels.size(); i++) {
            LocalDate labelDate = parseLabelAsDate(labels.get(i));
            if (labelDate == null || withinRange(labelDate, startDate, endDate)) {
                filteredLabels.add(labels.get(i));
                filteredValues.add(i < values.size() ? values.get(i) : 0.0);
            }
        }

        return ChartDTO.builder().title(chart.getTitle()).labels(filteredLabels).values(filteredValues).build();
    }

    private LocalDate parseLabelAsDate(String label) {
        try {
            return LocalDate.parse(label);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean withinRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return (startDate == null || !date.isBefore(startDate)) && (endDate == null || !date.isAfter(endDate));
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

    private void showError(String message) {
        Notifications.error(message);
    }
}
