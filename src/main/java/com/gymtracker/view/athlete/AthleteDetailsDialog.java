package com.gymtracker.view.athlete;

import com.gymtracker.dto.athlete.AthleteDetailDTO;
import com.gymtracker.dto.nutrition.NutritionPlanResponseDTO;
import com.gymtracker.dto.workout.WorkoutSessionSummaryDTO;
import com.gymtracker.enums.FatigueLevel;
import com.gymtracker.ui.component.NotificationBadge;
import com.gymtracker.ui.component.StatCard;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Read-only dialog showing the full picture of an athlete: personal
 * information, current mesocycle, nutrition plan summary, latest workout,
 * fatigue level, latest 1RM and assigned coach/nutritionist. Purely
 * presentational; the owning view supplies the already-fetched DTO.
 */
public class AthleteDetailsDialog extends Dialog {

    private final VerticalLayout content = new VerticalLayout();

    public AthleteDetailsDialog() {
        setClassName("app-dialog");
        setWidth("620px");
        setMaxWidth("95vw");
        setHeaderTitle("Athlete Details");

        Button closeButton = new Button(VaadinIcon.CLOSE.create(), event -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        closeButton.setAriaLabel("Close");
        getHeader().add(closeButton);

        content.setPadding(false);
        content.setSpacing(true);
        add(content);
    }

    public void showDetails(AthleteDetailDTO detail) {
        content.removeAll();
        content.add(
                buildPersonalInfoSection(detail),
                buildAssignmentsSection(detail),
                buildNutritionSection(detail),
                buildPerformanceSection(detail)
        );
        open();
    }

    private VerticalLayout buildPersonalInfoSection(AthleteDetailDTO detail) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);

        H3 title = sectionTitle("Personal Information");
        HorizontalLayout nameAndStatus = new HorizontalLayout();
        nameAndStatus.setWidthFull();
        nameAndStatus.setJustifyContentMode(JustifyContentMode.BETWEEN);
        nameAndStatus.add(
                new Span(detail.getFirstName() + " " + detail.getLastName()),
                new NotificationBadge(
                        Boolean.TRUE.equals(detail.getEnabled()) ? "Active" : "Inactive",
                        Boolean.TRUE.equals(detail.getEnabled()) ? NotificationBadge.BadgeType.SUCCESS : NotificationBadge.BadgeType.NEUTRAL)
        );

        section.add(title, nameAndStatus,
                infoRow("Email", detail.getEmail()),
                infoRow("Age", detail.getAge() != null ? detail.getAge() + " years" : "-"),
                infoRow("Gender", detail.getGender() != null ? detail.getGender().name() : "-"),
                infoRow("Weight", detail.getWeight() != null ? detail.getWeight() + " kg" : "-"),
                infoRow("Height", detail.getHeight() != null ? detail.getHeight() + " cm" : "-"),
                infoRow("Activity Level", detail.getActivityLevel() != null ? detail.getActivityLevel().name() : "-")
        );
        return section;
    }

    private VerticalLayout buildAssignmentsSection(AthleteDetailDTO detail) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);

        section.add(sectionTitle("Assignments"),
                infoRow("Current Mesocycle", valueOrDash(detail.getCurrentMesocycleName())),
                infoRow("Assigned Coach", valueOrDash(detail.getAssignedCoachName())),
                infoRow("Assigned Nutritionist", valueOrDash(detail.getAssignedNutritionistName()))
        );
        return section;
    }

    private VerticalLayout buildNutritionSection(AthleteDetailDTO detail) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.add(sectionTitle("Nutrition Plan Summary"));

        NutritionPlanResponseDTO plan = detail.getActiveNutritionPlan();
        if (plan == null) {
            section.add(new Span("No active nutrition plan."));
            return section;
        }

        HorizontalLayout cards = newCardsRow();
        cards.add(
                new StatCard(VaadinIcon.BULLSEYE, "Goal", plan.getGoal() != null ? plan.getGoal().name() : "-"),
                new StatCard(VaadinIcon.CUTLERY, "Calories", plan.getCalories() != null ? plan.getCalories() + " kcal" : "-"),
                new StatCard(VaadinIcon.FLASK, "Protein / Carbs / Fat", formatMacros(plan))
        );
        section.add(cards);
        return section;
    }

    private VerticalLayout buildPerformanceSection(AthleteDetailDTO detail) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.add(sectionTitle("Performance"));

        HorizontalLayout cards = newCardsRow();
        cards.add(
                new StatCard(VaadinIcon.CALENDAR, "Latest Workout", formatLatestWorkout(detail.getLatestWorkout())),
                buildFatigueCard(detail.getCurrentFatigueLevel()),
                new StatCard(VaadinIcon.TROPHY, "Latest 1RM", detail.getLatestOneRepMax() != null
                        ? String.format("%.1f kg", detail.getLatestOneRepMax()) : "Not available")
        );
        section.add(cards);
        return section;
    }

    private HorizontalLayout newCardsRow() {
        HorizontalLayout cards = new HorizontalLayout();
        cards.setWidthFull();
        cards.setSpacing(true);
        cards.getStyle().set("flex-wrap", "wrap");
        return cards;
    }

    private StatCard buildFatigueCard(FatigueLevel fatigueLevel) {
        return new StatCard(VaadinIcon.HEART, "Fatigue Level", fatigueLevel != null ? fatigueLevel.name() : "Not available");
    }

    private String formatLatestWorkout(WorkoutSessionSummaryDTO latestWorkout) {
        if (latestWorkout == null || latestWorkout.getDate() == null) {
            return "No workouts recorded";
        }
        return latestWorkout.getDate().toString();
    }

    private String formatMacros(NutritionPlanResponseDTO plan) {
        return String.format("%.0fg / %.0fg / %.0fg",
                valueOrZero(plan.getProtein()), valueOrZero(plan.getCarbohydrates()), valueOrZero(plan.getFat()));
    }

    private double valueOrZero(Double value) {
        return value != null ? value : 0d;
    }

    private HorizontalLayout infoRow(String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setJustifyContentMode(JustifyContentMode.BETWEEN);

        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");

        row.add(labelSpan, new Span(value));
        return row;
    }

    private H3 sectionTitle(String title) {
        H3 heading = new H3(title);
        heading.setClassName("athlete-details-section-title");
        return heading;
    }

    private String valueOrDash(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }
}
