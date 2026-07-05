# GymTracker

GymTracker is a training, nutrition and progress-tracking platform for gyms. Coaches design training programs, nutritionists manage diet plans, and athletes log workouts and track their own progress — all from one Spring Boot + Vaadin application backed by MongoDB.

> Looking for something specific? [Architecture](ARCHITECTURE.md) · [Database](DATABASE.md) · [Service API](API.md) · [Deployment](DEPLOYMENT.md) · [User guide](USER_GUIDE.md) · [Developer guide](DEVELOPER_GUIDE.md) · [Changelog](CHANGELOG.md)

---

## Overview

GymTracker is a **server-rendered Vaadin Flow application** (no separate frontend build, no public REST API) backed by **Spring Boot** and **MongoDB**. It supports three roles — Athlete, Coach, Nutritionist — each with a purpose-built set of screens, and enforces authorization in the service layer so that coaches and nutritionists only ever see data for athletes actually assigned to them.

## Features

- **Role-based dashboards** — training volume, fatigue level, recovery score, active alerts and quick actions tailored to Athlete / Coach / Nutritionist.
- **Mesocycle planning** — coaches build multi-week training programs with a day-by-day, exercise-by-exercise weekly planner (drag-and-drop reordering).
- **Workout logging** — athletes start, fill in (sets, reps, weight, RPE) and finish workout sessions; history is searchable by date range, mesocycle or status.
- **Nutrition plans** — nutritionists assign calorie/macro targets and goals (cutting/maintenance/bulking) per athlete, with a full history per athlete.
- **Automatic 1RM estimation** — one-rep-max estimated from logged sets (Epley/Brzycki/Lombardi formulas available).
- **Automatic fatigue scoring** — a fatigue level (LOW → CRITICAL) and recovery score computed from recent training load, RPE and 1RM trend.
- **Automatic alerts** — raised for critical fatigue, missed workouts, expired nutrition plans, completed mesocycles and performance drops; coaches acknowledge/resolve them.
- **Reports** — Athlete, Coach, Nutrition, Progress, Workout-History and Mesocycle reports, generated asynchronously and exportable to PDF/XLSX.
- **Statistics & charts** — training volume, 1RM progress and fatigue trend charts, scoped per role.

## Technologies

| Layer | Technology |
|---|---|
| Language / runtime | Java 21 |
| Application framework | Spring Boot 3.4.7 |
| UI framework | Vaadin Flow 24.6.10 (server-side rendered, no separate frontend to build) |
| Security | Spring Security (session-based, BCrypt passwords) |
| Database | MongoDB (Spring Data MongoDB) |
| Object mapping | MapStruct 1.6.3 |
| Caching | Spring Cache + Caffeine (`dashboards`, `statistics` regions) |
| Async execution | Spring `@Async` with a bounded `ThreadPoolTaskExecutor` |
| Build tool | Maven (wrapper included, no local Maven install required) |
| Testing | JUnit 5, Mockito, AssertJ, JaCoCo |
| Containerization | Docker (multi-stage build) + Docker Compose (app + MongoDB) |
| CI | GitHub Actions (`.github/workflows/ci.yml`) |

## Architecture summary

Clean Architecture, one-directional dependencies: **View (Vaadin) → Service (interface) → ServiceImpl → Repository (Spring Data MongoDB)**. Views never talk to repositories or Mongo directly. Cross-cutting concerns are centralized rather than duplicated per service:

- `AuthenticatedUserProvider` resolves the logged-in `User` from the Spring Security context (used by almost every service).
- `AthleteAssignmentService` is the single source of truth for "is this athlete assigned to this coach/nutritionist?" (a coach is assigned via a `Mesocycle`, a nutritionist via a `NutritionPlan`).
- DTOs at the View↔Service boundary, mapped by MapStruct; centralized `*Validator` classes for input/business-rule validation; a small shared `ui.component` package for reusable Vaadin widgets.

Full detail, request-flow diagrams and rationale in [ARCHITECTURE.md](ARCHITECTURE.md).

## Screenshots

> Screenshots are not yet captured in this repository. Suggested shots to add here once available:
>
> - `docs/screenshots/login.png` — Login screen
> - `docs/screenshots/dashboard-athlete.png` — Athlete dashboard
> - `docs/screenshots/dashboard-coach.png` — Coach dashboard
> - `docs/screenshots/mesocycle-planner.png` — Weekly mesocycle planner (drag-and-drop)
> - `docs/screenshots/workout-session.png` — Active workout session editor
> - `docs/screenshots/nutrition-plan.png` — Nutrition plan form
> - `docs/screenshots/statistics.png` — Statistics/charts view
>
> ```markdown
> ![Athlete dashboard](docs/screenshots/dashboard-athlete.png)
> ```

