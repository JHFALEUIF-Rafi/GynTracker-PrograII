package com.gymtracker.view.athlete;

import com.gymtracker.ui.component.SearchBar;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;

/**
 * Presentational filter bar for the athlete grid: free-text search (name,
 * email or coach), status filter and age range. It holds no filtering logic
 * itself; the owning view reads its current values and applies the filter.
 */
public class AthleteFilterBar extends HorizontalLayout {

    public static final String STATUS_ALL = "All";
    public static final String STATUS_ACTIVE = "Active";
    public static final String STATUS_INACTIVE = "Inactive";

    private final SearchBar searchField;
    private final ComboBox<String> statusFilter;
    private final IntegerField minAgeField;
    private final IntegerField maxAgeField;

    public AthleteFilterBar() {
        setClassName("athlete-filter-bar");
        setWidthFull();
        setAlignItems(Alignment.END);
        setSpacing(true);

        searchField = new SearchBar("Search by name, email or coach");
        searchField.setWidth("280px");

        statusFilter = new ComboBox<>("Status");
        statusFilter.setItems(STATUS_ALL, STATUS_ACTIVE, STATUS_INACTIVE);
        statusFilter.setValue(STATUS_ALL);
        statusFilter.setWidth("140px");

        minAgeField = new IntegerField("Min age");
        minAgeField.setWidth("110px");
        minAgeField.setMin(14);

        maxAgeField = new IntegerField("Max age");
        maxAgeField.setWidth("110px");
        maxAgeField.setMax(100);

        add(searchField, statusFilter, minAgeField, maxAgeField);
    }

    public void setOnFilterChange(Runnable listener) {
        searchField.addValueChangeListener(event -> listener.run());
        statusFilter.addValueChangeListener(event -> listener.run());
        minAgeField.addValueChangeListener(event -> listener.run());
        maxAgeField.addValueChangeListener(event -> listener.run());
    }

    public String getSearchTerm() {
        return searchField.getValue();
    }

    public String getStatus() {
        return statusFilter.getValue();
    }

    public Integer getMinAge() {
        return minAgeField.getValue();
    }

    public Integer getMaxAge() {
        return maxAgeField.getValue();
    }
}
