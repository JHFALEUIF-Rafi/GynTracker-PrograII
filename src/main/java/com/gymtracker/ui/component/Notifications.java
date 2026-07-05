package com.gymtracker.ui.component;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

/**
 * Centralized helper for showing toast notifications with a consistent
 * position, duration, and theme variant across every view.
 */
public final class Notifications {

    private static final int SUCCESS_DURATION_MS = 3000;
    private static final int ERROR_DURATION_MS = 4000;
    private static final int WARNING_DURATION_MS = 4000;

    private Notifications() {
    }

    public static void success(String message) {
        Notification notification = Notification.show(message, SUCCESS_DURATION_MS, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    public static void error(String message) {
        Notification notification = Notification.show(message, ERROR_DURATION_MS, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    public static void warning(String message) {
        Notification notification = Notification.show(message, WARNING_DURATION_MS, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
    }
}
