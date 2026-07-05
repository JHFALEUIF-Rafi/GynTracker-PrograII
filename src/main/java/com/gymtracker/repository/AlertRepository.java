package com.gymtracker.repository;

import com.gymtracker.entity.Alert;
import com.gymtracker.enums.AlertStatus;
import java.util.Collection;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository for alert persistence operations.
 */
public interface AlertRepository extends MongoRepository<Alert, String> {

    List<Alert> findByAthleteId(ObjectId athleteId);

    List<Alert> findByStatus(AlertStatus status);

    List<Alert> findByCoachId(ObjectId coachId);

    List<Alert> findByAthleteIdAndStatus(ObjectId athleteId, AlertStatus status);

    List<Alert> findByAthleteIdAndTypeAndStatus(ObjectId athleteId, String type, AlertStatus status);

    List<Alert> findByTypeIn(Collection<String> types);
}
