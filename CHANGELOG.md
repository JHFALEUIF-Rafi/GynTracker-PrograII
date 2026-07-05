# Changelog

All notable changes to GymTracker are documented in this file. Format loosely follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); this project does not yet use tagged releases or Semantic Versioning tags in git (current `pom.xml` version: `0.0.1-SNAPSHOT`), so entries are grouped by work area rather than by release number.

## [0.0.1-SNAPSHOT] — 2026-07-05

Initial feature-complete version of GymTracker: a Spring Boot 3 + Vaadin Flow 24 + MongoDB training/nutrition/progress-tracking platform for Athletes, Coaches and Nutritionists.

### Added

- **Core domain and modules**: exercise catalog, mesocycle (training program) planning with a weekly drag-and-drop planner, workout session logging and history, nutrition plan management, automatic alert generation (fatigue, missed workouts, expired nutrition plans, completed mesocycles, performance drops), role-based dashboards, statistics/charts, and asynchronous PDF/XLSX report generation.
- **Automatic calculations**: one-rep-max estimation (Epley/Brzycki/Lombardi formulas) and fatigue/recovery scoring from recent training load, RPE and 1RM trend.
- **Three-role security model**: Athlete, Coach, Nutritionist, with session-based Spring Security authentication (BCrypt passwords) and service-layer authorization scoped by ownership/assignment (a coach is "assigned" to an athlete via a `Mesocycle`; a nutritionist via a `NutritionPlan`).
- **`AuthenticatedUserProvider`** (`security` package): centralizes resolving the authenticated `User` from the Spring Security context, replacing a duplicated `getAuthenticatedUser()`/`extractAuthenticatedEmail()` pair that previously existed in nine separate service implementations.
- **`AthleteAssignmentService`** (`service`/`service.impl`): centralizes the "is this athlete assigned to this coach/nutritionist?" check, replacing duplicated assignment-lookup logic previously repeated across six service implementations.
- **Docker support**: multi-stage `Dockerfile` (Maven build stage + minimal `eclipse-temurin` JRE-Alpine runtime stage, non-root user), `docker-compose.yml` (application + MongoDB, named volume for data persistence, health checks on both services, internal bridge network), `.dockerignore`, `.env.example` — the full stack starts with `docker compose up`.
- **CI pipeline**: `.github/workflows/ci.yml` (GitHub Actions) — checkout, JDK 21 setup with Maven dependency caching, compilation verification, full test suite (against a real `mongo:7.0` service container), and jar packaging on every push and pull request; fails the build on any compilation or test failure.
- **DevOps tooling**: `.editorconfig` (consistent indentation/line-ending conventions across editors), `.gitattributes` (forces LF line endings for `mvnw` and other shell scripts, preventing a CRLF-corrupted wrapper from breaking on Linux/CI/Docker), improved `.gitignore` (environment files, logs, stale entries removed).
- **Test suite**: 200+ JUnit 5 tests across service (Mockito), repository (`@DataMongoTest` against a real MongoDB), validation, security and integration (`@SpringBootTest`) layers, with JaCoCo coverage reporting.
- **Full documentation set**: `README.md`, `ARCHITECTURE.md`, `DATABASE.md`, `API.md`, `USER_GUIDE.md`, `DEVELOPER_GUIDE.md`, `DEPLOYMENT.md`, `CHANGELOG.md` (this file).

### Changed

- **Performance**: MongoDB query patterns reviewed for N+1 lookups (replaced per-item finders with batched `findAllById`/`findByXIn` calls where found), indexes (`@Indexed`/`@CompoundIndex`) added to match actual derived-query shapes, pagination and lazy-loading reviewed across list views, Caffeine-backed bounded caching introduced for dashboard/statistics reads (replacing Spring Boot's default unbounded cache map), and a bounded `ThreadPoolTaskExecutor` introduced for `@Async` work (replacing the default unbounded-thread-per-call executor).
- **UI/UX**: every Vaadin view reviewed and brought to a consistent standard for alignment, responsive `FormLayout` steps, icon usage, empty states (`EmptyState` component), loading indicators (`LoadingSpinner`), color/typography consistency, notifications (`Notifications` helper), dialog styling (`app-dialog` CSS class), and accessibility (`aria-label` on icon-only buttons and grids).
- **Final refactoring pass**: SOLID/Clean Architecture review across the service layer — extracted the two shared components described above, removed cross-module repository dependencies that existed only to support now-centralized logic, removed dead/never-implemented service interfaces (`HistoryService`, `ProgressService`, `WorkoutValidationService`) and two empty packages (`component`, `util`) that contained only a `package-info.java`.
- **Environment configuration**: hardcoded values in `application.properties` replaced with environment-variable-driven placeholders (`SERVER_PORT`, `MONGODB_URI`, `MONGODB_DATABASE`, `LOG_LEVEL`, `SPRING_PROFILES_ACTIVE`); added `application-dev.yml`/`application-prod.yml` profile-specific overrides (browser auto-launch and devtools now correctly differ between local development and containerized/production runs).

### Fixed

- **Broken access control**: Coach and Nutritionist roles previously had broad, unrestricted read access to *any* athlete's data across five services (Mesocycle, NutritionPlan, WorkoutSession, Statistics, Alert) — this was identified during a security review as unintended (confirmed with the project owner) and fixed to require an actual coach/nutritionist-athlete assignment, mirroring the pattern already correctly implemented in the reporting module. `FatigueService` and `OneRepMaxService` are documented (see [API.md](API.md#cross-cutting-notes)) as not yet having this same scoping — a known, currently-accepted gap rather than a silently patched one.
- **Docker build reliability**: this project's Maven Wrapper (`mvnw`, "only-script" distribution) downloads Maven itself via `curl`/`wget` + `unzip`, neither of which exist in a bare `eclipse-temurin` Alpine JDK image — added to the Docker build stage.

### Security

- Authentication: session-based via Spring Security, manual login flow (`AuthenticationService`) with session-fixation protection (session ID rotation on login).
- Passwords: BCrypt hashing only; never logged, never returned in a DTO.
- CSRF: Spring Security's CSRF filter is disabled by design — Vaadin Flow has its own built-in CSRF/XSRF protection for its internal UIDL protocol, and the application exposes no separate form/REST surface that would need Spring's filter.
- Route protection: only `/`, `/login`, `/error`, `/static/**` are public; every other route requires an authenticated session.
- Input validation: centralized per-module validators (Jakarta Bean Validation + cross-field business rules) run before every write.
