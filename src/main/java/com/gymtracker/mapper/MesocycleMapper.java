package com.gymtracker.mapper;

import com.gymtracker.dto.mesocycle.MesocycleDetailDTO;
import com.gymtracker.dto.mesocycle.MesocycleRequestDTO;
import com.gymtracker.dto.mesocycle.MesocycleResponseDTO;
import com.gymtracker.dto.mesocycle.MesocycleSummaryDTO;
import com.gymtracker.dto.mesocycle.MesocycleWorkoutDayDTO;
import com.gymtracker.dto.mesocycle.MesocycleWorkoutExerciseDTO;
import com.gymtracker.entity.Mesocycle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for mesocycle conversions.
 */
@Mapper(componentModel = "spring", config = MapStructConfig.class, uses = ObjectIdMapper.class)
public interface MesocycleMapper {

    @Mapping(source = "targetRPE", target = "targetRpe")
    MesocycleResponseDTO toResponseDTO(Mesocycle entity);

    MesocycleSummaryDTO toSummaryDTO(Mesocycle entity);

    @Mapping(source = "targetRPE", target = "targetRpe")
    MesocycleDetailDTO toDetailDTO(Mesocycle entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(source = "targetRpe", target = "targetRPE")
    Mesocycle toEntity(MesocycleRequestDTO requestDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(source = "targetRpe", target = "targetRPE")
    void updateEntityFromRequest(MesocycleRequestDTO requestDTO, @MappingTarget Mesocycle entity);

    @Mapping(source = "targetRPE", target = "targetRpe")
    MesocycleWorkoutExerciseDTO toWorkoutExerciseDTO(Mesocycle.WorkoutExercise entity);

    @Mapping(source = "targetRpe", target = "targetRPE")
    Mesocycle.WorkoutExercise toWorkoutExercise(MesocycleWorkoutExerciseDTO dto);

    MesocycleWorkoutDayDTO toWorkoutDayDTO(Mesocycle.WorkoutDay entity);

    Mesocycle.WorkoutDay toWorkoutDay(MesocycleWorkoutDayDTO dto);
}
