package com.gymtracker.ui.component;

import com.vaadin.flow.component.html.Span;

/**
 * Small pill-shaped badge used to highlight a status, level or count.
 */
public class NotificationBadge extends Span {

    public enum BadgeType {
        SUCCESS,
        WARNING,
        ERROR,
        NEUTRAL
    }

    public NotificationBadge(String text, BadgeType type) {
        super(text);
        setClassName("notification-badge");
        addClassName("badge-" + type.name().toLowerCase());
        getElement().setAttribute("role", "status");
    }

    public void setBadgeType(BadgeType type) {
        removeClassNames("badge-success", "badge-warning", "badge-error", "badge-neutral");
        addClassName("badge-" + type.name().toLowerCase());
    }
}
