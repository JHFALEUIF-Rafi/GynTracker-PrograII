package com.gymtracker.view.settings;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * Language, theme and notification preferences.
 * <p>
 * Theme switching is real and immediate (toggles the Lumo dark/light
 * attribute on the page). Language and notification preferences have no
 * backend support at all (see {@link UserPreferencesHolder}), so they are
 * only kept for the current browser session:
 * <p>
 * TODO: Language is a placeholder for future i18n (Vaadin's
 * {@code I18NProvider}) - selecting it does not yet translate anything.
 * TODO: Notification preferences are not wired to any real notification
 * mechanism - there is none in this project yet.
 */
public class PreferencesView extends VerticalLayout {

    private final UserPreferencesHolder preferences;
    private final ComboBox<String> languageField = new ComboBox<>("Language");
    private final ComboBox<UserPreferencesHolder.Theme> themeField = new ComboBox<>("Theme");
    private final Checkbox emailNotificationsField = new Checkbox("Email notifications");
    private final Checkbox pushNotificationsField = new Checkbox("Push notifications");

    public PreferencesView(UserPreferencesHolder preferences) {
        this.preferences = preferences;

        setPadding(false);
        setSpacing(true);

        languageField.setItems("en", "es");
        languageField.setValue(preferences.getLanguage());
        languageField.setHelperText("Prepared for future translations - no effect yet.");
        languageField.addValueChangeListener(event -> preferences.setLanguage(event.getValue()));

        themeField.setItems(UserPreferencesHolder.Theme.values());
        themeField.setValue(preferences.getTheme());
        themeField.addValueChangeListener(event -> applyTheme(event.getValue()));

        emailNotificationsField.setValue(preferences.isEmailNotifications());
        emailNotificationsField.addValueChangeListener(event -> preferences.setEmailNotifications(event.getValue()));

        pushNotificationsField.setValue(preferences.isPushNotifications());
        pushNotificationsField.addValueChangeListener(event -> preferences.setPushNotifications(event.getValue()));

        add(sectionTitle("Language"), languageField,
                sectionTitle("Theme"), themeField,
                sectionTitle("Notifications"), emailNotificationsField, pushNotificationsField);

        applyTheme(preferences.getTheme());
    }

    private H4 sectionTitle(String title) {
        H4 heading = new H4(title);
        heading.setClassName("athlete-details-section-title");
        return heading;
    }

    private void applyTheme(UserPreferencesHolder.Theme theme) {
        preferences.setTheme(theme);
        UI ui = UI.getCurrent();
        switch (theme) {
            case DARK -> ui.getElement().setAttribute("theme", Lumo.DARK);
            case LIGHT -> ui.getElement().setAttribute("theme", Lumo.LIGHT);
            case SYSTEM -> ui.getElement().removeAttribute("theme");
        }
    }
}
