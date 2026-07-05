package com.gymtracker.view.report;

import com.gymtracker.dto.dashboard.ReportDTO;
import com.gymtracker.enums.ExportFormat;

/**
 * Wraps a generated {@link ReportDTO} with UI-only bookkeeping (export
 * format, status, cached export bytes) for display in
 * {@link ReportHistoryView}. ReportService has no persistence for generated
 * reports - every generate call is a fresh, in-memory computation - so this
 * is not a business DTO, only presentation/session state tracked by
 * {@link GeneratedReportsHolder}.
 */
public class GeneratedReportEntry {

    public enum Status {
        GENERATED,
        EXPORTED
    }

    private final ReportDTO report;
    private ExportFormat exportFormat;
    private Status status;
    private byte[] exportedBytes;

    public GeneratedReportEntry(ReportDTO report) {
        this.report = report;
        this.status = Status.GENERATED;
    }

    public ReportDTO getReport() {
        return report;
    }

    public ExportFormat getExportFormat() {
        return exportFormat;
    }

    public Status getStatus() {
        return status;
    }

    public byte[] getExportedBytes() {
        return exportedBytes;
    }

    public void markExported(ExportFormat format, byte[] bytes) {
        this.exportFormat = format;
        this.exportedBytes = bytes;
        this.status = Status.EXPORTED;
    }
}
