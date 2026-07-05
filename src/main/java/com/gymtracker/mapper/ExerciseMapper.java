package com.gymtracker.mapper;

import com.gymtracker.dto.exercise.ExerciseDetailDTO;
import com.gymtracker.dto.exercise.ExerciseRequestDTO;
import com.gymtracker.dto.exercise.ExerciseResponseDTO;
import com.gymtracker.dto.exercise.ExerciseSummaryDTO;
import com.gymtracker.entity.Exercise;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for exercise conversions.
 */
@Mapper(componentModel = "spring", config = MapStructConfig.class, uses = ObjectIdMapper.class)
public interface ExerciseMapper {

    ExerciseResponseDTO toResponseDTO(Exercise entity);

    ExerciseSummaryDTO toSummaryDTO(Exercise entity);

    ExerciseDetailDTO toDetailDTO(Exercise entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Exercise toEntity(ExerciseRequestDTO requestDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(ExerciseRequestDTO requestDTO, @MappingTarget Exercise entity);
}
