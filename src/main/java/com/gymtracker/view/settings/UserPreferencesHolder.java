package com.gymtracker.view.settings;

import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.stereotype.Component;

/**
 * Holds theme/language/notification preferences for the browser session.
 * <p>
 * TODO: none of these are persisted anywhere - User has no preferences
 * fields and UserService exposes none. Once a real "user preferences"
 * concept exists in the data model, replace this session holder with a
 * UserService call so preferences survive across logins/devices.
 */
@VaadinSessionScope
@Component
public class UserPreferencesHolder {

    public enum Theme {
        LIGHT,
        DARK,
        SYSTEM
    }

    private Theme theme = Theme.SYSTEM;
    private String language = "en";
    private boolean emailNotifications = true;
    private boolean pushNotifications = true;

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public boolean isPushNotifications() {
        return pushNotifications;
    }

    public void setPushNotifications(boolean pushNotifications) {
        this.pushNotifications = pushNotifications;
    }
}
