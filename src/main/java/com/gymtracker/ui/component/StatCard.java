package com.gymtracker.ui.component;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Reusable component for displaying a statistic card with label and value.
 */
public class StatCard extends VerticalLayout {

    private final H3 valueLabel;
    private final Span titleLabel;
    private final Icon icon;

    public StatCard(String title, String value) {
        this(null, title, value);
    }

    public StatCard(VaadinIcon iconType, String title, String value) {
        this.setClassName("stat-card");
        this.setPadding(true);
        this.setSpacing(false);

        icon = iconType != null ? iconType.create() : null;
        if (icon != null) {
            icon.setClassName("stat-card-icon");
        }

        titleLabel = new Span(title);
        titleLabel.setClassName("stat-card-title");

        valueLabel = new H3(value);
        valueLabel.setClassName("stat-card-value");
        valueLabel.getStyle().set("margin", "0");

        HorizontalLayout header = new HorizontalLayout(titleLabel);
        header.setClassName("stat-card-header");
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        if (icon != null) {
            header.add(icon);
        }

        add(header, valueLabel);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }
}
