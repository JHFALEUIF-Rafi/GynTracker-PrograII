package com.gymtracker.ui.component;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

/**
 * Reusable debounced search input with a magnifying-glass prefix icon.
 */
public class SearchBar extends TextField {

    public SearchBar(String placeholder) {
        setPlaceholder(placeholder);
        setPrefixComponent(VaadinIcon.SEARCH.create());
        setClearButtonVisible(true);
        setValueChangeMode(ValueChangeMode.LAZY);
        setValueChangeTimeout(300);
        setWidthFull();
        setClassName("search-bar");
    }
}
