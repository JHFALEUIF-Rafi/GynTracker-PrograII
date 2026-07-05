package com.gymtracker.repository;

import com.gymtracker.entity.Session;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository for workout session persistence operations.
 */
public interface SessionRepository extends MongoRepository<Session, String> {

    List<Session> findByAthleteId(ObjectId athleteId);

    List<Session> findByAthleteIdIn(Collection<ObjectId> athleteIds);

    List<Session> findByDateBetween(LocalDate startDate, LocalDate endDate);

    List<Session> findByMesocycleId(ObjectId mesocycleId);
}
