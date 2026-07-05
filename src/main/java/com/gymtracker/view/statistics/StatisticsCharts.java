package com.gymtracker.view.statistics;

import com.gymtracker.dto.dashboard.ChartDTO;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.List;

/**
 * Renders the workout volume, 1RM progression and fatigue trend charts.
 * <p>
 * TODO: Vaadin Charts ({@code com.vaadin:vaadin-charts-flow}) is a commercial
 * add-on and is not a dependency of this project (no license file present).
 * Per the task's explicit fallback, this renders a simple proportional bar
 * visualization of the same {@link ChartDTO} data instead of a real bar/line
 * chart. Once Vaadin Charts is licensed and added to pom.xml, replace
 * {@link #buildChartPlaceholder} with a real {@code Chart} component
 * (bar/line series bound to the same labels/values).
 */
public class StatisticsCharts extends VerticalLayout {

    private final VerticalLayout volumeChartContainer = new VerticalLayout();
    private final VerticalLayout oneRepMaxChartContainer = new VerticalLayout();
    private final VerticalLayout fatigueChartContainer = new VerticalLayout();

    public StatisticsCharts() {
        setPadding(false);
        setSpacing(true);

        HorizontalLayout row = new HorizontalLayout(volumeChartContainer, oneRepMaxChartContainer);
        row.setWidthFull();
        row.setSpacing(true);
        row.getStyle().set("flex-wrap", "wrap");
        volumeChartContainer.getStyle().set("flex", "1 1 300px");
        oneRepMaxChartContainer.getStyle().set("flex", "1 1 300px");

        add(row, fatigueChartContainer);
    }

    public void showCharts(ChartDTO volumeChart, ChartDTO oneRepMaxChart, ChartDTO fatigueChart) {
        volumeChartContainer.removeAll();
        volumeChartContainer.add(buildChartPlaceholder("Weekly Training Volume (bar)", volumeChart));

        oneRepMaxChartContainer.removeAll();
        oneRepMaxChartContainer.add(buildChartPlaceholder("One Rep Max Progression (line)", oneRepMaxChart));

        fatigueChartContainer.removeAll();
        fatigueChartContainer.add(buildChartPlaceholder("Fatigue Trend (line)", fatigueChart));
    }

    private VerticalLayout buildChartPlaceholder(String chartKind, ChartDTO chart) {
        VerticalLayout card = new VerticalLayout();
        card.setClassName("planner-day-card");
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)").set("border-radius", "8px");
        card.setWidthFull();

        H4 title = new H4(chart != null && chart.getTitle() != null ? chart.getTitle() : chartKind);
        title.getStyle().set("margin", "0 0 4px 0");
        Span subtitle = new Span(chartKind);
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "12px");
        card.add(title, subtitle);

        List<String> labels = chart != null ? chart.getLabels() : null;
        List<Double> values = chart != null ? chart.getValues() : null;

        if (labels == null || values == null || labels.isEmpty()) {
            HorizontalLayout emptyRow = new HorizontalLayout(VaadinIcon.INFO_CIRCLE.create(), new Span("No data available."));
            emptyRow.setAlignItems(Alignment.CENTER);
            emptyRow.setSpacing(true);
            emptyRow.getStyle().set("color", "var(--lumo-secondary-text-color)");
            card.add(emptyRow);
            return card;
        }

        double max = values.stream().mapToDouble(value -> value != null ? value : 0).max().orElse(1.0);
        for (int i = 0; i < labels.size(); i++) {
            double value = i < values.size() && values.get(i) != null ? values.get(i) : 0.0;
            card.add(buildBarRow(labels.get(i), value, max));
        }
        return card;
    }

    private HorizontalLayout buildBarRow(String label, double value, double max) {
        Span labelSpan = new Span(label);
        labelSpan.setWidth("90px");
        labelSpan.getStyle().set("font-size", "12px").set("flex-shrink", "0");

        Div barTrack = new Div();
        barTrack.setWidthFull();
        barTrack.getStyle().set("background-color", "var(--lumo-contrast-10pct)").set("border-radius", "4px")
                .set("height", "16px").set("position", "relative");

        Div barFill = new Div();
        double percent = max > 0 ? Math.min(100.0, (value / max) * 100.0) : 0.0;
        barFill.getStyle().set("width", percent + "%").set("background-color", "var(--lumo-primary-color)")
                .set("border-radius", "4px").set("height", "16px");
        barTrack.add(barFill);

        Span valueSpan = new Span(String.format("%.1f", value));
        valueSpan.getStyle().set("font-size", "12px").set("width", "60px").set("text-align", "right").set("flex-shrink", "0");

        HorizontalLayout row = new HorizontalLayout(labelSpan, barTrack, valueSpan);
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);
        row.setSpacing(true);
        return row;
    }
}
