package com.gymtracker.view.workout;

import com.gymtracker.dto.workout.WorkoutExerciseDTO;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Holds the athlete's in-progress workout for the duration of the browser
 * session. The data model has no persisted "in progress" state (sessions are
 * only ever written once, completed, per {@code 03_DATABASE.md}), so start /
 * resume / cancel are modeled here as transient UI state; only "Finish"
 * results in a real call to WorkoutSessionService. This holds no business
 * logic - it is a plain state container for the views.
 */
@VaadinSessionScope
@Component
public class ActiveWorkoutDraft {

    private boolean active;
    private String athleteId;
    private String mesocycleId;
    private LocalDate date;
    private LocalDateTime startedAt;
    private final List<WorkoutExerciseDTO> exercises = new ArrayList<>();

    public void start(String athleteId, String mesocycleId) {
        this.active = true;
        this.athleteId = athleteId;
        this.mesocycleId = mesocycleId;
        this.date = LocalDate.now();
        this.startedAt = LocalDateTime.now();
        this.exercises.clear();
    }

    public void cancel() {
        this.active = false;
        this.mesocycleId = null;
        this.exercises.clear();
    }

    public void finish() {
        this.active = false;
        this.mesocycleId = null;
        this.exercises.clear();
    }

    public boolean isActive() {
        return active;
    }

    public String getAthleteId() {
        return athleteId;
    }

    public String getMesocycleId() {
        return mesocycleId;
    }

    public void setMesocycleId(String mesocycleId) {
        this.mesocycleId = mesocycleId;
    }

    public LocalDate getDate() {
        return date;
    }

    public List<WorkoutExerciseDTO> getExercises() {
        return exercises;
    }

    public int getElapsedMinutes() {
        return startedAt != null ? (int) Duration.between(startedAt, LocalDateTime.now()).toMinutes() : 0;
    }
}
