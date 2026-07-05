package com.gymtracker.entity;

import com.gymtracker.enums.Difficulty;
import com.gymtracker.enums.Equipment;
import com.gymtracker.enums.ExerciseStatus;
import com.gymtracker.enums.ExerciseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document representing an exercise from the catalog.
 */
@Document(collection = "exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Exercise {

    @Id
    private ObjectId id;

    @NotBlank
    @Indexed(unique = true)
    private String name;

    @NotBlank
    @Indexed
    private String primaryMuscle;

    @NotEmpty
    private List<@NotBlank String> secondaryMuscles;

    @NotNull
    @Indexed
    private ExerciseType exerciseType;

    @NotNull
    @Indexed
    private Difficulty difficulty;

    @NotNull
    @Indexed
    private Equipment equipment;

    @NotNull
    @Indexed
    private ExerciseStatus status;

    @NotBlank
    private String description;

    @NotNull
    private LocalDateTime createdAt;

    @NotNull
    private LocalDateTime updatedAt;
}
