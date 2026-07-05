package com.gymtracker.ui.component;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Loading spinner component displayed while data is loading.
 */
public class LoadingSpinner extends VerticalLayout {

    private final ProgressBar progressBar;
    private final Span label;

    public LoadingSpinner() {
        this("Loading...");
    }

    public LoadingSpinner(String message) {
        this.setClassName("loading-spinner");
        this.getElement().setAttribute("role", "status");
        this.getElement().setAttribute("aria-live", "polite");
        this.setAlignItems(Alignment.CENTER);
        this.setJustifyContentMode(JustifyContentMode.CENTER);
        this.setSizeFull();

        progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.getElement().setAttribute("aria-label", message);

        label = new Span(message);
        label.setClassName("loading-spinner-label");

        add(progressBar, label);
    }
}
