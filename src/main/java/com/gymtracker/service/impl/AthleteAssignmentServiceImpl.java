package com.gymtracker.service.impl;

import com.gymtracker.entity.Mesocycle;
import com.gymtracker.entity.NutritionPlan;
import com.gymtracker.repository.MesocycleRepository;
import com.gymtracker.repository.NutritionPlanRepository;
import com.gymtracker.service.AthleteAssignmentService;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

/**
 * Business implementation for coach/nutritionist-to-athlete assignment checks.
 */
@Service
public class AthleteAssignmentServiceImpl implements AthleteAssignmentService {

    private final MesocycleRepository mesocycleRepository;
    private final NutritionPlanRepository nutritionPlanRepository;

    public AthleteAssignmentServiceImpl(
            MesocycleRepository mesocycleRepository,
            NutritionPlanRepository nutritionPlanRepository
    ) {
        this.mesocycleRepository = mesocycleRepository;
        this.nutritionPlanRepository = nutritionPlanRepository;
    }

    @Override
    public boolean isAthleteAssignedToCoach(ObjectId coachId, ObjectId athleteId) {
        return mesocycleRepository.findByCoachId(coachId).stream()
                .anyMatch(mesocycle -> Objects.equals(mesocycle.getAthleteId(), athleteId));
    }

    @Override
    public boolean isAthleteAssignedToNutritionist(ObjectId nutritionistId, ObjectId athleteId) {
        return nutritionPlanRepository.findByNutritionistId(nutritionistId).stream()
                .anyMatch(plan -> Objects.equals(plan.getAthleteId(), athleteId));
    }

    @Override
    public Set<ObjectId> assignedAthleteIdsForCoach(ObjectId coachId) {
        return mesocycleRepository.findByCoachId(coachId).stream()
                .map(Mesocycle::getAthleteId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<ObjectId> assignedAthleteIdsForNutritionist(ObjectId nutritionistId) {
        return nutritionPlanRepository.findByNutritionistId(nutritionistId).stream()
                .map(NutritionPlan::getAthleteId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
