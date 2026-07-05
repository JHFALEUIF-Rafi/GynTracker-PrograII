package com.gymtracker.view.workout;

import com.gymtracker.enums.Role;
import com.gymtracker.security.CustomUserDetails;
import com.gymtracker.security.SecurityConstants;
import com.gymtracker.service.WorkoutSessionService;
import com.gymtracker.ui.component.Toolbar;
import com.gymtracker.view.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Workout landing screen for Athletes: start a new workout or resume the one
 * in progress. Coaches and Nutritionists have no write access here - they
 * are forwarded to the read-only {@link WorkoutHistoryView}.
 */
@Route(value = "workouts", layout = MainLayout.class)
@PageTitle("Workouts - GymTracker")
public class WorkoutView extends VerticalLayout implements BeforeEnterObserver {

    private final WorkoutSessionService workoutSessionService;
    private final ActiveWorkoutDraft draft;
    private final VerticalLayout contentLayout = new VerticalLayout();
    private String currentAthleteId;

    public WorkoutView(WorkoutSessionService workoutSessionService, ActiveWorkoutDraft draft) {
        this.workoutSessionService = workoutSessionService;
        this.draft = draft;

        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new Toolbar("Workouts"), contentLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (getAuthenticatedRole() != Role.ATHLETE) {
            event.forwardTo(SecurityConstants.WORKOUT_HISTORY_ROUTE);
            return;
        }
        currentAthleteId = getAuthenticatedUserId();
        renderContent();
    }

    private void renderContent() {
        contentLayout.removeAll();
        if (draft.isActive()) {
            contentLayout.add(new WorkoutSessionView(workoutSessionService, draft, this::renderContent, this::renderContent));
        } else {
            contentLayout.add(buildStartCard());
        }
    }

    private VerticalLayout buildStartCard() {
        VerticalLayout card = new VerticalLayout();
        card.setClassName("stat-card");
        card.setPadding(true);
        card.setSpacing(true);
        card.setMaxWidth("420px");

        card.add(new H3("Ready to train?"),
                new Span("Start a new workout session to begin logging exercises and sets."));

        TextField mesocycleIdField = new TextField("Mesocycle ID");
        mesocycleIdField.setWidthFull();
        mesocycleIdField.setHelperText("The mesocycle this session belongs to.");

        Button startButton = new Button("Start Workout", VaadinIcon.PLAY.create(), event -> {
            draft.start(currentAthleteId, mesocycleIdField.getValue());
            renderContent();
        });
        startButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        startButton.setWidthFull();

        Button historyButton = new Button("View History", VaadinIcon.CLOCK.create(),
                event -> UI.getCurrent().navigate(SecurityConstants.WORKOUT_HISTORY_ROUTE));
        historyButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        historyButton.setWidthFull();

        card.add(mesocycleIdField, startButton, historyButton);
        return card;
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
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
