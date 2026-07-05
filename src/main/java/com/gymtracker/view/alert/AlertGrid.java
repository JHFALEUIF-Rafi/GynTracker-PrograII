package com.gymtracker.view.alert;

import com.gymtracker.dto.alert.AlertDTO;
import com.gymtracker.enums.AlertStatus;
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
import java.util.Set;
import java.util.function.Consumer;

/**
 * Presentational grid listing alerts. Priority is derived client-side from
 * the alert type (there is no persisted priority field), purely to drive the
 * Priority column and the row highlight. Filtering predicates and row
 * actions are supplied by the owning view; acknowledge/resolve actions are
 * only rendered when write access is enabled (Coach role).
 */
public class AlertGrid extends VerticalLayout {

    private static final Set<String> HIGH_PRIORITY_TYPES = Set.of("CRITICAL_FATIGUE", "PERFORMANCE_DROP");
    private static final Set<String> MEDIUM_PRIORITY_TYPES = Set.of("HIGH_FATIGUE", "MISSED_WORKOUT");

    private final Grid<AlertDTO> grid = new Grid<>(AlertDTO.class, false);
    private Consumer<AlertDTO> onViewDetails;
    private Consumer<AlertDTO> onAcknowledge;
    private Consumer<AlertDTO> onResolve;
    private boolean writeAccess;

    public AlertGrid() {
        setPadding(false);
        setSpacing(false);
        setSizeFull();

        grid.setSizeFull();
        grid.getElement().setAttribute("aria-label", "Alerts");
        grid.setClassNameGenerator(alert -> "alert-row-" + resolvePriority(alert.getType()).toLowerCase());
        grid.addColumn(AlertDTO::getType).setHeader("Type").setAutoWidth(true).setSortable(true);
        grid.addColumn(alert -> valueOrDash(alert.getAthleteName())).setHeader("Athlete").setAutoWidth(true);
        grid.addComponentColumn(this::renderStatusBadge).setHeader("Status").setAutoWidth(true);
        grid.addComponentColumn(this::renderPriorityBadge).setHeader("Priority").setAutoWidth(true);
        grid.addColumn(AlertDTO::getGeneratedAt).setHeader("Created At").setAutoWidth(true).setSortable(true);
        grid.addColumn(alert -> valueOrDash(alert.getCoachName())).setHeader("Assigned Coach").setAutoWidth(true);
        grid.addComponentColumn(this::renderActions).setHeader("").setAutoWidth(true).setFlexGrow(0);

        grid.setItems(new ArrayList<>());
        add(grid);
    }

    public void setItems(List<AlertDTO> alerts) {
        grid.setItems(alerts);
    }

    public void setFilter(SerializablePredicate<AlertDTO> filter) {
        ((ListDataProvider<AlertDTO>) grid.getDataProvider()).setFilter(filter);
    }

    public void setWriteAccess(boolean writeAccess) {
        this.writeAccess = writeAccess;
        grid.getDataProvider().refreshAll();
    }

    public void setOnViewDetails(Consumer<AlertDTO> onViewDetails) {
        this.onViewDetails = onViewDetails;
    }

    public void setOnAcknowledge(Consumer<AlertDTO> onAcknowledge) {
        this.onAcknowledge = onAcknowledge;
    }

    public void setOnResolve(Consumer<AlertDTO> onResolve) {
        this.onResolve = onResolve;
    }

    public static String resolvePriority(String type) {
        if (type != null && HIGH_PRIORITY_TYPES.contains(type)) {
            return "HIGH";
        }
        if (type != null && MEDIUM_PRIORITY_TYPES.contains(type)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private NotificationBadge renderStatusBadge(AlertDTO alert) {
        return switch (alert.getStatus()) {
            case ACTIVE -> new NotificationBadge("Active", NotificationBadge.BadgeType.ERROR);
            case ACKNOWLEDGED -> new NotificationBadge("Acknowledged", NotificationBadge.BadgeType.WARNING);
            case RESOLVED -> new NotificationBadge("Resolved", NotificationBadge.BadgeType.SUCCESS);
        };
    }

    private NotificationBadge renderPriorityBadge(AlertDTO alert) {
        String priority = resolvePriority(alert.getType());
        NotificationBadge.BadgeType type = switch (priority) {
            case "HIGH" -> NotificationBadge.BadgeType.ERROR;
            case "MEDIUM" -> NotificationBadge.BadgeType.WARNING;
            default -> NotificationBadge.BadgeType.NEUTRAL;
        };
        return new NotificationBadge(priority, type);
    }

    private HorizontalLayout renderActions(AlertDTO alert) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(false);

        Button viewButton = new Button(VaadinIcon.EYE.create());
        viewButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        viewButton.setAriaLabel("View details");
        viewButton.addClickListener(event -> {
            if (onViewDetails != null) {
                onViewDetails.accept(alert);
            }
        });
        actions.add(viewButton);

        if (writeAccess) {
            if (alert.getStatus() == AlertStatus.ACTIVE) {
                Button acknowledgeButton = new Button(VaadinIcon.CHECK.create());
                acknowledgeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
                acknowledgeButton.setAriaLabel("Acknowledge alert");
                acknowledgeButton.addClickListener(event -> {
                    if (onAcknowledge != null) {
                        onAcknowledge.accept(alert);
                    }
                });
                actions.add(acknowledgeButton);
            }

            if (alert.getStatus() == AlertStatus.ACKNOWLEDGED) {
                Button resolveButton = new Button(VaadinIcon.CHECK_CIRCLE.create());
                resolveButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SUCCESS);
                resolveButton.setAriaLabel("Resolve alert");
                resolveButton.addClickListener(event -> {
                    if (onResolve != null) {
                        onResolve.accept(alert);
                    }
                });
                actions.add(resolveButton);
            }
        }

        return actions;
    }

    private String valueOrDash(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }
}
