package com.gymtracker.mapper;

import com.gymtracker.dto.nutrition.NutritionPlanDetailDTO;
import com.gymtracker.dto.nutrition.NutritionPlanRequestDTO;
import com.gymtracker.dto.nutrition.NutritionPlanResponseDTO;
import com.gymtracker.dto.nutrition.NutritionPlanSummaryDTO;
import com.gymtracker.entity.NutritionPlan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for nutrition plan conversions.
 */
@Mapper(componentModel = "spring", config = MapStructConfig.class, uses = ObjectIdMapper.class)
public interface NutritionPlanMapper {

    NutritionPlanResponseDTO toResponseDTO(NutritionPlan entity);

    NutritionPlanSummaryDTO toSummaryDTO(NutritionPlan entity);

    NutritionPlanDetailDTO toDetailDTO(NutritionPlan entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    NutritionPlan toEntity(NutritionPlanRequestDTO requestDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(NutritionPlanRequestDTO requestDTO, @MappingTarget NutritionPlan entity);
}
