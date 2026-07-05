package com.gymtracker.service.impl;

import com.gymtracker.dto.dashboard.ChartDTO;
import com.gymtracker.dto.dashboard.StatisticsDTO;
import com.gymtracker.entity.Mesocycle;
import com.gymtracker.entity.NutritionPlan;
import com.gymtracker.entity.Session;
import com.gymtracker.entity.User;
import com.gymtracker.enums.MesocycleStatus;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.ResourceNotFoundException;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.repository.MesocycleRepository;
import com.gymtracker.repository.NutritionPlanRepository;
import com.gymtracker.repository.SessionRepository;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.config.CacheConfig;
import com.gymtracker.service.AthleteAssignmentService;
import com.gymtracker.service.StatisticsService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Business implementation for statistics and KPI computations.
 * Aggregates data from workouts, mesocycles, and nutrition plans.
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsServiceImpl.class);

    private final SessionRepository workoutSessionRepository;
    private final MesocycleRepository mesocycleRepository;
    private final NutritionPlanRepository nutritionPlanRepository;
    private final UserRepository userRepository;
    private final StatisticsServiceImpl self;
    private final AthleteAssignmentService athleteAssignmentService;

    public StatisticsServiceImpl(
            SessionRepository workoutSessionRepository,
            MesocycleRepository mesocycleRepository,
            NutritionPlanRepository nutritionPlanRepository,
            UserRepository userRepository,
            @Lazy StatisticsServiceImpl self,
            AthleteAssignmentService athleteAssignmentService
    ) {
        this.workoutSessionRepository = workoutSessionRepository;
        this.mesocycleRepository = mesocycleRepository;
        this.nutritionPlanRepository = nutritionPlanRepository;
        this.userRepository = userRepository;
        this.self = self;
        this.athleteAssignmentService = athleteAssignmentService;
    }

    /*
     * Each getXxxStatistics/getXxxChart method below performs its permission
     * check (ensureCanAccessStatistics) BEFORE delegating to a cached
     * compute* method through the self-proxy. This guarantees the permission
     * check always runs, even on a cache hit for a key some other caller
     * populated — @Cacheable would otherwise skip the method body entirely
     * (and the permission check with it) whenever the key already exists.
     */

    @Override
    public StatisticsDTO getAthleteStatistics(String athleteId) {
        validateIdentifier(athleteId, "Athlete id is required.");
        User athlete = getAthleteById(athleteId);
        ensureCanAccessAthleteStatistics(athlete.getId());
        return self.computeAthleteStatistics(athleteId, athlete);
    }

    @Cacheable(value = CacheConfig.STATISTICS_CACHE, key = "'athlete:' + #athleteId")
    public StatisticsDTO computeAthleteStatistics(String athleteId, User athlete) {
        ObjectId athleteObjectId = athlete.getId();
        List<Session> workouts = workoutSessionRepository.findByAthleteId(athleteObjectId);

        StatisticsDTO stats = StatisticsDTO.builder()
                .weeklyTrainingVolume(calculateWeeklyTrainingVolume(workouts))
                .monthlyTrainingVolume(calculateMonthlyTrainingVolume(workouts))
                .averageRpe(0.0)
                .averageWorkoutDuration(calculateAverageDuration(workouts))
                .averageWeeklySessions(calculateAverageWeeklySessions(workouts))
                .estimatedStrengthProgress(calculateStrengthProgress(workouts))
                .bodyWeightEvolution(calculateBodyWeightEvolution(athlete))
                .completedMesocycles(countCompletedMesocycles(athleteObjectId))
                .completedNutritionPlans(countCompletedNutritionPlans(athleteObjectId))
                .build();

        LOGGER.info("Statistics calculated for athlete={}", athleteId);
        return stats;
    }

    @Override
    public StatisticsDTO getCoachStatistics(String coachId) {
        validateIdentifier(coachId, "Coach id is required.");
        User coach = getCoachById(coachId);
        ensureCanAccessCoachStatistics(coach.getId());
        return self.computeCoachStatistics(coachId, coach);
    }

    @Cacheable(value = CacheConfig.STATISTICS_CACHE, key = "'coach:' + #coachId")
    public StatisticsDTO computeCoachStatistics(String coachId, User coach) {
        ObjectId coachObjectId = coach.getId();
        List<Mesocycle> coachMesocycles = mesocycleRepository.findByCoachId(coachObjectId);
        Set<ObjectId> assignedAthleteIds = coachMesocycles.stream()
                .map(Mesocycle::getAthleteId)
                .collect(Collectors.toSet());

        List<Session> totalWorkouts = assignedAthleteIds.isEmpty()
                ? List.of()
                : workoutSessionRepository.findByAthleteIdIn(assignedAthleteIds);

        StatisticsDTO stats = StatisticsDTO.builder()
                .weeklyTrainingVolume(calculateWeeklyTrainingVolume(totalWorkouts))
                .monthlyTrainingVolume(calculateMonthlyTrainingVolume(totalWorkouts))
                .averageRpe(0.0)
                .averageWorkoutDuration(calculateAverageDuration(totalWorkouts))
                .averageWeeklySessions(calculateAverageWeeklySessions(totalWorkouts))
                .estimatedStrengthProgress(calculateStrengthProgress(totalWorkouts))
                .completedMesocycles(countCoachMesocycles(coachObjectId))
                .completedNutritionPlans(countCoachNutritionPlans(coachObjectId))
                .build();

        LOGGER.info("Statistics calculated for coach={}", coachId);
        return stats;
    }

    @Override
    public StatisticsDTO getNutritionistStatistics(String nutritionistId) {
        validateIdentifier(nutritionistId, "Nutritionist id is required.");
        User nutritionist = getNutritionistById(nutritionistId);
        ensureCanAccessNutritionistStatistics(nutritionist.getId());
        return self.computeNutritionistStatistics(nutritionistId, nutritionist);
    }

    @Cacheable(value = CacheConfig.STATISTICS_CACHE, key = "'nutritionist:' + #nutritionistId")
    public StatisticsDTO computeNutritionistStatistics(String nutritionistId, User nutritionist) {
        ObjectId nutritionistObjectId = nutritionist.getId();
        List<NutritionPlan> plans = nutritionPlanRepository.findByNutritionistId(nutritionistObjectId);
        Set<ObjectId> assignedAthleteIds = plans.stream()
                .map(NutritionPlan::getAthleteId)
                .collect(Collectors.toSet());

        long completedPlans = plans.stream()
                .filter(plan -> plan.getEndDate() != null && plan.getEndDate().isBefore(LocalDate.now()))
                .count();

        StatisticsDTO stats = StatisticsDTO.builder()
                .completedNutritionPlans((int) completedPlans)
                .averageWeeklySessions((double) assignedAthleteIds.size())
                .build();

        LOGGER.info("Statistics calculated for nutritionist={}", nutritionistId);
        return stats;
    }

    @Override
    public ChartDTO getWorkoutVolumeChart(String athleteId) {
        validateIdentifier(athleteId, "Athlete id is required.");
        User athlete = getAthleteById(athleteId);
        ensureCanAccessAthleteStatistics(athlete.getId());
        return self.computeWorkoutVolumeChart(athleteId, athlete);
    }

    @Cacheable(value = CacheConfig.STATISTICS_CACHE, key = "'workoutVolumeChart:' + #athleteId")
    public ChartDTO computeWorkoutVolumeChart(String athleteId, User athlete) {
        List<Session> workouts = workoutSessionRepository.findByAthleteId(athlete.getId());

        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        for (int i = 12; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusMonths(i);
            String label = date.getMonth().toString().substring(0, 3) + " " + date.getYear();
            labels.add(label);

            final LocalDate monthStart = date.withDayOfMonth(1);
            final LocalDate monthEnd = date.withDayOfMonth(date.lengthOfMonth());

            double monthVolume = workouts.stream()
                    .filter(w -> !w.getDate().isBefore(monthStart) && !w.getDate().isAfter(monthEnd))
                    .mapToDouble(Session::getTotalVolume)
                    .sum();

            values.add(monthVolume);
        }

        LOGGER.debug("Workout volume chart generated for athlete={}", athleteId);
        return ChartDTO.builder()
                .title("Monthly Training Volume")
                .labels(labels)
                .values(values)
                .build();
    }

    @Override
    public ChartDTO getOneRepMaxChart(String athleteId) {
        validateIdentifier(athleteId, "Athlete id is required.");
        User athlete = getAthleteById(athleteId);
        ensureCanAccessAthleteStatistics(athlete.getId());
        return self.computeOneRepMaxChart(athleteId, athlete);
    }

    @Cacheable(value = CacheConfig.STATISTICS_CACHE, key = "'oneRepMaxChart:' + #athleteId")
    public ChartDTO computeOneRepMaxChart(String athleteId, User athlete) {
        List<Session> workouts = workoutSessionRepository.findByAthleteId(athlete.getId())
                .stream()
                .sorted(Comparator.comparing(Session::getDate))
                .toList();

        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        int limit = Math.min(workouts.size(), 12);
        for (int i = 0; i < limit; i++) {
            Session workout = workouts.get(i);
            labels.add(workout.getDate().toString());
            values.add(workout.getEstimatedOneRepMax() != null ? workout.getEstimatedOneRepMax() : 0.0);
        }

        LOGGER.debug("One Rep Max chart generated for athlete={}", athleteId);
        return ChartDTO.builder()
                .title("Estimated One Rep Max Progress")
                .labels(labels)
                .values(values)
                .build();
    }

    @Override
    public ChartDTO getFatigueChart(String athleteId) {
        validateIdentifier(athleteId, "Athlete id is required.");
        User athlete = getAthleteById(athleteId);
        ensureCanAccessAthleteStatistics(athlete.getId());

        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            labels.add(date.toString());
            values.add(50.0);
        }

        LOGGER.debug("Fatigue chart generated for athlete={}", athleteId);
        return ChartDTO.builder()
                .title("Weekly Fatigue Trend")
                .labels(labels)
                .values(values)
                .build();
    }

    private double calculateWeeklyTrainingVolume(List<Session> workouts) {
        LocalDate weekStart = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        LocalDate weekEnd = weekStart.plusDays(6);

        return workouts.stream()
                .filter(w -> !w.getDate().isBefore(weekStart) && !w.getDate().isAfter(weekEnd))
                .mapToDouble(Session::getTotalVolume)
                .sum();
    }

    private double calculateMonthlyTrainingVolume(List<Session> workouts) {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        return workouts.stream()
                .filter(w -> !w.getDate().isBefore(monthStart) && !w.getDate().isAfter(monthEnd))
                .mapToDouble(Session::getTotalVolume)
                .sum();
    }

    private double calculateAverageDuration(List<Session> workouts) {
        if (workouts.isEmpty()) {
            return 0.0;
        }

        return workouts.stream()
                .mapToDouble(Session::getDurationMinutes)
                .average()
                .orElse(0.0);
    }

    private double calculateAverageWeeklySessions(List<Session> workouts) {
        if (workouts.isEmpty()) {
            return 0.0;
        }

        LocalDate oldestWorkout = workouts.stream()
                .map(Session::getDate)
                .min(Comparator.naturalOrder())
                .orElse(LocalDate.now());

        long weeks = Math.max(ChronoUnit.WEEKS.between(oldestWorkout, LocalDate.now()), 1L);
        return (double) workouts.size() / weeks;
    }

    private double calculateStrengthProgress(List<Session> workouts) {
        if (workouts.size() < 2) {
            return 0.0;
        }

        List<Session> sorted = workouts.stream()
                .sorted(Comparator.comparing(Session::getDate).reversed())
                .toList();

        Double current = sorted.get(0).getEstimatedOneRepMax();
        Double previous = sorted.stream()
                .skip(Math.min(5, sorted.size() - 1))
                .findFirst()
                .map(Session::getEstimatedOneRepMax)
                .orElse(null);

        if (current == null || previous == null || previous == 0.0) {
            return 0.0;
        }

        return ((current - previous) / previous) * 100;
    }

    private double calculateBodyWeightEvolution(User user) {
        if (user.getWeight() == null) {
            return 0.0;
        }
        return (double) user.getWeight();
    }

    private int countCompletedMesocycles(ObjectId athleteId) {
        return (int) mesocycleRepository.findByAthleteId(athleteId)
                .stream()
                .filter(m -> MesocycleStatus.COMPLETED.equals(m.getStatus()))
                .count();
    }

    private int countCompletedNutritionPlans(ObjectId athleteId) {
        return (int) nutritionPlanRepository.findByAthleteId(athleteId)
                .stream()
                .filter(p -> p.getEndDate() != null && p.getEndDate().isBefore(LocalDate.now()))
                .count();
    }

    private int countCoachMesocycles(ObjectId coachId) {
        return (int) mesocycleRepository.findByCoachId(coachId)
                .stream()
                .filter(m -> MesocycleStatus.COMPLETED.equals(m.getStatus()))
                .count();
    }

    private int countCoachNutritionPlans(ObjectId coachId) {
        List<Mesocycle> coachMesocycles = mesocycleRepository.findByCoachId(coachId);
        Set<ObjectId> athleteIds = coachMesocycles.stream()
                .map(Mesocycle::getAthleteId)
                .collect(Collectors.toSet());

        if (athleteIds.isEmpty()) {
            return 0;
        }

        return (int) nutritionPlanRepository.findByAthleteIdIn(athleteIds).stream()
                .filter(p -> p.getEndDate() != null && p.getEndDate().isBefore(LocalDate.now()))
                .count();
    }

    private void validateIdentifier(String id, String message) {
        if (id == null || id.isBlank()) {
            throw new ValidationException(message);
        }
    }

    private User getAthleteById(String athleteId) {
        return userRepository.findById(athleteId)
                .orElseThrow(() -> new ResourceNotFoundException("Athlete not found: " + athleteId));
    }

    private User getCoachById(String coachId) {
        return userRepository.findById(coachId)
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found: " + coachId));
    }

    private User getNutritionistById(String nutritionistId) {
        return userRepository.findById(nutritionistId)
                .orElseThrow(() -> new ResourceNotFoundException("Nutritionist not found: " + nutritionistId));
    }

    /**
     * Athlete statistics may be viewed by the athlete themselves, or by a
     * coach/nutritionist actually assigned to that athlete (has a mesocycle
     * or nutrition plan for them, respectively) - matching the assignment
     * check already enforced by ReportServiceImpl.
     */
    private void ensureCanAccessAthleteStatistics(ObjectId athleteId) {
        User currentUser = getAuthenticatedUser();

        if (Objects.equals(currentUser.getId(), athleteId)) {
            return;
        }
        if (currentUser.getRole() == Role.COACH
                && athleteAssignmentService.isAthleteAssignedToCoach(currentUser.getId(), athleteId)) {
            return;
        }
        if (currentUser.getRole() == Role.NUTRITIONIST
                && athleteAssignmentService.isAthleteAssignedToNutritionist(currentUser.getId(), athleteId)) {
            return;
        }
        throw new UnauthorizedOperationException("User cannot access these statistics");
    }

    /**
     * A coach's own aggregate roster statistics are only visible to that
     * exact coach - no other role has a legitimate reason to view them.
     */
    private void ensureCanAccessCoachStatistics(ObjectId coachId) {
        User currentUser = getAuthenticatedUser();
        if (currentUser.getRole() != Role.COACH || !Objects.equals(currentUser.getId(), coachId)) {
            throw new UnauthorizedOperationException("User cannot access these statistics");
        }
    }

    /**
     * A nutritionist's own aggregate statistics are only visible to that
     * exact nutritionist.
     */
    private void ensureCanAccessNutritionistStatistics(ObjectId nutritionistId) {
        User currentUser = getAuthenticatedUser();
        if (currentUser.getRole() != Role.NUTRITIONIST || !Objects.equals(currentUser.getId(), nutritionistId)) {
            throw new UnauthorizedOperationException("User cannot access these statistics");
        }
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedOperationException("User not authenticated");
        }

        String userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UnauthorizedOperationException("User not found"));
    }
}
