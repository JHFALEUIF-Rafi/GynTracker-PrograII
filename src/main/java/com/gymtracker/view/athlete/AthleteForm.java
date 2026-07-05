package com.gymtracker.view.athlete;

import com.gymtracker.dto.athlete.AthleteDetailDTO;
import com.gymtracker.dto.athlete.AthleteRequestDTO;
import com.gymtracker.enums.ActivityLevel;
import com.gymtracker.enums.Gender;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;

/**
 * Presentational athlete profile form. Personal data is read-only (first
 * name, last name, email, age, gender, activity level); only weight and
 * height are editable by the athlete, per business rules. Validation is
 * sourced from the Bean Validation constraints already declared on
 * {@link AthleteRequestDTO}, so no rule is duplicated here.
 */
public class AthleteForm extends FormLayout {

    private final TextField firstNameField = new TextField("First Name");
    private final TextField lastNameField = new TextField("Last Name");
    private final EmailField emailField = new EmailField("Email");
    private final IntegerField ageField = new IntegerField("Age");
    private final ComboBox<Gender> genderField = new ComboBox<>("Gender");
    private final NumberField weightField = new NumberField("Weight (kg)");
    private final NumberField heightField = new NumberField("Height (cm)");
    private final ComboBox<ActivityLevel> activityLevelField = new ComboBox<>("Activity Level");

    private final BeanValidationBinder<AthleteRequestDTO> binder = new BeanValidationBinder<>(AthleteRequestDTO.class);
    private AthleteRequestDTO workingCopy;

    public AthleteForm() {
        setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep("500px", 2)
        );

        firstNameField.setReadOnly(true);
        lastNameField.setReadOnly(true);
        emailField.setReadOnly(true);
        ageField.setReadOnly(true);

        genderField.setItems(Gender.values());
        genderField.setReadOnly(true);

        activityLevelField.setItems(ActivityLevel.values());
        activityLevelField.setReadOnly(true);

        weightField.setMin(0.1);
        heightField.setMin(0.1);

        binder.forField(weightField)
                .asRequired("Weight is required.")
                .bind("weight");
        binder.forField(heightField)
                .asRequired("Height is required.")
                .bind("height");

        add(firstNameField, lastNameField, emailField, ageField, genderField, activityLevelField, weightField, heightField);
    }

    public void setAthlete(AthleteDetailDTO detail) {
        firstNameField.setValue(nullToEmpty(detail.getFirstName()));
        lastNameField.setValue(nullToEmpty(detail.getLastName()));
        emailField.setValue(nullToEmpty(detail.getEmail()));
        if (detail.getAge() != null) {
            ageField.setValue(detail.getAge());
        }
        genderField.setValue(detail.getGender());
        activityLevelField.setValue(detail.getActivityLevel());

        workingCopy = AthleteRequestDTO.builder()
                .firstName(detail.getFirstName())
                .lastName(detail.getLastName())
                .email(detail.getEmail())
                .age(detail.getAge())
                .gender(detail.getGender())
                .weight(detail.getWeight())
                .height(detail.getHeight())
                .activityLevel(detail.getActivityLevel())
                .build();
        binder.setBean(workingCopy);
    }

    public boolean isValid() {
        return binder.isValid();
    }

    public boolean isDirty() {
        return binder.hasChanges();
    }

    public AthleteRequestDTO getValue() {
        return workingCopy;
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
