# GymTracker - Development Roadmap

## Objective

This document defines the exact implementation order for the GymTracker project.

GitHub Copilot must follow this roadmap step by step.

Never skip steps.

Never implement future modules before finishing the current one.

Each phase must compile successfully before moving to the next.

---

# Phase 1 - Project Initialization

## Step 1

Create the Spring Boot project using Maven.

Include the dependencies defined in 01_TECH_STACK.md.

---

## Step 2

Configure MongoDB connection.

Verify successful connection.

Do not continue until persistence works.

---

## Step 3

Configure Vaadin.

Verify that the default view loads successfully.

---

## Step 4

Create the complete package structure defined in 02_ARCHITECTURE.md.

Do not create additional packages.

---

## Step 5

Configure application properties.

Environment configuration.

Mongo configuration.

Logging configuration.

---

# Phase 2 - Domain Model

## Step 6

Create Enums

Role

Gender

ActivityLevel

ExerciseType

Difficulty

Equipment

MesocycleStatus

WorkoutStatus

AlertStatus

NutritionGoal

FatigueLevel

---

## Step 7

Create MongoDB Entities

User

NutritionPlan

Exercise

Mesocycle

Session

Alert

Only define fields and constructors.

No business logic.

---

## Step 8

Create DTO classes.

---

## Step 9

Create Mapper classes.

---

## Step 10

Create Repository interfaces.

All repositories extend MongoRepository.

---

# Phase 3 - Validation

## Step 11

Create validation package.

---

## Step 12

Create Validators.

AthleteValidator

ExerciseValidator

WorkoutValidator

NutritionValidator

MesocycleValidator

---

## Step 13

Test validation independently.

---

# Phase 4 - Service Layer

## Step 14

Create Service interfaces.

---

## Step 15

Create Service implementations.

Do not create Views yet.

---

## Step 16

Implement AthleteService.

---

## Step 17

Implement NutritionPlanService.

---

## Step 18

Implement ExerciseService.

---

## Step 19

Implement MesocycleService.

---

## Step 20

Implement WorkoutSessionService.

---

## Step 21

Implement OneRepMaxService.

---

## Step 22

Implement FatigueService.

---

## Step 23

Implement AlertService.

---

## Step 24

Implement DashboardService.

---

## Step 25

Implement StatisticsService.

---

## Step 26

Implement ReportService.

---

# Phase 5 - Authentication

## Step 27

Implement Login View.

---

## Step 28

Implement role verification.

---

## Step 29

Restrict navigation according to user role.

---

# Phase 6 - Athlete Module

## Step 30

Implement Athlete Profile View.

---

## Step 31

Implement Athlete Profile Form.

---

## Step 32

Implement Nutrition Plan Views.

---

## Step 33

Implement Nutrition History.

---

# Phase 7 - Coach Module

## Step 34

Implement Exercise CRUD.

---

## Step 35

Implement Exercise Catalog.

---

## Step 36

Implement Mesocycle CRUD.

---

## Step 37

Implement Mesocycle Assignment.

---

## Step 38

Implement Workload Validation.

---

# Phase 8 - Workout Module

## Step 39

Implement Workout Session View.

---

## Step 40

Implement Exercise Execution Components.

---

## Step 41

Implement Automatic Volume Calculation.

---

## Step 42

Implement Estimated One Rep Max calculation.

---

## Step 43

Implement Fatigue Analysis.

---

## Step 44

Implement Deload Alerts.

---

# Phase 9 - Dashboard

## Step 45

Implement Dashboard.

---

## Step 46

Implement Statistics.

---

## Step 47

Implement Charts.

---

## Step 48

Implement Workout History.

---

## Step 49

Implement Reports.

---

# Phase 10 - Finalization

## Step 50

Review permissions.

---

## Step 51

Review validations.

---

## Step 52

Review architecture.

---

## Step 53

Remove duplicated logic.

---

## Step 54

Improve code quality.

---

## Step 55

Refactor when necessary.

---

## Step 56

Test MongoDB persistence.

---

## Step 57

Review DTO usage.

---

## Step 58

Review Services.

---

## Step 59

Review Repositories.

---

## Step 60

Review Views.

---

## Step 61

Review responsive behavior.

---

## Step 62

Review notifications.

---

## Step 63

Review exception handling.

---

## Step 64

Review logging.

---

## Step 65

Execute complete application tests.

---

## Step 66

Verify every Functional Requirement (RF1–RF4).

---

## Step 67

Prepare project for delivery.

---

# Development Rules

Each phase must compile before starting the next.

Every commit should correspond to one or more completed steps.

Never implement business logic inside Views.

Never access repositories directly from Views.

Always implement unit-testable Services.

Every feature must be validated before moving forward.

---

# GitHub Copilot Rules

Follow this roadmap exactly.

Do not anticipate future phases.

Do not generate unrelated files.

Complete one step before suggesting the next.

Respect every previous document in the docs folder.

If a requested implementation conflicts with this roadmap, ask for clarification instead of making assumptions.