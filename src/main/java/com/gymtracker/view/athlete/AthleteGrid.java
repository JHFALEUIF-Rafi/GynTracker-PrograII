package com.gymtracker.view.athlete;

import com.gymtracker.dto.athlete.AthleteSummaryDTO;
import com.gymtracker.ui.component.NotificationBadge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.function.SerializablePredicate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Presentational grid listing athletes with the columns required for Coaches
 * and Nutritionists to review their assigned athletes. Holds no business
 * logic: filtering predicates and the details action are supplied by the
 * owning view.
 */
public class AthleteGrid extends VerticalLayout {

    private final Grid<AthleteSummaryDTO> grid = new Grid<>(AthleteSummaryDTO.class, false);
    private Consumer<AthleteSummaryDTO> onViewDetails;

    public AthleteGrid() {
        setPadding(false);
        setSpacing(false);
        setSizeFull();

        grid.setSizeFull();
        grid.getElement().setAttribute("aria-label", "Athletes");
        grid.addColumn(athlete -> athlete.getFirstName() + " " + athlete.getLastName())
                .setHeader("Full Name").setAutoWidth(true).setSortable(true);
        grid.addColumn(AthleteSummaryDTO::getEmail)
                .setHeader("Email").setAutoWidth(true).setSortable(true);
        grid.addColumn(AthleteSummaryDTO::getAge)
                .setHeader("Age").setAutoWidth(true).setSortable(true);
        grid.addColumn(athlete -> formatDecimal(athlete.getWeight()))
                .setHeader("Weight (kg)").setAutoWidth(true);
        grid.addColumn(athlete -> formatDecimal(athlete.getHeight()))
                .setHeader("Height (cm)").setAutoWidth(true);
        grid.addColumn(athlete -> valueOrDash(athlete.getCurrentMesocycleName()))
                .setHeader("Current Mesocycle").setAutoWidth(true);
        grid.addColumn(athlete -> valueOrDash(athlete.getCurrentCoachName()))
                .setHeader("Current Coach").setAutoWidth(true);
        grid.addComponentColumn(this::renderStatusBadge)
                .setHeader("Status").setAutoWidth(true);
        grid.addColumn(athlete -> athlete.getLastWorkoutDate() != null ? athlete.getLastWorkoutDate().toString() : "Never")
                .setHeader("Last Workout").setAutoWidth(true).setSortable(true);
        grid.addComponentColumn(this::renderDetailsButton)
                .setHeader("").setAutoWidth(true).setFlexGrow(0);

        grid.setItems(new ArrayList<>());
        add(grid);
    }

    public void setItems(List<AthleteSummaryDTO> athletes) {
        grid.setItems(athletes);
    }

    public void setFilter(SerializablePredicate<AthleteSummaryDTO> filter) {
        ((ListDataProvider<AthleteSummaryDTO>) grid.getDataProvider()).setFilter(filter);
    }

    public void setOnViewDetails(Consumer<AthleteSummaryDTO> onViewDetails) {
        this.onViewDetails = onViewDetails;
    }

    private NotificationBadge renderStatusBadge(AthleteSummaryDTO athlete) {
        boolean active = Boolean.TRUE.equals(athlete.getEnabled());
        return new NotificationBadge(active ? "Active" : "Inactive",
                active ? NotificationBadge.BadgeType.SUCCESS : NotificationBadge.BadgeType.NEUTRAL);
    }

    private Button renderDetailsButton(AthleteSummaryDTO athlete) {
        Button button = new Button(VaadinIcon.EYE.create());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        button.setAriaLabel("View details");
        button.addClickListener(event -> {
            if (onViewDetails != null) {
                onViewDetails.accept(athlete);
            }
        });
        return button;
    }

    private String formatDecimal(Double value) {
        return value != null ? String.format("%.1f", value) : "-";
    }

    private String valueOrDash(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }
}
