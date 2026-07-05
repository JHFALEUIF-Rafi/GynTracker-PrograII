package com.gymtracker.view.nutrition;

import com.gymtracker.dto.nutrition.NutritionPlanDetailDTO;
import com.gymtracker.ui.component.NotificationBadge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Read-only dialog with the full picture of a nutrition plan. Purely
 * presentational; the owning view supplies the already-fetched DTO.
 */
public class NutritionPlanDetailsDialog extends Dialog {

    private final VerticalLayout content = new VerticalLayout();

    public NutritionPlanDetailsDialog() {
        setClassName("app-dialog");
        setWidth("560px");
        setMaxWidth("95vw");
        setHeaderTitle("Nutrition Plan Details");

        Button closeButton = new Button(VaadinIcon.CLOSE.create(), event -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        closeButton.setAriaLabel("Close");
        getHeader().add(closeButton);

        content.setPadding(false);
        content.setSpacing(false);
        add(content);
    }

    public void showDetails(NutritionPlanDetailDTO detail) {
        content.removeAll();

        boolean active = Boolean.TRUE.equals(detail.getActive());
        HorizontalLayout athleteAndStatus = new HorizontalLayout();
        athleteAndStatus.setWidthFull();
        athleteAndStatus.setJustifyContentMode(JustifyContentMode.BETWEEN);
        athleteAndStatus.add(new Span(valueOrDash(detail.getAthleteName())),
                new NotificationBadge(active ? "Active" : "Inactive",
                        active ? NotificationBadge.BadgeType.SUCCESS : NotificationBadge.BadgeType.NEUTRAL));

        content.add(athleteAndStatus,
                infoRow("Nutritionist", valueOrDash(detail.getNutritionistName())),
                infoRow("Goal", detail.getGoal() != null ? detail.getGoal().name() : "-"),
                infoRow("Calories", detail.getCalories() + " kcal"),
                infoRow("Protein", formatNumber(detail.getProtein()) + " g"),
                infoRow("Carbohydrates", formatNumber(detail.getCarbohydrates()) + " g"),
                infoRow("Fat", formatNumber(detail.getFat()) + " g"),
                infoRow("Start Date", String.valueOf(detail.getStartDate())),
                infoRow("End Date", String.valueOf(detail.getEndDate())),
                infoRow("Observations", valueOrDash(detail.getObservations()))
        );
        open();
    }

    private HorizontalLayout infoRow(String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setJustifyContentMode(JustifyContentMode.BETWEEN);

        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");

        row.add(labelSpan, new Span(value));
        return row;
    }

    private String formatNumber(Double value) {
        return value != null ? String.format("%.1f", value) : "-";
    }

    private String valueOrDash(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }
}
