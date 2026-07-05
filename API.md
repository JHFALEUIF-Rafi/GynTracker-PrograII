# Service API

GymTracker is a **server-rendered Vaadin application with no public REST API**. "API" in this document means the internal **Service layer** (`com.gymtracker.service`) — the single contract every Vaadin view is allowed to call (see [ARCHITECTURE.md](ARCHITECTURE.md)). Views never call repositories directly, so this document is the complete, authoritative list of every operation the application can perform.

A `@RestControllerAdvice` (`GlobalExceptionHandler`) does exist, mapping exceptions to HTTP statuses, but the application defines **no `@Controller`/`@RestController` endpoints**, so it is never actually invoked in the running app — it would only matter if a future change added real HTTP endpoints. Today, every exception below propagates synchronously back to the calling Vaadin view, which catches it and shows a `Notification`.

## How to read this document

For each service:
- **Purpose** — what the service is responsible for.
- A table of every method: **signature**, **input DTO(s)**, **output DTO**, and the **security requirement** enforced by the implementation (role + per-resource ownership/assignment check, if any).

"Assigned" means: a coach is assigned to an athlete if the coach has created a `Mesocycle` for them; a nutritionist is assigned if they have created a `NutritionPlan` for them (`AthleteAssignmentService` — see [ARCHITECTURE.md](ARCHITECTURE.md#service-layer)).

All exceptions referenced below are from `com.gymtracker.exception` unless noted otherwise.

---

## AlertService

**Purpose:** generates system alerts (fatigue, missed workouts, expired nutrition plans, completed mesocycles, performance drops) and lets coaches triage them.

| Method | Input | Output | Security requirement |
|---|---|---|---|
| `generateFatigueAlert(String athleteId)` | — | `CompletableFuture<AlertDTO>` | No role check — validates `athleteId` resolves to an `ATHLETE`. Called internally (e.g. after logging a workout), async. |
| `generateMissedWorkoutAlert(String athleteId)` | — | `CompletableFuture<AlertDTO>` | Same as above. |
| `generateNutritionPlanExpiredAlert(String athleteId)` | — | `CompletableFuture<AlertDTO>` | Same as above. |
| `generateMesocycleCompletedAlert(String athleteId)` | — | `CompletableFuture<AlertDTO>` | Same as above. |
| `generatePerformanceDropAlert(String athleteId)` | — | `CompletableFuture<AlertDTO>` | Same as above. |
| `getAlertsByAthlete(String athleteId)` | — | `List<AlertDTO>` | ATHLETE: only own (`"Athletes may only view their own alerts."`). COACH: only if assigned (`"Coaches may only view alerts for their assigned athletes."`). NUTRITIONIST: only if assigned, and only nutrition-related alert types (`"Nutritionists may only view alerts for their assigned athletes."`). |
| `getAlertsByCoach(String coachId)` | — | `List<AlertDTO>` | COACH: only own (`"Coaches may only view their own alerts."`). ATHLETE: forbidden (`"Athletes cannot view coach alerts."`). NUTRITIONIST: allowed, result filtered to nutrition-related alerts for assigned athletes only. |
| `acknowledgeAlert(String alertId)` | — | `AlertDTO` | COACH only (`"Only coaches may acknowledge or resolve alerts."`) and must own the alert (`"Coaches may only acknowledge their own alerts."`); alert must be `ACTIVE`. |
| `resolveAlert(String alertId)` | — | `AlertDTO` | COACH only, must own the alert (`"Coaches may only resolve their own alerts."`); alert must be `ACKNOWLEDGED`. |
| `deleteResolvedAlerts()` | — | `int` | Always throws `BusinessRuleException("Resolved alerts must remain in history and cannot be deleted.")` — no role can call it successfully; resolved alerts are permanent history by design. |
| `getAlertsForCurrentUser()` | — | `List<AlertDTO>` | Dispatches by the caller's own role to the equivalent of `getAlertsByAthlete`/`getAlertsByCoach`, or a nutrition-scoped query for nutritionists. |

## AthleteAssignmentService

**Purpose:** the single source of truth for coach/nutritionist ↔ athlete assignment. A pure internal helper — not a security boundary of its own, but relied on by every other service that scopes data to "my assigned athletes."

| Method | Input | Output | Security requirement |
|---|---|---|---|
| `isAthleteAssignedToCoach(ObjectId coachId, ObjectId athleteId)` | — | `boolean` | None — internal query helper, no `UnauthorizedOperationException` throw sites. |
| `isAthleteAssignedToNutritionist(ObjectId nutritionistId, ObjectId athleteId)` | — | `boolean` | None — internal query helper. |
| `assignedAthleteIdsForCoach(ObjectId coachId)` | — | `Set<ObjectId>` | None — internal query helper. |
| `assignedAthleteIdsForNutritionist(ObjectId nutritionistId)` | — | `Set<ObjectId>` | None — internal query helper. |

## AthleteService

**Purpose:** athlete profile read/update, and athlete listing/search for coaches and nutritionists.

| Method | Input | Output | Security requirement |
|---|---|---|---|
| `getAthleteById(String athleteId)` | — | `AthleteDetailDTO` | ATHLETE: only own (`"Athletes can only view their own profile."`). COACH: only if assigned (`"Coaches may only view profiles of their assigned athletes."`). NUTRITIONIST: only if assigned (`"Nutritionists may only view profiles of their assigned athletes."`). |
| `getCurrentAthlete()` | — | `AthleteDetailDTO` | ATHLETE role only (`"Only athletes can request current athlete profile."`). |
| `updateAthleteProfile(AthleteRequestDTO)` | `AthleteRequestDTO` | `AthleteResponseDTO` | ATHLETE role only, implicitly self only (`"Only athletes may update their own profile."`). |
| `getAllAthletes()` | — | `List<AthleteSummaryDTO>` | ATHLETE forbidden (`"Athletes cannot list all athletes."`). COACH/NUTRITIONIST: full roster, unscoped. |
| `searchAthletes(String keyword)` | — | `List<AthleteSummaryDTO>` | ATHLETE forbidden (`"Athletes cannot search other athletes."`). COACH/NUTRITIONIST allowed. |
| `existsByEmail(String email)` | — | `boolean` | Any authenticated user. |
| `getAthletesForCurrentUser()` | — | `List<AthleteSummaryDTO>` | COACH → assigned athletes only. NUTRITIONIST → assigned athletes only. ATHLETE → forbidden (`"Athletes cannot list other athletes."`). |

## AuthenticationService

**Purpose:** the pre-authentication entry point used by the login screen.

| Method | Input | Output | Security requirement |
|---|---|---|---|
| `authenticate(String email, String password)` | email + password | `AuthenticatedUserDTO` | Public/anonymous — no prior authentication required (this *is* the login operation). Invokes Spring Security's `AuthenticationManager`/`DaoAuthenticationProvider` (BCrypt verification via `CustomUserDetailsService`), then manually persists the `SecurityContext` into the HTTP session and rotates the session id (session-fixation protection). Throws `org.springframework.security.core.AuthenticationException` for invalid credentials or a disabled/locked account. |

## DashboardService

**Purpose:** role-specific dashboard aggregation (training volume, fatigue, alerts, active plan, etc.), cached.

| Method | Input | Output | Security requirement |
|---|---|---|---|
| `getAthleteDashboard(String athleteId)` | — | `DashboardDTO` | Caller role must be ATHLETE **and** `currentUser.id == athleteId` (`"User role is not allowed to access this dashboard."` / `"Users may only access their own dashboard."`). |
| `getCoachDashboard(String coachId)` | — | `DashboardDTO` | Caller role must be COACH **and** `currentUser.id == coachId` (same message pair). |
| `getNutritionistDashboard(String nutritionistId)` | — | `DashboardDTO` | Caller role must be NUTRITIONIST **and** `currentUser.id == nutritionistId` (same message pair). |
| `refreshDashboard(String userId)` | — | `DashboardDTO` | `currentUser.id == userId` (role-agnostic self-check) — `"Users may only refresh their own dashboard."` |
| `getDashboardSummary(String userId)` | — | `DashboardDTO` | `currentUser.id == userId` — `"Users may only access their own dashboard summary."`; dispatches to the matching role-specific dashboard. |

## ExerciseService

**Purpose:** the shared exercise catalog (name, muscle group, type, difficulty, equipment, status).

| Method | Input | Output | Security requirement |
|---|---|---|---|
| `createExercise(ExerciseRequestDTO)` | `ExerciseRequestDTO` | `ExerciseResponseDTO` | COACH role only (`"Only coaches may create, update or deactivate exercises."`). |
| `updateExercise(String exerciseId, ExerciseRequestDTO)` | `ExerciseRequestDTO` | `ExerciseResponseDTO` | COACH role only, same message. |
| `deactivateExercise(String exerciseId)` | — | `ExerciseResponseDTO` | COACH role only, same message. Blocked (`BusinessRuleException`) if the exercise is still used by an `ACTIVE` mesocycle. |
| `getExerciseById(String exerciseId)` | — | `ExerciseDetailDTO` | Any authenticated user. |
| `getAllExercises()` | — | `List<ExerciseSummaryDTO>` | Any authenticated user. |
| `searchExercises(String keyword)` | — | `List<ExerciseSummaryDTO>` | Any authenticated user. |
| `filterExercises(ExerciseType, Difficulty, Equipment, ExerciseStatus)` | filter params | `List<ExerciseSummaryDTO>` | Any authenticated user. |
| `existsByName(String name)` | — | `boolean` | Any authenticated user. |

## FatigueService

**Purpose:** derives an athlete's fatigue level/recovery score from recent training load, RPE and 1RM trend.

| Method | Input | Output | Security requirement |
|---|---|---|---|
| `calculateFatigue(String athleteId)` | — | `CompletableFuture<FatigueDTO>` | **No authorization check** — only validates `athleteId` resolves to an `ATHLETE`. See [Cross-cutting notes](#cross-cutting-notes). |
| `getCurrentFatigueLevel(String athleteId)` | — | `FatigueLevel` | No authorization check. |
| `getFatigueHistory(String athleteId)` | — | `List<FatigueDTO>` | No authorization check. |
| `evaluateWorkoutLoad(String workoutSessionId)` | — | `Double` | No authorization check; requires the session to be `COMPLETED`. |
| `calculateWeeklyTrainingLoad(String athleteId)` | — | `Double` | No authorization check. |
| `calculateRecoveryScore(String athleteId)` | — | `Double` | No authorization check. |
| `calculateFatigueScore(String athleteId)` | — | `Double` | No authorization check. |
| `calculateFatigueLevel(String athleteId)` | — | `FatigueLevel` | No authorization check (delegates to `getCurrentFatigueLevel`). |
| `getFatigueHistoryChart(String athleteId)` | — | `ChartDTO` | No authorization check. |

## MesocycleService

**Purpose:** training-program (mesocycle) authoring by coaches; scoped read access for athletes/nutritionists.

| Method | Input | Output | Security requirement |
|---|---|---|---|
| `createMesocycle(MesocycleRequestDTO)` | `MesocycleRequestDTO` | `MesocycleResponseDTO` | COACH role only (`"Only coaches may create or modify mesocycles."`); the request's `coachId` must equal the authenticated coach (`"Coaches may only create or update their own mesocycles."`). |
| `updateMesocycle(String mesocycleId, MesocycleRequestDTO)` | `MesocycleRequestDTO` | `MesocycleResponseDTO` | COACH role only + must own the existing mesocycle (`"Coaches may only modify their own mesocycles."`) + request coach must match. Archived mesocycles cannot be updated. |
| `activateMesocycle(String mesocycleId)` | — | `MesocycleResponseDTO` | COACH role only + must own the mesocycle. |
| `archiveMesocycle(String mesocycleId)` | — | `MesocycleResponseDTO` | COACH role only + must own the mesocycle. |
| `getMesocycleById(String mesocycleId)` | — | `MesocycleDetailDTO` | ATHLETE: only own. COACH: only ones they own. NUTRITIONIST: only for athletes assigned to them. Violation → `"User does not have permission to view this mesocycle."` |
| `getActiveMesocycle(String athleteId)` | — | `MesocycleResponseDTO` | Same rule as `getMesocycleById`. |
| `getMesocyclesByAthlete(String athleteId)` | — | `List<MesocycleSummaryDTO>` | Same rule, applied per item (unreadable mesocycles are silently filtered out, not thrown). |
| `getMesocyclesByCoach(String coachId)` | — | `List<MesocycleSummaryDTO>` | ATHLETE forbidden (`"Athletes cannot list mesocycles by coach."`). COACH: only own (`"Coaches can only list their own mesocycles."`). NUTRITIONIST forbidden (`"Nutritionists cannot list mesocycles by coach."`). |
| `searchMesocycles(String keyword)` | — | `List<MesocycleSummaryDTO>` | Scoped automatically per role in the Mongo query (ATHLETE → own; COACH → own; NUTRITIONIST → assigned athletes only). |
| `duplicateMesocycle(String mesocycleId)` | — | `MesocycleResponseDTO` | COACH role only + must own the original mesocycle. |
| `getMesocyclesForCurrentUser()` | — | `List<MesocycleSummaryDTO>` | ATHLETE → own. COACH → created by them. NUTRITIONIST → assigned athletes only. *(Interface Javadoc says nutritionists see "the full catalog read-only" — the implementation is actually narrower/assignment-scoped; treat the implementation, not the Javadoc, as authoritative.)* |

## NutritionPlanService

**Purpose:** nutrition-plan authoring by nutritionists; scoped read access for athletes/coaches.

| Method | Input | Output | Security requirement |
|---|---|---|---|
| `createNutritionPlan(NutritionPlanRequestDTO)` | `NutritionPlanRequestDTO` | `NutritionPlanResponseDTO` | NUTRITIONIST role only (`"Only nutritionists may create or modify nutrition plans."`); request `nutritionistId` must match the authenticated nutritionist (`"Nutritionists may only create or update their own plans."`). |
| `updateNutritionPlan(String planId, NutritionPlanRequestDTO)` | `NutritionPlanRequestDTO` | `NutritionPlanResponseDTO` | Same as create. |
| `deactivateNutritionPlan(String planId)` | — | `NutritionPlanResponseDTO` | NUTRITIONIST role only. *(No per-plan ownership check here — unlike Mesocycle's activate/archive, any nutritionist can deactivate any plan.)* |
| `getNutritionPlanById(String planId)` | — | `NutritionPlanDetailDTO` | ATHLETE: only own (`"Athletes may only view their own nutrition plans."`). COACH: only for assigned athletes (`"Coaches may only view nutrition plans for their assigned athletes."`). NUTRITIONIST: only for assigned athletes (`"Nutritionists may only view nutrition plans for their assigned athletes."`). |
| `getActiveNutritionPlan(String athleteId)` | — | `NutritionPlanResponseDTO` | Same rule as `getNutritionPlanById`. |
| `getNutritionHistory(String athleteId)` | — | `List<NutritionPlanSummaryDTO>` | Same rule as `getNutritionPlanById`. |
| `getNutritionPlansByNutritionist(String nutritionistId)` | — | `List<NutritionPlanSummaryDTO>` | ATHLETE forbidden. NUTRITIONIST: only own (`"Nutritionists can only view their own plans."`). COACH forbidden. |
| `searchNutritionPlans(String keyword)` | — | `List<NutritionPlanSummaryDTO>` | Scoped automatically per role (ATHLETE → own; COACH → assigned athletes' plans; NUTRITIONIST → plans they created). |
| `getNutritionPlansForCurrentUser()` | — | `List<NutritionPlanSummaryDTO>` | ATHLETE → own. NUTRITIONIST → created by them. COACH → assigned athletes only. *(Same Javadoc-vs-implementation caveat as `MesocycleService.getMesocyclesForCurrentUser()`.)* |

## OneRepMaxService

**Purpose:** estimates and tracks one-rep-max (1RM) from logged sets.

| Method | Input | Output | Security requirement |
|---|---|---|---|
| `calculateOneRepMax(String athleteId)` | — | `CompletableFuture<List<OneRepMaxDTO>>` | **No authorization check** — only validates `athleteId` resolves to an `ATHLETE`. See [Cross-cutting notes](#cross-cutting-notes). |
| `calculateExerciseOneRepMax(String athleteId, String exerciseId)` | — | `OneRepMaxDTO` | No authorization check. |
| `getLatestOneRepMax(String athleteId, String exerciseId)` | — | `OneRepMaxDTO` | No authorization check. |
| `getOneRepMaxHistory(String athleteId, String exerciseId)` | — | `List<OneRepMaxDTO>` | No authorization check. |
| `compareOneRepMax(String athleteId, String exerciseId)` | — | `OneRepMaxComparisonDTO` | No authorization check. |
| `estimateOneRepMax(Double weight, Integer repetitions)` | weight + reps | `Double` | Pure calculation, no user context — no authorization check. |
| `getCurrentEstimatedOneRepMax(String athleteId)` | — | `Double` | No authorization check. |

## ReportService

**Purpose:** generates and exports (PDF/XLSX) Athlete/Coach/Nutrition/Progress/Workout-History/Mesocycle reports.

| Method | Input | Output | Security requirement |
|---|---|---|---|
| `generateAthleteReport(String athleteId)` | — | `ReportDTO` | NUTRITIONIST forbidden (`"Nutritionists may only generate nutrition reports."`). ATHLETE: only own (`"Athletes may only generate their own reports."`). COACH: only if assigned (`"Coaches may only generate reports for assigned athletes."`). |
| `generateCoachReport(String coachId)` | — | `ReportDTO` | Caller role must be COACH **and** `currentUser.id == coachId` (`"Only authenticated coaches can generate coach reports."`). |
| `generateNutritionReport(String athleteId)` | — | `ReportDTO` | ATHLETE: only own. COACH: only if assigned. NUTRITIONIST: only if assigned (`"Nutritionists may only generate nutrition reports for assigned athletes."`). |
| `generateProgressReport(String athleteId)` | — | `ReportDTO` | Same rule as `generateAthleteReport`. |
| `generateWorkoutHistoryReport(String athleteId)` | — | `ReportDTO` | Same rule as `generateAthleteReport`. |
| `generateMesocycleReport(String mesocycleId)` | — | `ReportDTO` | NUTRITIONIST forbidden (`"Nutritionists cannot generate mesocycle reports."`). ATHLETE: only their own mesocycle. COACH: only their own mesocycle. |
| `exportReport(ReportDTO, ExportFormat)` | `ReportDTO` + format | `CompletableFuture<byte[]>` | ATHLETE: only own reports. COACH: own coach reports, or reports for assigned athletes. NUTRITIONIST: nutrition-type reports for assigned athletes only. Any other combination → `"Role not authorized for report export."` |

## StatisticsService

**Purpose:** KPI/chart computation (training volume, 1RM progress, fatigue trend), cached.

| Method | Input | Output | Security requirement |
|---|---|---|---|
| `getAthleteStatistics(String athleteId)` | — | `StatisticsDTO` | Self-athlete OK. COACH OK only if assigned. NUTRITIONIST OK only if assigned. Else `"User cannot access these statistics"`. |
| `getCoachStatistics(String coachId)` | — | `StatisticsDTO` | Caller role must be COACH **and** `currentUser.id == coachId`. |
| `getNutritionistStatistics(String nutritionistId)` | — | `StatisticsDTO` | Caller role must be NUTRITIONIST **and** `currentUser.id == nutritionistId`. |
| `getWorkoutVolumeChart(String athleteId)` | — | `ChartDTO` | Same rule as `getAthleteStatistics`. |
| `getOneRepMaxChart(String athleteId)` | — | `ChartDTO` | Same rule as `getAthleteStatistics`. |
| `getFatigueChart(String athleteId)` | — | `ChartDTO` | Same rule as `getAthleteStatistics`. |

Implementation note: the permission check runs in the public `get*` method *before* delegating to a `@Cacheable` `compute*` method through a self-injected proxy, specifically so the check always executes even on a cache hit (see [ARCHITECTURE.md](ARCHITECTURE.md#spring-cache)).

## UserService

**Purpose:** the authenticated caller's own generic profile, account and security settings — shared by every role. (Athlete-specific biometric data is `AthleteService`'s responsibility, not this one.)

| Method | Input | Output | Security requirement |
|---|---|---|---|
| `getCurrentUserProfile()` | — | `UserProfileDTO` | No role restriction — implicitly self-only (no target-id parameter). |
| `updateProfile(UserProfileUpdateDTO)` | `UserProfileUpdateDTO` | `UserProfileDTO` | Implicitly self-only, no role restriction. |
| `changePassword(ChangePasswordRequestDTO)` | `ChangePasswordRequestDTO` | — (`void`) | Implicitly self-only. Throws `UnauthorizedOperationException("Current password is incorrect.")` if the supplied current password doesn't match — a credential check, not a role check. |

## WorkoutSessionService

**Purpose:** athlete workout-session logging and history, scoped read access for coaches/nutritionists.

| Method | Input | Output | Security requirement |
|---|---|---|---|
| `createWorkoutSession(WorkoutSessionRequestDTO)` | `WorkoutSessionRequestDTO` | `WorkoutSessionResponseDTO` | ATHLETE role only (`"Only athletes may register workout sessions."`); the session's `athleteId` must equal the authenticated user (`"Athletes may only register their own workout sessions."`). Also validates the mesocycle belongs to that athlete and completed exercises belong to that mesocycle. |
| `getWorkoutSessionById(String sessionId)` | — | `WorkoutSessionDetailDTO` | ATHLETE: only own. COACH: only for assigned athletes. NUTRITIONIST: only for assigned athletes. Else `"User does not have permission to view these workout sessions."` |
| `getWorkoutSessionsByAthlete(String athleteId)` | — | `List<WorkoutSessionSummaryDTO>` | Same rule as `getWorkoutSessionById`. |
| `getWorkoutSessionsByMesocycle(String mesocycleId)` | — | `List<WorkoutSessionSummaryDTO>` | Same rule, checked against the mesocycle's athlete, then each session filtered again individually. |
| `getWorkoutSessionsByDateRange(LocalDate startDate, LocalDate endDate)` | date range | `List<WorkoutSessionSummaryDTO>` | No upfront target check — every session in range is filtered per-item by the same rule (own/assigned only), so the result is naturally scoped. |

---

## Cross-cutting notes

- **Consistent assignment model.** Coach↔Athlete and Nutritionist↔Athlete access scoping is centralized in `AthleteAssignmentService` and reused, by explicit design (documented in several `service.impl` Javadoc comments), across `AlertService`, `AthleteService`, `MesocycleService`, `NutritionPlanService`, `WorkoutSessionService`, `ReportService` and `StatisticsService`. Several of those comments note explicitly: *"Broad 'any coach/nutritionist can read' access was a broken-access-control gap, not an intended design"* — i.e. this scoping was a deliberate security fix, not incidental behavior.
- **Two services with no authorization checks at all.** `FatigueService` and `OneRepMaxService` have zero `UnauthorizedOperationException` throw sites — every method is reachable by any authenticated user for any `athleteId`, regardless of role or assignment. In practice, the Vaadin views and other services (Dashboard, Report, Alert, Statistics) that call into these two always check authorization *before* calling them — but there is no defense-in-depth at the Fatigue/OneRepMax layer itself. This is a known, current limitation, not a bug that was silently patched while writing this document.
- **Interface Javadoc vs. implementation drift.** `MesocycleService.getMesocyclesForCurrentUser()` and `NutritionPlanService.getNutritionPlansForCurrentUser()` both carry Javadoc claiming the "other side" role sees "the full catalog (read-only)" — nutritionists for mesocycles, coaches for nutrition plans. The actual implementations are narrower (assignment-scoped only), consistent with the broken-access-control hardening described above. This document describes the *implemented* (narrower, more restrictive) behavior; treat the Javadoc in those two interfaces as stale.
- **`deleteResolvedAlerts()`** always throws — it is intentionally unusable by any role, not a bug.
