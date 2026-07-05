package com.gymtracker.service.impl;

import com.gymtracker.dto.athlete.AthleteDetailDTO;
import com.gymtracker.dto.athlete.AthleteRequestDTO;
import com.gymtracker.dto.athlete.AthleteResponseDTO;
import com.gymtracker.dto.athlete.AthleteSummaryDTO;
import com.gymtracker.dto.mesocycle.MesocycleResponseDTO;
import com.gymtracker.dto.mesocycle.MesocycleSummaryDTO;
import com.gymtracker.dto.nutrition.NutritionPlanResponseDTO;
import com.gymtracker.dto.nutrition.NutritionPlanSummaryDTO;
import com.gymtracker.dto.workout.WorkoutSessionSummaryDTO;
import com.gymtracker.entity.User;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.DuplicateResourceException;
import com.gymtracker.exception.ResourceNotFoundException;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.mapper.AthleteMapper;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.security.AuthenticatedUserProvider;
import com.gymtracker.service.AthleteService;
import com.gymtracker.service.FatigueService;
import com.gymtracker.service.MesocycleService;
import com.gymtracker.service.NutritionPlanService;
import com.gymtracker.service.OneRepMaxService;
import com.gymtracker.service.WorkoutSessionService;
import com.gymtracker.validation.AthleteValidator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Business implementation for athlete profile management.
 */
