package com.gymtracker.dto.athlete;

import com.gymtracker.enums.ActivityLevel;
import com.gymtracker.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for creating or updating an athlete profile.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AthleteRequestDTO {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @NotNull
    @Min(14)
    @Max(100)
    private Integer age;

    @NotNull
    private Gender gender;

    @NotNull
    @Positive
    private Double weight;

    @NotNull
    @Positive
    private Double height;

    @NotNull
    private ActivityLevel activityLevel;
}
