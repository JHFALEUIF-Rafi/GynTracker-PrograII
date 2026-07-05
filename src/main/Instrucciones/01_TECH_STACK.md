# GymTracker - Technology Stack

## Objective

This document defines every technology, framework, library, architectural decision and coding convention that must be used throughout the GymTracker project.

GitHub Copilot must strictly follow these specifications.

---

# Programming Language

Java 21

Only Java must be used for backend and business logic.

---

# Framework

Spring Boot 3.x

The application must use Spring Boot as the main framework.

Responsibilities:

- Dependency Injection
- Bean Management
- Configuration
- REST support (if needed)
- MongoDB integration
- Security integration
- Validation

---

# Frontend

Vaadin 24

Vaadin is the ONLY frontend technology allowed.

The interface must be developed completely with Java.

Do NOT use:

- HTML pages
- JSP
- Thymeleaf
- Angular
- React
- Vue
- JavaScript frameworks

Only use Vaadin components.

---

# Database

MongoDB

The application must use MongoDB as the only persistence engine.

Use:

Spring Data MongoDB

Never use relational databases.

Forbidden:

MySQL

MariaDB

SQLite

PostgreSQL

Oracle

SQL Server

H2

---

# Build Tool

Maven

Dependency management must be handled using Maven.

Do not use Gradle.

---

# Required Spring Dependencies

The project should include at least:

- Spring Web
- Spring Data MongoDB
- Spring Boot Validation
- Spring Security (optional for future implementation)
- Lombok (optional)
- Vaadin
- DevTools

---

# Recommended Maven Structure

src/

main/

java/

resources/

test/

---

# Packages

The project should follow this package organization.

com.gymtracker

│

├── config

├── entity

├── repository

├── service

├── service.impl

├── dto

├── mapper

├── validation

├── security

├── util

├── exception

├── view

├── component

└── GymTrackerApplication

No additional packages should be created without a valid reason.

---

# Design Pattern

The project must follow these patterns.

MVC

Repository Pattern

Service Layer Pattern

Dependency Injection

Factory Pattern (only if necessary)

Builder Pattern (only if necessary)

Avoid Singleton implementations unless managed by Spring.

---

# Object-Oriented Principles

Always apply:

Encapsulation

Inheritance

Polymorphism

Abstraction

SOLID Principles

DRY

KISS

Composition over inheritance whenever appropriate.

---

# Naming Conventions

Classes

PascalCase

Example

AthleteService

CoachView

NutritionPlan

MesocycleRepository

---

Interfaces

Must begin with I only if required by the team's convention.

Preferred:

AthleteService

Implementation:

AthleteServiceImpl

---

Methods

camelCase

Examples

createWorkout()

assignMesocycle()

calculateFatigue()

estimateOneRepMax()

---

Variables

camelCase

Example

currentWorkout

estimatedOneRepMax

assignedCoach

---

Constants

UPPER_SNAKE_CASE

Example

MAX_RPE

DEFAULT_REST_TIME

---

Collections

Use meaningful names.

Correct

athletes

nutritionPlans

sessions

workouts

Avoid

list

array

temp

data

---

# Exception Handling

Never ignore exceptions.

Create custom exceptions whenever appropriate.

Example

AthleteNotFoundException

MesocycleValidationException

NutritionPlanException

Use GlobalExceptionHandler if REST endpoints are added in the future.

---

# Validation

Always validate:

Null values

Empty strings

Negative numbers

Invalid dates

Invalid RPE values

Invalid repetitions

Invalid weight

Never trust user input.

---

# Logging

Use SLF4J logging.

Do not use System.out.println() in production code.

---

# Date API

Use java.time

Examples

LocalDate

LocalDateTime

Duration

Avoid java.util.Date whenever possible.

---

# Dependency Injection

Always use constructor injection.

Avoid field injection.

Example

Good

private final AthleteRepository repository;

Never

@Autowired

private AthleteRepository repository;

---

# MongoDB Mapping

Use Spring Data MongoDB annotations.

Examples

@Document

@Id

@Indexed

@Field

Do not use JPA annotations.

Forbidden

@Entity

@Table

@Column

@OneToMany

@ManyToOne

@ManyToMany

@GeneratedValue

---

# DTO Usage

Views should never expose database entities directly.

Always use DTOs whenever data travels between layers.

---

# UI Components

Use Vaadin components only.

Examples

AppLayout

VerticalLayout

HorizontalLayout

Grid

Dialog

Notification

Tabs

ComboBox

DatePicker

TextField

PasswordField

Button

FormLayout

Accordion

Charts (later modules)

---

# File Storage

Do not store application data in:

TXT

CSV

JSON

XML

Persistence must always use MongoDB.

---

# Testing

JUnit 5

Mockito

Unit tests should focus on:

Services

Business Rules

Validation

Repositories when necessary

---

# Forbidden Technologies

GitHub Copilot must never introduce:

Hibernate

JPA

SQL

JDBC

Servlets

JSP

JavaFX

Swing

React

Angular

Vue

Thymeleaf

SQLite

MySQL

---

# Final Rule

Every generated class must respect this document.

If a generated solution conflicts with these specifications, this document always takes precedence.