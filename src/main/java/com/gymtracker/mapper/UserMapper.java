package com.gymtracker.mapper;

import com.gymtracker.dto.user.UserProfileDTO;
import com.gymtracker.dto.user.UserProfileUpdateDTO;
import com.gymtracker.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for generic user profile conversions.
 */
@Mapper(componentModel = "spring", config = MapStructConfig.class, uses = ObjectIdMapper.class)
public interface UserMapper {

    UserProfileDTO toProfileDTO(User entity);

    void updateEntityFromRequest(UserProfileUpdateDTO requestDTO, @MappingTarget User entity);
}
