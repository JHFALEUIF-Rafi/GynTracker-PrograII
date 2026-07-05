package com.gymtracker.service;

import com.gymtracker.dto.nutrition.NutritionPlanDetailDTO;
import com.gymtracker.dto.nutrition.NutritionPlanRequestDTO;
import com.gymtracker.dto.nutrition.NutritionPlanResponseDTO;
import com.gymtracker.dto.nutrition.NutritionPlanSummaryDTO;
import java.util.List;

/**
 * Service contract for nutrition plan management operations.
 */
public interface NutritionPlanService {

    NutritionPlanResponseDTO createNutritionPlan(NutritionPlanRequestDTO requestDTO);

    NutritionPlanResponseDTO updateNutritionPlan(String planId, NutritionPlanRequestDTO requestDTO);

    NutritionPlanResponseDTO deactivateNutritionPlan(String planId);

    NutritionPlanDetailDTO getNutritionPlanById(String planId);

    NutritionPlanResponseDTO getActiveNutritionPlan(String athleteId);

    List<NutritionPlanSummaryDTO> getNutritionHistory(String athleteId);

    List<NutritionPlanSummaryDTO> getNutritionPlansByNutritionist(String nutritionistId);

    List<NutritionPlanSummaryDTO> searchNutritionPlans(String keyword);

    /**
     * Returns the nutrition plans visible to the authenticated caller: an
     * Athlete sees their own, a Nutritionist sees the ones they created, and
     * a Coach sees the full catalog (read-only).
     */
    List<NutritionPlanSummaryDTO> getNutritionPlansForCurrentUser();
}
