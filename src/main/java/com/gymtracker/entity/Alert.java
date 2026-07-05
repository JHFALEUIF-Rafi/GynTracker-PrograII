package com.gymtracker.entity;

import com.gymtracker.enums.AlertStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
 * MongoDB document representing fatigue alerts sent to coaches.
 * <p>
 * Compound indexes match the actual query shapes used by AlertServiceImpl:
 * duplicate-active-alert checks filter by athlete + type + status, and
 * athlete/coach alert listings are commonly narrowed by status as well.
 */
@Document(collection = "alerts")
@CompoundIndexes({
        @CompoundIndex(name = "athlete_status_idx", def = "{ 'athleteId': 1, 'status': 1 }"),
        @CompoundIndex(name = "athlete_type_status_idx", def = "{ 'athleteId': 1, 'type': 1, 'status': 1 }"),
        @CompoundIndex(name = "coach_status_idx", def = "{ 'coachId': 1, 'status': 1 }")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Alert {

    @Id
    private ObjectId id;

    @NotNull
    @Indexed
    private ObjectId athleteId;

    @NotNull
    @Indexed
    private ObjectId coachId;

    @NotBlank
    private String type;

    @NotBlank
    private String message;

    @NotNull
    @Indexed
    private AlertStatus status;

    @NotNull
    private LocalDateTime generatedAt;

    private LocalDateTime reviewedAt;
}
