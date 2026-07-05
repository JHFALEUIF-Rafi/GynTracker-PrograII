package com.gymtracker.view.report;

import com.gymtracker.enums.ExportFormat;
import com.gymtracker.service.ReportService;
import com.gymtracker.ui.component.LoadingSpinner;
import com.gymtracker.ui.component.Notifications;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.server.StreamResource;
import java.io.ByteArrayInputStream;
import lombok.extern.slf4j.Slf4j;

/**
 * Dialog for exporting a generated report to PDF or XLSX. Export is
 * genuinely asynchronous ({@code ReportService.exportReport} returns a
 * {@code CompletableFuture<byte[]>}, backed by {@code @Async}), so this shows
 * a real progress indicator while waiting and only reveals the download link
 * once the future completes, using {@code UI.access()} to update the UI
 * safely from the background thread. The application has no {@code @Push}
 * configured, so a {@code UI.access()} update from a background thread would
 * otherwise sit queued until the next client round-trip; a short poll
 * interval is enabled only while this dialog is waiting so the result
 * actually reaches the browser, then disabled again once it arrives.
 */
@Slf4j
public class ExportOptionsDialog extends Dialog {

    private final ReportService reportService;
    private final GeneratedReportEntry entry;
    private final Runnable onExported;

    private final RadioButtonGroup<ExportFormat> formatField = new RadioButtonGroup<>();
    private final Button exportButton = new Button("Export");
    private final VerticalLayout resultArea = new VerticalLayout();

    public ExportOptionsDialog(ReportService reportService, GeneratedReportEntry entry, Runnable onExported) {
        this.reportService = reportService;
        this.entry = entry;
        this.onExported = onExported;

        setClassName("app-dialog");
        setHeaderTitle("Export Report");
        setWidth("420px");

        formatField.setLabel("Format");
        formatField.setItems(ExportFormat.values());
        formatField.setValue(ExportFormat.PDF);

        exportButton.setIcon(VaadinIcon.DOWNLOAD.create());
        exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        exportButton.addClickListener(event -> startExport());

        Button closeButton = new Button("Close", VaadinIcon.CLOSE.create(), event -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        resultArea.setPadding(false);
        resultArea.setSpacing(false);

        VerticalLayout content = new VerticalLayout(formatField, resultArea);
        content.setPadding(false);
        add(content);
        getFooter().add(new HorizontalLayout(closeButton, exportButton));

        if (entry.getStatus() == GeneratedReportEntry.Status.EXPORTED && entry.getExportedBytes() != null) {
            formatField.setValue(entry.getExportFormat());
            showDownloadLink(entry.getExportFormat(), entry.getExportedBytes());
        }
    }

    private void startExport() {
        ExportFormat format = formatField.getValue();
        if (format == null) {
            showError("Select an export format.");
            return;
        }

        exportButton.setEnabled(false);
        resultArea.removeAll();
        resultArea.add(new LoadingSpinner());

        UI ui = UI.getCurrent();
        ui.setPollInterval(500);
        reportService.exportReport(entry.getReport(), format)
                .thenAccept(bytes -> ui.access(() -> {
                    ui.setPollInterval(-1);
                    entry.markExported(format, bytes);
                    showDownloadLink(format, bytes);
                    exportButton.setEnabled(true);
                    onExported.run();
                }))
                .exceptionally(exception -> {
                    log.error("Error exporting report", exception);
                    ui.access(() -> {
                        ui.setPollInterval(-1);
                        resultArea.removeAll();
                        showError("Could not export the report. Please try again.");
                        exportButton.setEnabled(true);
                    });
                    return null;
                });
    }

    private void showDownloadLink(ExportFormat format, byte[] bytes) {
        resultArea.removeAll();
        String extension = format == ExportFormat.PDF ? "pdf" : "xlsx";
        String fileName = sanitizeFileName(entry.getReport().getTitle()) + "." + extension;

        StreamResource resource = new StreamResource(fileName, () -> new ByteArrayInputStream(bytes));
        Anchor downloadLink = new Anchor(resource, "Download " + fileName);
        downloadLink.getElement().setAttribute("download", true);
        downloadLink.setClassName("download-link");
        downloadLink.getElement().insertChild(0, VaadinIcon.DOWNLOAD.create().getElement());
        resultArea.add(downloadLink);
    }

    private String sanitizeFileName(String title) {
        return title != null ? title.replaceAll("[^a-zA-Z0-9-_]", "_") : "report";
    }

    private void showError(String message) {
        Notifications.error(message);
    }
}
