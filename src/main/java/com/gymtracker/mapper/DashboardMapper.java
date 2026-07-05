package com.gymtracker.mapper;

import com.gymtracker.dto.dashboard.ChartDTO;
import com.gymtracker.dto.dashboard.DashboardDTO;
import com.gymtracker.dto.dashboard.ProgressDTO;
import com.gymtracker.dto.dashboard.StatisticsDTO;
import com.gymtracker.entity.Session;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for dashboard-oriented projections from session data.
 */
@Mapper(componentModel = "spring", config = MapStructConfig.class, uses = ObjectIdMapper.class)
public interface DashboardMapper {

    @Mapping(source = "date", target = "lastWorkoutDate")
    @Mapping(source = "totalVolume", target = "trainingVolume")
    @Mapping(source = "estimatedOneRepMax", target = "estimatedOneRepMax")
    DashboardDTO toResponseDTO(Session entity);

    @Mapping(source = "totalVolume", target = "weeklyTrainingVolume")
    @Mapping(source = "totalVolume", target = "monthlyTrainingVolume")
    @Mapping(source = "durationMinutes", target = "averageWorkoutDuration")
    StatisticsDTO toSummaryDTO(Session entity);

    @Mapping(source = "estimatedOneRepMax", target = "strengthProgress")
    @Mapping(source = "totalVolume", target = "volumeProgress")
    ProgressDTO toDetailDTO(Session entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "athleteId", ignore = true)
    @Mapping(target = "mesocycleId", ignore = true)
    @Mapping(target = "date", source = "lastWorkoutDate")
    @Mapping(target = "durationMinutes", ignore = true)
    @Mapping(target = "completedExercises", ignore = true)
    @Mapping(target = "totalVolume", source = "trainingVolume")
    @Mapping(target = "estimatedOneRepMax", source = "estimatedOneRepMax")
    Session toEntity(DashboardDTO requestDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "athleteId", ignore = true)
    @Mapping(target = "mesocycleId", ignore = true)
    @Mapping(target = "date", source = "lastWorkoutDate")
    @Mapping(target = "durationMinutes", ignore = true)
    @Mapping(target = "completedExercises", ignore = true)
    @Mapping(target = "totalVolume", source = "trainingVolume")
    @Mapping(target = "estimatedOneRepMax", source = "estimatedOneRepMax")
    void updateEntityFromRequest(DashboardDTO requestDTO, @MappingTarget Session entity);

    @Mapping(target = "labels", ignore = true)
    @Mapping(target = "values", ignore = true)
    ChartDTO toChartDTO(Session entity);
}