@Service
public class AthleteServiceImpl implements AthleteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AthleteServiceImpl.class);

    private final UserRepository userRepository;
    private final AthleteMapper athleteMapper;
    private final AthleteValidator athleteValidator;
    private final MesocycleService mesocycleService;
    private final NutritionPlanService nutritionPlanService;
    private final WorkoutSessionService workoutSessionService;
    private final FatigueService fatigueService;
    private final OneRepMaxService oneRepMaxService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public AthleteServiceImpl(
            UserRepository userRepository,
            AthleteMapper athleteMapper,
            AthleteValidator athleteValidator,
            MesocycleService mesocycleService,
            NutritionPlanService nutritionPlanService,
            WorkoutSessionService workoutSessionService,
            FatigueService fatigueService,
            OneRepMaxService oneRepMaxService,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.userRepository = userRepository;
        this.athleteMapper = athleteMapper;
        this.athleteValidator = athleteValidator;
        this.mesocycleService = mesocycleService;
        this.nutritionPlanService = nutritionPlanService;
        this.workoutSessionService = workoutSessionService;
        this.fatigueService = fatigueService;
        this.oneRepMaxService = oneRepMaxService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @Override
    public AthleteDetailDTO getAthleteById(String athleteId) {
        athleteValidator.validateDelete(athleteId);
        User currentUser = getAuthenticatedUser();
        User athlete = findAthleteById(athleteId);
        ensureCanViewAthleteProfile(currentUser, athlete);

        AthleteDetailDTO detail = athleteMapper.toDetailDTO(athlete);
        enrichWithPerformanceData(detail, athlete.getId().toHexString());
        return detail;
    }

    /**
     * A coach may only view the full profile of an athlete actually assigned
     * to them (has a mesocycle for that athlete); a nutritionist may only
     * view profiles of athletes assigned to them (has a nutrition plan for
     * that athlete) - matching the assignment check already enforced by
     * ReportServiceImpl. Broad "any coach/nutritionist can view any athlete's
     * full profile" access was a broken-access-control gap, not an intended
     * design.
     */
    private void ensureCanViewAthleteProfile(User currentUser, User athlete) {
        String athleteId = athlete.getId().toHexString();
        if (currentUser.getRole() == Role.ATHLETE && !Objects.equals(currentUser.getId(), athlete.getId())) {
            LOGGER.warn("Unauthorized access to athlete profile by user email={}", currentUser.getEmail());
            throw new UnauthorizedOperationException("Athletes can only view their own profile.");
        }
        if (currentUser.getRole() == Role.COACH
                && !athleteIdsAssignedToCoach(currentUser.getId().toHexString()).contains(athleteId)) {
            LOGGER.warn("Unauthorized access to athlete profile by coach email={}", currentUser.getEmail());
            throw new UnauthorizedOperationException("Coaches may only view profiles of their assigned athletes.");
        }
        if (currentUser.getRole() == Role.NUTRITIONIST
                && !athleteIdsAssignedToNutritionist(currentUser.getId().toHexString()).contains(athleteId)) {
            LOGGER.warn("Unauthorized access to athlete profile by nutritionist email={}", currentUser.getEmail());
            throw new UnauthorizedOperationException("Nutritionists may only view profiles of their assigned athletes.");
        }
    }

    @Override
    public AthleteDetailDTO getCurrentAthlete() {
        User currentUser = getAuthenticatedUser();
        if (currentUser.getRole() != Role.ATHLETE) {
            LOGGER.warn("Unauthorized current athlete request by user email={}", currentUser.getEmail());
            throw new UnauthorizedOperationException("Only athletes can request current athlete profile.");
        }
        return athleteMapper.toDetailDTO(currentUser);
    }

    @Override
    public AthleteResponseDTO updateAthleteProfile(AthleteRequestDTO requestDTO) {
        User currentUser = getAuthenticatedUser();
        if (currentUser.getRole() != Role.ATHLETE) {
            LOGGER.warn("Unauthorized profile update attempt by user email={}", currentUser.getEmail());
            throw new UnauthorizedOperationException("Only athletes may update their own profile.");
        }

        try {
            athleteValidator.validateUpdate(requestDTO);
        } catch (ValidationException exception) {
            LOGGER.warn("Athlete profile validation failure for email={}: {}", currentUser.getEmail(), exception.getMessage());
            throw exception;
        }

        if (!currentUser.getEmail().equalsIgnoreCase(requestDTO.getEmail())
                && userRepository.existsByEmail(requestDTO.getEmail())) {
            LOGGER.warn("Duplicate athlete email update attempt by user email={}, requestedEmail={}",
                    currentUser.getEmail(), requestDTO.getEmail());
            throw new DuplicateResourceException("Athlete email already exists.");
        }

        athleteMapper.updateEntityFromRequest(requestDTO, currentUser);
        currentUser.setUpdatedAt(LocalDateTime.now());
        User savedAthlete = userRepository.save(currentUser);

        LOGGER.info("Profile updated for athlete id={}, email={}", savedAthlete.getId(), savedAthlete.getEmail());
        return athleteMapper.toResponseDTO(savedAthlete);
    }

    @Override
    public List<AthleteSummaryDTO> getAllAthletes() {
        User currentUser = getAuthenticatedUser();
        if (currentUser.getRole() == Role.ATHLETE) {
            LOGGER.warn("Unauthorized athlete list access by athlete email={}", currentUser.getEmail());
            throw new UnauthorizedOperationException("Athletes cannot list all athletes.");
        }

        return enrichSummaries(userRepository.findByRole(Role.ATHLETE));
    }

    @Override
    public List<AthleteSummaryDTO> searchAthletes(String keyword) {
        User currentUser = getAuthenticatedUser();
        if (currentUser.getRole() == Role.ATHLETE) {
            LOGGER.warn("Unauthorized athlete search access by athlete email={}", currentUser.getEmail());
            throw new UnauthorizedOperationException("Athletes cannot search other athletes.");
        }
        if (keyword == null || keyword.isBlank()) {
            throw new ValidationException("Search keyword is required.");
        }

        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        List<User> matchingAthletes = userRepository.findByRole(Role.ATHLETE).stream()
                .filter(athlete -> matchesKeyword(athlete, normalizedKeyword))
                .toList();
        return enrichSummaries(matchingAthletes);
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email is required.");
        }
        return userRepository.existsByEmail(email);
    }

    @Override
    public List<AthleteSummaryDTO> getAthletesForCurrentUser() {
        User currentUser = getAuthenticatedUser();
        String currentUserId = currentUser.getId().toHexString();

        return switch (currentUser.getRole()) {
            case COACH -> resolveAthleteSummaries(athleteIdsAssignedToCoach(currentUserId));
            case NUTRITIONIST -> resolveAthleteSummaries(athleteIdsAssignedToNutritionist(currentUserId));
            case ATHLETE -> {
                LOGGER.warn("Unauthorized athlete list access by athlete email={}", currentUser.getEmail());
                throw new UnauthorizedOperationException("Athletes cannot list other athletes.");
            }
        };
    }

    private Set<String> athleteIdsAssignedToCoach(String coachId) {
        return mesocycleService.getMesocyclesByCoach(coachId).stream()
                .map(MesocycleSummaryDTO::getAthleteId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> athleteIdsAssignedToNutritionist(String nutritionistId) {
        return nutritionPlanService.getNutritionPlansByNutritionist(nutritionistId).stream()
                .map(NutritionPlanSummaryDTO::getAthleteId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<AthleteSummaryDTO> resolveAthleteSummaries(Set<String> athleteIds) {
        List<User> athletes = athleteIds.stream()
                .map(this::findAthleteByIdSafe)
                .flatMap(Optional::stream)
                .toList();
        return enrichSummaries(athletes);
    }

    /**
     * Builds summaries for a list of athletes, resolving every referenced
     * coach's name with a single batched {@code findAllById} call instead of
     * one lookup per athlete.
     */
    private List<AthleteSummaryDTO> enrichSummaries(List<User> athletes) {
        Map<String, MesocycleResponseDTO> activeMesocycleByAthleteId = new HashMap<>();
        for (User athlete : athletes) {
            MesocycleResponseDTO activeMesocycle = mesocycleService.getActiveMesocycle(athlete.getId().toHexString());
            if (activeMesocycle != null) {
                activeMesocycleByAthleteId.put(athlete.getId().toHexString(), activeMesocycle);
            }
        }

        Set<String> coachIds = activeMesocycleByAthleteId.values().stream()
                .map(MesocycleResponseDTO::getCoachId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, String> coachNamesById = resolveUserFullNames(coachIds);

        return athletes.stream()
                .map(athlete -> {
                    AthleteSummaryDTO summary = athleteMapper.toSummaryDTO(athlete);
                    String athleteId = athlete.getId().toHexString();

                    MesocycleResponseDTO activeMesocycle = activeMesocycleByAthleteId.get(athleteId);
                    if (activeMesocycle != null) {
                        summary.setCurrentMesocycleName(activeMesocycle.getName());
                        summary.setCurrentCoachId(activeMesocycle.getCoachId());
                        summary.setCurrentCoachName(coachNamesById.get(activeMesocycle.getCoachId()));
                    }

                    workoutSessionService.getWorkoutSessionsByAthlete(athleteId).stream()
                            .map(WorkoutSessionSummaryDTO::getDate)
                            .filter(Objects::nonNull)
                            .max(LocalDate::compareTo)
                            .ifPresent(summary::setLastWorkoutDate);

                    return summary;
                })
                .toList();
    }

    private Map<String, String> resolveUserFullNames(Collection<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(user -> user.getId().toHexString(),
                        user -> user.getFirstName() + " " + user.getLastName()));
    }

    private void enrichWithPerformanceData(AthleteDetailDTO detail, String athleteId) {
        MesocycleResponseDTO activeMesocycle = mesocycleService.getActiveMesocycle(athleteId);
        if (activeMesocycle != null) {
            detail.setCurrentMesocycleName(activeMesocycle.getName());
            detail.setAssignedCoachName(resolveUserFullName(activeMesocycle.getCoachId()));
        }

        NutritionPlanResponseDTO activePlan = nutritionPlanService.getActiveNutritionPlan(athleteId);
        if (activePlan != null) {
            detail.setActiveNutritionPlan(activePlan);
            detail.setAssignedNutritionistName(resolveUserFullName(activePlan.getNutritionistId()));
        }

        List<WorkoutSessionSummaryDTO> sessions = workoutSessionService.getWorkoutSessionsByAthlete(athleteId);
        sessions.stream()
                .filter(session -> session.getDate() != null)
                .max(Comparator.comparing(WorkoutSessionSummaryDTO::getDate))
                .ifPresent(detail::setLatestWorkout);

        detail.setCurrentFatigueLevel(fatigueService.getCurrentFatigueLevel(athleteId));

        boolean hasOneRepMaxHistory = sessions.stream()
                .anyMatch(session -> session.getEstimatedOneRepMax() != null && session.getEstimatedOneRepMax() > 0);
        detail.setLatestOneRepMax(hasOneRepMaxHistory ? oneRepMaxService.getCurrentEstimatedOneRepMax(athleteId) : null);
    }

    private String resolveUserFullName(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        return userRepository.findById(userId)
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .orElse(null);
    }

    private Optional<User> findAthleteByIdSafe(String athleteId) {
        return userRepository.findById(athleteId).filter(user -> user.getRole() == Role.ATHLETE);
    }

    private User getAuthenticatedUser() {
        return authenticatedUserProvider.getAuthenticatedUser();
    }

    private User findAthleteById(String athleteId) {
        User athlete = userRepository.findById(athleteId)
                .orElseThrow(() -> new ResourceNotFoundException("Athlete not found with id: " + athleteId));
        if (athlete.getRole() != Role.ATHLETE) {
            throw new ResourceNotFoundException("Athlete not found with id: " + athleteId);
        }
        return athlete;
    }

    private boolean matchesKeyword(User athlete, String normalizedKeyword) {
        return containsIgnoreCase(athlete.getFirstName(), normalizedKeyword)
                || containsIgnoreCase(athlete.getLastName(), normalizedKeyword)
                || containsIgnoreCase(athlete.getEmail(), normalizedKeyword)
                || containsIgnoreCase(athlete.getId() != null ? athlete.getId().toHexString() : null, normalizedKeyword);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
