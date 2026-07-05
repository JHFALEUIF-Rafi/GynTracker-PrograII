package com.gymtracker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymtracker.entity.Mesocycle;
import com.gymtracker.enums.MesocycleStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

/**
 * Repository test verifying Mesocycle persistence and derived query methods
 * against a real MongoDB instance (test database).
 */
@DataMongoTest
class MesocycleRepositoryTest {

    @Autowired
    private MesocycleRepository mesocycleRepository;

    private ObjectId coachId;
    private ObjectId athleteId;

    @AfterEach
    void cleanUp() {
        mesocycleRepository.deleteAll();
    }

    private Mesocycle buildMesocycle(ObjectId coachId, ObjectId athleteId, String name, MesocycleStatus status) {
        Mesocycle.WorkoutExercise exercise = Mesocycle.WorkoutExercise.builder()
                .exerciseId(new ObjectId())
                .sets(3)
                .repetitions(10)
                .targetWeight(50.0)
                .targetRPE(7)
                .build();
        Mesocycle.WorkoutDay day = Mesocycle.WorkoutDay.builder()
                .dayName("Day 1")
                .exercises(List.of(exercise))
                .build();

        return Mesocycle.builder()
                .coachId(coachId)
                .athleteId(athleteId)
                .name(name)
                .durationWeeks(8)
                .targetRPE(7)
                .status(status)
                .createdAt(LocalDateTime.now())
                .days(List.of(day))
                .build();
    }

    @Test
    void findsMesocyclesByAthleteId() {
        coachId = new ObjectId();
        athleteId = new ObjectId();
        mesocycleRepository.save(buildMesocycle(coachId, athleteId, "Hypertrophy Block", MesocycleStatus.ACTIVE));
        mesocycleRepository.save(buildMesocycle(coachId, new ObjectId(), "Other Athlete Block", MesocycleStatus.DRAFT));

        List<Mesocycle> results = mesocycleRepository.findByAthleteId(athleteId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Hypertrophy Block");
    }

    @Test
    void findsMesocyclesByCoachId() {
        coachId = new ObjectId();
        mesocycleRepository.save(buildMesocycle(coachId, new ObjectId(), "Block A", MesocycleStatus.ACTIVE));
        mesocycleRepository.save(buildMesocycle(coachId, new ObjectId(), "Block B", MesocycleStatus.DRAFT));
        mesocycleRepository.save(buildMesocycle(new ObjectId(), new ObjectId(), "Other Coach Block", MesocycleStatus.ACTIVE));

        List<Mesocycle> results = mesocycleRepository.findByCoachId(coachId);

        assertThat(results).hasSize(2);
    }

    @Test
    void findsMesocyclesByStatus() {
        mesocycleRepository.save(buildMesocycle(new ObjectId(), new ObjectId(), "Archived Block", MesocycleStatus.ARCHIVED));
        mesocycleRepository.save(buildMesocycle(new ObjectId(), new ObjectId(), "Active Block", MesocycleStatus.ACTIVE));

        List<Mesocycle> archived = mesocycleRepository.findByStatus(MesocycleStatus.ARCHIVED);

        assertThat(archived).hasSize(1);
        assertThat(archived.get(0).getName()).isEqualTo("Archived Block");
    }
}
