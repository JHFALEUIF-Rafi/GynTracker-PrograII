package com.gymtracker.repository;

import com.gymtracker.entity.Mesocycle;
import com.gymtracker.enums.MesocycleStatus;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository for mesocycle persistence operations.
 */
public interface MesocycleRepository extends MongoRepository<Mesocycle, String> {

    List<Mesocycle> findByAthleteId(ObjectId athleteId);

    List<Mesocycle> findByCoachId(ObjectId coachId);

    List<Mesocycle> findByStatus(MesocycleStatus status);
}
