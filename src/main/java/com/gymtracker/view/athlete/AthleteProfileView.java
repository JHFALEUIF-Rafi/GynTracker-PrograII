package com.gymtracker.view.athlete;

import com.gymtracker.dto.athlete.AthleteDetailDTO;
import com.gymtracker.dto.athlete.AthleteRequestDTO;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.security.SecurityConstants;
import com.gymtracker.service.AthleteService;
import com.gymtracker.ui.component.ConfirmDialog;
import com.gymtracker.ui.component.EmptyState;
import com.gymtracker.ui.component.LoadingSpinner;
import com.gymtracker.ui.component.Notifications;
import com.gymtracker.ui.component.Toolbar;
import com.gymtracker.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Self-service profile screen. Athletes may view their personal information
 * and edit their weight and height; every other field is read-only. Coaches
 * and Nutritionists are forwarded to the dashboard, since this page is for an
 * athlete's own profile only - they review other athletes through
 * {@link AthleteView} instead.
 */
@Slf4j
@Route(value = "athletes/profile", layout = MainLayout.class)
@PageTitle("My Profile - GymTracker")
public class AthleteProfileView extends VerticalLayout implements BeforeEnterObserver {

    private final AthleteService athleteService;
    private final VerticalLayout contentLayout = new VerticalLayout();
    private final AthleteForm athleteForm = new AthleteForm();
    private AthleteDetailDTO currentAthlete;

    public AthleteProfileView(AthleteService athleteService) {
        this.athleteService = athleteService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();

        Toolbar toolbar = new Toolbar("My Profile");
        add(toolbar, contentLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (getAuthenticatedRole() != Role.ATHLETE) {
            event.forwardTo(SecurityConstants.DASHBOARD_ROUTE);
            return;
        }
        loadProfile();
    }

    private void loadProfile() {
        contentLayout.removeAll();
        contentLayout.add(new LoadingSpinner());

        try {
            currentAthlete = athleteService.getCurrentAthlete();
            contentLayout.removeAll();
            contentLayout.add(buildProfileForm());
        } catch (UnauthorizedOperationException e) {
            log.warn("Unauthorized profile access: {}", e.getMessage());
            contentLayout.removeAll();
            contentLayout.add(new EmptyState(VaadinIcon.LOCK, "Not Available", "You are not allowed to view this profile."));
        } catch (Exception e) {
            log.error("Error loading athlete profile", e);
            contentLayout.removeAll();
            contentLayout.add(new EmptyState(VaadinIcon.WARNING, "Something Went Wrong", "Could not load your profile. Please try again."));
        }
    }

    private VerticalLayout buildProfileForm() {
        athleteForm.setAthlete(currentAthlete);

        Button saveButton = new Button("Save Changes", VaadinIcon.CHECK.create(), event -> attemptSave());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", VaadinIcon.CLOSE.create(), event -> attemptCancel());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout actions = new HorizontalLayout(cancelButton, saveButton);

        VerticalLayout wrapper = new VerticalLayout(athleteForm, actions);
        wrapper.setPadding(false);
        wrapper.setSpacing(true);
        return wrapper;
    }

    private void attemptSave() {
        if (!athleteForm.isValid()) {
            showError("Please review the highlighted fields before saving.");
            return;
        }

        try {
            AthleteRequestDTO requestDTO = athleteForm.getValue();
            athleteService.updateAthleteProfile(requestDTO);
            showSuccess("Profile updated successfully.");
            loadProfile();
        } catch (ValidationException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating athlete profile", e);
            showError("Could not update your profile. Please try again.");
        }
    }

    private void attemptCancel() {
        if (!athleteForm.isDirty()) {
            return;
        }

        ConfirmDialog confirmDialog = new ConfirmDialog("Discard Changes", "You have unsaved changes. Discard them?");
        confirmDialog.setOnConfirm(this::loadProfile);
        confirmDialog.open();
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

    private void showSuccess(String message) {
        Notifications.success(message);
    }

    private void showError(String message) {
        Notifications.error(message);
    }
}
