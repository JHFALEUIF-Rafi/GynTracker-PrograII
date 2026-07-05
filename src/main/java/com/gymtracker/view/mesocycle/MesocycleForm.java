package com.gymtracker.view.mesocycle;

import com.gymtracker.dto.mesocycle.MesocycleDetailDTO;
import com.gymtracker.dto.mesocycle.MesocycleRequestDTO;
import com.gymtracker.enums.MesocycleStatus;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;

/**
 * Presentational mesocycle create/edit form. The Coach is always the
 * authenticated user (enforced server-side), so it is not an editable field
 * here. The Athlete is referenced by id only, since this form depends solely
 * on MesocycleService and has no athlete directory to search against.
 * Validation of scalar fields is sourced from the Bean Validation constraints
 * already declared on {@link MesocycleRequestDTO}; the weekly plan is
 * validated separately by {@link WeeklyPlannerComponent}.
 */
public class MesocycleForm extends FormLayout {

    private final TextField nameField = new TextField("Mesocycle Name");
    private final TextField athleteIdField = new TextField("Athlete ID");
    private final IntegerField durationWeeksField = new IntegerField("Duration (weeks)");
    private final IntegerField targetRpeField = new IntegerField("Target RPE");
    private final TextArea notesField = new TextArea("Notes");
    private final ComboBox<MesocycleStatus> statusField = new ComboBox<>("Status");
    private final WeeklyPlannerComponent weeklyPlanner = new WeeklyPlannerComponent();

    private final BeanValidationBinder<MesocycleRequestDTO> binder = new BeanValidationBinder<>(MesocycleRequestDTO.class);
    private MesocycleRequestDTO workingCopy;
    private String coachId;

    public MesocycleForm() {
        setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep("500px", 2)
        );

        durationWeeksField.setMin(1);
        targetRpeField.setMin(1);
        targetRpeField.setMax(10);
        statusField.setItems(MesocycleStatus.DRAFT, MesocycleStatus.ACTIVE);
        notesField.setMaxLength(500);
        setColspan(notesField, 2);

        binder.forField(nameField).asRequired("Name is required.").bind("name");
        binder.forField(athleteIdField).asRequired("Athlete id is required.").bind("athleteId");
        binder.forField(durationWeeksField).asRequired("Duration is required.").bind("durationWeeks");
        binder.forField(targetRpeField).asRequired("Target RPE is required.").bind("targetRpe");
        binder.forField(notesField).bind("notes");
        binder.forField(statusField).asRequired("Status is required.").bind("status");

        add(nameField, athleteIdField, durationWeeksField, targetRpeField, statusField, notesField);
        add(new H4("Weekly Plan"), weeklyPlanner);
        setColspan(weeklyPlanner, 2);
    }

    public void setCoachId(String coachId) {
        this.coachId = coachId;
    }

    public void setNewMesocycle() {
        workingCopy = MesocycleRequestDTO.builder().status(MesocycleStatus.DRAFT).build();
        binder.setBean(workingCopy);
        weeklyPlanner.setValue(null);
    }

    public void setMesocycle(MesocycleDetailDTO detail) {
        workingCopy = MesocycleRequestDTO.builder()
                .athleteId(detail.getAthleteId())
                .name(detail.getName())
                .durationWeeks(detail.getDurationWeeks())
                .targetRpe(detail.getTargetRpe())
                .notes(detail.getNotes())
                .status(detail.getStatus())
                .days(detail.getDays())
                .build();
        binder.setBean(workingCopy);
        weeklyPlanner.setValue(detail.getDays());
    }

    public boolean isValid() {
        return binder.isValid() && weeklyPlanner.isValid();
    }

    public MesocycleRequestDTO getValue() {
        workingCopy.setCoachId(coachId);
        workingCopy.setDays(weeklyPlanner.getValue());
        return workingCopy;
    }
}
