package com.gymtracker.view.report;

import com.gymtracker.dto.dashboard.ReportDTO;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.service.ReportService;
import com.gymtracker.ui.component.Notifications;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

/**
 * Dialog for generating a new report. The available report types depend on
 * the authenticated role, matching the permissions given for this module
 * (Athletes may only generate their own Progress Report; Coaches generate
 * athlete/coach/progress/workout-history/mesocycle reports; Nutritionists
 * generate nutrition reports). Generation itself is synchronous in
 * ReportService (no {@code @Async}), so this only shows a brief busy state,
 * not a long-running progress indicator - that is reserved for export, which
 * is genuinely asynchronous (see {@link ExportOptionsDialog}).
 */
@Slf4j
public class ReportGeneratorDialog extends Dialog {

    private record ReportTypeOption(String label, boolean needsTargetId, String targetLabel,
                                     Function<String, ReportDTO> generator) {
    }

    private final GeneratedReportsHolder holder;
    private final Runnable onGenerated;
    private final ComboBox<ReportTypeOption> typeField = new ComboBox<>("Report Type");
    private final TextField targetIdField = new TextField("Target ID");
    private final Button generateButton = new Button("Generate");

    public ReportGeneratorDialog(ReportService reportService, GeneratedReportsHolder holder,
                                  Role role, String currentUserId, Runnable onGenerated) {
        this.holder = holder;
        this.onGenerated = onGenerated;

        setClassName("app-dialog");
        setHeaderTitle("New Report");
        setWidth("480px");

        List<ReportTypeOption> options = buildOptions(reportService, role, currentUserId);
        typeField.setItems(options);
        typeField.setItemLabelGenerator(ReportTypeOption::label);
        typeField.addValueChangeListener(event -> updateTargetIdVisibility(event.getValue()));

        targetIdField.setWidthFull();
        targetIdField.setVisible(false);

        generateButton.setIcon(VaadinIcon.PLAY.create());
        generateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        generateButton.addClickListener(event -> attemptGenerate());

        Button cancelButton = new Button("Cancel", VaadinIcon.CLOSE.create(), event -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        VerticalLayout content = new VerticalLayout(typeField, targetIdField);
        content.setPadding(false);
        add(content);
        getFooter().add(new HorizontalLayout(cancelButton, generateButton));

        if (!options.isEmpty()) {
            typeField.setValue(options.get(0));
        }
    }

    private List<ReportTypeOption> buildOptions(ReportService reportService, Role role, String currentUserId) {
        List<ReportTypeOption> options = new ArrayList<>();
        switch (role) {
            case ATHLETE -> options.add(new ReportTypeOption("Progress Report", false, null,
                    id -> reportService.generateProgressReport(currentUserId)));
            case COACH -> {
                options.add(new ReportTypeOption("Athlete Report", true, "Athlete ID",
                        reportService::generateAthleteReport));
                options.add(new ReportTypeOption("Coach Report", false, null,
                        id -> reportService.generateCoachReport(currentUserId)));
                options.add(new ReportTypeOption("Progress Report", true, "Athlete ID",
                        reportService::generateProgressReport));
                options.add(new ReportTypeOption("Workout History Report", true, "Athlete ID",
                        reportService::generateWorkoutHistoryReport));
                options.add(new ReportTypeOption("Mesocycle Report", true, "Mesocycle ID",
                        reportService::generateMesocycleReport));
            }
            case NUTRITIONIST -> options.add(new ReportTypeOption("Nutrition Report", true, "Athlete ID",
                    reportService::generateNutritionReport));
        }
        return options;
    }

    private void updateTargetIdVisibility(ReportTypeOption option) {
        boolean needsTargetId = option != null && option.needsTargetId();
        targetIdField.setVisible(needsTargetId);
        targetIdField.clear();
        if (needsTargetId) {
            targetIdField.setLabel(option.targetLabel());
        }
    }

    private void attemptGenerate() {
        ReportTypeOption option = typeField.getValue();
        if (option == null) {
            showError("Select a report type.");
            return;
        }
        if (option.needsTargetId() && (targetIdField.getValue() == null || targetIdField.getValue().isBlank())) {
            showError(option.targetLabel() + " is required.");
            return;
        }

        generateButton.setEnabled(false);
        generateButton.setText("Generating...");
        try {
            ReportDTO report = option.generator().apply(targetIdField.getValue());
            holder.add(report);
            showSuccess("Report generated successfully.");
            onGenerated.run();
            close();
        } catch (ValidationException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            log.error("Error generating report", e);
            showError("Could not generate the report. Please try again.");
        } finally {
            generateButton.setEnabled(true);
            generateButton.setText("Generate");
        }
    }

    private void showSuccess(String message) {
        Notifications.success(message);
    }

    private void showError(String message) {
        Notifications.error(message);
    }
}
