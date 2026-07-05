package com.gymtracker.view.nutrition;

import com.gymtracker.dto.nutrition.NutritionPlanDetailDTO;
import com.gymtracker.dto.nutrition.NutritionPlanRequestDTO;
import com.gymtracker.enums.NutritionGoal;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;

/**
 * Presentational nutrition plan create/edit form. The Nutritionist is always
 * the authenticated user (enforced server-side), so it is not an editable
 * field here. The Athlete is referenced by id only, since this form depends
 * solely on NutritionPlanService and has no athlete directory to search
 * against. Validation of scalar fields is sourced from the Bean Validation
 * constraints already declared on {@link NutritionPlanRequestDTO}.
 */
public class NutritionPlanForm extends FormLayout {

    private final TextField athleteIdField = new TextField("Athlete ID");
    private final ComboBox<NutritionGoal> goalField = new ComboBox<>("Goal");
    private final IntegerField caloriesField = new IntegerField("Calories");
    private final NumberField proteinField = new NumberField("Protein (g)");
    private final NumberField carbohydratesField = new NumberField("Carbohydrates (g)");
    private final NumberField fatField = new NumberField("Fat (g)");
    private final DatePicker startDateField = new DatePicker("Start Date");
    private final DatePicker endDateField = new DatePicker("End Date");
    private final TextArea observationsField = new TextArea("Observations");
    private final Checkbox activeField = new Checkbox("Set as active plan");

    private final BeanValidationBinder<NutritionPlanRequestDTO> binder =
            new BeanValidationBinder<>(NutritionPlanRequestDTO.class);
    private NutritionPlanRequestDTO workingCopy;
    private String nutritionistId;

    public NutritionPlanForm() {
        setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep("500px", 2)
        );

        goalField.setItems(NutritionGoal.values());
        caloriesField.setMin(1);
        proteinField.setMin(0);
        carbohydratesField.setMin(0);
        fatField.setMin(0);
        observationsField.setMaxLength(500);
        setColspan(observationsField, 2);

        binder.forField(athleteIdField).asRequired("Athlete id is required.").bind("athleteId");
        binder.forField(goalField).asRequired("Goal is required.").bind("goal");
        binder.forField(caloriesField).asRequired("Calories are required.").bind("calories");
        binder.forField(proteinField).asRequired("Protein is required.").bind("protein");
        binder.forField(carbohydratesField).asRequired("Carbohydrates are required.").bind("carbohydrates");
        binder.forField(fatField).asRequired("Fat is required.").bind("fat");
        binder.forField(startDateField).asRequired("Start date is required.").bind("startDate");
        binder.forField(endDateField).asRequired("End date is required.").bind("endDate");
        binder.forField(observationsField).bind("observations");
        binder.forField(activeField).bind("active");

        add(athleteIdField, goalField, caloriesField, proteinField, carbohydratesField, fatField,
                startDateField, endDateField, activeField, observationsField);
    }

    public void setNutritionistId(String nutritionistId) {
        this.nutritionistId = nutritionistId;
    }

    public void setNewPlan() {
        workingCopy = NutritionPlanRequestDTO.builder().active(true).build();
        binder.setBean(workingCopy);
    }

    public void setPlan(NutritionPlanDetailDTO detail) {
        workingCopy = NutritionPlanRequestDTO.builder()
                .athleteId(detail.getAthleteId())
                .goal(detail.getGoal())
                .calories(detail.getCalories())
                .protein(detail.getProtein())
                .carbohydrates(detail.getCarbohydrates())
                .fat(detail.getFat())
                .observations(detail.getObservations())
                .startDate(detail.getStartDate())
                .endDate(detail.getEndDate())
                .active(detail.getActive())
                .build();
        binder.setBean(workingCopy);
    }

    public boolean isValid() {
        return binder.isValid();
    }

    public NutritionPlanRequestDTO getValue() {
        workingCopy.setNutritionistId(nutritionistId);
        return workingCopy;
    }
}
