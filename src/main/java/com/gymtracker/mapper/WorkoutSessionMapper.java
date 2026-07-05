package com.gymtracker.mapper;

import com.gymtracker.dto.workout.WorkoutExerciseDTO;
import com.gymtracker.dto.workout.WorkoutSessionDetailDTO;
import com.gymtracker.dto.workout.WorkoutSessionRequestDTO;
import com.gymtracker.dto.workout.WorkoutSessionResponseDTO;
import com.gymtracker.dto.workout.WorkoutSessionSummaryDTO;
import com.gymtracker.dto.workout.WorkoutSetDTO;
import com.gymtracker.entity.Session;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for workout session conversions.
 */
@Mapper(componentModel = "spring", config = MapStructConfig.class, uses = ObjectIdMapper.class)
public interface WorkoutSessionMapper {

    WorkoutSessionResponseDTO toResponseDTO(Session entity);

    WorkoutSessionSummaryDTO toSummaryDTO(Session entity);

    WorkoutSessionDetailDTO toDetailDTO(Session entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "totalVolume", ignore = true)
    @Mapping(target = "estimatedOneRepMax", ignore = true)
    Session toEntity(WorkoutSessionRequestDTO requestDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "totalVolume", ignore = true)
    @Mapping(target = "estimatedOneRepMax", ignore = true)
    void updateEntityFromRequest(WorkoutSessionRequestDTO requestDTO, @MappingTarget Session entity);

    Session.CompletedExercise toCompletedExercise(WorkoutExerciseDTO dto);

    WorkoutExerciseDTO toWorkoutExerciseDTO(Session.CompletedExercise entity);

    Session.CompletedSet toCompletedSet(WorkoutSetDTO dto);

    WorkoutSetDTO toWorkoutSetDTO(Session.CompletedSet entity);
}
