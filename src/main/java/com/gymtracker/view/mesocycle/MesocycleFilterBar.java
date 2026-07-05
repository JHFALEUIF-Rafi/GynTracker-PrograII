package com.gymtracker.view.mesocycle;

import com.gymtracker.enums.MesocycleStatus;
import com.gymtracker.ui.component.SearchBar;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import java.util.List;

/**
 * Presentational filter bar for the mesocycle grid: free-text search by name,
 * status filter and coach filter. The coach options are supplied by the
 * owning view from the already-loaded list, since this component depends on
 * no service. Holds no filtering logic itself.
 */
public class MesocycleFilterBar extends HorizontalLayout {

    private final SearchBar searchField;
    private final ComboBox<MesocycleStatus> statusFilter;
    private final ComboBox<String> coachFilter;

    public MesocycleFilterBar() {
        setClassName("athlete-filter-bar");
        setWidthFull();
        setAlignItems(Alignment.END);
        setSpacing(true);

        searchField = new SearchBar("Search by name");
        searchField.setWidth("240px");

        statusFilter = new ComboBox<>("Status");
        statusFilter.setItems(MesocycleStatus.values());
        statusFilter.setWidth("150px");

        coachFilter = new ComboBox<>("Coach");
        coachFilter.setWidth("180px");

        add(searchField, statusFilter, coachFilter);
    }

    public void setCoachOptions(List<String> coachNames) {
        coachFilter.setItems(coachNames);
    }

    public void setOnFilterChange(Runnable listener) {
        searchField.addValueChangeListener(event -> listener.run());
        statusFilter.addValueChangeListener(event -> listener.run());
        coachFilter.addValueChangeListener(event -> listener.run());
    }

    public String getSearchTerm() {
        return searchField.getValue();
    }

    public MesocycleStatus getStatus() {
        return statusFilter.getValue();
    }

    public String getCoachName() {
        return coachFilter.getValue();
    }
}
