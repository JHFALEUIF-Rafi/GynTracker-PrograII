# GymTracker - Coding Standards

## Objective

This document defines the coding standards, development conventions and quality rules that GitHub Copilot must follow throughout the GymTracker project.

Every generated class, interface, method and package must comply with these standards.

---

# General Principles

Always prioritize:

- Readability
- Maintainability
- Scalability
- Reusability
- Simplicity

Every line of code should have a clear purpose.

---

# Clean Code

Follow Clean Code principles.

Classes should be small.

Methods should be short.

Avoid duplicated code.

Avoid unnecessary complexity.

Avoid deeply nested conditions.

Prefer early returns.

Avoid magic numbers.

Use constants whenever possible.

---

# SOLID Principles

Always apply:

S — Single Responsibility Principle

O — Open/Closed Principle

L — Liskov Substitution Principle

I — Interface Segregation Principle

D — Dependency Inversion Principle

---

# Naming Conventions

## Classes

PascalCase

Examples

AthleteService

WorkoutSession

NutritionPlan

AlertService

---

## Interfaces

Use descriptive names.

Examples

AthleteService

ExerciseService

AuthenticationService

Implementation classes must end with Impl.

Examples

AthleteServiceImpl

ExerciseServiceImpl

---

## Methods

camelCase

Examples

createAthlete()

registerWorkout()

calculateFatigue()

estimateOneRepMax()

generateAlert()

assignNutritionPlan()

---

## Variables

camelCase

Examples

currentWorkout

estimatedFatigue

totalVolume

nutritionPlan

coachId

---

## Constants

UPPER_SNAKE_CASE

Examples

MAX_RPE

DEFAULT_REST_TIME

MIN_WEIGHT

MAX_SETS

---

# Method Rules

Methods should perform only one task.

Avoid methods longer than 40 lines.

If a method becomes too large, extract private helper methods.

Prefer descriptive method names.

Avoid abbreviations.

Bad

calc()

Good

calculateEstimatedOneRepMax()

---

# Class Rules

Each class should have one responsibility.

Avoid God Classes.

Avoid Utility classes with unrelated methods.

Prefer composition instead of inheritance.

---

# Constructors

Prefer constructor injection.

Never instantiate dependencies using new inside Services.

Correct

public AthleteServiceImpl(AthleteRepository repository)

Incorrect

private AthleteRepository repository = new AthleteRepository();

---

# Dependency Injection

Always use Spring Dependency Injection.

Never use field injection.

Use constructor injection only.

---

# Repository Rules

Repositories must only perform database access.

Repositories must never contain:

Business rules

Validation

Calculations

Formatting

Logging logic

Repositories should extend MongoRepository.

---

# Service Rules

Business logic belongs only in Services.

Services are responsible for:

Validation

Calculations

Business rules

Repository communication

DTO conversion

Alert generation

Services must never communicate directly with Views.

---

# View Rules

Views are responsible only for:

Displaying information

Receiving user input

Calling Services

Showing notifications

Navigation

Views must never:

Access repositories

Perform calculations

Implement business rules

Manipulate MongoDB

---

# Entity Rules

Entities represent MongoDB documents.

Entities should not contain business logic.

Entities may contain:

Constructors

Getters

Setters

equals()

hashCode()

toString()

Simple helper methods

---

# DTO Rules

Never expose Entities directly to Views.

Always return DTOs.

DTOs should contain only transported information.

No business logic.

---

# Validation Rules

Always validate:

Null values

Negative numbers

Invalid dates

Invalid RPE

Invalid repetitions

Invalid calories

Invalid names

Invalid email

Invalid password

Never trust user input.

---

# Exception Handling

Never swallow exceptions.

Create custom exceptions when appropriate.

Examples

AthleteNotFoundException

ExerciseNotFoundException

MesocycleValidationException

FatigueCalculationException

Throw meaningful exceptions.

Never throw generic Exception.

---

# Logging

Use SLF4J.

Never use:

System.out.println()

System.err.println()

Use log levels appropriately.

INFO

WARN

ERROR

DEBUG

---

# Comments

Avoid unnecessary comments.

Code should explain itself.

Write comments only when explaining:

Complex business rules

Algorithms

Important decisions

Never comment obvious code.

---

# Formatting

Use consistent indentation.

Maximum line length:

120 characters.

One blank line between methods.

Group related methods together.

---

# Collections

Prefer

List

Set

Map

Avoid arrays unless strictly necessary.

Use Optional when appropriate.

Never return null collections.

Return empty collections instead.

---

# Java Features

Prefer

Streams

LocalDate

LocalDateTime

Records (only for immutable DTOs if appropriate)

Enums

Avoid legacy APIs.

---

# Enum Usage

Use enums instead of String whenever values are predefined.

Examples

Role

Gender

ExerciseType

AlertStatus

MesocycleStatus

NutritionGoal

---

# Database Rules

Never write MongoDB queries inside Views.

Never manipulate ObjectId manually.

Always use Repository methods.

Prefer derived query methods before custom queries.

---

# Performance

Avoid unnecessary database queries.

Reuse existing data whenever possible.

Avoid duplicated calculations.

Lazy load data when appropriate.

---

# Security

Never store passwords in plain text.

Passwords must always be encrypted.

Never expose internal IDs unnecessarily.

Always validate user permissions before executing sensitive actions.

---

# UI Rules

Every action must provide user feedback.

Show notifications after:

Create

Update

Delete

Validation errors

System errors

Loading indicators should be used when operations take noticeable time.

---

# Testing

Business logic should be testable.

Avoid tightly coupled classes.

Prefer dependency injection to facilitate unit testing.

---

# GitHub Copilot Rules

Whenever generating code:

Generate complete classes.

Do not leave TODO comments.

Do not generate placeholder methods.

Do not duplicate existing logic.

Always reuse existing services.

Always respect the project architecture.

Always follow SOLID.

Always generate production-ready code.

If two solutions are possible:

Choose the most maintainable one.

If a generated solution violates this document:

This document takes precedence.