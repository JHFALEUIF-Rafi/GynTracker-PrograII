package com.gymtracker.entity;

import com.gymtracker.enums.NutritionGoal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document representing athlete nutrition plans.
 * <p>
 * Compound indexes match the actual query shape: finding an athlete's active
 * plan (only one may be active at a time per business rule).
 */
@Document(collection = "nutritionPlans")
@CompoundIndexes({
        @CompoundIndex(name = "athlete_active_idx", def = "{ 'athleteId': 1, 'active': 1 }"),
        @CompoundIndex(name = "nutritionist_active_idx", def = "{ 'nutritionistId': 1, 'active': 1 }")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class NutritionPlan {

    @Id
    private ObjectId id;

    @NotNull
    @Indexed
    private ObjectId athleteId;

    @NotNull
    @Indexed
    private ObjectId nutritionistId;

    @NotNull
    private NutritionGoal goal;

    @NotNull
    @Positive
    private Integer calories;

    @NotNull
    @PositiveOrZero
    private Double protein;

    @NotNull
    @PositiveOrZero
    private Double carbohydrates;

    @NotNull
    @PositiveOrZero
    private Double fat;

    @Size(max = 500)
    private String observations;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    @Indexed
    private Boolean active;

    @NotNull
    private LocalDateTime createdAt;
}
