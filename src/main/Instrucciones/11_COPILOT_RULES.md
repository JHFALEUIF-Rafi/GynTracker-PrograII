# GymTracker - GitHub Copilot Rules

## Objective

This document defines how GitHub Copilot must behave while assisting in the development of GymTracker.

These rules have priority over Copilot's default suggestions.

Copilot must always prioritize consistency, maintainability, and compliance with the project documentation.

---

# Primary Source of Truth

Before generating code, always follow the documents in the following order:

1. 00_PROJECT_CONTEXT.md
2. 01_TECH_STACK.md
3. 02_ARCHITECTURE.md
4. 03_DATABASE.md
5. 04_CODING_STANDARDS.md
6. 05_UI_GUIDELINES.md
7. 06_MODULE_RF1.md
8. 07_MODULE_RF2.md
9. 08_MODULE_RF3.md
10. 09_MODULE_RF4.md
11. 10_DEVELOPMENT_ROADMAP.md

If two documents appear to conflict, the one with the lower number takes precedence unless explicitly stated otherwise.

---

# General Behavior

Always generate production-ready code.

Never generate demonstration code.

Never generate placeholder implementations.

Never leave TODO comments.

Never create incomplete methods.

Every generated class must compile successfully.

---

# Before Writing Code

Always determine:

- Which module is being implemented.
- Which package the new class belongs to.
- Whether a similar class already exists.
- Whether the requested functionality already exists.

Reuse existing code whenever possible.

---

# Project Architecture

Always respect the layered architecture.

Allowed flow

View

↓

Service

↓

Repository

↓

MongoDB

Never bypass layers.

Never access repositories from Views.

Never implement business logic inside Views.

Never perform persistence inside Services without using a Repository.

---

# File Creation Rules

Before creating a new class:

Check whether an appropriate class already exists.

If it exists

Extend it.

Reuse it.

Refactor it.

Do not duplicate responsibilities.

Create a new file only when it introduces a genuinely new responsibility.

---

# Service Rules

Business logic belongs only inside Services.

Services should

Validate

Calculate

Coordinate repositories

Call mappers

Throw custom exceptions

Never access UI components.

---

# Repository Rules

Repositories only access MongoDB.

Repositories never

Validate data

Calculate values

Generate reports

Format information

Repositories extend MongoRepository.

---

# View Rules

Views only

Display data

Collect user input

Call Services

Navigate

Show notifications

Views never

Calculate business values

Instantiate repositories

Manipulate MongoDB

Duplicate validation logic

---

# Entity Rules

Entities represent MongoDB documents.

Entities must not contain

Business rules

Database queries

UI logic

Entities may contain

Constructors

Getters

Setters

equals()

hashCode()

toString()

---

# DTO Rules

Always communicate between View and Service using DTOs.

Never expose database entities directly.

---

# Validation Rules

Always validate user input.

Never trust UI values.

Use Validators whenever possible.

Never duplicate validation code inside Views.

---

# Exception Rules

Throw meaningful custom exceptions.

Never throw generic Exception.

Handle exceptions consistently.

Provide informative messages.

---

# Logging Rules

Use SLF4J.

Never use

System.out.println()

System.err.println()

Use appropriate log levels.

---

# Naming Rules

Use descriptive names.

Avoid abbreviations.

Bad

calc()

tmp

obj

Good

calculateFatigue()

estimatedOneRepMax

assignedNutritionPlan

---

# Code Duplication

Before generating new code

Search for an existing implementation.

If similar logic exists

Reuse it.

Extract common behavior when necessary.

Never duplicate algorithms.

---

# Modifying Existing Code

When modifying existing classes

Preserve public APIs whenever possible.

Avoid breaking changes.

Maintain backward compatibility inside the project.

Refactor carefully.

---

# Refactoring

Refactor only when

Readability improves.

Duplication decreases.

Architecture improves.

Never refactor unrelated modules.

---

# Performance

Avoid unnecessary database queries.

Avoid loading unused data.

Prefer efficient repository methods.

Avoid repeated calculations.

Cache only when explicitly required.

---

# Security

Respect user roles.

Always verify permissions before executing restricted actions.

Never expose sensitive information.

Passwords must always be encrypted.

Never hardcode credentials.

---

# MongoDB Rules

Always use Spring Data MongoDB.

Never introduce SQL concepts.

Prefer embedded documents for small structures.

Use references only when appropriate.

Respect indexes defined in 03_DATABASE.md.

---

# Vaadin Rules

Use only Vaadin components.

Do not introduce

React

Angular

Vue

Thymeleaf

JSP

JavaFX

Swing

Maintain a consistent user interface.

---

# Unit Testing

Generate testable code.

Favor constructor injection.

Avoid static dependencies.

Keep business logic isolated.

---

# Documentation

When generating public classes or complex algorithms

Add concise JavaDoc only when it improves understanding.

Do not document obvious code.

---

# Roadmap Compliance

Always implement features according to

10_DEVELOPMENT_ROADMAP.md

Do not jump ahead.

Finish the current phase before implementing the next one.

---

# If Information Is Missing

Do not invent business rules.

Instead

Use the existing documentation.

Infer only from documented requirements.

If a decision could affect architecture or business logic, request clarification instead of guessing.

---

# Code Quality Checklist

Before considering a task complete, verify:

- The code compiles.
- Architecture rules are respected.
- SOLID principles are maintained.
- No duplicated logic exists.
- Naming conventions are followed.
- DTOs are used correctly.
- Repositories only access MongoDB.
- Views do not contain business logic.
- Services contain business rules.
- Validation is centralized.
- Exceptions are handled consistently.

---

# Response Style

When generating code:

Provide complete implementations.

Do not omit imports.

Do not leave empty methods.

Generate all required classes for the requested feature.

If multiple files are required, clearly separate each file.

Explain architectural decisions briefly before presenting code.

---

# Final Rule

GymTracker is a modular, maintainable and scalable application.

Every generated class must contribute to those goals.

If a requested implementation conflicts with any document inside the `docs/` directory, the documentation always takes precedence.

Never sacrifice architecture for shorter code.

When in doubt, choose the solution that is more maintainable, more modular and more consistent with the project documentation.