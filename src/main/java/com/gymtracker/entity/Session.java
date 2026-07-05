package com.gymtracker.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
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
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document representing one completed workout session.
 * <p>
 * The compound index matches the common query shape of an athlete's
 * sessions filtered/sorted by date (history views, weekly/monthly volume
 * aggregation).
 */
@Document(collection = "sessions")
@CompoundIndex(name = "athlete_date_idx", def = "{ 'athleteId': 1, 'date': 1 }")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Session {

    @Id
    private ObjectId id;

    @NotNull
    @Indexed
    private ObjectId athleteId;

    @NotNull
    @Indexed
    private ObjectId mesocycleId;

    @NotNull
    @Indexed
    private LocalDate date;

    @NotNull
    @Positive
    private Integer durationMinutes;

    @NotEmpty
    @Valid
    private List<CompletedExercise> completedExercises;

    @NotNull
    @Positive
    private Double totalVolume;

    @NotNull
    @Positive
    private Double estimatedOneRepMax;

    /**
     * Embedded document representing one completed exercise during a session.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompletedExercise {

        @NotNull
        private ObjectId exerciseId;

        @NotEmpty
        @Valid
        private List<CompletedSet> sets;
    }

    /**
     * Embedded document representing one completed set.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompletedSet {

        @NotNull
        @Positive
        private Double weight;

        @NotNull
        @Positive
        private Integer repetitions;

        @NotNull
        @Min(1)
        @Max(10)
        private Integer rpe;
    }
}
