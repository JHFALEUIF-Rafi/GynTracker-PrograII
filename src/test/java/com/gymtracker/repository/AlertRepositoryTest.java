package com.gymtracker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymtracker.entity.Alert;
import com.gymtracker.enums.AlertStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

/**
 * Repository test verifying Alert persistence and derived query methods
 * against a real MongoDB instance (test database).
 */
@DataMongoTest
class AlertRepositoryTest {

    @Autowired
    private AlertRepository alertRepository;

    @AfterEach
    void cleanUp() {
        alertRepository.deleteAll();
    }

    private Alert buildAlert(ObjectId athleteId, ObjectId coachId, String type, AlertStatus status) {
        return Alert.builder()
                .athleteId(athleteId)
                .coachId(coachId)
                .type(type)
                .message("Alert message")
                .status(status)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void findsAlertsByAthleteId() {
        ObjectId athleteId = new ObjectId();
        alertRepository.save(buildAlert(athleteId, new ObjectId(), "HIGH_FATIGUE", AlertStatus.ACTIVE));
        alertRepository.save(buildAlert(new ObjectId(), new ObjectId(), "HIGH_FATIGUE", AlertStatus.ACTIVE));

        List<Alert> results = alertRepository.findByAthleteId(athleteId);

        assertThat(results).hasSize(1);
    }

    @Test
    void findsAlertsByStatus() {
        alertRepository.save(buildAlert(new ObjectId(), new ObjectId(), "MISSED_WORKOUT", AlertStatus.ACTIVE));
        alertRepository.save(buildAlert(new ObjectId(), new ObjectId(), "MISSED_WORKOUT", AlertStatus.RESOLVED));

        List<Alert> active = alertRepository.findByStatus(AlertStatus.ACTIVE);

        assertThat(active).hasSize(1);
    }

    @Test
    void findsAlertsByCoachId() {
        ObjectId coachId = new ObjectId();
        alertRepository.save(buildAlert(new ObjectId(), coachId, "PERFORMANCE_DROP", AlertStatus.ACTIVE));
        alertRepository.save(buildAlert(new ObjectId(), new ObjectId(), "PERFORMANCE_DROP", AlertStatus.ACTIVE));

        List<Alert> results = alertRepository.findByCoachId(coachId);

        assertThat(results).hasSize(1);
    }

    @Test
    void findsAlertsByAthleteIdAndStatus() {
        ObjectId athleteId = new ObjectId();
        alertRepository.save(buildAlert(athleteId, new ObjectId(), "CRITICAL_FATIGUE", AlertStatus.ACTIVE));
        alertRepository.save(buildAlert(athleteId, new ObjectId(), "CRITICAL_FATIGUE", AlertStatus.RESOLVED));

        List<Alert> results = alertRepository.findByAthleteIdAndStatus(athleteId, AlertStatus.ACTIVE);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(AlertStatus.ACTIVE);
    }

    @Test
    void findsAlertsByAthleteIdAndTypeAndStatus() {
        ObjectId athleteId = new ObjectId();
        alertRepository.save(buildAlert(athleteId, new ObjectId(), "NUTRITION_PLAN_EXPIRED", AlertStatus.ACTIVE));
        alertRepository.save(buildAlert(athleteId, new ObjectId(), "MISSED_WORKOUT", AlertStatus.ACTIVE));

        List<Alert> results = alertRepository.findByAthleteIdAndTypeAndStatus(
                athleteId, "NUTRITION_PLAN_EXPIRED", AlertStatus.ACTIVE);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getType()).isEqualTo("NUTRITION_PLAN_EXPIRED");
    }
}
