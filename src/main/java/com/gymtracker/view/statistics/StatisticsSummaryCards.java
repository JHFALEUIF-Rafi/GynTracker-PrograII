package com.gymtracker.view.statistics;

import com.gymtracker.dto.dashboard.StatisticsDTO;
import com.gymtracker.ui.component.StatCard;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Renders {@link StatisticsDTO} as a row of reusable {@link StatCard}
 * components. Purely presentational.
 */
public class StatisticsSummaryCards extends VerticalLayout {

    private final HorizontalLayout cardsRow = new HorizontalLayout();

    public StatisticsSummaryCards() {
        setPadding(false);
        setSpacing(false);

        cardsRow.setWidthFull();
        cardsRow.setSpacing(true);
        cardsRow.getStyle().set("flex-wrap", "wrap");
        add(cardsRow);
    }

    public void showFullStatistics(StatisticsDTO stats) {
        cardsRow.removeAll();
        cardsRow.add(
                new StatCard(VaadinIcon.TRENDING_UP, "Weekly Volume", formatNumber(stats.getWeeklyTrainingVolume()) + " kg"),
                new StatCard(VaadinIcon.CALENDAR, "Monthly Volume", formatNumber(stats.getMonthlyTrainingVolume()) + " kg"),
                new StatCard(VaadinIcon.CALENDAR_CLOCK, "Training Frequency", formatNumber(stats.getAverageWeeklySessions()) + " sessions/wk"),
                new StatCard(VaadinIcon.DASHBOARD, "Average RPE", formatNumber(stats.getAverageRpe())),
                new StatCard(VaadinIcon.CLOCK, "Workout Duration", formatNumber(stats.getAverageWorkoutDuration()) + " min"),
                new StatCard(VaadinIcon.TROPHY, "1RM Progress", formatNumber(stats.getEstimatedStrengthProgress()) + " %"),
                new StatCard(VaadinIcon.CHECK_CIRCLE, "Completed Mesocycles", String.valueOf(stats.getCompletedMesocycles())),
                new StatCard(VaadinIcon.FLASK, "Completed Nutrition Plans", String.valueOf(stats.getCompletedNutritionPlans()))
        );
    }

    public void showNutritionStatistics(StatisticsDTO stats) {
        cardsRow.removeAll();
        cardsRow.add(
                new StatCard(VaadinIcon.FLASK, "Completed Nutrition Plans", String.valueOf(stats.getCompletedNutritionPlans())),
                new StatCard(VaadinIcon.USERS, "Assigned Athletes", String.valueOf(stats.getAverageWeeklySessions() != null
                        ? stats.getAverageWeeklySessions().intValue() : 0))
        );
    }

    private String formatNumber(Double value) {
        return value != null ? String.format("%.1f", value) : "-";
    }
}
