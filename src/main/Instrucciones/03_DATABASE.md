# GymTracker - Database Design

## Objective

This document defines the complete MongoDB data model for GymTracker.

GitHub Copilot must follow this document when creating:

- Entities
- Repositories
- Services
- Relationships
- Queries

This document has priority over any generated design.

---

# Database

MongoDB

Database name:

gymtracker

---

# Collections

The application contains the following collections:

users

nutritionPlans

exercises

mesocycles

sessions

alerts

---

# Collection: users

Represents every authenticated user.

A user can have one of three roles.

ATHLETE

COACH

NUTRITIONIST

Document

{
    "_id": ObjectId,

    "role": "ATHLETE",

    "email": String,

    "password": String,

    "firstName": String,

    "lastName": String,

    "age": Integer,

    "gender": String,

    "height": Double,

    "weight": Double,

    "activityLevel": String,

    "enabled": Boolean,

    "createdAt": LocalDateTime,

    "updatedAt": LocalDateTime
}

---

Indexes

email (unique)

role

---

Business Rules

Email must be unique.

Password must always be encrypted.

Only ATHLETE stores biometric information.

COACH and NUTRITIONIST ignore athlete-specific fields.

---

# Collection: nutritionPlans

Each nutrition plan belongs to one athlete.

Created only by a Nutritionist.

Document

{
    "_id": ObjectId,

    "athleteId": ObjectId,

    "nutritionistId": ObjectId,

    "goal": String,

    "calories": Integer,

    "protein": Double,

    "carbohydrates": Double,

    "fat": Double,

    "observations": String,

    "startDate": LocalDate,

    "endDate": LocalDate,

    "active": Boolean,

    "createdAt": LocalDateTime
}

---

Indexes

athleteId

nutritionistId

active

---

Business Rules

Only one active nutrition plan per athlete.

Previous plans are never deleted.

They become inactive.

---

# Collection: exercises

Exercise catalog.

Document

{
    "_id": ObjectId,

    "name": String,

    "primaryMuscle": String,

    "secondaryMuscles": List<String>,

    "exerciseType": String,

    "description": String
}

---

Indexes

name

primaryMuscle

exerciseType

---

Business Rules

Exercise names must be unique.

Exercises are reusable.

Never duplicated.

---

# Collection: mesocycles

Training plans created by coaches.

Document

{
    "_id": ObjectId,

    "coachId": ObjectId,

    "athleteId": ObjectId,

    "name": String,

    "durationWeeks": Integer,

    "targetRPE": Integer,

    "notes": String,

    "status": String,

    "createdAt": LocalDateTime,

    "days": [

        {

            "dayName": String,

            "exercises": [

                {

                    "exerciseId": ObjectId,

                    "sets": Integer,

                    "repetitions": Integer,

                    "targetWeight": Double,

                    "targetRPE": Integer

                }

            ]

        }

    ]
}

---

Indexes

coachId

athleteId

status

---

Business Rules

Mesocycles are designed manually.

The system validates workload.

The system never generates routines automatically.

---

# Collection: sessions

Represents one completed workout.

Document

{
    "_id": ObjectId,

    "athleteId": ObjectId,

    "mesocycleId": ObjectId,

    "date": LocalDate,

    "durationMinutes": Integer,

    "completedExercises": [

        {

            "exerciseId": ObjectId,

            "sets": [

                {

                    "weight": Double,

                    "repetitions": Integer,

                    "rpe": Integer

                }

            ]

        }

    ],

    "totalVolume": Double,

    "estimatedOneRepMax": Double
}

---

Indexes

athleteId

date

mesocycleId

---

Business Rules

Sessions are immutable.

Sessions are never edited after completion.

Historical information must always be preserved.

---

# Collection: alerts

Stores fatigue notifications.

Document

{
    "_id": ObjectId,

    "athleteId": ObjectId,

    "coachId": ObjectId,

    "type": "DELOAD",

    "message": String,

    "status": "ACTIVE",

    "generatedAt": LocalDateTime,

    "reviewedAt": LocalDateTime
}

---

Indexes

coachId

athleteId

status

---

Business Rules

Only the Coach may close an alert.

Athletes have read-only access.

Alerts remain stored for historical analysis.

---

# Relationships

users

↓

nutritionPlans

One Athlete

↓

Many Nutrition Plans

users

↓

mesocycles

One Coach

↓

Many Mesocycles

users

↓

sessions

One Athlete

↓

Many Sessions

users

↓

alerts

One Coach

↓

Many Alerts

---

# Reference Strategy

Use references (ObjectId)

Do not duplicate users.

Embed only small structures.

Examples

Workout Sets

Workout Days

Exercise Lists

---

# Data Integrity Rules

Never delete historical sessions.

Never delete historical nutrition plans.

Never delete fatigue alerts.

Prefer logical deletion instead of physical deletion.

---

# Validation Rules

Weight > 0

Height > 0

Calories > 0

Protein >= 0

Fat >= 0

Carbohydrates >= 0

RPE

1 to 10

Duration

Greater than zero

Weeks

Greater than zero

---

# MongoDB Annotations

GitHub Copilot should use

@Document

@Id

@Indexed

@Field

@DBRef only when necessary.

Prefer embedded documents whenever possible.

---

# Repository Rules

Every collection has one Repository.

UserRepository

NutritionPlanRepository

ExerciseRepository

MesocycleRepository

SessionRepository

AlertRepository

All repositories extend

MongoRepository

---

# Future Collections

notifications

auditLogs

applicationSettings

These collections are optional and should not be implemented until required.

---

# Final Rule

This database model is the official persistence model for GymTracker.

GitHub Copilot must not create additional collections or modify relationships unless explicitly requested.