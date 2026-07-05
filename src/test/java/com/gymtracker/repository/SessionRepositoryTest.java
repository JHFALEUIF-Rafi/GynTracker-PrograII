package com.gymtracker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymtracker.entity.Session;
import java.time.LocalDate;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

/**
 * Repository test verifying Session persistence and derived query methods
 * against a real MongoDB instance (test database).
 */
@DataMongoTest
class SessionRepositoryTest {

    @Autowired
    private SessionRepository sessionRepository;

    @AfterEach
    void cleanUp() {
        sessionRepository.deleteAll();
    }

    private Session buildSession(ObjectId athleteId, ObjectId mesocycleId, LocalDate date) {
        Session.CompletedSet set = Session.CompletedSet.builder()
                .weight(60.0)
                .repetitions(8)
                .rpe(7)
                .build();
        Session.CompletedExercise exercise = Session.CompletedExercise.builder()
                .exerciseId(new ObjectId())
                .sets(List.of(set))
                .build();

        return Session.builder()
                .athleteId(athleteId)
                .mesocycleId(mesocycleId)
                .date(date)
                .durationMinutes(60)
                .completedExercises(List.of(exercise))
                .totalVolume(480.0)
                .estimatedOneRepMax(75.0)
                .build();
    }

    @Test
    void findsSessionsByAthleteId() {
        ObjectId athleteId = new ObjectId();
        sessionRepository.save(buildSession(athleteId, new ObjectId(), LocalDate.now()));
        sessionRepository.save(buildSession(new ObjectId(), new ObjectId(), LocalDate.now()));

        List<Session> results = sessionRepository.findByAthleteId(athleteId);

        assertThat(results).hasSize(1);
    }

    @Test
    void findsSessionsByDateBetween() {
        LocalDate today = LocalDate.now();
        sessionRepository.save(buildSession(new ObjectId(), new ObjectId(), today.minusDays(2)));
        sessionRepository.save(buildSession(new ObjectId(), new ObjectId(), today.minusDays(20)));

        List<Session> results = sessionRepository.findByDateBetween(today.minusDays(5), today);

        assertThat(results).hasSize(1);
    }

    @Test
    void findsSessionsByMesocycleId() {
        ObjectId mesocycleId = new ObjectId();
        sessionRepository.save(buildSession(new ObjectId(), mesocycleId, LocalDate.now()));
        sessionRepository.save(buildSession(new ObjectId(), new ObjectId(), LocalDate.now()));

        List<Session> results = sessionRepository.findByMesocycleId(mesocycleId);

        assertThat(results).hasSize(1);
    }
}
