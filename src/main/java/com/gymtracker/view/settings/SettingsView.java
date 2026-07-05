package com.gymtracker.view.settings;

import com.gymtracker.dto.user.UserProfileDTO;
import com.gymtracker.service.UserService;
import com.gymtracker.ui.component.EmptyState;
import com.gymtracker.ui.component.LoadingSpinner;
import com.gymtracker.ui.component.Toolbar;
import com.gymtracker.view.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;

/**
 * Settings screen: account overview (with logout), security (change
 * password) and preferences. Every authenticated role sees only their own
 * data - there is no target-user concept here, {@link UserService} always
 * resolves the authenticated caller. No business logic lives here.
 */
@Slf4j
@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Settings - GymTracker")
public class SettingsView extends VerticalLayout {

    private final UserService userService;
    private final AccountSettingsForm accountSettingsForm = new AccountSettingsForm();
    private final PreferencesView preferencesView;
    private final VerticalLayout contentLayout = new VerticalLayout();

    public SettingsView(UserService userService, UserPreferencesHolder preferencesHolder) {
        this.userService = userService;
        this.preferencesView = new PreferencesView(preferencesHolder);

        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(new Toolbar("Settings"), contentLayout);
        loadSettings();
    }

    private void loadSettings() {
        contentLayout.removeAll();
        contentLayout.add(new LoadingSpinner());

        try {
            UserProfileDTO profile = userService.getCurrentUserProfile();
            accountSettingsForm.setProfile(profile);

            contentLayout.removeAll();
            contentLayout.add(buildSection("Account", accountSettingsForm),
                    buildSection("Security", buildSecuritySection()),
                    buildSection("Preferences", preferencesView));
        } catch (Exception e) {
            log.error("Error loading settings", e);
            contentLayout.removeAll();
            contentLayout.add(new EmptyState(VaadinIcon.WARNING, "Something Went Wrong", "Could not load your settings. Please try again."));
        }
    }

    private HorizontalLayout buildSecuritySection() {
        Button changePasswordButton = new Button("Change Password", VaadinIcon.LOCK.create(),
                event -> new ChangePasswordDialog(userService, () -> { }).open());
        changePasswordButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return new HorizontalLayout(changePasswordButton);
    }

    private VerticalLayout buildSection(String title, Component content) {
        VerticalLayout section = new VerticalLayout();
        section.setClassName("planner-day-card");
        section.setPadding(true);
        section.setSpacing(true);
        section.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)").set("border-radius", "8px");

        H3 sectionTitle = new H3(title);
        sectionTitle.getStyle().set("margin", "0 0 8px 0");

        section.add(sectionTitle, content);
        return section;
    }
}
