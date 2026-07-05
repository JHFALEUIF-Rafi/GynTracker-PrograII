package com.gymtracker.view.report;

import com.gymtracker.dto.dashboard.ReportDTO;
import com.gymtracker.service.ReportService;
import com.gymtracker.ui.component.EmptyState;
import com.gymtracker.ui.component.NotificationBadge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Lists the reports generated in this browser session (see
 * {@link GeneratedReportsHolder} for why there is no persisted history) with
 * search/filter by athlete, coach, date range and report type, and an export
 * action per row. Purely presentational.
 */
public class ReportHistoryView extends VerticalLayout {

    private final ReportService reportService;
    private final GeneratedReportsHolder holder;
    private final Grid<GeneratedReportEntry> grid = new Grid<>(GeneratedReportEntry.class, false);
    private final VerticalLayout listArea = new VerticalLayout();

    private final TextField athleteFilter = new TextField("Athlete ID");
    private final TextField coachFilter = new TextField("Coach ID");
    private final ComboBox<String> typeFilter = new ComboBox<>("Report Type");
    private final DatePicker startDateFilter = new DatePicker("From");
    private final DatePicker endDateFilter = new DatePicker("To");

    public ReportHistoryView(ReportService reportService, GeneratedReportsHolder holder) {
        this.reportService = reportService;
        this.holder = holder;

        setPadding(false);
        setSpacing(true);

        buildGrid();
        listArea.setPadding(false);
        listArea.setSpacing(true);
        add(listArea);

        refresh();
    }

    public void refresh() {
        listArea.removeAll();
        List<GeneratedReportEntry> entries = holder.getEntries();

        if (entries.isEmpty()) {
            listArea.add(new EmptyState(VaadinIcon.FILE_TEXT, "No Reports Yet", "Generate a report to see it listed here."));
            return;
        }

        typeFilter.setItems(entries.stream()
                .map(entry -> entry.getReport().getReportType())
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList()));

        grid.setItems(entries);
        listArea.add(buildFilterBar(), grid);
        listArea.setFlexGrow(1, grid);
    }

    private HorizontalLayout buildFilterBar() {
        athleteFilter.setWidth("160px");
        coachFilter.setWidth("160px");
        typeFilter.setWidth("200px");
        startDateFilter.setWidth("150px");
        endDateFilter.setWidth("150px");

        athleteFilter.addValueChangeListener(event -> applyFilter());
        coachFilter.addValueChangeListener(event -> applyFilter());
        typeFilter.addValueChangeListener(event -> applyFilter());
        startDateFilter.addValueChangeListener(event -> applyFilter());
        endDateFilter.addValueChangeListener(event -> applyFilter());

        HorizontalLayout bar = new HorizontalLayout(athleteFilter, coachFilter, typeFilter, startDateFilter, endDateFilter);
        bar.setAlignItems(Alignment.END);
        bar.setSpacing(true);
        bar.getStyle().set("flex-wrap", "wrap");
        return bar;
    }

    private void applyFilter() {
        String normalizedAthlete = normalize(athleteFilter.getValue());
        String normalizedCoach = normalize(coachFilter.getValue());
        String type = typeFilter.getValue();
        LocalDate startDate = startDateFilter.getValue();
        LocalDate endDate = endDateFilter.getValue();

        ((ListDataProvider<GeneratedReportEntry>) grid.getDataProvider()).setFilter(entry -> {
            ReportDTO report = entry.getReport();
            boolean matchesAthlete = normalizedAthlete.isBlank()
                    || containsIgnoreCase(report.getRequestedForUserId(), normalizedAthlete);
            boolean matchesCoach = normalizedCoach.isBlank()
                    || containsIgnoreCase(report.getGeneratedByUserId(), normalizedCoach);
            boolean matchesType = type == null || type.equals(report.getReportType());
            boolean matchesDate = (startDate == null && endDate == null) || (report.getGeneratedAt() != null
                    && withinRange(report.getGeneratedAt().toLocalDate(), startDate, endDate));
            return matchesAthlete && matchesCoach && matchesType && matchesDate;
        });
    }

    private void buildGrid() {
        grid.setHeight("420px");
        grid.setWidthFull();
        grid.getElement().setAttribute("aria-label", "Generated reports");
        grid.addColumn(entry -> entry.getReport().getTitle()).setHeader("Report Name").setAutoWidth(true).setSortable(true);
        grid.addColumn(entry -> entry.getReport().getReportType()).setHeader("Report Type").setAutoWidth(true);
        grid.addColumn(entry -> entry.getReport().getGeneratedByRole()).setHeader("Generated By").setAutoWidth(true);
        grid.addColumn(entry -> entry.getReport().getGeneratedAt()).setHeader("Generated Date").setAutoWidth(true).setSortable(true);
        grid.addColumn(entry -> entry.getExportFormat() != null ? entry.getExportFormat().name() : "-")
                .setHeader("Export Format").setAutoWidth(true);
        grid.addComponentColumn(this::renderStatusBadge).setHeader("Status").setAutoWidth(true);
        grid.addComponentColumn(this::renderExportButton).setHeader("").setAutoWidth(true).setFlexGrow(0);
        grid.setItems(List.of());
    }

    private NotificationBadge renderStatusBadge(GeneratedReportEntry entry) {
        boolean exported = entry.getStatus() == GeneratedReportEntry.Status.EXPORTED;
        return new NotificationBadge(exported ? "Exported" : "Generated",
                exported ? NotificationBadge.BadgeType.SUCCESS : NotificationBadge.BadgeType.NEUTRAL);
    }

    private Button renderExportButton(GeneratedReportEntry entry) {
        Button button = new Button(VaadinIcon.DOWNLOAD.create(), event -> {
            ExportOptionsDialog dialog = new ExportOptionsDialog(reportService, entry, this::refresh);
            dialog.open();
        });
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        button.setAriaLabel("Export report");
        return button;
    }

    private String normalize(String value) {
        return value != null ? value.trim().toLowerCase(Locale.ROOT) : "";
    }

    private boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private boolean withinRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return (startDate == null || !date.isBefore(startDate)) && (endDate == null || !date.isAfter(endDate));
    }
}
