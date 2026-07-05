package com.gymtracker.service.impl;

import com.gymtracker.dto.dashboard.ChartDTO;
import com.gymtracker.dto.fatigue.FatigueDTO;
import com.gymtracker.entity.Mesocycle;
import com.gymtracker.entity.Session;
import com.gymtracker.entity.User;
import com.gymtracker.enums.FatigueLevel;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.BusinessRuleException;
import com.gymtracker.exception.ResourceNotFoundException;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.repository.MesocycleRepository;
import com.gymtracker.repository.SessionRepository;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.service.FatigueService;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Business implementation for fatigue analysis using completed workout history.
 */
@Service
public class FatigueServiceImpl implements FatigueService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FatigueServiceImpl.class);

    private final SessionRepository workoutSessionRepository;
    private final MesocycleRepository mesocycleRepository;
    private final UserRepository userRepository;

    public FatigueServiceImpl(
            SessionRepository workoutSessionRepository,
            MesocycleRepository mesocycleRepository,
            UserRepository userRepository
    ) {
        this.workoutSessionRepository = workoutSessionRepository;
        this.mesocycleRepository = mesocycleRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Async
    @CacheEvict(value = "dashboards", allEntries = true, beforeInvocation = true)
    public CompletableFuture<FatigueDTO> calculateFatigue(String athleteId) {
        try {
            User athlete = getAthleteById(athleteId);
            List<Session> completedSessions = getCompletedSessions(athlete.getId());
            if (completedSessions.isEmpty()) {
                throw new BusinessRuleException("No completed workout sessions found for fatigue evaluation.");
            }

            Session latestSession = completedSessions.stream()
                    .max(Comparator.comparing(Session::getDate))
                    .orElseThrow(() -> new BusinessRuleException("No completed workout sessions found for fatigue evaluation."));

            FatigueDTO previousSnapshot = completedSessions.size() > 1
                    ? buildFatigueSnapshot(athlete, completedSessions.get(completedSessions.size() - 2), completedSessions)
                    : null;
            FatigueDTO currentSnapshot = buildFatigueSnapshot(athlete, latestSession, completedSessions);

            if (previousSnapshot != null && previousSnapshot.getFatigueLevel() != currentSnapshot.getFatigueLevel()) {
                LOGGER.info("Fatigue level changed athleteId={} from {} to {}",
                        athleteId, previousSnapshot.getFatigueLevel(), currentSnapshot.getFatigueLevel());
            }

            LOGGER.info("Fatigue calculated athleteId={}, fatigueScore={}, level={}",
                    athleteId, currentSnapshot.getFatigueScore(), currentSnapshot.getFatigueLevel());
            return CompletableFuture.completedFuture(currentSnapshot);
        } catch (RuntimeException exception) {
            LOGGER.warn("Fatigue calculation failure athleteId={}: {}", athleteId, exception.getMessage());
            return CompletableFuture.failedFuture(exception);
        }
    }

    @Override
    public FatigueLevel getCurrentFatigueLevel(String athleteId) {
        return calculateFatigueScoreLevel(athleteId).fatigueLevel();
    }

    @Override
    public List<FatigueDTO> getFatigueHistory(String athleteId) {
        User athlete = getAthleteById(athleteId);
        List<Session> completedSessions = getCompletedSessions(athlete.getId());
        return completedSessions.stream()
                .sorted(Comparator.comparing(Session::getDate))
                .map(session -> buildFatigueSnapshot(athlete, session, completedSessions))
                .toList();
    }

    @Override
    public Double evaluateWorkoutLoad(String workoutSessionId) {
        validateIdentifier(workoutSessionId, "Workout session id is required.");
        Session session = workoutSessionRepository.findById(workoutSessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found with id: " + workoutSessionId));
        ensureCompletedSession(session);

        double averageRpe = calculateAverageRpe(session);
        double load = session.getTotalVolume() * (averageRpe / 10.0d) * durationLoadFactor(session.getDurationMinutes());
        return round(load);
    }

    @Override
    public Double calculateWeeklyTrainingLoad(String athleteId) {
        User athlete = getAthleteById(athleteId);
        List<Session> completedSessions = getCompletedSessions(athlete.getId());
        LocalDate latestDate = completedSessions.stream()
                .map(Session::getDate)
                .max(LocalDate::compareTo)
                .orElseThrow(() -> new BusinessRuleException("No completed workout sessions found."));
        LocalDate weekStart = latestDate.minusDays(6);

        double weeklyLoad = completedSessions.stream()
                .filter(session -> !session.getDate().isBefore(weekStart) && !session.getDate().isAfter(latestDate))
                .mapToDouble(this::safeTotalVolume)
                .sum();
        return round(weeklyLoad);
    }

    @Override
    public Double calculateRecoveryScore(String athleteId) {
        FatigueScoreLevel scoreLevel = calculateFatigueScoreLevel(athleteId);
        double recoveryScore = clamp(100.0d - scoreLevel.fatigueScore(), 0.0d, 100.0d);
        LOGGER.info("Recovery score calculated athleteId={}, recoveryScore={}", athleteId, recoveryScore);
        return round(recoveryScore);
    }

    @Override
    public Double calculateFatigueScore(String athleteId) {
        return calculateFatigueScoreLevel(athleteId).fatigueScore();
    }

    @Override
    public FatigueLevel calculateFatigueLevel(String athleteId) {
        return getCurrentFatigueLevel(athleteId);
    }

    @Override
    public ChartDTO getFatigueHistoryChart(String athleteId) {
        List<FatigueDTO> history = getFatigueHistory(athleteId);
        return ChartDTO.builder()
                .title("Fatigue History")
                .labels(history.stream().map(snapshot -> snapshot.getDate().toString()).toList())
                .values(history.stream().map(FatigueDTO::getFatigueScore).toList())
                .build();
    }

    private FatigueDTO buildFatigueSnapshot(User athlete, Session pivotSession, List<Session> allCompletedSessions) {
        LocalDate pivotDate = pivotSession.getDate();
        LocalDate weekStart = pivotDate.minusDays(6);

        List<Session> weeklySessions = allCompletedSessions.stream()
                .filter(session -> !session.getDate().isBefore(weekStart) && !session.getDate().isAfter(pivotDate))
                .toList();

        double weeklyLoad = weeklySessions.stream().mapToDouble(this::safeTotalVolume).sum();
        double averageDuration = weeklySessions.stream().mapToInt(this::safeDuration).average().orElse(0.0d);
        double averageRpe = weeklySessions.stream().mapToDouble(this::calculateAverageRpe).average().orElse(0.0d);
        int weeklyFrequency = weeklySessions.size();
        int consecutiveTrainingDays = calculateConsecutiveTrainingDays(weeklySessions);
        double recentOneRepMaxProgressPenalty = calculateRecentOneRepMaxProgressPenalty(weeklySessions);
        boolean athleteHasActiveMesocycle = mesocycleRepository.findByAthleteId(athlete.getId()).stream()
                .anyMatch(mesocycle -> isActiveMesocycleAtDate(mesocycle, pivotDate));

        double fatigueScore = computeFatigueScore(
                weeklyLoad,
                averageDuration,
                averageRpe,
                weeklyFrequency,
                consecutiveTrainingDays,
                recentOneRepMaxProgressPenalty,
                athleteHasActiveMesocycle
        );

        FatigueLevel fatigueLevel = classifyFatigueLevel(fatigueScore);
        double recoveryScore = clamp(100.0d - fatigueScore, 0.0d, 100.0d);

        return FatigueDTO.builder()
                .athleteId(athlete.getId().toHexString())
                .sessionId(pivotSession.getId().toHexString())
                .date(pivotDate)
                .fatigueScore(round(fatigueScore))
                .fatigueLevel(fatigueLevel)
                .recoveryScore(round(recoveryScore))
                .weeklyTrainingLoad(round(weeklyLoad))
                .build();
    }

    private FatigueScoreLevel calculateFatigueScoreLevel(String athleteId) {
        User athlete = getAthleteById(athleteId);
        List<Session> completedSessions = getCompletedSessions(athlete.getId());
        if (completedSessions.isEmpty()) {
            throw new BusinessRuleException("No completed workout sessions found for fatigue evaluation.");
        }
        Session latestSession = completedSessions.stream()
                .max(Comparator.comparing(Session::getDate))
                .orElseThrow(() -> new BusinessRuleException("No completed workout sessions found for fatigue evaluation."));

        FatigueDTO currentSnapshot = buildFatigueSnapshot(athlete, latestSession, completedSessions);
        return new FatigueScoreLevel(currentSnapshot.getFatigueScore(), currentSnapshot.getFatigueLevel());
    }

    private List<Session> getCompletedSessions(ObjectId athleteId) {
        return workoutSessionRepository.findByAthleteId(athleteId).stream()
                .filter(this::isCompletedSession)
                .sorted(Comparator.comparing(Session::getDate))
                .toList();
    }

    private boolean isCompletedSession(Session session) {
        return session != null
                && session.getCompletedExercises() != null
                && !session.getCompletedExercises().isEmpty();
    }

    private void ensureCompletedSession(Session session) {
        if (!isCompletedSession(session)) {
            throw new BusinessRuleException("Workout session is not completed.");
        }
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

    private void validateIdentifier(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
    }

    private double computeFatigueScore(
            double weeklyLoad,
            double averageDuration,
            double averageRpe,
            int weeklyFrequency,
            int consecutiveTrainingDays,
            double oneRepMaxProgressPenalty,
            boolean hasActiveMesocycle
    ) {
        double volumeScore = clamp((weeklyLoad / 10000.0d) * 100.0d, 0.0d, 100.0d);
        double durationScore = clamp((averageDuration / 120.0d) * 100.0d, 0.0d, 100.0d);
        double rpeScore = clamp(((averageRpe - 1.0d) / 9.0d) * 100.0d, 0.0d, 100.0d);
        double frequencyScore = clamp((weeklyFrequency / 7.0d) * 100.0d, 0.0d, 100.0d);
        double consecutiveScore = clamp((consecutiveTrainingDays / 7.0d) * 100.0d, 0.0d, 100.0d);
        double mesocycleModifier = hasActiveMesocycle ? 1.0d : 0.9d;

        double fatigueScore = (
                (volumeScore * 0.25d)
                        + (durationScore * 0.15d)
                        + (rpeScore * 0.25d)
                        + (frequencyScore * 0.15d)
                        + (consecutiveScore * 0.10d)
                        + (oneRepMaxProgressPenalty * 0.10d)
        ) * mesocycleModifier;
        return clamp(fatigueScore, 0.0d, 100.0d);
    }

    private double calculateAverageRpe(Session session) {
        return session.getCompletedExercises().stream()
                .flatMap(exercise -> exercise.getSets().stream())
                .mapToDouble(set -> set.getRpe() != null ? set.getRpe() : 0.0d)
                .average()
                .orElse(0.0d);
    }

    private int calculateConsecutiveTrainingDays(List<Session> sessions) {
        if (sessions.isEmpty()) {
            return 0;
        }
        List<LocalDate> dates = sessions.stream()
                .map(Session::getDate)
                .distinct()
                .sorted()
                .toList();

        int maxStreak = 1;
        int currentStreak = 1;
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i - 1).plusDays(1).equals(dates.get(i))) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }
        return maxStreak;
    }

    private double calculateRecentOneRepMaxProgressPenalty(List<Session> sessions) {
        List<Session> recentSessions = sessions.stream()
                .sorted(Comparator.comparing(Session::getDate))
                .skip(Math.max(0, sessions.size() - 4))
                .toList();
        if (recentSessions.size() < 2) {
            return 50.0d;
        }

        double first = recentSessions.get(0).getEstimatedOneRepMax();
        double last = recentSessions.get(recentSessions.size() - 1).getEstimatedOneRepMax();
        if (first <= 0) {
            return 50.0d;
        }

        double percentChange = ((last - first) / first) * 100.0d;
        if (percentChange >= 2.0d) {
            return 20.0d;
        }
        if (percentChange >= 0.0d) {
            return 40.0d;
        }
        if (percentChange >= -2.0d) {
            return 60.0d;
        }
        if (percentChange >= -5.0d) {
            return 80.0d;
        }
        return 100.0d;
    }

    private boolean isActiveMesocycleAtDate(Mesocycle mesocycle, LocalDate date) {
        if (mesocycle == null || mesocycle.getCreatedAt() == null) {
            return false;
        }
        return mesocycle.getCreatedAt().toLocalDate().isBefore(date.plusDays(1));
    }

    private FatigueLevel classifyFatigueLevel(double fatigueScore) {
        if (fatigueScore < 25.0d) {
            return FatigueLevel.LOW;
        }
        if (fatigueScore < 50.0d) {
            return FatigueLevel.MODERATE;
        }
        if (fatigueScore < 75.0d) {
            return FatigueLevel.HIGH;
        }
        return FatigueLevel.CRITICAL;
    }

    private int safeDuration(Session session) {
        return session.getDurationMinutes() != null ? session.getDurationMinutes() : 0;
    }

    private double safeTotalVolume(Session session) {
        return session.getTotalVolume() != null ? session.getTotalVolume() : 0.0d;
    }

    private double durationLoadFactor(Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes <= 0) {
            return 1.0d;
        }
        return clamp(durationMinutes / 60.0d, 0.5d, 2.0d);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    private record FatigueScoreLevel(double fatigueScore, FatigueLevel fatigueLevel) {
    }
}
