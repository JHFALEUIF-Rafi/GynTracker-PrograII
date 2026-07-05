package com.gymtracker.view.mesocycle;

import com.gymtracker.dto.mesocycle.MesocycleSummaryDTO;
import com.gymtracker.enums.MesocycleStatus;
import com.gymtracker.ui.component.NotificationBadge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.function.SerializablePredicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Presentational grid listing mesocycles. Start/End Date are computed for
 * display from createdAt + durationWeeks, since the mesocycle data model has
 * no stored date range. Filtering predicates and row actions are supplied by
 * the owning view; edit/duplicate/archive actions are only rendered when
 * write access is enabled (Coach role).
 */
public class MesocycleGrid extends VerticalLayout {

    private final Grid<MesocycleSummaryDTO> grid = new Grid<>(MesocycleSummaryDTO.class, false);
    private Consumer<MesocycleSummaryDTO> onViewDetails;
    private Consumer<MesocycleSummaryDTO> onEdit;
    private Consumer<MesocycleSummaryDTO> onDuplicate;
    private Consumer<MesocycleSummaryDTO> onArchive;
    private boolean writeAccess;

    public MesocycleGrid() {
        setPadding(false);
        setSpacing(false);
        setSizeFull();

        grid.setSizeFull();
        grid.getElement().setAttribute("aria-label", "Mesocycles");
        grid.addColumn(MesocycleSummaryDTO::getName).setHeader("Name").setAutoWidth(true).setSortable(true);
        grid.addColumn(mesocycle -> valueOrDash(mesocycle.getAthleteName())).setHeader("Athlete").setAutoWidth(true);
        grid.addColumn(mesocycle -> valueOrDash(mesocycle.getCoachName())).setHeader("Coach").setAutoWidth(true);
        grid.addColumn(MesocycleSummaryDTO::getDurationWeeks).setHeader("Weeks").setAutoWidth(true);
        grid.addColumn(this::formatStartDate).setHeader("Start Date").setAutoWidth(true);
        grid.addColumn(this::formatEndDate).setHeader("End Date").setAutoWidth(true);
        grid.addComponentColumn(this::renderStatusBadge).setHeader("Status").setAutoWidth(true);
        grid.addComponentColumn(this::renderActions).setHeader("").setAutoWidth(true).setFlexGrow(0);

        grid.setItems(new ArrayList<>());
        add(grid);
    }

    public void setItems(List<MesocycleSummaryDTO> mesocycles) {
        grid.setItems(mesocycles);
    }

    public void setFilter(SerializablePredicate<MesocycleSummaryDTO> filter) {
        ((ListDataProvider<MesocycleSummaryDTO>) grid.getDataProvider()).setFilter(filter);
    }

    public void setWriteAccess(boolean writeAccess) {
        this.writeAccess = writeAccess;
        grid.getDataProvider().refreshAll();
    }

    public void setOnViewDetails(Consumer<MesocycleSummaryDTO> onViewDetails) {
        this.onViewDetails = onViewDetails;
    }

    public void setOnEdit(Consumer<MesocycleSummaryDTO> onEdit) {
        this.onEdit = onEdit;
    }

    public void setOnDuplicate(Consumer<MesocycleSummaryDTO> onDuplicate) {
        this.onDuplicate = onDuplicate;
    }

    public void setOnArchive(Consumer<MesocycleSummaryDTO> onArchive) {
        this.onArchive = onArchive;
    }

    private String formatStartDate(MesocycleSummaryDTO mesocycle) {
        return mesocycle.getCreatedAt() != null ? mesocycle.getCreatedAt().toLocalDate().toString() : "-";
    }

    private String formatEndDate(MesocycleSummaryDTO mesocycle) {
        if (mesocycle.getCreatedAt() == null || mesocycle.getDurationWeeks() == null) {
            return "-";
        }
        LocalDate endDate = mesocycle.getCreatedAt().toLocalDate().plusWeeks(mesocycle.getDurationWeeks());
        return endDate.toString();
    }

    private NotificationBadge renderStatusBadge(MesocycleSummaryDTO mesocycle) {
        return switch (mesocycle.getStatus()) {
            case ACTIVE -> new NotificationBadge("Active", NotificationBadge.BadgeType.SUCCESS);
            case DRAFT -> new NotificationBadge("Draft", NotificationBadge.BadgeType.WARNING);
            case COMPLETED -> new NotificationBadge("Completed", NotificationBadge.BadgeType.NEUTRAL);
            case ARCHIVED -> new NotificationBadge("Archived", NotificationBadge.BadgeType.NEUTRAL);
        };
    }

    private HorizontalLayout renderActions(MesocycleSummaryDTO mesocycle) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(false);

        Button viewButton = new Button(VaadinIcon.EYE.create());
        viewButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        viewButton.setAriaLabel("View details");
        viewButton.addClickListener(event -> {
            if (onViewDetails != null) {
                onViewDetails.accept(mesocycle);
            }
        });
        actions.add(viewButton);

        if (writeAccess) {
            boolean archived = mesocycle.getStatus() == MesocycleStatus.ARCHIVED;

            Button duplicateButton = new Button(VaadinIcon.COPY.create());
            duplicateButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            duplicateButton.setAriaLabel("Duplicate mesocycle");
            duplicateButton.addClickListener(event -> {
                if (onDuplicate != null) {
                    onDuplicate.accept(mesocycle);
                }
            });
            actions.add(duplicateButton);

            if (!archived) {
                Button editButton = new Button(VaadinIcon.EDIT.create());
                editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
                editButton.setAriaLabel("Edit mesocycle");
                editButton.addClickListener(event -> {
                    if (onEdit != null) {
                        onEdit.accept(mesocycle);
                    }
                });
                actions.add(editButton);

                Button archiveButton = new Button(VaadinIcon.ARCHIVE.create());
                archiveButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
                archiveButton.setAriaLabel("Archive mesocycle");
                archiveButton.addClickListener(event -> {
                    if (onArchive != null) {
                        onArchive.accept(mesocycle);
                    }
                });
                actions.add(archiveButton);
            }
        }

        return actions;
    }

    private String valueOrDash(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }
}
