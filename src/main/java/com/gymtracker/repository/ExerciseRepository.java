package com.gymtracker.repository;

import com.gymtracker.entity.Exercise;
import com.gymtracker.enums.Difficulty;
import com.gymtracker.enums.Equipment;
import com.gymtracker.enums.ExerciseStatus;
import com.gymtracker.enums.ExerciseType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository for exercise persistence operations.
 */
public interface ExerciseRepository extends MongoRepository<Exercise, String> {

    Optional<Exercise> findByName(String name);

    boolean existsByName(String name);

    List<Exercise> findByStatus(ExerciseStatus status);

    List<Exercise> findByExerciseType(ExerciseType exerciseType);

    List<Exercise> findByDifficulty(Difficulty difficulty);

    List<Exercise> findByEquipment(Equipment equipment);
}
