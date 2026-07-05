# GymTracker - Module RF2
# Exercise Library & Mesocycle Management

## Objective

This document defines the complete implementation of Module RF2.

GitHub Copilot must implement this module exactly as specified.

The module manages:

- Exercise Library
- Mesocycle Design
- Workout Planning
- Athlete Assignment
- Training Validation

This module is exclusively managed by users with the COACH role.

---

# Functional Requirements

RF2.1

Exercise Library

RF2.2

Mesocycle Design and Assignment

---

# User Permissions

ATHLETE

Can

View assigned mesocycle

View exercises

View training days

Cannot

Create exercises

Modify exercises

Delete exercises

Create mesocycles

Assign mesocycles

---

COACH

Can

Create exercises

Edit exercises

Deactivate exercises

Create mesocycles

Edit mesocycles

Assign mesocycles

Duplicate mesocycles

Review workload validation

---

NUTRITIONIST

Can

View athlete information

Cannot

Access exercise management

Modify mesocycles

Assign routines

---

# Required Views

ExerciseView

ExerciseFormView

MesocycleView

MesocycleFormView

MesocycleAssignmentView

ExerciseCatalogView

TrainingCalendarView

---

# Required Components

ExerciseCard

ExerciseSelector

WorkoutDayComponent

WorkoutExerciseComponent

MesocycleSummaryCard

ValidationDialog

ExerciseFilterComponent

---

# Required Services

ExerciseService

MesocycleService

WorkoutValidationService

---

# Required Repositories

ExerciseRepository

MesocycleRepository

UserRepository

---

# Required DTOs

ExerciseDTO

ExerciseRequestDTO

ExerciseResponseDTO

MesocycleDTO

MesocycleRequestDTO

WorkoutDayDTO

WorkoutExerciseDTO

---

# Required Mappers

ExerciseMapper

MesocycleMapper

---

# Required Validators

ExerciseValidator

MesocycleValidator

WorkoutValidator

---

# Exercise

Each exercise contains

Name

Primary Muscle

Secondary Muscles

Exercise Type

Description

Equipment

Difficulty

Status

Created Date

Updated Date

---

# Exercise Type

Use Enum

STRENGTH

CARDIO

BODYWEIGHT

MOBILITY

---

# Difficulty

Use Enum

BEGINNER

INTERMEDIATE

ADVANCED

---

# Equipment

Use Enum

BARBELL

DUMBBELL

MACHINE

BODYWEIGHT

CABLE

SMITH_MACHINE

KETTLEBELL

OTHER

---

# Mesocycle

Each mesocycle contains

Name

Coach

Assigned Athlete

Duration

Weeks

Training Days

Target RPE

Notes

Status

Creation Date

---

# Mesocycle Status

Enum

DRAFT

ACTIVE

COMPLETED

ARCHIVED

---

# Workout Day

Each workout day contains

Day Name

Exercises

Notes

Estimated Duration

---

# Workout Exercise

Contains

Exercise

Sets

Repetitions

Target Weight

Target RPE

Rest Time

Execution Order

---

# CRUD Operations

Exercise

Create

Read

Update

Deactivate

Search

Filter

Mesocycle

Create

Read

Update

Duplicate

Assign

Archive

History

Never permanently delete.

---

# Search

Exercises

Name

Primary Muscle

Exercise Type

Equipment

Difficulty

Status

---

# Filters

Mesocycles

Coach

Athlete

Status

Date

Duration

---

# Workload Validation

Before assigning a mesocycle the system must validate

Repeated muscle groups

Weekly volume

Consecutive training days

Maximum configured workload

Repeated exercises

Target RPE consistency

---

# Validation Rules

Exercise Name

Required

Unique

Primary Muscle

Required

Sets

Greater than zero

Repetitions

Greater than zero

Target Weight

Greater than or equal to zero

Target RPE

Between 1 and 10

Duration

Greater than zero

Rest Time

Greater than zero

---

# Business Rules

Only Coaches may create exercises.

Only Coaches may modify exercises.

Only Coaches may assign mesocycles.

Exercises cannot be deleted if they are being used by a mesocycle.

Inactive exercises remain stored.

Mesocycles remain stored permanently.

Historical mesocycles must never be deleted.

The system validates workload before assignment.

The system never generates training plans automatically.

The Coach is always responsible for the final decision.

---

# Assignment Rules

One Athlete

One Active Mesocycle

Creating a new active mesocycle automatically archives the previous active mesocycle.

---

# Notifications

Exercise created

Exercise updated

Exercise deactivated

Mesocycle created

Mesocycle updated

Mesocycle assigned

Validation warning

Assignment successful

---

# Dashboard Information

Total Exercises

Active Mesocycles

Athletes Assigned

Average Weekly Volume

Validation Warnings

Pending Assignments

---

# Future Compatibility

Exercise Images

Exercise Videos

Exercise Categories

Exercise Favorites

Routine Templates

Copy Previous Mesocycle

Calendar Integration

These features should NOT be implemented now.

---

# Acceptance Criteria

The module is complete only if

Exercises can be managed.

Mesocycles can be created.

Mesocycles can be assigned.

Validation works correctly.

Only Coaches have write permissions.

Athletes have read-only access.

Historical information is preserved.

MongoDB persistence works correctly.

Views are fully functional.

---

# GitHub Copilot Rules

Business logic belongs only to Services.

Views never access repositories.

Repositories never contain business logic.

Validation belongs to Validators.

Exercise names must be unique.

Every mesocycle must belong to exactly one coach.

Every active mesocycle must belong to exactly one athlete.

Generate modular and reusable code.

Respect the architecture documents.

Respect MongoDB design.

Respect Coding Standards.

Generate production-ready code.

Do not leave TODO comments.

Do not create placeholder implementations.