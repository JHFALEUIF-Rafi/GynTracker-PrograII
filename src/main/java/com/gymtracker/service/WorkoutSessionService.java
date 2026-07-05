package com.gymtracker.service;

import com.gymtracker.dto.workout.WorkoutSessionDetailDTO;
import com.gymtracker.dto.workout.WorkoutSessionRequestDTO;
import com.gymtracker.dto.workout.WorkoutSessionResponseDTO;
import com.gymtracker.dto.workout.WorkoutSessionSummaryDTO;
import java.time.LocalDate;
import java.util.List;

/**
 * Service contract for workout session registration and history operations.
 */
public interface WorkoutSessionService {

    WorkoutSessionResponseDTO createWorkoutSession(WorkoutSessionRequestDTO requestDTO);

    WorkoutSessionDetailDTO getWorkoutSessionById(String sessionId);

    List<WorkoutSessionSummaryDTO> getWorkoutSessionsByAthlete(String athleteId);

    List<WorkoutSessionSummaryDTO> getWorkoutSessionsByMesocycle(String mesocycleId);

    List<WorkoutSessionSummaryDTO> getWorkoutSessionsByDateRange(LocalDate startDate, LocalDate endDate);
}