## Installation

Prerequisites: **Java 21+**, a **MongoDB** instance reachable at `mongodb://localhost:27017` (or override the URI — see below). Maven itself is *not* required — the wrapper (`mvnw`/`mvnw.cmd`) downloads it automatically.

```bash
git clone <repository-url>
cd gymtracker
```

There is no self-service sign-up screen — the first user account(s) must be created directly in the `users` collection (hashed password, a role). See [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md#creating-a-test-user) for a copy-pasteable example.

## Running locally

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Open **http://localhost:8080**. The login screen (`/login`) is public; every other route requires authentication. By default the `dev` profile is active (browser auto-launches, devtools live-reload enabled). Override configuration via environment variables (`SERVER_PORT`, `MONGODB_URI`, `MONGODB_DATABASE`, `LOG_LEVEL`, `SPRING_PROFILES_ACTIVE`) or by editing `src/main/resources/application.properties` — see [DEPLOYMENT.md](DEPLOYMENT.md#environment-variables).

To build a runnable jar instead:

```bash
./mvnw clean package -DskipTests
java -jar target/gymtracker-0.0.1-SNAPSHOT.jar
```

## Running with Docker

The whole stack (application + MongoDB) starts with a single command:

```bash
cp .env.example .env      # adjust values if needed
docker compose up -d
```

This builds the app from the multi-stage `Dockerfile`, starts a `mongodb` container first (waiting for its health check), then starts `gymtracker-app` once MongoDB is healthy. MongoDB data persists in a named Docker volume across restarts/rebuilds.

```bash
docker compose logs -f gymtracker-app   # follow logs
docker compose down                     # stop (keeps the data volume)
docker compose down -v                  # stop and delete the data volume
```

Full details (image build, environment variables, health checks, production hardening) in [DEPLOYMENT.md](DEPLOYMENT.md).

## Roles

| Role | Can do |
|---|---|
| **Athlete** | View/edit own profile, follow assigned mesocycle, log workouts, view workout history, view nutrition plan and history, view own alerts, view own statistics, generate a progress report |
| **Coach** | Manage the exercise catalog, design mesocycles for assigned athletes, review assigned athletes' workout history, acknowledge/resolve alerts, view roster statistics, generate reports |
| **Nutritionist** | Create and manage nutrition plans for assigned athletes, view nutrition-related alerts for assigned athletes, view nutrition statistics, generate nutrition reports |

An athlete is considered **assigned** to a coach once that coach has created a mesocycle for them, and assigned to a nutritionist once that nutritionist has created a nutrition plan for them — see [ARCHITECTURE.md](ARCHITECTURE.md#authorization-model) and [USER_GUIDE.md](USER_GUIDE.md) for full workflows.

## Project structure

```
src/main/java/com/gymtracker/
├── entity/       MongoDB documents (User, Exercise, Mesocycle, NutritionPlan, Session, Alert)
├── repository/   Spring Data MongoDB repositories
├── service/      Business interfaces + service/impl/ implementations
├── dto/          Data transfer objects (view ↔ service boundary), one subpackage per module
├── mapper/       MapStruct entity ↔ DTO mappers
├── validation/   Centralized request/business-rule validators
├── exception/    Custom exceptions + global handler
├── security/     Spring Security config, CustomUserDetails, AuthenticatedUserProvider
├── config/       Cache (Caffeine) and async executor configuration
├── view/         Vaadin views and view-local dialogs/components, one package per module
└── ui/component/ Small reusable UI building blocks shared across views
```

Full rationale and dependency rules in [ARCHITECTURE.md](ARCHITECTURE.md).

## Documentation index

| Document | Covers |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Layered architecture, package responsibilities, security model, caching/async |
| [DATABASE.md](DATABASE.md) | MongoDB collections, schemas, indexes, relationships, validation rules |
| [API.md](API.md) | Every service interface: purpose, input/output DTOs, security requirements |
| [DEPLOYMENT.md](DEPLOYMENT.md) | Docker deployment, environment variables, production configuration |
| [USER_GUIDE.md](USER_GUIDE.md) | Workflows for Athletes, Coaches and Nutritionists |
| [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) | Project structure, coding conventions, how to add a module, best practices |
| [CHANGELOG.md](CHANGELOG.md) | Version history |

## License

Public domain (Unlicense) — see [LICENSE.md](LICENSE.md).
