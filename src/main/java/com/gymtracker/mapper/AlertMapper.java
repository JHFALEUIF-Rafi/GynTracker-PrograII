package com.gymtracker.mapper;

import com.gymtracker.dto.alert.AlertDTO;
import com.gymtracker.dto.alert.AlertSummaryDTO;
import com.gymtracker.entity.Alert;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for alert conversions.
 */
@Mapper(componentModel = "spring", config = MapStructConfig.class, uses = ObjectIdMapper.class)
public interface AlertMapper {

    AlertDTO toResponseDTO(Alert entity);

    AlertSummaryDTO toSummaryDTO(Alert entity);

    AlertDTO toDetailDTO(Alert entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "generatedAt", ignore = true)
    Alert toEntity(AlertDTO requestDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "generatedAt", ignore = true)
    void updateEntityFromRequest(AlertDTO requestDTO, @MappingTarget Alert entity);
}
