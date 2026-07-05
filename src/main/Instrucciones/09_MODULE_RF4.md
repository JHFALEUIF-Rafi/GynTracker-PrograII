# GymTracker - Module RF4
# Dashboard, Statistics & Reports

## Objective

This module provides data visualization, progress tracking, historical reports and dashboards for every user role.

The objective is to transform stored workout and nutrition data into useful information for athletes, coaches and nutritionists.

The module never modifies information.

It only analyzes and presents data.

---

# Functional Requirements

RF4.1

Workout History

RF4.2

Dashboard

Statistics

Reports

Charts

Performance Indicators

---

# User Permissions

## ATHLETE

Can

- View personal dashboard
- View workout history
- View estimated One Rep Max
- View fatigue history
- View nutrition progress
- View personal statistics

Cannot

- View other athletes

---

## COACH

Can

- View dashboard of every assigned athlete
- Compare athlete progress
- View fatigue reports
- View workload reports
- View mesocycle statistics
- Export reports (future)

---

## NUTRITIONIST

Can

- View athlete biometric evolution
- View nutrition plan history
- View nutrition adherence
- View body weight evolution

Cannot

- Modify training statistics

---

# Required Views

DashboardView

StatisticsView

WorkoutHistoryView

ProgressView

FatigueDashboardView

CoachDashboardView

NutritionDashboardView

ReportView

---

# Required Components

StatisticCard

ProgressChart

WorkoutVolumeChart

OneRepMaxChart

FatigueChart

NutritionCard

HistoryGrid

DashboardHeader

FilterPanel

SummaryCard

---

# Required Services

DashboardService

StatisticsService

ReportService

HistoryService

---

# Required Repositories

SessionRepository

NutritionPlanRepository

AlertRepository

MesocycleRepository

UserRepository

---

# Required DTOs

DashboardDTO

StatisticsDTO

HistoryDTO

ProgressDTO

ReportDTO

ChartDTO

---

# Dashboard Cards

Athlete Dashboard

Display

Current Weight

Current Active Nutrition Plan

Estimated One Rep Max

Training Volume

Current Mesocycle

Current Fatigue Level

Completed Sessions

Last Workout

---

Coach Dashboard

Display

Assigned Athletes

Active Mesocycles

Open Fatigue Alerts

Completed Sessions

Average Athlete Progress

Weekly Training Volume

Recent Activity

---

Nutritionist Dashboard

Display

Assigned Athletes

Active Nutrition Plans

Recent Updates

Weight Evolution

Nutrition Goal Distribution

---

# Charts

Generate charts for

Workout Volume

Estimated One Rep Max

Workout Frequency

Body Weight

Fatigue Score

Completed Sessions

Training Consistency

Monthly Progress

---

# History

Workout History

Display

Workout Date

Exercises

Sets

Repetitions

Weight

Volume

Estimated 1RM

Fatigue Level

Duration

Status

---

# Search

Workout History

Search by

Date

Exercise

Mesocycle

Coach

Athlete

---

# Filters

Date Range

Week

Month

Year

Exercise

Mesocycle

Coach

Athlete

Status

---

# Statistics

Calculate

Weekly Training Volume

Monthly Training Volume

Average RPE

Average Workout Duration

Average Weekly Sessions

Estimated Strength Progress

Body Weight Evolution

Completed Mesocycles

Completed Nutrition Plans

---

# Progress Indicators

Display

Strength Progress

Volume Progress

Workout Consistency

Fatigue Trend

Weight Trend

Training Frequency

Nutrition Adherence

---

# Reports

Generate reports containing

Athlete Information

Workout History

Nutrition Summary

Mesocycle Summary

Estimated One Rep Max

Fatigue History

Charts

Progress Indicators

---

# Report Format

Current implementation

View only

Future implementation

PDF

Excel

CSV

These export features must NOT be implemented now.

---

# Business Rules

Historical information is read-only.

Reports never modify stored information.

Dashboard calculations are generated dynamically.

Statistics must always use historical data.

Current information should always be updated automatically.

---

# Notifications

Display

Recent Workout Completed

New Nutrition Plan

New Mesocycle Assigned

Fatigue Alert

Recent Progress

Notifications are informational only.

---

# Performance

Dashboard should load efficiently.

Avoid unnecessary database queries.

Reuse calculated data whenever possible.

Load charts asynchronously if necessary.

Never duplicate calculations already performed by Services.

---

# Future Compatibility

AI Performance Analysis

Machine Learning Predictions

Cloud Dashboards

Coach Comparison

Athlete Ranking

Email Reports

PDF Export

Excel Export

These features should NOT be implemented now.

---

# Acceptance Criteria

The module is complete only if

Every role has its own dashboard.

Workout history is searchable.

Statistics are correctly calculated.

Charts display valid information.

Historical reports are available.

Dashboard updates automatically.

MongoDB persistence is respected.

Views are responsive.

Architecture is respected.

---

# GitHub Copilot Rules

Dashboard must never access repositories directly.

All statistics belong to StatisticsService.

DashboardService prepares dashboard data only.

ReportService generates report information.

HistoryService retrieves historical data.

Views display DTOs only.

Repositories never perform calculations.

Generate reusable dashboard components.

Respect every previous architecture document.

Generate production-ready code.

Do not leave placeholder implementations.

Do not duplicate business logic.