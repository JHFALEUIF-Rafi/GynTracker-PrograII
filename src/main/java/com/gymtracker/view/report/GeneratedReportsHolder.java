package com.gymtracker.view.report;

import com.gymtracker.dto.dashboard.ReportDTO;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Holds the reports generated during the browser session, since
 * ReportService does not persist generated reports anywhere (no repository,
 * no "list past reports" method). This is a plain state container - it holds
 * no business logic.
 */
@VaadinSessionScope
@Component
public class GeneratedReportsHolder {

    private final List<GeneratedReportEntry> entries = new ArrayList<>();

    public void add(ReportDTO report) {
        entries.add(0, new GeneratedReportEntry(report));
    }

    public List<GeneratedReportEntry> getEntries() {
        return entries;
    }
}
