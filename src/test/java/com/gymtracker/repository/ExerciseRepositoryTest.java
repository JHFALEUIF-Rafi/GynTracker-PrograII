package com.gymtracker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymtracker.entity.Exercise;
import com.gymtracker.enums.Difficulty;
import com.gymtracker.enums.Equipment;
import com.gymtracker.enums.ExerciseStatus;
import com.gymtracker.enums.ExerciseType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

/**
 * Repository test verifying Exercise persistence and derived query methods
 * against a real MongoDB instance (test database).
 */
@DataMongoTest
class ExerciseRepositoryTest {

    @Autowired
    private ExerciseRepository exerciseRepository;

    @AfterEach
    void cleanUp() {
        exerciseRepository.deleteAll();
    }

    private Exercise buildExercise(String name, ExerciseType type, Difficulty difficulty,
                                    Equipment equipment, ExerciseStatus status) {
        return Exercise.builder()
                .name(name)
                .primaryMuscle("Chest")
                .secondaryMuscles(List.of("Triceps"))
                .exerciseType(type)
                .difficulty(difficulty)
                .equipment(equipment)
                .status(status)
                .description("Description")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void findsExerciseByName() {
        exerciseRepository.save(buildExercise("Bench Press", ExerciseType.STRENGTH, Difficulty.INTERMEDIATE,
                Equipment.BARBELL, ExerciseStatus.ACTIVE));

        var found = exerciseRepository.findByName("Bench Press");

        assertThat(found).isPresent();
        assertThat(found.get().getEquipment()).isEqualTo(Equipment.BARBELL);
    }

    @Test
    void existsByNameReflectsPersistedState() {
        exerciseRepository.save(buildExercise("Squat", ExerciseType.STRENGTH, Difficulty.ADVANCED,
                Equipment.BARBELL, ExerciseStatus.ACTIVE));

        assertThat(exerciseRepository.existsByName("Squat")).isTrue();
        assertThat(exerciseRepository.existsByName("Nonexistent")).isFalse();
    }

    @Test
    void findsExercisesByStatus() {
        exerciseRepository.save(buildExercise("Active One", ExerciseType.STRENGTH, Difficulty.BEGINNER,
                Equipment.DUMBBELL, ExerciseStatus.ACTIVE));
        exerciseRepository.save(buildExercise("Inactive One", ExerciseType.STRENGTH, Difficulty.BEGINNER,
                Equipment.DUMBBELL, ExerciseStatus.INACTIVE));

        List<Exercise> active = exerciseRepository.findByStatus(ExerciseStatus.ACTIVE);

        assertThat(active).hasSize(1);
        assertThat(active.get(0).getName()).isEqualTo("Active One");
    }

    @Test
    void findsExercisesByType() {
        exerciseRepository.save(buildExercise("Run", ExerciseType.CARDIO, Difficulty.BEGINNER,
                Equipment.BODYWEIGHT, ExerciseStatus.ACTIVE));
        exerciseRepository.save(buildExercise("Deadlift", ExerciseType.STRENGTH, Difficulty.ADVANCED,
                Equipment.BARBELL, ExerciseStatus.ACTIVE));

        List<Exercise> cardio = exerciseRepository.findByExerciseType(ExerciseType.CARDIO);

        assertThat(cardio).hasSize(1);
        assertThat(cardio.get(0).getName()).isEqualTo("Run");
    }

    @Test
    void findsExercisesByDifficulty() {
        exerciseRepository.save(buildExercise("Beginner Exercise", ExerciseType.MOBILITY, Difficulty.BEGINNER,
                Equipment.BODYWEIGHT, ExerciseStatus.ACTIVE));

        List<Exercise> beginnerExercises = exerciseRepository.findByDifficulty(Difficulty.BEGINNER);

        assertThat(beginnerExercises).hasSize(1);
    }

    @Test
    void findsExercisesByEquipment() {
        exerciseRepository.save(buildExercise("Kettlebell Swing", ExerciseType.STRENGTH, Difficulty.INTERMEDIATE,
                Equipment.KETTLEBELL, ExerciseStatus.ACTIVE));

        List<Exercise> kettlebellExercises = exerciseRepository.findByEquipment(Equipment.KETTLEBELL);

        assertThat(kettlebellExercises).hasSize(1);
    }
}
