package com.gymtracker.mapper;

import com.gymtracker.dto.athlete.AthleteDetailDTO;
import com.gymtracker.dto.athlete.AthleteRequestDTO;
import com.gymtracker.dto.athlete.AthleteResponseDTO;
import com.gymtracker.dto.athlete.AthleteSummaryDTO;
import com.gymtracker.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for athlete-related conversions.
 */
@Mapper(componentModel = "spring", config = MapStructConfig.class, uses = ObjectIdMapper.class)
public interface AthleteMapper {

    AthleteResponseDTO toResponseDTO(User entity);

    AthleteSummaryDTO toSummaryDTO(User entity);

    AthleteDetailDTO toDetailDTO(User entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(AthleteRequestDTO requestDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(AthleteRequestDTO requestDTO, @MappingTarget User entity);
}
