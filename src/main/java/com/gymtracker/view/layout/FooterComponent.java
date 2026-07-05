package com.gymtracker.view.layout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import java.time.Year;

/**
 * Footer component displaying app version and copyright information.
 */
public class FooterComponent extends HorizontalLayout {

    private static final String APP_VERSION = "1.0.0";

    public FooterComponent() {
        setPadding(true);
        setSpacing(true);
        setWidthFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setHeight("60px");
        getStyle()
                .set("background-color", "var(--lumo-base-color)")
                .set("border-top", "1px solid var(--lumo-divider-color)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "12px");

        Span copyright = new Span(
                "GymTracker v" + APP_VERSION + " © " + Year.now().getValue()
        );
        copyright.getStyle()
                .set("text-align", "center")
                .set("color", "var(--lumo-secondary-text-color)");

        add(copyright);
    }
}
