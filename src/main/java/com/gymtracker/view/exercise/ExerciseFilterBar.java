package com.gymtracker.view.exercise;

import com.gymtracker.enums.Difficulty;
import com.gymtracker.enums.Equipment;
import com.gymtracker.enums.ExerciseStatus;
import com.gymtracker.enums.ExerciseType;
import com.gymtracker.ui.component.SearchBar;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * Presentational filter bar for the exercise catalog: free-text search by
 * name plus type/difficulty/equipment/status filters. Holds no filtering
 * logic itself; the owning view reads its current values and applies them.
 */
public class ExerciseFilterBar extends HorizontalLayout {

    private final SearchBar searchField;
    private final ComboBox<ExerciseType> typeFilter;
    private final ComboBox<Difficulty> difficultyFilter;
    private final ComboBox<Equipment> equipmentFilter;
    private final ComboBox<ExerciseStatus> statusFilter;

    public ExerciseFilterBar() {
        setClassName("athlete-filter-bar");
        setWidthFull();
        setAlignItems(Alignment.END);
        setSpacing(true);

        searchField = new SearchBar("Search by name");
        searchField.setWidth("240px");

        typeFilter = new ComboBox<>("Type");
        typeFilter.setItems(ExerciseType.values());
        typeFilter.setWidth("150px");

        difficultyFilter = new ComboBox<>("Difficulty");
        difficultyFilter.setItems(Difficulty.values());
        difficultyFilter.setWidth("150px");

        equipmentFilter = new ComboBox<>("Equipment");
        equipmentFilter.setItems(Equipment.values());
        equipmentFilter.setWidth("150px");

        statusFilter = new ComboBox<>("Status");
        statusFilter.setItems(ExerciseStatus.values());
        statusFilter.setWidth("140px");

        add(searchField, typeFilter, difficultyFilter, equipmentFilter, statusFilter);
    }

    public void setOnFilterChange(Runnable listener) {
        searchField.addValueChangeListener(event -> listener.run());
        typeFilter.addValueChangeListener(event -> listener.run());
        difficultyFilter.addValueChangeListener(event -> listener.run());
        equipmentFilter.addValueChangeListener(event -> listener.run());
        statusFilter.addValueChangeListener(event -> listener.run());
    }

    public String getSearchTerm() {
        return searchField.getValue();
    }

    public ExerciseType getType() {
        return typeFilter.getValue();
    }

    public Difficulty getDifficulty() {
        return difficultyFilter.getValue();
    }

    public Equipment getEquipment() {
        return equipmentFilter.getValue();
    }

    public ExerciseStatus getStatus() {
        return statusFilter.getValue();
    }
}
