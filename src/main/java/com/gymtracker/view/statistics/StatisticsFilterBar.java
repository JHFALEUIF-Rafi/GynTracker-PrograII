package com.gymtracker.view.statistics;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import java.time.LocalDate;

/**
 * Presentational filter bar for statistics: date range, exercise and
 * mesocycle. Date range is applied client-side over the already-fetched
 * chart data. Exercise and Mesocycle filters are disabled placeholders:
 * StatisticsService has no per-exercise or per-mesocycle breakdown - doing
 * so would require ExerciseService/MesocycleService, which this module does
 * not consume. TODO: wire these once StatisticsService exposes that data.
 */
public class StatisticsFilterBar extends HorizontalLayout {

    private final DatePicker startDateField;
    private final DatePicker endDateField;
    private final ComboBox<String> exerciseFilter;
    private final TextField mesocycleFilter;

    public StatisticsFilterBar() {
        setClassName("athlete-filter-bar");
        setWidthFull();
        setAlignItems(Alignment.END);
        setSpacing(true);

        startDateField = new DatePicker("Start Date");
        startDateField.setWidth("160px");

        endDateField = new DatePicker("End Date");
        endDateField.setWidth("160px");

        exerciseFilter = new ComboBox<>("Exercise");
        exerciseFilter.setWidth("180px");
        exerciseFilter.setEnabled(false);
        exerciseFilter.setHelperText("Not available yet");

        mesocycleFilter = new TextField("Mesocycle");
        mesocycleFilter.setWidth("180px");
        mesocycleFilter.setEnabled(false);
        mesocycleFilter.setHelperText("Not available yet");

        add(startDateField, endDateField, exerciseFilter, mesocycleFilter);
    }

    public void setOnFilterChange(Runnable listener) {
        startDateField.addValueChangeListener(event -> listener.run());
        endDateField.addValueChangeListener(event -> listener.run());
    }

    public LocalDate getStartDate() {
        return startDateField.getValue();
    }

    public LocalDate getEndDate() {
        return endDateField.getValue();
    }
}
