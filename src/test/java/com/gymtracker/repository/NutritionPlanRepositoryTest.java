package com.gymtracker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymtracker.entity.NutritionPlan;
import com.gymtracker.enums.NutritionGoal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

/**
 * Repository test verifying NutritionPlan persistence and derived/@Query
 * methods against a real MongoDB instance (test database).
 */
@DataMongoTest
class NutritionPlanRepositoryTest {

    @Autowired
    private NutritionPlanRepository nutritionPlanRepository;

    @AfterEach
    void cleanUp() {
        nutritionPlanRepository.deleteAll();
    }

    private NutritionPlan buildPlan(ObjectId athleteId, ObjectId nutritionistId, NutritionGoal goal, boolean active) {
        return NutritionPlan.builder()
                .athleteId(athleteId)
                .nutritionistId(nutritionistId)
                .goal(goal)
                .calories(2500)
                .protein(180.0)
                .carbohydrates(250.0)
                .fat(70.0)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().plusDays(20))
                .active(active)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void findsPlansByAthleteId() {
        ObjectId athleteId = new ObjectId();
        nutritionPlanRepository.save(buildPlan(athleteId, new ObjectId(), NutritionGoal.CUTTING, true));
        nutritionPlanRepository.save(buildPlan(new ObjectId(), new ObjectId(), NutritionGoal.BULKING, true));

        List<NutritionPlan> results = nutritionPlanRepository.findByAthleteId(athleteId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getGoal()).isEqualTo(NutritionGoal.CUTTING);
    }

    @Test
    void findsPlansByStatus() {
        nutritionPlanRepository.save(buildPlan(new ObjectId(), new ObjectId(), NutritionGoal.MAINTENANCE, true));
        nutritionPlanRepository.save(buildPlan(new ObjectId(), new ObjectId(), NutritionGoal.MAINTENANCE, false));

        List<NutritionPlan> activePlans = nutritionPlanRepository.findByStatus(true);
        List<NutritionPlan> inactivePlans = nutritionPlanRepository.findByStatus(false);

        assertThat(activePlans).hasSize(1);
        assertThat(inactivePlans).hasSize(1);
    }

    @Test
    void findsPlansByNutritionGoal() {
        nutritionPlanRepository.save(buildPlan(new ObjectId(), new ObjectId(), NutritionGoal.BULKING, true));
        nutritionPlanRepository.save(buildPlan(new ObjectId(), new ObjectId(), NutritionGoal.CUTTING, true));

        List<NutritionPlan> bulkingPlans = nutritionPlanRepository.findByNutritionGoal(NutritionGoal.BULKING);

        assertThat(bulkingPlans).hasSize(1);
    }

    @Test
    void findsPlansByNutritionistId() {
        ObjectId nutritionistId = new ObjectId();
        nutritionPlanRepository.save(buildPlan(new ObjectId(), nutritionistId, NutritionGoal.MAINTENANCE, true));
        nutritionPlanRepository.save(buildPlan(new ObjectId(), new ObjectId(), NutritionGoal.MAINTENANCE, true));

        List<NutritionPlan> results = nutritionPlanRepository.findByNutritionistId(nutritionistId);

        assertThat(results).hasSize(1);
    }
}
