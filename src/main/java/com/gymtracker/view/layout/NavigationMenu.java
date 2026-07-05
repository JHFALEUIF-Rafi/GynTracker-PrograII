package com.gymtracker.view.layout;

import com.gymtracker.enums.Role;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import lombok.extern.slf4j.Slf4j;

/**
 * Navigation menu component using Vaadin SideNav.
 * Menu items visibility depends on user role.
 */
@Slf4j
public class NavigationMenu extends SideNav {

    public NavigationMenu() {
        setWidth("250px");
        getStyle()
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "8px 0 0 8px");
    }

    public void setupMenuForRole(Role role) {
        removeAll();

        addNavItem("Dashboard", "dashboard", VaadinIcon.DASHBOARD);

        switch (role) {
            case ATHLETE -> setupAthleteMenu();
            case COACH -> setupCoachMenu();
            case NUTRITIONIST -> setupNutritionistMenu();
        }

        addNavItem("Alerts", "alerts", VaadinIcon.BELL);
        addNavItem("Statistics", "statistics", VaadinIcon.BAR_CHART);
        addNavItem("My Account", "profile", VaadinIcon.USER_CARD);
        addNavItem("Settings", "settings", VaadinIcon.COG);
    }

    private void setupAthleteMenu() {
        addNavItem("My Profile", "athletes/profile", VaadinIcon.USER);
        addNavItem("My Mesocycle", "mesocycles", VaadinIcon.CALENDAR);
        addNavItem("Workouts", "workouts", VaadinIcon.CALENDAR);
        addNavItem("Workout History", "workouts/history", VaadinIcon.CLOCK);
        addNavItem("Nutrition", "nutrition", VaadinIcon.FLASK);
        addNavItem("Nutrition History", "nutrition/history", VaadinIcon.CLOCK);
        addNavItem("Reports", "reports", VaadinIcon.FILE_TEXT);
    }

    private void setupCoachMenu() {
        addNavItem("Athletes", "athletes", VaadinIcon.USERS);
        addNavItem("Exercises", "exercises", VaadinIcon.HAMMER);
        addNavItem("Mesocycles", "mesocycles", VaadinIcon.CALENDAR);
        addNavItem("Workout History", "workouts/history", VaadinIcon.CLOCK);
        addNavItem("Nutrition Plans", "nutrition", VaadinIcon.FLASK);
        addNavItem("Reports", "reports", VaadinIcon.FILE_TEXT);
    }

    private void setupNutritionistMenu() {
        addNavItem("Nutrition Plans", "nutrition", VaadinIcon.FLASK);
        addNavItem("Nutrition History", "nutrition/history", VaadinIcon.CLOCK);
        addNavItem("Athletes", "athletes", VaadinIcon.USERS);
        addNavItem("Mesocycles", "mesocycles", VaadinIcon.CALENDAR);
        addNavItem("Workout History", "workouts/history", VaadinIcon.CLOCK);
        addNavItem("Reports", "reports", VaadinIcon.FILE_TEXT);
    }

    private void addNavItem(String label, String route, VaadinIcon icon) {
        SideNavItem item = new SideNavItem(label, route, new Icon(icon));
        item.getStyle()
                .set("border-radius", "8px")
                .set("margin", "4px 8px");
        addItem(item);
        log.debug("Added menu item: {}", label);
    }
}
