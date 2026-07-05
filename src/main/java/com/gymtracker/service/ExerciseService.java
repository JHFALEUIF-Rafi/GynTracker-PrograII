package com.gymtracker.service;

import com.gymtracker.dto.exercise.ExerciseDetailDTO;
import com.gymtracker.dto.exercise.ExerciseRequestDTO;
import com.gymtracker.dto.exercise.ExerciseResponseDTO;
import com.gymtracker.dto.exercise.ExerciseSummaryDTO;
import com.gymtracker.enums.Difficulty;
import com.gymtracker.enums.Equipment;
import com.gymtracker.enums.ExerciseStatus;
import com.gymtracker.enums.ExerciseType;
import java.util.List;

/**
 * Service contract for exercise catalog management operations.
 */
public interface ExerciseService {

    ExerciseResponseDTO createExercise(ExerciseRequestDTO requestDTO);

    ExerciseResponseDTO updateExercise(String exerciseId, ExerciseRequestDTO requestDTO);

    ExerciseResponseDTO deactivateExercise(String exerciseId);

    ExerciseDetailDTO getExerciseById(String exerciseId);

    List<ExerciseSummaryDTO> getAllExercises();

    List<ExerciseSummaryDTO> searchExercises(String keyword);

    List<ExerciseSummaryDTO> filterExercises(
            ExerciseType type,
            Difficulty difficulty,
            Equipment equipment,
            ExerciseStatus status
    );

    boolean existsByName(String name);
}
