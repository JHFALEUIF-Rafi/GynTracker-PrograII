package com.gymtracker.view.report;

import com.gymtracker.enums.Role;
import com.gymtracker.security.CustomUserDetails;
import com.gymtracker.service.ReportService;
import com.gymtracker.ui.component.Toolbar;
import com.gymtracker.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reports screen. Coaches generate athlete/coach/progress/workout-history/
 * mesocycle reports for their assigned athletes; Nutritionists generate
 * nutrition reports; Athletes generate only their own progress report. No
 * business logic lives here - every computation and export is delegated to
 * {@link ReportService}.
 */
@Route(value = "reports", layout = MainLayout.class)
@PageTitle("Reports - GymTracker")
@PermitAll
public class ReportsView extends VerticalLayout implements BeforeEnterObserver {

    private final ReportService reportService;
    private final GeneratedReportsHolder holder;
    private final Toolbar toolbar = new Toolbar("Reports");
    private ReportHistoryView historyView;

    private Role currentRole;
    private String currentUserId;

    public ReportsView(ReportService reportService, GeneratedReportsHolder holder) {
        this.reportService = reportService;
        this.holder = holder;

        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(toolbar);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        currentRole = getAuthenticatedRole();
        currentUserId = getAuthenticatedUserId();

        if (historyView == null) {
            historyView = new ReportHistoryView(reportService, holder);
            add(historyView);

            Button newReportButton = new Button("New Report", VaadinIcon.PLUS.create(), clickEvent -> openGeneratorDialog());
            newReportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            toolbar.addAction(newReportButton);
        } else {
            historyView.refresh();
        }
    }

    private void openGeneratorDialog() {
        ReportGeneratorDialog dialog = new ReportGeneratorDialog(
                reportService, holder, currentRole, currentUserId, historyView::refresh);
        dialog.open();
    }

    private String getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        return null;
    }

    private Role getAuthenticatedRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getAuthorities().stream()
                .map(authority -> {
                    String value = authority.getAuthority();
                    if (value.contains("ATHLETE")) {
                        return Role.ATHLETE;
                    }
                    if (value.contains("COACH")) {
                        return Role.COACH;
                    }
                    if (value.contains("NUTRITIONIST")) {
                        return Role.NUTRITIONIST;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
