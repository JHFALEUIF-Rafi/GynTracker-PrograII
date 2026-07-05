# GymTracker - Module RF1
# Athlete Profile & Nutrition Plan

## Objective

This document defines the complete implementation of Module RF1.

GitHub Copilot must implement this module exactly as described.

The module manages:

- Athlete biometric profile
- Nutrition plans
- Role permissions
- CRUD operations
- Validations

---

# Functional Requirements

RF1.1

Athlete Biometric Profile

RF1.2

Nutrition Plan Management

---

# User Roles

ATHLETE

Can

View profile

Edit own biometric information

View nutrition plan

Cannot

Create nutrition plans

Edit nutrition plans

Delete nutrition plans

View other athletes

---

COACH

Can

View athlete profile

View nutrition plan

Cannot

Create nutrition plans

Modify nutrition plans

Delete nutrition plans

---

NUTRITIONIST

Can

View athlete profile

Create nutrition plan

Edit nutrition plan

Deactivate nutrition plan

View nutrition history

Cannot

Modify workout information

---

# Required Views

Create the following Vaadin Views.

AthleteProfileView

NutritionPlanView

NutritionPlanForm

NutritionPlanHistoryView

NutritionDashboardView

---

# Required Components

Create reusable components.

AthleteProfileForm

NutritionPlanFormComponent

NutritionSummaryCard

MacronutrientCard

ConfirmationDialog

---

# Required Services

AthleteService

NutritionPlanService

---

# Required Repositories

UserRepository

NutritionPlanRepository

---

# Required DTOs

AthleteDTO

NutritionPlanDTO

NutritionPlanRequestDTO

NutritionPlanResponseDTO

---

# Required Mappers

AthleteMapper

NutritionPlanMapper

---

# Required Validators

AthleteValidator

NutritionPlanValidator

---

# Athlete Profile

Every athlete profile must contain

First Name

Last Name

Email

Age

Gender

Weight

Height

Activity Level

Creation Date

Last Update

---

# Activity Level

Allowed values

SEDENTARY

LIGHT

MODERATE

ACTIVE

VERY_ACTIVE

Implement as Enum.

---

# Nutrition Goal

Allowed values

CUTTING

MAINTENANCE

BULKING

Implement as Enum.

---

# Nutrition Plan

Fields

Goal

Calories

Protein

Carbohydrates

Fat

Observations

Start Date

End Date

Status

Nutritionist

Athlete

Created Date

Updated Date

---

# CRUD Operations

Athlete

View

Update own profile

Nutrition Plan

Create

Read

Update

Deactivate

History

Never physically delete.

---

# Business Rules

Only Nutritionists may create nutrition plans.

Only Nutritionists may modify nutrition plans.

Athletes have read-only access.

Coaches have read-only access.

Only one nutrition plan may be ACTIVE for an athlete.

Creating a new ACTIVE plan automatically deactivates the previous one.

Nutrition plans must remain stored permanently.

History must always be preserved.

---

# Validations

Age

Minimum

14

Maximum

100

Weight

Greater than zero.

Height

Greater than zero.

Calories

Greater than zero.

Protein

Greater than or equal to zero.

Carbohydrates

Greater than or equal to zero.

Fat

Greater than or equal to zero.

End Date

Cannot be before Start Date.

Observations

Maximum

500 characters.

---

# Notifications

Show notifications after

Profile updated

Nutrition plan created

Nutrition plan updated

Nutrition plan deactivated

Validation errors

---

# Dashboard Card

Display

Current Goal

Calories

Protein

Carbohydrates

Fat

Plan Status

Nutritionist

Validity Period

---

# Search

Nutritionists can search athletes by

Name

Email

ID

---

# Filters

Nutrition History

Date

Status

Goal

Nutritionist

---

# Security Rules

Before every operation verify

Authenticated user

Role

Permissions

Ownership

---

# Future Compatibility

Leave architecture prepared for

PDF Export

Email Notifications

Nutrition Templates

Automatic reminders

These features should NOT be implemented now.

---

# Acceptance Criteria

The module is complete only if

Athletes can update their biometric profile.

Nutritionists can manage nutrition plans.

Coaches can consult nutrition plans.

History is preserved.

Role permissions are respected.

Validation works correctly.

MongoDB persistence works correctly.

Views are fully functional.

Services contain all business logic.

Repositories only access MongoDB.

---

# GitHub Copilot Rules

Generate complete production-ready code.

Do not leave TODOs.

Respect architecture.

Respect MongoDB model.

Respect Coding Standards.

Respect UI Guidelines.

Never duplicate logic.

Always create modular classes.

Every View must communicate only with Services.

Business logic belongs exclusively in the Service layer.

Validation belongs exclusively in Validators.