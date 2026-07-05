package com.gymtracker.entity;

import com.gymtracker.enums.MesocycleStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;
import java.util.List;
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
 * MongoDB document representing a coach-designed training mesocycle.
 * <p>
 * Compound indexes match the actual query shapes: finding an athlete's
 * active mesocycle, and finding a coach's active mesocycles.
 */
@Document(collection = "mesocycles")
@CompoundIndexes({
        @CompoundIndex(name = "athlete_status_idx", def = "{ 'athleteId': 1, 'status': 1 }"),
        @CompoundIndex(name = "coach_status_idx", def = "{ 'coachId': 1, 'status': 1 }")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Mesocycle {

    @Id
    private ObjectId id;

    @NotNull
    @Indexed
    private ObjectId coachId;

    @NotNull
    @Indexed
    private ObjectId athleteId;

    @NotBlank
    private String name;

    @NotNull
    @Positive
    private Integer durationWeeks;

    @NotNull
    @Min(1)
    @Max(10)
    private Integer targetRPE;

    private String notes;

    @NotNull
    @Indexed
    private MesocycleStatus status;

    @NotNull
    private LocalDateTime createdAt;

    @NotEmpty
    @Valid
    private List<WorkoutDay> days;

    /**
     * Embedded document representing one planned training day.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkoutDay {

        @NotBlank
        private String dayName;

        @NotEmpty
        @Valid
        private List<WorkoutExercise> exercises;
    }

    /**
     * Embedded document representing one planned exercise inside a training day.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkoutExercise {

        @NotNull
        private ObjectId exerciseId;

        @NotNull
        @Positive
        private Integer sets;

        @NotNull
        @Positive
        private Integer repetitions;

        @NotNull
        @PositiveOrZero
        private Double targetWeight;

        @NotNull
        @Min(1)
        @Max(10)
        private Integer targetRPE;
    }
}
