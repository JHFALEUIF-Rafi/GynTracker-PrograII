package com.gymtracker.ui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * Reusable toolbar with a title on the left and action components on the right.
 */
public class Toolbar extends HorizontalLayout {

    private final H2 titleLabel;
    private final HorizontalLayout actionsArea;

    public Toolbar(String title) {
        setClassName("app-toolbar");
        setWidthFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.BETWEEN);
        getStyle().set("flex-wrap", "wrap");

        titleLabel = new H2(title);
        titleLabel.setClassName("app-toolbar-title");
        titleLabel.getStyle().set("margin", "0");

        actionsArea = new HorizontalLayout();
        actionsArea.setClassName("app-toolbar-actions");
        actionsArea.setSpacing(true);
        actionsArea.setAlignItems(Alignment.CENTER);
        actionsArea.getStyle().set("flex-wrap", "wrap");

        add(titleLabel, actionsArea);
    }

    public void addAction(Component... components) {
        actionsArea.add(components);
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }
}
