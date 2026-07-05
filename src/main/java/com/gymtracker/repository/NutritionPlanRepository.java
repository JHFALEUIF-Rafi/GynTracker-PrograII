package com.gymtracker.repository;

import com.gymtracker.entity.NutritionPlan;
import com.gymtracker.enums.NutritionGoal;
import java.util.Collection;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

/**
 * Repository for nutrition plan persistence operations.
 */
public interface NutritionPlanRepository extends MongoRepository<NutritionPlan, String> {

    List<NutritionPlan> findByAthleteId(ObjectId athleteId);

    List<NutritionPlan> findByAthleteIdIn(Collection<ObjectId> athleteIds);

    @Query("{ 'active': ?0 }")
    List<NutritionPlan> findByStatus(Boolean status);

    @Query("{ 'goal': ?0 }")
    List<NutritionPlan> findByNutritionGoal(NutritionGoal nutritionGoal);

    List<NutritionPlan> findByNutritionistId(ObjectId nutritionistId);
}
