package com.gymtracker.view.auth;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.value.ValueChangeMode;

/**
 * Presentational login form: logo, title, subtitle, credential fields and the
 * login action. It holds no authentication or navigation logic; the owning
 * view reacts to {@link #getLoginButton()} clicks and reads the field values.
 */
public class LoginFormComponent extends VerticalLayout {

    private final EmailField emailField;
    private final PasswordField passwordField;
    private final Checkbox rememberMeCheckbox;
    private final Button loginButton;

    public LoginFormComponent() {
        setClassName("login-card");
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.STRETCH);

        Icon logo = VaadinIcon.HAMMER.create();
        logo.setClassName("login-logo");
        logo.setSize("40px");

        H1 title = new H1("GymTracker");
        title.setClassName("login-title");

        Span subtitle = new Span("Sign in to manage your training, nutrition and progress.");
        subtitle.setClassName("login-subtitle");

        emailField = new EmailField("Email");
        emailField.setWidthFull();
        emailField.setRequiredIndicatorVisible(true);
        emailField.setValueChangeMode(ValueChangeMode.EAGER);
        emailField.setErrorMessage("Email is required.");
        emailField.setAutocomplete(Autocomplete.EMAIL);
        emailField.setPrefixComponent(VaadinIcon.USER.create());

        passwordField = new PasswordField("Password");
        passwordField.setWidthFull();
        passwordField.setRequiredIndicatorVisible(true);
        passwordField.setValueChangeMode(ValueChangeMode.EAGER);
        passwordField.setErrorMessage("Password is required.");
        passwordField.setAutocomplete(Autocomplete.CURRENT_PASSWORD);
        passwordField.setPrefixComponent(VaadinIcon.LOCK.create());

        rememberMeCheckbox = new Checkbox("Remember me");

        loginButton = new Button("Login", VaadinIcon.SIGN_IN.create());
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidthFull();
        loginButton.addClickShortcut(Key.ENTER);

        add(logo, title, subtitle, emailField, passwordField, rememberMeCheckbox, loginButton);
    }

    /**
     * Validates that the required fields are filled in, marking invalid
     * fields so the user gets immediate feedback.
     *
     * @return true when both email and password are present
     */
    public boolean validate() {
        boolean valid = true;

        if (emailField.isEmpty()) {
            emailField.setInvalid(true);
            valid = false;
        } else {
            emailField.setInvalid(false);
        }

        if (passwordField.isEmpty()) {
            passwordField.setInvalid(true);
            valid = false;
        } else {
            passwordField.setInvalid(false);
        }

        return valid;
    }

    public void setProcessing(boolean processing) {
        loginButton.setEnabled(!processing);
        emailField.setEnabled(!processing);
        passwordField.setEnabled(!processing);
        rememberMeCheckbox.setEnabled(!processing);
        loginButton.setText(processing ? "Signing in..." : "Login");
    }

    public String getEmail() {
        return emailField.getValue();
    }

    public String getPassword() {
        return passwordField.getValue();
    }

    public boolean isRememberMe() {
        return rememberMeCheckbox.getValue();
    }

    public Button getLoginButton() {
        return loginButton;
    }

    public void focusEmail() {
        emailField.focus();
    }
}
