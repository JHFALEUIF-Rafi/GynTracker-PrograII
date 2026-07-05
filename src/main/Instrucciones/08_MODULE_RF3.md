# GymTracker - Module RF3
# Progress Engine, Fatigue Analysis & Performance Tracking

## Objective

This module is responsible for tracking athlete performance, recording completed workouts, estimating strength progression and detecting accumulated fatigue.

The system analyzes workout history but never makes training decisions automatically.

Only the Coach can decide how to adjust an athlete's training plan.

---

# Functional Requirements

RF3.1

Workout Session Registration

RF3.2

Progress Engine

Fatigue Analysis

Estimated One Rep Max (1RM)

Deload Alert Generation

---

# User Permissions

## ATHLETE

Can

- Start a workout session
- Register completed sets
- Register weight
- Register repetitions
- Register RPE
- Finish workout
- View own statistics

Cannot

- Modify historical sessions
- Delete sessions
- Ignore fatigue alerts
- Modify estimated 1RM

---

## COACH

Can

- View every athlete session
- View progress history
- View estimated 1RM
- View fatigue analysis
- Receive Deload alerts
- Review historical performance

Cannot

- Modify completed workout sessions

---

## NUTRITIONIST

Can

- View athlete performance

Cannot

- Register workouts
- Modify sessions
- Generate alerts

---

# Required Views

WorkoutSessionView

WorkoutExecutionView

WorkoutHistoryView

PerformanceView

FatigueAnalysisView

DeloadAlertView

---

# Required Components

WorkoutCard

ExerciseExecutionCard

SetInputComponent

WorkoutSummaryCard

FatigueIndicator

OneRepMaxCard

ProgressCard

AlertCard

---

# Required Services

WorkoutSessionService

ProgressService

FatigueService

OneRepMaxService

AlertService

---

# Required Repositories

SessionRepository

AlertRepository

MesocycleRepository

UserRepository

---

# Required DTOs

WorkoutSessionDTO

WorkoutExerciseDTO

WorkoutSetDTO

FatigueDTO

OneRepMaxDTO

AlertDTO

ProgressDTO

---

# Required Validators

WorkoutValidator

SetValidator

FatigueValidator

---

# Workout Session

Each session contains

Athlete

Mesocycle

Date

Duration

Exercises

Total Volume

Estimated 1RM

Fatigue Score

Completion Status

---

# Exercise Execution

Each exercise contains

Exercise

Sets

Execution Order

Target Weight

Actual Weight

Target Repetitions

Actual Repetitions

Target RPE

Actual RPE

---

# Set

Each completed set contains

Weight

Repetitions

RPE

Completion Time

---

# Session Status

Enum

STARTED

IN_PROGRESS

COMPLETED

CANCELLED

---

# Workout Registration

The athlete registers

Weight

Repetitions

RPE

After each completed set.

The system automatically updates

Total Volume

Estimated 1RM

Workout Progress

---

# Volume Calculation

Training Volume

Weight × Repetitions

Session Volume

Sum of all completed sets

Mesocycle Volume

Sum of all completed sessions

---

# One Rep Max

The system estimates the current One Rep Max.

Recommended formula

Epley Formula

Estimated1RM = Weight × (1 + Repetitions / 30)

The formula must be isolated inside OneRepMaxService.

Never calculate inside Views.

---

# Fatigue Analysis

The fatigue engine analyzes

Average RPE

Training Volume

Training Frequency

Session History

Recent Performance

The analysis produces

Fatigue Score

Fatigue Level

Recommendation

---

# Fatigue Levels

Enum

LOW

MODERATE

HIGH

CRITICAL

---

# Deload Alert

When accumulated fatigue exceeds the configured threshold

Generate

DELOAD ALERT

The alert contains

Athlete

Coach

Fatigue Score

Reason

Creation Date

Status

The system never applies a deload automatically.

Only the Coach decides.

---

# Alert Status

Enum

ACTIVE

ACKNOWLEDGED

RESOLVED

---

# Business Rules

Workout sessions cannot be edited after completion.

Workout history must always be preserved.

Sessions cannot be deleted.

The system automatically calculates

Volume

Estimated 1RM

Fatigue Score

The system never modifies athlete routines.

Only the Coach can review fatigue alerts.

---

# Validation Rules

Weight

Greater than zero

Repetitions

Greater than zero

RPE

Between 1 and 10

Workout Duration

Greater than zero

Every session must contain at least one completed exercise.

---

# Dashboard Information

Display

Current Estimated 1RM

Current Fatigue Level

Current Training Volume

Last Workout

Weekly Sessions

Monthly Progress

Open Deload Alerts

---

# Search

Workout History

Date

Exercise

Mesocycle

Coach

---

# Filters

Workout History

Week

Month

Year

Exercise

Mesocycle

---

# Notifications

Workout Started

Workout Saved

Workout Completed

Fatigue Alert Generated

Estimated 1RM Updated

Validation Error

---

# Future Compatibility

Velocity Based Training (VBT)

Heart Rate Integration

Wearable Devices

Automatic Recovery Analysis

Artificial Intelligence Recommendations

Cloud Synchronization

These features should NOT be implemented now.

---

# Acceptance Criteria

The module is complete only if

Athletes can register workouts.

Workout volume is calculated automatically.

Estimated 1RM is calculated automatically.

Fatigue analysis works correctly.

Deload alerts are generated.

Historical sessions are preserved.

Views are functional.

MongoDB persistence works correctly.

Business rules are respected.

---

# GitHub Copilot Rules

Never calculate business values inside Views.

All calculations belong to Services.

OneRepMaxService is responsible only for 1RM estimation.

FatigueService is responsible only for fatigue analysis.

AlertService is responsible only for alert generation.

Repositories only access MongoDB.

Never duplicate calculation logic.

All formulas must be centralized.

Generate modular and reusable code.

Respect every architecture document.

Generate production-ready code.

Do not leave TODO comments.

Do not generate placeholder implementations.