package com.gymtracker.mapper;

import org.bson.types.ObjectId;
import org.mapstruct.Mapper;

/**
 * Shared conversion methods between ObjectId and String.
 */
@Mapper(componentModel = "spring")
public interface ObjectIdMapper {

    default String toString(ObjectId id) {
        return id == null ? null : id.toHexString();
    }

    default ObjectId toObjectId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return new ObjectId(id);
    }
}
