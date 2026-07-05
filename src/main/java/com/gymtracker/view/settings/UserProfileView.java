package com.gymtracker.view.settings;

import com.gymtracker.dto.user.UserProfileDTO;
import com.gymtracker.dto.user.UserProfileUpdateDTO;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.service.UserService;
import com.gymtracker.ui.component.EmptyState;
import com.gymtracker.ui.component.LoadingSpinner;
import com.gymtracker.ui.component.Notifications;
import com.gymtracker.ui.component.Toolbar;
import com.gymtracker.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;

/**
 * Self-service profile screen for any authenticated role. Only the generic
 * fields (first/last name) are editable here through {@link UserService};
 * Athlete-specific biometric fields remain on {@code AthleteProfileView}
 * (AthleteService), which this module does not consume.
 */
@Slf4j
@Route(value = "profile", layout = MainLayout.class)
@PageTitle("My Profile - GymTracker")
public class UserProfileView extends VerticalLayout {

    private final UserService userService;
    private final VerticalLayout contentLayout = new VerticalLayout();

    private final TextField firstNameField = new TextField("First Name");
    private final TextField lastNameField = new TextField("Last Name");
    private final EmailField emailField = new EmailField("Email");

    public UserProfileView(UserService userService) {
        this.userService = userService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new Toolbar("My Profile"), contentLayout);
        loadProfile();
    }

    private void loadProfile() {
        contentLayout.removeAll();
        contentLayout.add(new LoadingSpinner());

        try {
            UserProfileDTO profile = userService.getCurrentUserProfile();
            contentLayout.removeAll();
            contentLayout.add(buildForm(profile));
        } catch (Exception e) {
            log.error("Error loading user profile", e);
            contentLayout.removeAll();
            contentLayout.add(new EmptyState(VaadinIcon.WARNING, "Something Went Wrong", "Could not load your profile. Please try again."));
        }
    }

    private VerticalLayout buildForm(UserProfileDTO profile) {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        emailField.setValue(profile.getEmail() != null ? profile.getEmail() : "");
        emailField.setReadOnly(true);

        firstNameField.setValue(profile.getFirstName() != null ? profile.getFirstName() : "");
        lastNameField.setValue(profile.getLastName() != null ? profile.getLastName() : "");

        Upload profilePictureUpload = new Upload(new MemoryBuffer());
        profilePictureUpload.setEnabled(false);
        profilePictureUpload.getElement().setAttribute("title",
                "Not available yet - profile pictures are not part of the current data model.");

        form.add(emailField, firstNameField, lastNameField);

        Button saveButton = new Button("Save Changes", VaadinIcon.CHECK.create(), event -> attemptSave());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout wrapper = new VerticalLayout(form, profilePictureUpload, saveButton);
        wrapper.setPadding(false);
        wrapper.setSpacing(true);
        return wrapper;
    }

    private void attemptSave() {
        if (firstNameField.getValue() == null || firstNameField.getValue().isBlank()
                || lastNameField.getValue() == null || lastNameField.getValue().isBlank()) {
            showError("First name and last name are required.");
            return;
        }

        try {
            UserProfileUpdateDTO requestDTO = UserProfileUpdateDTO.builder()
                    .firstName(firstNameField.getValue())
                    .lastName(lastNameField.getValue())
                    .build();
            userService.updateProfile(requestDTO);
            showSuccess("Profile updated successfully.");
        } catch (ValidationException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating user profile", e);
            showError("Could not update your profile. Please try again.");
        }
    }

    private void showSuccess(String message) {
        Notifications.success(message);
    }

    private void showError(String message) {
        Notifications.error(message);
    }
}
