package com.gymtracker.service;

import java.util.Set;
import org.bson.types.ObjectId;

/**
 * Answers whether a given athlete is actually assigned to a given coach or
 * nutritionist. An athlete is assigned to a coach if the coach has created a
 * mesocycle for them; an athlete is assigned to a nutritionist if the
 * nutritionist has created a nutrition plan for them. Every module that
 * exposes per-athlete data to Coach/Nutritionist roles enforces this same
 * assignment check before granting read access.
 */
public interface AthleteAssignmentService {

    boolean isAthleteAssignedToCoach(ObjectId coachId, ObjectId athleteId);

    boolean isAthleteAssignedToNutritionist(ObjectId nutritionistId, ObjectId athleteId);

    Set<ObjectId> assignedAthleteIdsForCoach(ObjectId coachId);

    Set<ObjectId> assignedAthleteIdsForNutritionist(ObjectId nutritionistId);
}
