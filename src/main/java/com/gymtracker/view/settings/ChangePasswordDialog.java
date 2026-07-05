package com.gymtracker.view.settings;

import com.gymtracker.dto.user.ChangePasswordRequestDTO;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.service.UserService;
import com.gymtracker.ui.component.Notifications;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import lombok.extern.slf4j.Slf4j;

/**
 * Dialog for changing the authenticated user's password: current password is
 * verified server-side, new password must be confirmed. No business logic
 * lives here - validation and the actual change are delegated to
 * {@link UserService}.
 */
@Slf4j
public class ChangePasswordDialog extends Dialog {

    private final UserService userService;
    private final Runnable onChanged;

    private final PasswordField currentPasswordField = new PasswordField("Current Password");
    private final PasswordField newPasswordField = new PasswordField("New Password");
    private final PasswordField confirmPasswordField = new PasswordField("Confirm New Password");

    public ChangePasswordDialog(UserService userService, Runnable onChanged) {
        this.userService = userService;
        this.onChanged = onChanged;

        setClassName("app-dialog");
        setHeaderTitle("Change Password");
        setWidth("400px");

        currentPasswordField.setWidthFull();
        currentPasswordField.setRequiredIndicatorVisible(true);

        newPasswordField.setWidthFull();
        newPasswordField.setRequiredIndicatorVisible(true);
        newPasswordField.setHelperText("At least 8 characters.");

        confirmPasswordField.setWidthFull();
        confirmPasswordField.setRequiredIndicatorVisible(true);

        Button changeButton = new Button("Change Password", VaadinIcon.CHECK.create(), event -> attemptChange());
        changeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", VaadinIcon.CLOSE.create(), event -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        VerticalLayout content = new VerticalLayout(currentPasswordField, newPasswordField, confirmPasswordField);
        content.setPadding(false);
        add(content);
        getFooter().add(new HorizontalLayout(cancelButton, changeButton));
    }

    private void attemptChange() {
        if (isBlank(currentPasswordField) || isBlank(newPasswordField) || isBlank(confirmPasswordField)) {
            showError("All fields are required.");
            return;
        }

        try {
            ChangePasswordRequestDTO requestDTO = ChangePasswordRequestDTO.builder()
                    .currentPassword(currentPasswordField.getValue())
                    .newPassword(newPasswordField.getValue())
                    .confirmNewPassword(confirmPasswordField.getValue())
                    .build();

            userService.changePassword(requestDTO);
            showSuccess("Password changed successfully.");
            onChanged.run();
            close();
        } catch (UnauthorizedOperationException e) {
            showError("Current password is incorrect.");
        } catch (ValidationException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            log.error("Error changing password", e);
            showError("Could not change your password. Please try again.");
        }
    }

    private boolean isBlank(PasswordField field) {
        return field.getValue() == null || field.getValue().isBlank();
    }

    private void showSuccess(String message) {
        Notifications.success(message);
    }

    private void showError(String message) {
        Notifications.error(message);
    }
}
