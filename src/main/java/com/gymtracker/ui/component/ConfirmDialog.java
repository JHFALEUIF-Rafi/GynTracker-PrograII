package com.gymtracker.ui.component;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * Reusable confirm/cancel dialog for actions that need explicit user confirmation.
 */
public class ConfirmDialog extends Dialog {

    private Runnable onConfirm;

    public ConfirmDialog(String title, String message) {
        setClassName("app-dialog");
        setHeaderTitle(title);
        setModal(true);
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
        setWidth("min(90vw, 420px)");
        getElement().setAttribute("aria-label", title);

        add(new Span(message));

        Button confirmButton = new Button("Confirm", VaadinIcon.CHECK.create(), event -> handleConfirm());
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        confirmButton.addClickShortcut(Key.ENTER);

        Button cancelButton = new Button("Cancel", VaadinIcon.CLOSE.create(), event -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout buttons = new HorizontalLayout(cancelButton, confirmButton);
        getFooter().add(buttons);
    }

    public void setOnConfirm(Runnable onConfirm) {
        this.onConfirm = onConfirm;
    }

    private void handleConfirm() {
        close();
        if (onConfirm != null) {
            onConfirm.run();
        }
    }
}
