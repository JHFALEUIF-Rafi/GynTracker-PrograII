package com.gymtracker.view.nutrition;

import com.gymtracker.dto.nutrition.NutritionPlanSummaryDTO;
import com.gymtracker.ui.component.NotificationBadge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.function.SerializablePredicate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Presentational grid listing nutrition plans. Filtering predicates and row
 * actions are supplied by the owning view; edit/deactivate actions are only
 * rendered when write access is enabled (Nutritionist role).
 */
public class NutritionPlanGrid extends VerticalLayout {

    private final Grid<NutritionPlanSummaryDTO> grid = new Grid<>(NutritionPlanSummaryDTO.class, false);
    private Consumer<NutritionPlanSummaryDTO> onViewDetails;
    private Consumer<NutritionPlanSummaryDTO> onEdit;
    private Consumer<NutritionPlanSummaryDTO> onDeactivate;
    private boolean writeAccess;

    public NutritionPlanGrid() {
        setPadding(false);
        setSpacing(false);
        setSizeFull();

        grid.setSizeFull();
        grid.getElement().setAttribute("aria-label", "Nutrition plans");
        grid.addColumn(plan -> valueOrDash(plan.getAthleteName())).setHeader("Athlete").setAutoWidth(true).setSortable(true);
        grid.addColumn(plan -> valueOrDash(plan.getNutritionistName())).setHeader("Nutritionist").setAutoWidth(true);
        grid.addColumn(plan -> plan.getGoal() != null ? plan.getGoal().name() : "-").setHeader("Goal").setAutoWidth(true);
        grid.addColumn(NutritionPlanSummaryDTO::getCalories).setHeader("Calories").setAutoWidth(true);
        grid.addColumn(plan -> formatNumber(plan.getProtein())).setHeader("Protein (g)").setAutoWidth(true);
        grid.addColumn(plan -> formatNumber(plan.getCarbohydrates())).setHeader("Carbohydrates (g)").setAutoWidth(true);
        grid.addColumn(plan -> formatNumber(plan.getFat())).setHeader("Fat (g)").setAutoWidth(true);
        grid.addColumn(NutritionPlanSummaryDTO::getStartDate).setHeader("Start Date").setAutoWidth(true).setSortable(true);
        grid.addColumn(NutritionPlanSummaryDTO::getEndDate).setHeader("End Date").setAutoWidth(true);
        grid.addComponentColumn(this::renderStatusBadge).setHeader("Status").setAutoWidth(true);
        grid.addComponentColumn(this::renderActions).setHeader("").setAutoWidth(true).setFlexGrow(0);

        grid.setItems(new ArrayList<>());
        add(grid);
    }

    public void setItems(List<NutritionPlanSummaryDTO> plans) {
        grid.setItems(plans);
    }

    public void setFilter(SerializablePredicate<NutritionPlanSummaryDTO> filter) {
        ((ListDataProvider<NutritionPlanSummaryDTO>) grid.getDataProvider()).setFilter(filter);
    }

    public void setWriteAccess(boolean writeAccess) {
        this.writeAccess = writeAccess;
        grid.getDataProvider().refreshAll();
    }

    public void setOnViewDetails(Consumer<NutritionPlanSummaryDTO> onViewDetails) {
        this.onViewDetails = onViewDetails;
    }

    public void setOnEdit(Consumer<NutritionPlanSummaryDTO> onEdit) {
        this.onEdit = onEdit;
    }

    public void setOnDeactivate(Consumer<NutritionPlanSummaryDTO> onDeactivate) {
        this.onDeactivate = onDeactivate;
    }

    private NotificationBadge renderStatusBadge(NutritionPlanSummaryDTO plan) {
        boolean active = Boolean.TRUE.equals(plan.getActive());
        return new NotificationBadge(active ? "Active" : "Inactive",
                active ? NotificationBadge.BadgeType.SUCCESS : NotificationBadge.BadgeType.NEUTRAL);
    }

    private HorizontalLayout renderActions(NutritionPlanSummaryDTO plan) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(false);

        Button viewButton = new Button(VaadinIcon.EYE.create());
        viewButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        viewButton.setAriaLabel("View details");
        viewButton.addClickListener(event -> {
            if (onViewDetails != null) {
                onViewDetails.accept(plan);
            }
        });
        actions.add(viewButton);

        if (writeAccess) {
            boolean active = Boolean.TRUE.equals(plan.getActive());

            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            editButton.setAriaLabel("Edit nutrition plan");
            editButton.addClickListener(event -> {
                if (onEdit != null) {
                    onEdit.accept(plan);
                }
            });
            actions.add(editButton);

            if (active) {
                Button deactivateButton = new Button(VaadinIcon.BAN.create());
                deactivateButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
                deactivateButton.setAriaLabel("Deactivate nutrition plan");
                deactivateButton.addClickListener(event -> {
                    if (onDeactivate != null) {
                        onDeactivate.accept(plan);
                    }
                });
                actions.add(deactivateButton);
            }
        }

        return actions;
    }

    private String formatNumber(Double value) {
        return value != null ? String.format("%.1f", value) : "-";
    }

    private String valueOrDash(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }
}
