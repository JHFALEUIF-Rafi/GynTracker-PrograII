package com.gymtracker.service.impl;

import com.gymtracker.dto.nutrition.NutritionPlanDetailDTO;
import com.gymtracker.dto.nutrition.NutritionPlanRequestDTO;
import com.gymtracker.dto.nutrition.NutritionPlanResponseDTO;
import com.gymtracker.dto.nutrition.NutritionPlanSummaryDTO;
import com.gymtracker.entity.NutritionPlan;
import com.gymtracker.entity.User;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.BusinessRuleException;
import com.gymtracker.exception.DuplicateResourceException;
import com.gymtracker.exception.ResourceNotFoundException;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.mapper.NutritionPlanMapper;
import com.gymtracker.repository.NutritionPlanRepository;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.security.AuthenticatedUserProvider;
import com.gymtracker.service.AthleteAssignmentService;
import com.gymtracker.service.NutritionPlanService;
import com.gymtracker.validation.NutritionPlanValidator;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

/**
 * Business implementation for nutrition plan lifecycle management.
 */
@Service
public class NutritionPlanServiceImpl implements NutritionPlanService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NutritionPlanServiceImpl.class);

    private final NutritionPlanRepository nutritionPlanRepository;
    private final UserRepository userRepository;
    private final NutritionPlanMapper nutritionPlanMapper;
    private final NutritionPlanValidator nutritionPlanValidator;
    private final MongoOperations mongoTemplate;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final AthleteAssignmentService athleteAssignmentService;

    public NutritionPlanServiceImpl(
            NutritionPlanRepository nutritionPlanRepository,
            UserRepository userRepository,
            NutritionPlanMapper nutritionPlanMapper,
            NutritionPlanValidator nutritionPlanValidator,
            MongoOperations mongoTemplate,
            AuthenticatedUserProvider authenticatedUserProvider,
            AthleteAssignmentService athleteAssignmentService
    ) {
        this.nutritionPlanRepository = nutritionPlanRepository;
        this.userRepository = userRepository;
        this.nutritionPlanMapper = nutritionPlanMapper;
        this.nutritionPlanValidator = nutritionPlanValidator;
        this.mongoTemplate = mongoTemplate;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.athleteAssignmentService = athleteAssignmentService;
    }

    @Override
    @CacheEvict(value = "dashboards", allEntries = true)
    public NutritionPlanResponseDTO createNutritionPlan(NutritionPlanRequestDTO requestDTO) {
        User currentUser = getAuthenticatedUser();
        ensureNutritionistCanWrite(currentUser);
        validateNutritionPlanRequestForCreate(requestDTO, currentUser);

        ObjectId athleteId = getAthleteById(requestDTO.getAthleteId()).getId();
        ObjectId nutritionistId = getNutritionistById(requestDTO.getNutritionistId()).getId();

        validateNoDuplicatePlan(requestDTO, athleteId, null);

        if (Boolean.TRUE.equals(requestDTO.getActive())) {
            deactivateOtherActivePlans(athleteId, null);
        }

        NutritionPlan nutritionPlan = nutritionPlanMapper.toEntity(requestDTO);
        nutritionPlan.setAthleteId(athleteId);
        nutritionPlan.setNutritionistId(nutritionistId);
        nutritionPlan.setCreatedAt(LocalDateTime.now());

        NutritionPlan savedPlan = nutritionPlanRepository.save(nutritionPlan);
        LOGGER.info("Nutrition plan created id={}, athleteId={}, nutritionistId={}",
                savedPlan.getId(), savedPlan.getAthleteId(), savedPlan.getNutritionistId());
        return nutritionPlanMapper.toResponseDTO(savedPlan);
    }

    @Override
    @CacheEvict(value = "dashboards", allEntries = true)
    public NutritionPlanResponseDTO updateNutritionPlan(String planId, NutritionPlanRequestDTO requestDTO) {
        User currentUser = getAuthenticatedUser();
        ensureNutritionistCanWrite(currentUser);
        validateNutritionPlanRequestForUpdate(planId, requestDTO, currentUser);

        NutritionPlan existingPlan = findNutritionPlanById(planId);
        ObjectId athleteId = getAthleteById(requestDTO.getAthleteId()).getId();
        ObjectId nutritionistId = getNutritionistById(requestDTO.getNutritionistId()).getId();

        validateNoDuplicatePlan(requestDTO, athleteId, existingPlan.getId());

        if (Boolean.TRUE.equals(requestDTO.getActive())) {
            deactivateOtherActivePlans(athleteId, existingPlan.getId());
        }

        nutritionPlanMapper.updateEntityFromRequest(requestDTO, existingPlan);
        existingPlan.setAthleteId(athleteId);
        existingPlan.setNutritionistId(nutritionistId);
        NutritionPlan savedPlan = nutritionPlanRepository.save(existingPlan);

        LOGGER.info("Nutrition plan updated id={}, athleteId={}", savedPlan.getId(), savedPlan.getAthleteId());
        return nutritionPlanMapper.toResponseDTO(savedPlan);
    }

    @Override
    @CacheEvict(value = "dashboards", allEntries = true)
    public NutritionPlanResponseDTO deactivateNutritionPlan(String planId) {
        User currentUser = getAuthenticatedUser();
        ensureNutritionistCanWrite(currentUser);
        nutritionPlanValidator.validateDelete(planId);

        NutritionPlan plan = findNutritionPlanById(planId);
        if (!Boolean.TRUE.equals(plan.getActive())) {
            throw new BusinessRuleException("Nutrition plan is already inactive.");
        }

        plan.setActive(false);
        NutritionPlan savedPlan = nutritionPlanRepository.save(plan);
        LOGGER.info("Nutrition plan deactivated id={}, athleteId={}", savedPlan.getId(), savedPlan.getAthleteId());
        return nutritionPlanMapper.toResponseDTO(savedPlan);
    }

    @Override
    public NutritionPlanDetailDTO getNutritionPlanById(String planId) {
        nutritionPlanValidator.validateDelete(planId);
        User currentUser = getAuthenticatedUser();
        NutritionPlan plan = findNutritionPlanById(planId);
        ensureReadPermission(currentUser, plan.getAthleteId());

        NutritionPlanDetailDTO detail = nutritionPlanMapper.toDetailDTO(plan);
        detail.setAthleteName(resolveUserFullName(plan.getAthleteId()));
        detail.setNutritionistName(resolveUserFullName(plan.getNutritionistId()));
        return detail;
    }

    @Override
    public NutritionPlanResponseDTO getActiveNutritionPlan(String athleteId) {
        validateIdentifier(athleteId, "Athlete id is required.");
        User currentUser = getAuthenticatedUser();
        User athlete = getAthleteById(athleteId);
        ensureReadPermission(currentUser, athlete.getId());

        NutritionPlan activePlan = nutritionPlanRepository.findByAthleteId(athlete.getId()).stream()
                .filter(plan -> Boolean.TRUE.equals(plan.getActive()))
                .max(Comparator.comparing(NutritionPlan::getCreatedAt))
                .orElseThrow(() -> new ResourceNotFoundException("Active nutrition plan not found for athlete: " + athleteId));

        return nutritionPlanMapper.toResponseDTO(activePlan);
    }

    @Override
    public List<NutritionPlanSummaryDTO> getNutritionHistory(String athleteId) {
        validateIdentifier(athleteId, "Athlete id is required.");
        User currentUser = getAuthenticatedUser();
        User athlete = getAthleteById(athleteId);
        ensureReadPermission(currentUser, athlete.getId());

        List<NutritionPlan> plans = nutritionPlanRepository.findByAthleteId(athlete.getId()).stream()
                .sorted(Comparator.comparing(NutritionPlan::getCreatedAt).reversed())
                .toList();
        return toEnrichedSummaries(plans);
    }

    @Override
    public List<NutritionPlanSummaryDTO> getNutritionPlansByNutritionist(String nutritionistId) {
        User currentUser = getAuthenticatedUser();
        User nutritionist = getNutritionistById(nutritionistId);

        if (currentUser.getRole() == Role.ATHLETE) {
            LOGGER.warn("Unauthorized nutritionist plans access by athlete email={}", currentUser.getEmail());
            throw new UnauthorizedOperationException("Athletes cannot list nutrition plans by nutritionist.");
        }

        if (currentUser.getRole() == Role.NUTRITIONIST && !Objects.equals(currentUser.getId(), nutritionist.getId())) {
            LOGGER.warn("Unauthorized nutritionist plans access by nutritionist email={}", currentUser.getEmail());
            throw new UnauthorizedOperationException("Nutritionists can only view their own plans.");
        }

        if (currentUser.getRole() == Role.COACH) {
            LOGGER.warn("Unauthorized nutritionist plans access by coach email={}", currentUser.getEmail());
            throw new UnauthorizedOperationException("Coaches cannot list nutrition plans by nutritionist.");
        }

        List<NutritionPlan> plans = nutritionPlanRepository.findByNutritionistId(nutritionist.getId()).stream()
                .sorted(Comparator.comparing(NutritionPlan::getCreatedAt).reversed())
                .toList();
        return toEnrichedSummaries(plans);
    }

    @Override
    public List<NutritionPlanSummaryDTO> searchNutritionPlans(String keyword) {
        User currentUser = getAuthenticatedUser();
        if (keyword == null || keyword.isBlank()) {
            throw new ValidationException("Search keyword is required.");
        }

        Pattern keywordPattern = Pattern.compile(Pattern.quote(keyword.trim()), Pattern.CASE_INSENSITIVE);
        Criteria keywordCriteria = new Criteria().orOperator(
                Criteria.where("idStr").regex(keywordPattern),
                Criteria.where("athleteIdStr").regex(keywordPattern),
                Criteria.where("nutritionistIdStr").regex(keywordPattern),
                Criteria.where("goal").regex(keywordPattern),
                Criteria.where("observations").regex(keywordPattern)
        );

        Criteria scopeCriteria = switch (currentUser.getRole()) {
            case ATHLETE -> Criteria.where("athleteId").is(currentUser.getId());
            case COACH -> Criteria.where("athleteId")
                    .in(athleteAssignmentService.assignedAthleteIdsForCoach(currentUser.getId()));
            case NUTRITIONIST -> Criteria.where("nutritionistId").is(currentUser.getId());
        };
        Criteria criteria = new Criteria().andOperator(keywordCriteria, scopeCriteria);

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.addFields()
                        .addField("idStr").withValue(ConvertOperators.ToString.toString("$_id"))
                        .addField("athleteIdStr").withValue(ConvertOperators.ToString.toString("$athleteId"))
                        .addField("nutritionistIdStr").withValue(ConvertOperators.ToString.toString("$nutritionistId"))
                        .build(),
                Aggregation.match(criteria)
        );

        List<NutritionPlan> plans = mongoTemplate.aggregate(aggregation, "nutritionPlans", NutritionPlan.class)
                .getMappedResults().stream()
                .sorted(Comparator.comparing(NutritionPlan::getCreatedAt).reversed())
                .toList();
        return toEnrichedSummaries(plans);
    }

    @Override
    public List<NutritionPlanSummaryDTO> getNutritionPlansForCurrentUser() {
        User currentUser = getAuthenticatedUser();
        String currentUserId = currentUser.getId().toHexString();

        return switch (currentUser.getRole()) {
            case ATHLETE -> getNutritionHistory(currentUserId);
            case NUTRITIONIST -> getNutritionPlansByNutritionist(currentUserId);
            case COACH -> {
                Set<ObjectId> assignedAthleteIds =
                        athleteAssignmentService.assignedAthleteIdsForCoach(currentUser.getId());
                List<NutritionPlan> plans = assignedAthleteIds.isEmpty()
                        ? List.of()
                        : nutritionPlanRepository.findAll().stream()
                        .filter(plan -> assignedAthleteIds.contains(plan.getAthleteId()))
                        .sorted(Comparator.comparing(NutritionPlan::getCreatedAt).reversed())
                        .toList();
                yield toEnrichedSummaries(plans);
            }
        };
    }

    /**
     * Builds summaries for a list of nutrition plans, resolving every referenced
     * athlete/nutritionist name with a single batched {@code findAllById} call
     * instead of two lookups per plan.
     */
    private List<NutritionPlanSummaryDTO> toEnrichedSummaries(List<NutritionPlan> plans) {
        Set<String> userIds = new HashSet<>();
        for (NutritionPlan plan : plans) {
            if (plan.getAthleteId() != null) {
                userIds.add(plan.getAthleteId().toHexString());
            }
            if (plan.getNutritionistId() != null) {
                userIds.add(plan.getNutritionistId().toHexString());
            }
        }
        Map<String, String> namesById = resolveUserFullNames(userIds);

        return plans.stream()
                .map(plan -> {
                    NutritionPlanSummaryDTO summary = nutritionPlanMapper.toSummaryDTO(plan);
                    summary.setAthleteName(plan.getAthleteId() != null ? namesById.get(plan.getAthleteId().toHexString()) : null);
                    summary.setNutritionistName(plan.getNutritionistId() != null ? namesById.get(plan.getNutritionistId().toHexString()) : null);
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

    private String resolveUserFullName(ObjectId userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId.toHexString())
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .orElse(null);
    }

    private void validateNutritionPlanRequestForCreate(NutritionPlanRequestDTO requestDTO, User currentUser) {
        try {
            nutritionPlanValidator.validateCreate(requestDTO);
            nutritionPlanValidator.validateAssignment(requestDTO.getAthleteId(), requestDTO.getNutritionistId());
        } catch (ValidationException exception) {
            LOGGER.warn("Nutrition plan validation failure for create by email={}: {}", currentUser.getEmail(), exception.getMessage());
            throw exception;
        }
        ensureAuthenticatedNutritionistMatchesRequest(currentUser, requestDTO.getNutritionistId());
    }

    private void validateNutritionPlanRequestForUpdate(String planId, NutritionPlanRequestDTO requestDTO, User currentUser) {
        try {
            nutritionPlanValidator.validateDelete(planId);
            nutritionPlanValidator.validateUpdate(requestDTO);
            nutritionPlanValidator.validateAssignment(requestDTO.getAthleteId(), requestDTO.getNutritionistId());
        } catch (ValidationException exception) {
            LOGGER.warn("Nutrition plan validation failure for update by email={}: {}", currentUser.getEmail(), exception.getMessage());
            throw exception;
        }
        ensureAuthenticatedNutritionistMatchesRequest(currentUser, requestDTO.getNutritionistId());
    }

    private void deactivateOtherActivePlans(ObjectId athleteId, ObjectId exceptPlanId) {
        List<NutritionPlan> plans = nutritionPlanRepository.findByAthleteId(athleteId);
        for (NutritionPlan plan : plans) {
            if (Boolean.TRUE.equals(plan.getActive())
                    && (exceptPlanId == null || !Objects.equals(plan.getId(), exceptPlanId))) {
                plan.setActive(false);
                nutritionPlanRepository.save(plan);
            }
        }
    }

    private void validateNoDuplicatePlan(NutritionPlanRequestDTO requestDTO, ObjectId athleteId, ObjectId ignorePlanId) {
        boolean duplicated = nutritionPlanRepository.findByAthleteId(athleteId).stream()
                .filter(plan -> ignorePlanId == null || !Objects.equals(plan.getId(), ignorePlanId))
                .anyMatch(plan -> Objects.equals(plan.getNutritionistId().toHexString(), requestDTO.getNutritionistId())
                        && plan.getGoal() == requestDTO.getGoal()
                        && Objects.equals(plan.getStartDate(), requestDTO.getStartDate())
                        && Objects.equals(plan.getEndDate(), requestDTO.getEndDate()));
        if (duplicated) {
            throw new DuplicateResourceException("A nutrition plan with the same period and goal already exists.");
        }
    }

    private void ensureNutritionistCanWrite(User currentUser) {
        if (currentUser.getRole() != Role.NUTRITIONIST) {
            LOGGER.warn("Unauthorized nutrition plan write attempt by email={}", currentUser.getEmail());
            throw new UnauthorizedOperationException("Only nutritionists may create or modify nutrition plans.");
        }
    }

    /**
     * A coach may only read nutrition plans for athletes actually assigned
     * to them (has a mesocycle for that athlete); a nutritionist may only
     * read plans for athletes assigned to them (has a nutrition plan for
     * that athlete) - matching the assignment check already enforced by
     * ReportServiceImpl. Broad "any coach/nutritionist can read" access was
     * a broken-access-control gap, not an intended design.
     */
    private void ensureReadPermission(User currentUser, ObjectId athleteId) {
        if (currentUser.getRole() == Role.ATHLETE && !Objects.equals(currentUser.getId(), athleteId)) {
            LOGGER.warn("Unauthorized nutrition plan read attempt by athlete email={}", currentUser.getEmail());
            throw new UnauthorizedOperationException("Athletes may only view their own nutrition plans.");
        }
        if (currentUser.getRole() == Role.COACH
                && !athleteAssignmentService.isAthleteAssignedToCoach(currentUser.getId(), athleteId)) {
            LOGGER.warn("Unauthorized nutrition plan read attempt by coach email={}", currentUser.getEmail());
            throw new UnauthorizedOperationException("Coaches may only view nutrition plans for their assigned athletes.");
        }
        if (currentUser.getRole() == Role.NUTRITIONIST
                && !athleteAssignmentService.isAthleteAssignedToNutritionist(currentUser.getId(), athleteId)) {
            LOGGER.warn("Unauthorized nutrition plan read attempt by nutritionist email={}", currentUser.getEmail());
            throw new UnauthorizedOperationException("Nutritionists may only view nutrition plans for their assigned athletes.");
        }
    }

    private void ensureAuthenticatedNutritionistMatchesRequest(User currentUser, String nutritionistId) {
        if (!Objects.equals(currentUser.getId().toHexString(), nutritionistId)) {
            LOGGER.warn("Unauthorized nutritionist mismatch. Authenticated email={}, requestNutritionistId={}",
                    currentUser.getEmail(), nutritionistId);
            throw new UnauthorizedOperationException("Nutritionists may only create or update their own plans.");
        }
    }

    private NutritionPlan findNutritionPlanById(String planId) {
        return nutritionPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Nutrition plan not found with id: " + planId));
    }

    private User getAthleteById(String athleteId) {
        User athlete = userRepository.findById(athleteId)
                .orElseThrow(() -> new ResourceNotFoundException("Athlete not found with id: " + athleteId));
        if (athlete.getRole() != Role.ATHLETE) {
            throw new BusinessRuleException("Referenced user is not an athlete.");
        }
        return athlete;
    }

    private User getNutritionistById(String nutritionistId) {
        User nutritionist = userRepository.findById(nutritionistId)
                .orElseThrow(() -> new ResourceNotFoundException("Nutritionist not found with id: " + nutritionistId));
        if (nutritionist.getRole() != Role.NUTRITIONIST) {
            throw new BusinessRuleException("Referenced user is not a nutritionist.");
        }
        return nutritionist;
    }

    private User getAuthenticatedUser() {
        return authenticatedUserProvider.getAuthenticatedUser();
    }

    private void validateIdentifier(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
    }
}
