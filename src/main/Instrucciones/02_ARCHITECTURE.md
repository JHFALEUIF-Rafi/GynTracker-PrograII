# GymTracker - Software Architecture

## Objective

This document defines the software architecture of GymTracker.

GitHub Copilot must respect this architecture when generating code.

No class should violate the responsibilities defined here.

---

# Architectural Style

The application follows a layered architecture.

Presentation Layer

↓

Business Layer

↓

Persistence Layer

↓

MongoDB

Each layer has a single responsibility.

No layer should bypass another.

---

# Project Structure

com.gymtracker

│

├── config
│
├── entity
│
├── repository
│
├── service
│
├── service.impl
│
├── dto
│
├── mapper
│
├── validation
│
├── exception
│
├── util
│
├── security
│
├── view
│
├── component
│
└── GymTrackerApplication.java

---

# Layer Responsibilities

## View Layer

Package

view

Responsibilities

Display information.

Receive user input.

Show notifications.

Navigate between screens.

Never implement business logic.

Never access MongoDB directly.

Never call repositories.

Views may only communicate with Services.

---

## Component Layer

Package

component

Contains reusable UI components.

Examples

Navigation Bar

Sidebar

Header

Dialogs

Custom Forms

Charts

Cards

Menus

Components must not contain business logic.

---

## Service Layer

Package

service

Contains interfaces.

Defines business operations.

No implementation should exist here.

Example

AthleteService

CoachService

WorkoutService

---

## Service Implementation

Package

service.impl

Contains the implementation of every service.

Responsibilities

Business rules

Calculations

Validation calls

Repository communication

DTO conversion

Notification triggers

Services may communicate with multiple repositories.

---

## Repository Layer

Package

repository

Every repository must extend

MongoRepository

Repositories only perform persistence operations.

Repositories must never contain business logic.

---

## Entity Layer

Package

entity

Contains MongoDB documents.

Each entity represents one collection.

Entities should contain

Fields

Constructors

Getters

Setters

Basic helper methods

No business calculations.

---

## DTO Layer

Package

dto

DTOs transport data between layers.

Views should receive DTOs instead of Entities.

DTOs should not contain business logic.

---

## Mapper Layer

Package

mapper

Responsible for converting

Entity

↓

DTO

and

DTO

↓

Entity

Mapping code should never exist inside Views.

---

## Validation Layer

Package

validation

Contains reusable validators.

Examples

WeightValidator

RPEValidator

NutritionValidator

MesocycleValidator

Validation should be centralized.

---

## Exception Layer

Package

exception

Contains custom exceptions.

Examples

AthleteNotFoundException

CoachNotFoundException

WorkoutValidationException

NutritionPlanException

FatigueAnalysisException

---

## Utility Layer

Package

util

Contains helper classes.

Examples

DateUtils

CalculationUtils

StringUtils

UnitConverter

Utility classes must not contain business rules.

---

## Config Layer

Package

config

Contains Spring configuration.

Mongo configuration

Vaadin configuration

Application configuration

Bean configuration

---

## Security Layer

Package

security

Reserved for authentication.

Authorization

Role management

Password encryption

Login

Access control

Although authentication may be implemented later, the architecture must already support it.

---

# Dependency Rules

Allowed

View

↓

Service

↓

Repository

↓

MongoDB

Forbidden

Repository → View

Repository → Service

Entity → Repository

View → Repository

View → MongoDB

View → Entity

Service → View

---

# Request Flow

Every user action should follow this sequence.

User

↓

Vaadin View

↓

Service

↓

Validation

↓

Repository

↓

MongoDB

↓

Repository

↓

Service

↓

DTO

↓

View

↓

User

No shortcuts are allowed.

---

# MongoDB Collections

The initial collections are

users

exercises

nutritionPlans

mesocycles

sessions

alerts

Future collections may include

notifications

auditLogs

systemConfiguration

---

# Service Responsibilities

AthleteService

Manage athlete profiles.

CoachService

Manage coach operations.

NutritionService

Manage nutrition plans.

ExerciseService

Manage exercise catalog.

MesocycleService

Manage training programs.

SessionService

Register workout sessions.

FatigueService

Calculate fatigue.

Estimate One Rep Max.

Generate Deload Alerts.

DashboardService

Generate statistics.

Generate reports.

Build dashboard information.

---

# View Responsibilities

LoginView

Authentication.

DashboardView

System home page.

AthleteView

Athlete operations.

CoachView

Coach operations.

NutritionistView

Nutrition management.

WorkoutView

Workout registration.

ExerciseView

Exercise catalog.

StatisticsView

Charts.

Reports.

Progress.

ProfileView

User information.

---

# Communication Rules

Views communicate only with Services.

Services communicate with:

Repositories

Validators

Mappers

Utilities

Repositories communicate only with MongoDB.

Entities never communicate directly with Services.

---

# Business Rule Ownership

Business logic belongs only inside Services.

Examples

Calculate 1RM

Calculate Fatigue

Generate Deload

Validate Mesocycle

Assign Nutrition Plan

Assign Coach

Never inside Views.

Never inside Repositories.

---

# Scalability

The architecture must allow future implementation of

REST API

JWT Authentication

Mobile Application

AI Recommendation Engine

Cloud Deployment

Without major modifications.

---

# Code Quality

Every class should have one responsibility.

Avoid duplicated logic.

Favor composition.

Keep methods short.

Avoid God Classes.

Avoid circular dependencies.

---

# GitHub Copilot Rules

Whenever generating code

Respect every package.

Respect every layer.

Do not skip layers.

Do not merge responsibilities.

Always generate modular code.

Always generate reusable code.

Always generate maintainable code.

If there is any conflict between simplicity and architecture,

Architecture always wins.