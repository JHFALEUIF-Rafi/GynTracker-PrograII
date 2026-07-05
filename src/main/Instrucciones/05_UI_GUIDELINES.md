# GymTracker - UI Guidelines

## Objective

This document defines the visual structure, navigation, user experience and UI conventions for the GymTracker application.

GitHub Copilot must follow these guidelines when generating Vaadin Views and Components.

The objective is to provide a modern, clean, responsive and consistent user interface.

---

# UI Technology

Framework

Vaadin 24

Theme

Lumo

Icons

Vaadin Icons

Layouts

AppLayout

VerticalLayout

HorizontalLayout

FormLayout

Scroller

SplitLayout

---

# Application Layout

The application must use AppLayout.

Structure

+------------------------------------------------------+
| Header                                               |
+------------+-----------------------------------------+
|            |                                         |
| Sidebar    |              Main View                  |
|            |                                         |
|            |                                         |
|            |                                         |
+------------+-----------------------------------------+

---

# Header

The Header contains

Application logo

Application name

Current user

Role badge

Notifications button

Logout button

---

# Sidebar Navigation

The Sidebar changes according to the authenticated user's role.

Menus not allowed for a role must never be displayed.

---

# Athlete Menu

Dashboard

My Profile

My Nutrition Plan

My Mesocycle

Workout Session

Workout History

Progress

Settings

Logout

---

# Coach Menu

Dashboard

Athletes

Exercises

Mesocycles

Fatigue Alerts

Reports

Statistics

Settings

Logout

---

# Nutritionist Menu

Dashboard

Athletes

Nutrition Plans

Reports

Settings

Logout

---

# Login View

The first screen of the application.

Components

Application logo

Email

Password

Login button

Forgot password (future feature)

Validation messages

---

# Dashboard

Each role has its own dashboard.

Dashboard cards

Number of athletes

Active mesocycles

Active nutrition plans

Fatigue alerts

Completed sessions

Recent activity

---

# Forms

Every form must use FormLayout.

Rules

Labels always visible.

Required fields clearly indicated.

Validation messages displayed below the component.

Use placeholders only when necessary.

---

# Buttons

Primary

Save

Create

Login

Assign

Secondary

Cancel

Back

Clear

Danger

Delete

Remove

---

# Notifications

Every important operation must show feedback.

Examples

Profile updated.

Workout saved.

Nutrition plan assigned.

Exercise created.

Fatigue alert generated.

---

# Dialogs

Dialogs should be used for

Confirmation

Delete actions

Details

Warnings

Never navigate to another page for simple confirmations.

---

# Tables

Use Vaadin Grid.

Tables must support

Sorting

Filtering

Pagination if necessary

Responsive resizing

Selection

---

# Charts

Dashboard charts should include

Workout volume

Estimated One Rep Max

Training frequency

Fatigue level

Completed workouts

Nutrition adherence (future)

Charts should be simple and interactive.

---

# Forms Validation

Validation should occur

While typing when possible

Before saving

Before updating

Before deleting

Never allow invalid data.

---

# Colors

Use Lumo defaults.

Primary

Blue

Success

Green

Warning

Orange

Danger

Red

Avoid excessive custom colors.

---

# Typography

Use default Lumo typography.

Titles

H1

Sections

H2

Cards

H3

Normal text

Paragraph

---

# Icons

Always use Vaadin Icons.

Examples

User

Dumbbell

Heart

Clipboard

Chart

Calendar

Warning

Bell

---

# Responsive Design

The application should work correctly on

Desktop

Laptop

Tablet

Mobile (basic support)

Avoid fixed widths.

Prefer percentage or responsive layouts.

---

# Navigation Rules

Views should navigate using Vaadin Router.

Never manually manipulate URLs.

Every secured View should verify user permissions.

---

# UX Principles

The interface should be

Simple

Fast

Intuitive

Minimalist

Professional

Avoid unnecessary dialogs.

Avoid excessive clicks.

Keep workflows short.

---

# Accessibility

Use meaningful labels.

Buttons should include icons and text when appropriate.

Provide keyboard navigation where possible.

Maintain sufficient contrast.

---

# Loading States

Operations that take noticeable time should display

ProgressBar

Loading indicator

Spinner

Never leave the user without feedback.

---

# Error Messages

Messages must be clear.

Good

"The nutrition plan has been updated successfully."

Bad

"Error."

Explain what happened whenever possible.

---

# GitHub Copilot Rules

Always reuse existing components.

Do not duplicate layouts.

Keep visual consistency.

Follow the AppLayout structure.

Respect role-based navigation.

Generate responsive Vaadin Views.

Do not introduce HTML, CSS frameworks or JavaScript unless explicitly requested.

The entire interface must be implemented using Vaadin components.