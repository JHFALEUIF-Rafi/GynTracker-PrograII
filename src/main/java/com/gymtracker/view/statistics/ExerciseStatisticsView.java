package com.gymtracker.view.statistics;

import com.gymtracker.ui.component.EmptyState;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Section for per-exercise statistics: most performed exercises and exercise
 * distribution (pie chart).
 * <p>
 * TODO: StatisticsService has no per-exercise breakdown at all (no exercise
 * frequency counts, no distribution data) - that aggregation does not exist
 * anywhere reachable from this service today, and computing it would need
 * ExerciseService, which this module does not consume. This renders a
 * placeholder until StatisticsService exposes that data; when it does,
 * replace this with a real list plus a pie chart (see the TODO in
 * StatisticsCharts about Vaadin Charts not being a project dependency yet).
 */
public class ExerciseStatisticsView extends VerticalLayout {

    public ExerciseStatisticsView() {
        setClassName("planner-day-card");
        setPadding(true);
        setSpacing(false);
        getStyle().set("border", "1px solid var(--lumo-contrast-20pct)").set("border-radius", "8px");

        H4 title = new H4("Most Performed Exercises (pie chart)");
        title.getStyle().set("margin", "0 0 8px 0");

        add(title, new EmptyState(VaadinIcon.CHART, "Not Available Yet",
                "Per-exercise statistics are not available from StatisticsService yet."));
    }
}
