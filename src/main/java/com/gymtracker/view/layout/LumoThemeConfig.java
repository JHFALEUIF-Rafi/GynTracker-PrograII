package com.gymtracker.view.layout;

import org.springframework.stereotype.Component;

/**
 * Lumo theme configuration for GymTracker.
 * Applies custom colors and spacing to the entire application.
 * <p>
 * The {@code @Theme} annotation itself lives on {@link AppShellConfig} -
 * Vaadin requires app-shell annotations on a dedicated
 * {@code AppShellConfigurator} class, not an arbitrary bean.
 */
@Component
public class LumoThemeConfig {

    public LumoThemeConfig() {
        applyCustomTheme();
    }

    private void applyCustomTheme() {
        System.setProperty("lumo.primary-color", "#1565C0");
        System.setProperty("lumo.primary-text-color", "white");
        System.setProperty("lumo.secondary-text-color", "#666666");
        System.setProperty("lumo.divider-color", "#E8E8E8");
        System.setProperty("lumo-border-radius", "8px");
    }
}
