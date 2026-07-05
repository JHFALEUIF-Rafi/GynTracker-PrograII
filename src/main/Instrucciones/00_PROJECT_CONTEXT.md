# GymTracker - Project Context

## Purpose

GymTracker is a web application developed with Java, Spring Boot, Vaadin and MongoDB.

The objective is to provide a platform that connects three different professional roles involved in strength training:

- Athlete
- Coach
- Nutritionist

The system allows professionals to prescribe training and nutrition plans while athletes execute them and record their performance. The application must analyze historical data to help professionals make informed decisions.

This project follows Object-Oriented Programming principles and must maintain a clean software architecture.

---

# Main Problem

Current fitness applications generally work as digital notebooks.

They do not:

- analyze accumulated fatigue
- estimate progression
- validate training load
- connect coaches with nutritionists
- maintain role-based permissions

GymTracker solves these problems by integrating all participants into a single platform.

---

# Target Users

## Athlete

Responsibilities

- Register biometric information.
- View assigned nutrition plan.
- View assigned training program.
- Register every completed workout.
- Register RPE after each exercise.
- Consult personal progress.

Restrictions

- Cannot modify nutrition plans.
- Cannot modify training programs.
- Cannot dismiss fatigue alerts.

---

## Coach

Responsibilities

- Create exercises.
- Create training mesocycles.
- Assign routines.
- Monitor athlete performance.
- Receive fatigue alerts.
- Decide whether a Deload Week is necessary.

Restrictions

- Cannot modify nutrition plans.

---

## Nutritionist

Responsibilities

- Create nutrition plans.
- Modify nutrition plans.
- Update nutrition plans.
- Review athlete biometric information.

Restrictions

- Cannot create routines.
- Cannot modify workouts.

---

# Functional Modules

The application is divided into four independent modules.

## Module 1

Athlete Profile

Nutrition Plans

---

## Module 2

Exercise Library

Mesocycle Designer

---

## Module 3

Workout Execution

Progress Engine

Fatigue Analysis

1RM Estimation

---

## Module 4

Statistics

Reports

Dashboard

Progress Charts

---

# Business Rules

The following rules are mandatory.

## Nutrition

Only Nutritionists can create or edit nutrition plans.

Athletes have read-only access.

Coaches cannot modify nutrition plans.

---

## Training

Only Coaches can create exercises.

Only Coaches can create mesocycles.

The system never generates routines automatically.

The Coach is responsible for every training decision.

---

## Fatigue

The system analyzes historical workout information.

If accumulated fatigue exceeds a configurable threshold:

Generate a Deload Alert.

The system DOES NOT force a deload.

The Coach makes the final decision.

---

## Progress

The system estimates the athlete's One Rep Max (1RM).

The estimation is based on previous workout history.

Historical information must never be deleted automatically.

---

# Non Functional Requirements

The application must be:

- Modular
- Extensible
- Maintainable
- Scalable
- Responsive
- Easy to understand

---

# Software Design Principles

Every generated class must follow:

- Object-Oriented Programming
- SOLID
- DRY
- Single Responsibility Principle
- Dependency Injection

---

# Persistence

The application stores all information in MongoDB.

No information should be stored in memory as the primary source.

No local files should be used for persistence.

---

# Authentication

The application contains three user roles.

ATHLETE

COACH

NUTRITIONIST

Every screen must validate permissions before allowing operations.

---

# Goal for GitHub Copilot

Whenever generating code, always respect this document.

Never invent business rules.

Never change user permissions.

Never merge responsibilities between modules.

If something is unclear, prioritize modularity and maintainability over shortcuts.

Generate production-quality code whenever possible.