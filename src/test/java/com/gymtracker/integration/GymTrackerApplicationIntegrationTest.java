package com.gymtracker.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * Full application context integration test: verifies every bean (services,
 * repositories, mappers, validators, security configuration, Vaadin
 * auto-configuration) wires together correctly against the real MongoDB test
 * database.
 */
@SpringBootTest
class GymTrackerApplicationIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void applicationContextLoadsSuccessfully() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void coreServiceBeansAreRegistered() {
        assertThat(applicationContext.getBean(com.gymtracker.service.AuthenticationService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.gymtracker.service.UserService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.gymtracker.service.AthleteService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.gymtracker.service.ExerciseService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.gymtracker.service.MesocycleService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.gymtracker.service.WorkoutSessionService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.gymtracker.service.NutritionPlanService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.gymtracker.service.AlertService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.gymtracker.service.DashboardService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.gymtracker.service.StatisticsService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.gymtracker.service.ReportService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.gymtracker.service.FatigueService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.gymtracker.service.OneRepMaxService.class)).isNotNull();
    }

    @Test
    void securityBeansAreRegistered() {
        assertThat(applicationContext.getBean(org.springframework.security.crypto.password.PasswordEncoder.class)).isNotNull();
        assertThat(applicationContext.getBean(com.gymtracker.security.CustomUserDetailsService.class)).isNotNull();
        assertThat(applicationContext.getBean(org.springframework.security.authentication.AuthenticationManager.class)).isNotNull();
    }
}
