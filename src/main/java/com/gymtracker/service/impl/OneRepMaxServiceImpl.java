package com.gymtracker.service.impl;

import com.gymtracker.dto.workout.OneRepMaxComparisonDTO;
import com.gymtracker.dto.workout.OneRepMaxDTO;
import com.gymtracker.entity.Exercise;
import com.gymtracker.entity.Session;
import com.gymtracker.entity.User;
import com.gymtracker.enums.OneRepMaxFormula;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.BusinessRuleException;
import com.gymtracker.exception.ResourceNotFoundException;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.repository.ExerciseRepository;
import com.gymtracker.repository.SessionRepository;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.service.OneRepMaxService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Business implementation for one-rep-max estimation based on workout history.
 */
@Service
public class OneRepMaxServiceImpl implements OneRepMaxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OneRepMaxServiceImpl.class);
    private static final OneRepMaxFormula ACTIVE_FORMULA = OneRepMaxFormula.EPLEY;

    private final SessionRepository workoutSessionRepository;
    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;

    public OneRepMaxServiceImpl(
            SessionRepository workoutSessionRepository,
            ExerciseRepository exerciseRepository,
            UserRepository userRepository
    ) {
        this.workoutSessionRepository = workoutSessionRepository;
        this.exerciseRepository = exerciseRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Async
    public CompletableFuture<List<OneRepMaxDTO>> calculateOneRepMax(String athleteId) {
        try {
            User athlete = getAthleteById(athleteId);
            List<Session> completedSessions = getCompletedSessions(athlete.getId());
            Map<ObjectId, OneRepMaxDTO> bestByExercise = new HashMap<>();

            for (Session session : completedSessions) {
                processSessionForBestValues(athlete, session, bestByExercise);
            }

            List<OneRepMaxDTO> result = bestByExercise.values().stream()
                    .sorted(Comparator.comparing(OneRepMaxDTO::getEstimatedOneRepMax).reversed())
                    .toList();
            LOGGER.info("1RM calculated for athleteId={} using formula={} with exercises={}",
                    athleteId, ACTIVE_FORMULA, result.size());
            return CompletableFuture.completedFuture(result);
        } catch (RuntimeException exception) {
            LOGGER.warn("1RM calculation failure for athleteId={} using formula={}: {}",
                    athleteId, ACTIVE_FORMULA, exception.getMessage());
            return CompletableFuture.failedFuture(exception);
        }
    }

    @Override
    public OneRepMaxDTO calculateExerciseOneRepMax(String athleteId, String exerciseId) {
        List<OneRepMaxDTO> history = getOneRepMaxHistory(athleteId, exerciseId);
        return history.stream()
                .max(Comparator.comparing(OneRepMaxDTO::getEstimatedOneRepMax))
                .orElseThrow(() -> new BusinessRuleException("No valid one-rep-max data found for this exercise."));
    }

    @Override
    public OneRepMaxDTO getLatestOneRepMax(String athleteId, String exerciseId) {
        List<OneRepMaxDTO> history = getOneRepMaxHistory(athleteId, exerciseId);
        return history.stream()
                .max(Comparator.comparing(OneRepMaxDTO::getDate))
                .orElseThrow(() -> new BusinessRuleException("No valid one-rep-max history found for this exercise."));
    }

    @Override
    public List<OneRepMaxDTO> getOneRepMaxHistory(String athleteId, String exerciseId) {
        User athlete = getAthleteById(athleteId);
        Exercise exercise = getExerciseById(exerciseId);

        List<OneRepMaxDTO> history = new ArrayList<>();
        List<Session> completedSessions = getCompletedSessions(athlete.getId());
        for (Session session : completedSessions) {
            appendSessionHistory(athlete, exercise, session, history);
        }

        return history.stream()
                .sorted(Comparator.comparing(OneRepMaxDTO::getDate))
                .toList();
    }

    @Override
    public OneRepMaxComparisonDTO compareOneRepMax(String athleteId, String exerciseId) {
        List<OneRepMaxDTO> history = getOneRepMaxHistory(athleteId, exerciseId);
        if (history.size() < 2) {
            throw new BusinessRuleException("At least two one-rep-max records are required for comparison.");
        }

        OneRepMaxDTO latest = history.get(history.size() - 1);
        OneRepMaxDTO previous = history.get(history.size() - 2);
        double absoluteChange = latest.getEstimatedOneRepMax() - previous.getEstimatedOneRepMax();
        double percentageChange = previous.getEstimatedOneRepMax() == 0
                ? 0
                : (absoluteChange / previous.getEstimatedOneRepMax()) * 100.0d;

        return OneRepMaxComparisonDTO.builder()
                .athleteId(athleteId)
                .exerciseId(exerciseId)
                .latestValue(latest.getEstimatedOneRepMax())
                .previousValue(previous.getEstimatedOneRepMax())
                .absoluteChange(absoluteChange)
                .percentageChange(percentageChange)
                .trend(resolveTrend(absoluteChange))
                .build();
    }

    @Override
    public Double estimateOneRepMax(Double weight, Integer repetitions) {
        validateSetData(weight, repetitions);
        return applyFormula(weight, repetitions);
    }

    @Override
    public Double getCurrentEstimatedOneRepMax(String athleteId) {
        User athlete = getAthleteById(athleteId);
        List<Session> completedSessions = getCompletedSessions(athlete.getId());

        return completedSessions.stream()
                .flatMap(session -> session.getCompletedExercises().stream())
                .flatMap(exercise -> exercise.getSets().stream())
                .map(this::estimateValidSetOrNull)
                .filter(Objects::nonNull)
                .max(Double::compareTo)
                .orElseThrow(() -> new BusinessRuleException("No valid one-rep-max data found for athlete."));
    }

    private void processSessionForBestValues(User athlete, Session session, Map<ObjectId, OneRepMaxDTO> bestByExercise) {
        for (Session.CompletedExercise completedExercise : session.getCompletedExercises()) {
            Exercise exercise = exerciseRepository.findById(completedExercise.getExerciseId().toHexString())
                    .orElse(null);
            if (exercise == null) {
                continue;
            }

            Double bestSessionValue = completedExercise.getSets().stream()
                    .map(this::estimateValidSetOrNull)
                    .filter(Objects::nonNull)
                    .max(Double::compareTo)
                    .orElse(null);

            if (bestSessionValue == null) {
                continue;
            }

            OneRepMaxDTO candidate = buildOneRepMaxDto(athlete, exercise, session, bestSessionValue);
            OneRepMaxDTO currentBest = bestByExercise.get(completedExercise.getExerciseId());
            if (currentBest == null || candidate.getEstimatedOneRepMax() > currentBest.getEstimatedOneRepMax()) {
                bestByExercise.put(completedExercise.getExerciseId(), candidate);
            }
        }
    }

    private void appendSessionHistory(User athlete, Exercise exercise, Session session, List<OneRepMaxDTO> history) {
        Session.CompletedExercise matchedExercise = session.getCompletedExercises().stream()
                .filter(completedExercise -> Objects.equals(completedExercise.getExerciseId(), exercise.getId()))
                .findFirst()
                .orElse(null);
        if (matchedExercise == null) {
            return;
        }

        Double bestSessionValue = matchedExercise.getSets().stream()
                .map(this::estimateValidSetOrNull)
                .filter(Objects::nonNull)
                .max(Double::compareTo)
                .orElse(null);

        if (bestSessionValue == null) {
            return;
        }

        history.add(buildOneRepMaxDto(athlete, exercise, session, bestSessionValue));
    }

    private OneRepMaxDTO buildOneRepMaxDto(User athlete, Exercise exercise, Session session, Double value) {
        return OneRepMaxDTO.builder()
                .athleteId(athlete.getId().toHexString())
                .exerciseId(exercise.getId().toHexString())
                .exerciseName(exercise.getName())
                .sessionId(session.getId().toHexString())
                .date(session.getDate())
                .estimatedOneRepMax(value)
                .formula(ACTIVE_FORMULA.name())
                .build();
    }

    private List<Session> getCompletedSessions(ObjectId athleteId) {
        List<Session> sessions = workoutSessionRepository.findByAthleteId(athleteId);
        return sessions.stream()
                .filter(this::isCompletedSession)
                .toList();
    }

    private boolean isCompletedSession(Session session) {
        return session.getCompletedExercises() != null && !session.getCompletedExercises().isEmpty();
    }

    private User getAthleteById(String athleteId) {
        validateIdentifier(athleteId, "Athlete id is required.");
        User athlete = userRepository.findById(athleteId)
                .orElseThrow(() -> new ResourceNotFoundException("Athlete not found with id: " + athleteId));
        if (athlete.getRole() != Role.ATHLETE) {
            throw new BusinessRuleException("Referenced user is not an athlete.");
        }
        return athlete;
    }

    private Exercise getExerciseById(String exerciseId) {
        validateIdentifier(exerciseId, "Exercise id is required.");
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise not found with id: " + exerciseId));
    }

    private void validateIdentifier(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
    }

    private void validateSetData(Double weight, Integer repetitions) {
        if (weight == null || weight <= 0) {
            throw new ValidationException("Weight must be greater than zero.");
        }
        if (repetitions == null || repetitions <= 0) {
            throw new ValidationException("Repetitions must be greater than zero.");
        }
    }

    private Double estimateValidSetOrNull(Session.CompletedSet set) {
        if (set == null || set.getWeight() == null || set.getWeight() <= 0
                || set.getRepetitions() == null || set.getRepetitions() <= 0) {
            return null;
        }
        return applyFormula(set.getWeight(), set.getRepetitions());
    }

    private Double applyFormula(Double weight, Integer repetitions) {
        return switch (ACTIVE_FORMULA) {
            case EPLEY -> weight * (1 + (repetitions / 30.0d));
            case BRZYCKI -> repetitions >= 37 ? null : weight * (36.0d / (37.0d - repetitions));
            case LOMBARDI -> weight * Math.pow(repetitions, 0.10d);
        };
    }

    private String resolveTrend(double absoluteChange) {
        if (absoluteChange > 0) {
            return "UP";
        }
        if (absoluteChange < 0) {
            return "DOWN";
        }
        return "STABLE";
    }
}
