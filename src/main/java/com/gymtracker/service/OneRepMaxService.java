package com.gymtracker.service;

import com.gymtracker.dto.workout.OneRepMaxComparisonDTO;
import com.gymtracker.dto.workout.OneRepMaxDTO;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service contract for one-rep-max estimations.
 */
public interface OneRepMaxService {

    CompletableFuture<List<OneRepMaxDTO>> calculateOneRepMax(String athleteId);

    OneRepMaxDTO calculateExerciseOneRepMax(String athleteId, String exerciseId);

    OneRepMaxDTO getLatestOneRepMax(String athleteId, String exerciseId);

    List<OneRepMaxDTO> getOneRepMaxHistory(String athleteId, String exerciseId);

    OneRepMaxComparisonDTO compareOneRepMax(String athleteId, String exerciseId);

    Double estimateOneRepMax(Double weight, Integer repetitions);

    Double getCurrentEstimatedOneRepMax(String athleteId);
}
