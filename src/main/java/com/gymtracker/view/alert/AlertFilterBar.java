package com.gymtracker.view.alert;

import com.gymtracker.enums.AlertStatus;
import com.gymtracker.ui.component.SearchBar;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import java.time.LocalDate;
import java.util.List;

/**
 * Presentational filter bar for the alert grid: free-text search, status,
 * type, athlete and date filters. Holds no filtering logic itself; the
 * owning view reads its current values and applies the filter. Type options
 * are supplied by the owning view from the already-loaded list, since alert
 * types are plain strings, not a fixed enum.
 */
public class AlertFilterBar extends HorizontalLayout {

    private final SearchBar searchField;
    private final ComboBox<AlertStatus> statusFilter;
    private final ComboBox<String> typeFilter;
    private final TextField athleteFilter;
    private final DatePicker dateFilter;

    public AlertFilterBar() {
        setClassName("athlete-filter-bar");
        setWidthFull();
        setAlignItems(Alignment.END);
        setSpacing(true);

        searchField = new SearchBar("Search alerts");
        searchField.setWidth("220px");

        statusFilter = new ComboBox<>("Status");
        statusFilter.setItems(AlertStatus.values());
        statusFilter.setWidth("150px");

        typeFilter = new ComboBox<>("Type");
        typeFilter.setWidth("180px");

        athleteFilter = new TextField("Athlete");
        athleteFilter.setWidth("160px");

        dateFilter = new DatePicker("Date");
        dateFilter.setWidth("160px");

        add(searchField, statusFilter, typeFilter, athleteFilter, dateFilter);
    }

    public void setTypeOptions(List<String> types) {
        typeFilter.setItems(types);
    }

    public void setOnFilterChange(Runnable listener) {
        searchField.addValueChangeListener(event -> listener.run());
        statusFilter.addValueChangeListener(event -> listener.run());
        typeFilter.addValueChangeListener(event -> listener.run());
        athleteFilter.addValueChangeListener(event -> listener.run());
        dateFilter.addValueChangeListener(event -> listener.run());
    }

    public String getSearchTerm() {
        return searchField.getValue();
    }

    public AlertStatus getStatus() {
        return statusFilter.getValue();
    }

    public String getType() {
        return typeFilter.getValue();
    }

    public String getAthleteName() {
        return athleteFilter.getValue();
    }

    public LocalDate getDate() {
        return dateFilter.getValue();
    }
}
