# Developer Guide

This guide is for anyone adding code to GymTracker. Read [ARCHITECTURE.md](ARCHITECTURE.md) first if you haven't — this guide assumes you know the layering and dependency rules described there.

## Project structure

```
src/main/java/com/gymtracker/
├── entity/       MongoDB documents
├── repository/   Spring Data MongoDB repositories
├── service/      Business interfaces + service/impl/ implementations
├── dto/          Data transfer objects (view ↔ service boundary), one subpackage per module
├── mapper/       MapStruct entity ↔ DTO mappers
├── validation/   Centralized request validators
├── exception/    Custom exceptions + global handler
├── security/     Spring Security configuration, custom UserDetails, AuthenticatedUserProvider
├── config/       Cache and async executor configuration
├── view/         Vaadin views and view-local components, one package per module
└── ui/component/ Small reusable UI building blocks shared across views
```

Every package has a `package-info.java` with a one-line responsibility statement — check it before deciding where a new class belongs. Full rationale for the layering in [ARCHITECTURE.md](ARCHITECTURE.md#package-organization).

## Environment setup

1. **JDK 21** on your `PATH` (`java -version` should print 21 or newer).
2. **MongoDB** running locally on the default port (`mongodb://localhost:27017`), or point `MONGODB_URI`/`spring.data.mongodb.uri` at another instance.
3. No local Maven install is required — use the wrapper (`./mvnw` / `mvnw.cmd`). `.gitattributes` forces `mvnw` to keep LF line endings regardless of your local `core.autocrlf` setting, since a CRLF-corrupted `mvnw` fails to run on Linux (CI, Docker).
4. **IDE:** any IDE with a Vaadin plugin gives you Java hotswap (edit-and-see-it without restarting). IntelliJ, VS Code and Eclipse all have one — see the plugin's own docs for the "Debug/Run with Hotswap Agent" launch mode. An `.editorconfig` at the repo root sets indentation (4 spaces for Java/XML, 2 for YAML/JSON) and line-ending conventions — most IDEs pick it up automatically.
5. Clone, then run once to pull dependencies and generate the frontend bundle:

   ```bash
   ./mvnw spring-boot:run
   ```

   Alternatively, `docker compose up -d` starts the app *and* a MongoDB container together — see [DEPLOYMENT.md](DEPLOYMENT.md) if you'd rather not install MongoDB locally.

### Creating a test user

There is no sign-up screen — insert a user document directly. Generate a BCrypt hash for your chosen password (any BCrypt generator works, or run a one-off `PasswordEncoder.encode(...)` call from a scratch test), then insert via `mongosh`:

```js
use gymtracker
db.users.insertOne({
  role: "COACH",                 // ATHLETE | COACH | NUTRITIONIST
  email: "coach@example.com",
  password: "$2a$10$...",        // BCrypt hash — never plaintext
  firstName: "Jane",
  lastName: "Coach",
  enabled: true,
  createdAt: new Date(),
  updatedAt: new Date()
})
```

Athlete accounts should additionally set `age`, `gender`, `height`, `weight`, `activityLevel` since those fields drive fatigue/1RM calculations and the profile screen.

## Coding conventions

Enforced by convention (no linter/checkstyle configured today — please follow them by hand; `.editorconfig` covers whitespace/indentation mechanically):

- **Constructor injection only** — never `@Autowired` fields, never `new SomeRepository()` inside a service.
- **Interfaces in `service`, implementations in `service.impl` named `<X>Impl`.**
- **PascalCase** classes, **camelCase** methods/variables, **UPPER_SNAKE_CASE** constants.
- Prefer **enums** over raw strings for any fixed set of values (see existing enums in `com.gymtracker.enums` before adding a new one).
- Methods stay short and single-purpose; extract private helpers rather than growing a method past ~40 lines.
- **Never return `null` collections** — return an empty `List`/`Set`/`Map` instead.
- **Never swallow exceptions** — throw one of the existing custom exceptions (see [Exceptions](#exceptions)) with a meaningful message, or add a new one if none fits.
- Use **SLF4J** (`LoggerFactory.getLogger(...)`) — never `System.out`/`System.err`.
- Comment only the non-obvious (a business-rule reason, a workaround, an invariant) — not what the code already says.
- No TODOs or placeholder methods in submitted code — either implement it or don't add the stub.

## Naming conventions

| What | Convention | Example |
|---|---|---|
| Service interface | `<Module>Service` | `MesocycleService` |
| Service implementation | `<Module>ServiceImpl` | `MesocycleServiceImpl` |
| Repository | `<Entity>Repository` | `MesocycleRepository` |
| Request DTO (view → service) | `<Module>RequestDTO` | `MesocycleRequestDTO` |
| Response DTO (single-item write/read result) | `<Module>ResponseDTO` | `MesocycleResponseDTO` |
| Summary DTO (grid/list row) | `<Module>SummaryDTO` | `MesocycleSummaryDTO` |
| Detail DTO (single-item full read) | `<Module>DetailDTO` | `MesocycleDetailDTO` |
| Mapper | `<Module>Mapper` (MapStruct interface) | `MesocycleMapper` |
| Validator | `<Module>Validator` | `MesocycleValidator` |
| View (routed) | `<Module>View` | `MesocycleView` |
| Details dialog | `<Module>DetailsDialog` | `MesocycleDetailsDialog` |
| Custom exception | `<Reason>Exception`, extending the closest existing base | `MesocycleValidationException` |
| Enum | plural concept, singular type name, `UPPER_SNAKE_CASE` values | `MesocycleStatus.ACTIVE` |

Not every module needs every DTO suffix — e.g. read-only/system-generated modules (Alert) skip `RequestDTO`; aggregate-only modules (Dashboard, Statistics) skip `SummaryDTO`/`DetailDTO`. Add only what the view actually consumes.

## Layer-by-layer conventions

### Entities (`entity`)

- One class per collection, `@Document(collection = "...")`.
- Bean Validation annotations (`@NotNull`, `@NotBlank`, `@Positive`, `@Min`/`@Max`, ...) directly on fields — these are the single source of truth for constraints, reused by DTO validators.
- `@Indexed` on any field used in a derived query; `@CompoundIndex`/`@CompoundIndexes` when two or more fields are always queried together (check the query shape first — see [DATABASE.md](DATABASE.md#indexes) for existing examples).
- Embed small, always-together structures (sets inside a completed exercise, exercises inside a training day) as static inner classes rather than a separate collection.
- No business logic, no calculations.

### Repositories (`repository`)

- Extend `MongoRepository<Entity, String>` even though `@Id` is typed `ObjectId` — Spring Data handles the conversion transparently.
- Prefer **derived query methods** (`findByAthleteId`, `findByCoachIdAndStatus`) over `@Query`; reach for `@Query` only when a derived name can't express the query (see `NutritionPlanRepository.findByStatus` for an example using a mismatched field name).
- Add a batch method (`findByAthleteIdIn(Collection<ObjectId>)`) whenever a service would otherwise loop calling a single-item finder — N+1 query patterns are a recurring review finding in this codebase, avoid introducing new ones.

### Services (`service` / `service.impl`)

Every service method that touches another user's data must, in this order:

1. **Resolve the authenticated user** — inject `AuthenticatedUserProvider` (in the `security` package) and call `authenticatedUserProvider.getAuthenticatedUser()`. Do **not** write a new private `getAuthenticatedUser()`/`extractAuthenticatedEmail()` pair — that exact duplication existed across nine service classes before being centralized into this one component; a new service should depend on it from day one. (The one legitimate exception is a service that only needs the caller's email/authorities without a full `User` load — see `ExerciseServiceImpl`, which uses the provider's lower-level `requireAuthentication()`/`extractEmail(Authentication)` methods instead.)
2. **Validate the request** — delegate to the module's validator.
3. **Check authorization** — not just role, but relationship: an Athlete may only touch their own data; a Coach/Nutritionist may only touch data for athletes actually assigned to them. Inject `AthleteAssignmentService` (in `service`/`service.impl`) and call `isAthleteAssignedToCoach`/`isAthleteAssignedToNutritionist`/`assignedAthleteIdsForCoach`/`assignedAthleteIdsForNutritionist` — do not re-implement the `mesocycleRepository.findByCoachId(...).stream().anyMatch(...)` query again; that was the exact duplication this service replaced across seven other services.
4. Do the work (repository calls, calculations), then map to a DTO before returning.

Never let a "any coach/nutritionist can see everyone's data" shortcut creep back in — that exact class of bug was found and fixed across multiple services in a prior security review, and the fix (assignment-scoped access via `AthleteAssignmentService`) is the pattern to follow for anything new. See [API.md](API.md#cross-cutting-notes) for the two existing services (`FatigueService`, `OneRepMaxService`) that still lack this check — don't use them as a template for a new service.

### Mappers (`mapper`)

- MapStruct interface, `@Mapper(config = MapStructConfig.class)` (shared config sets `componentModel = "spring"`, `unmappedTargetPolicy = IGNORE`).
- Add `uses = ObjectIdMapper.class` whenever the mapper needs to convert between `ObjectId` and `String`.
- Never hand-write entity↔DTO conversion in a service — add a mapper method instead.

### Validation (`validation`)

- One validator per aggregate, extending `BaseValidator` (wraps a `jakarta.validation.Validator` for `@Valid`-annotated Bean Validation, plus a `requireCondition(condition, message, ExceptionConstructor::new)` helper for cross-field rules Bean Validation can't express — e.g. "new password must differ from current password").
- Called from the service, never from the view.

### Exceptions (`exception`)

| Exception | HTTP-equivalent meaning | Use for |
|---|---|---|
| `ValidationException` | 400 | request data fails validation |
| `ResourceNotFoundException` | 404 | referenced id doesn't exist |
| `UnauthorizedOperationException` | 403 | authenticated but not allowed to do this |
| `DuplicateResourceException` | 409 | unique-constraint violation (email, exercise name, duplicate plan) |
| `BusinessRuleException` | 422 | valid request, but violates a domain rule (e.g. deactivating an exercise still in use) |
| `InvalidOperationException` | 409 | operation not valid in the entity's current state |

`GlobalExceptionHandler` (`@RestControllerAdvice`) maps these for any future REST endpoint, but today every consumer is a Vaadin view, which catches these itself and shows a notification (see [View conventions](#views--ui) below) — it does not rely on the global handler, since there are no `@Controller`/`@RestController` endpoints for it to intercept.

### Views & UI (`view`, `ui.component`)

- One subpackage per module under `view`, named after the module (`view.mesocycle`, `view.alert`, ...).
- A module typically has: a `*View` (the `@Route`), a `*Grid`, a `*Form`, a `*FilterBar`, and a `*DetailsDialog` — follow this shape for a new module unless it genuinely doesn't need one of these pieces.
- **Reuse `ui.component` before writing a new UI primitive.** Available today: `Toolbar` (title + right-aligned actions), `SearchBar` (debounced, prefixed with a search icon), `EmptyState` (icon + title + message for "nothing to show"), `LoadingSpinner` (indeterminate progress + label), `StatCard` (icon + title + value), `AlertCard`, `NotificationBadge` (status pill), `ConfirmDialog` (confirm/cancel with a message), `Notifications` (static `success(...)`/`error(...)`/`warning(...)` — always use this instead of calling `Notification.show(...)` + setting theme variants by hand).
- Custom dialogs get the `app-dialog` CSS class (defined in `src/main/resources/themes/gymtracker/styles.css`) for a consistent header/footer border.
- Every create/update/delete action shows a success or error notification via `Notifications`; every load that can take a moment shows a `LoadingSpinner`; every empty list shows an `EmptyState` with a relevant icon — not a bare "no data" string.
- Icon-only buttons get `.setAriaLabel(...)`; every `Grid` gets `grid.getElement().setAttribute("aria-label", "...")`.
- Forms use `FormLayout.setResponsiveSteps(...)` (single column on narrow viewports, two columns above ~500px) — copy an existing form's responsive-step configuration.
- Views call **services only** — never a repository, never an entity.
- No view class carries a `@RolesAllowed`/`@PermitAll` annotation in this codebase — the actual security boundary is the service layer (see [ARCHITECTURE.md](ARCHITECTURE.md#authorization-model)). A view redirecting/hiding UI for the "wrong" role is a UX nicety, not something to rely on for access control.

## How to add a new module

Follow this sequence (it's the one every existing module was built with):

1. **Entity** — add the `@Document` class (or reuse an existing one) with the fields and indexes you need. Update [DATABASE.md](DATABASE.md) if you add a collection or index.
2. **Repository** — derived query methods for the access patterns you'll actually use.
3. **DTOs** — `*RequestDTO` (input), `*ResponseDTO`/`*SummaryDTO` (list output), `*DetailDTO` (single-item output with enrichment) as needed; only the fields the view actually needs. See [Naming conventions](#naming-conventions).
4. **Mapper** — MapStruct interface converting entity ↔ the DTOs above.
5. **Validator** — extend `BaseValidator`; centralize every rule here, not in the service.
6. **Service interface + `*ServiceImpl`** — inject `AuthenticatedUserProvider` and (if the module scopes data by coach/nutritionist assignment) `AthleteAssignmentService` rather than reimplementing either; add business rules, authorization, repository orchestration, DTO mapping. See [Services](#services-service--serviceimpl) above.
7. **View package** — `*View` (`@Route(value = "...", layout = MainLayout.class)`), plus grid/form/filter-bar/details-dialog as needed, reusing `ui.component` pieces.
8. **Navigation** — add the route to `NavigationMenu` for the role(s) that should see it.
9. **Tests** — repository test (`@DataMongoTest`), service test (Mockito), and a validator test if the validator has any cross-field logic.

Update [ARCHITECTURE.md](ARCHITECTURE.md#module-map), [API.md](API.md) and [USER_GUIDE.md](USER_GUIDE.md) once the module is user-facing.

## Best practices

- **Don't duplicate cross-cutting logic.** If you find yourself writing the same helper a second time (an authentication lookup, an assignment check, a name-resolution batch query), it likely belongs as a shared, constructor-injected component instead — see `AuthenticatedUserProvider` and `AthleteAssignmentService` for the established pattern.
- **Batch lookups, don't loop single-item finders.** Resolving names for a list of ids should be one `findAllById(ids)` call, not N calls to `findById`.
- **Push filtering into MongoDB.** List/search operations build `Criteria`/aggregation queries (with `Pattern.quote`-escaped regex for keyword search) rather than loading a whole collection and filtering in Java.
- **Re-check authorization on every call**, never assume a prior check (e.g. in the view) already covered it — the service layer is the actual security boundary.
- **Keep PRs scoped.** This project has consistently been built one module/concern at a time; a PR mixing an unrelated refactor with a feature is harder to review and revert.
- **Don't add dependencies casually.** Every new dependency was added for a stated reason in this project (e.g. Caffeine to replace Spring's unbounded default cache map, MapStruct to avoid hand-written mapping) — do the same: know why before adding one.

## Testing

```bash
./mvnw test                                  # full suite
./mvnw test -Dtest=MesocycleServiceImplTest   # a single test class
```

JaCoCo writes an HTML coverage report to `target/site/jacoco/index.html` after `./mvnw test`. The same commands run in CI (`.github/workflows/ci.yml`) against a real `mongo:7.0` service container — see [Continuous integration](#continuous-integration) below.

### Repository tests

`@DataMongoTest` against a real local MongoDB, pointed at a dedicated `gymtracker_test` database (`src/test/resources/application.properties`) — **never** the dev database. Verifies derived queries and indexes actually behave as expected against a real MongoDB engine (not a mock).

### Service tests

JUnit 5 + Mockito + AssertJ, one test class per `*ServiceImpl`. The established pattern:

- Mock repositories and collaborating services with `@Mock` (`@ExtendWith(MockitoExtension.class)`).
- Use the **real, generated** MapStruct mapper and the **real** Bean Validation validator instead of mocking them — this exercises the actual mapping/validation code, not a stub of it.
- A generated MapStruct mapper impl has an `@Autowired private ObjectIdMapper objectIdMapper` field that's `null` when you `new` the mapper directly in a test — wire it manually:
  ```java
  ReflectionTestUtils.setField(mapper, "objectIdMapper", new ObjectIdMapperImpl());
  ```
- Construct a **real** `AuthenticatedUserProvider` (and, if the service under test needs it, a real `AthleteAssignmentServiceImpl`) wrapping the test's already-mocked repositories, rather than mocking `AuthenticatedUserProvider`/`AthleteAssignmentService` themselves:
  ```java
  AuthenticatedUserProvider authenticatedUserProvider = new AuthenticatedUserProvider(userRepository); // userRepository is @Mock
  AthleteAssignmentService athleteAssignmentService =
          new AthleteAssignmentServiceImpl(mesocycleRepository, nutritionPlanRepository); // both @Mock
  ```
  This requires zero changes to existing `authenticateAs()`/`SecurityContextHolder` test helpers and zero changes to existing repository stubs — it's an additive constructor-injection change only.
- Simulate an authenticated user via `SecurityContextHolder` + `TestingAuthenticationToken`, and clear the context in `@AfterEach`:
  ```java
  TestingAuthenticationToken token = new TestingAuthenticationToken(email, null);
  token.setAuthenticated(true);   // the 2-arg constructor defaults to NOT authenticated
  SecurityContextHolder.getContext().setAuthentication(token);
  ```
  Use a `CustomUserDetails` instance (not a plain email string) as the principal for any service that casts the principal directly to `UserDetails` (e.g. `StatisticsServiceImpl`, which was deliberately left with its own distinct `getAuthenticatedUser()` rather than migrated to `AuthenticatedUserProvider`, since its behavior differs subtly).
- When testing a service method guarded by `@Cacheable` through a self-injected proxy (see `StatisticsServiceImpl`'s `@Lazy StatisticsServiceImpl self` pattern), construct the service with `self = null` and then `ReflectionTestUtils.setField(service, "self", service)` — a plain `new`'d instance has no Spring proxy, so the cache annotation is simply inert in the test, which is fine; you're testing the business logic, not the caching.
- Mockito's inline mock maker cannot instrument certain concrete framework wrapper classes on very new JDKs (observed: `VaadinServletRequest`/`VaadinServletResponse`, `MongoTemplate`, `HttpSession` in some configurations). Two ways around it, both already used in this codebase:
  - Prefer the **interface** where one exists and the class is otherwise a drop-in (`MongoOperations` instead of `MongoTemplate` — this is why service constructors take `MongoOperations`, not `MongoTemplate`).
  - Where only a concrete wrapper class exists, construct a **real instance** of it wrapping a mocked plain interface, instead of mocking the wrapper class itself (see `AuthenticationServiceImplTest` building a real `VaadinServletRequest` around a mocked `HttpServletRequest`).
- Mockito is in **strict stubbing** mode — an `@Mock` stub that's never consumed by the code path under test fails the test (`UnnecessaryStubbingException`). If a shared `@BeforeEach`/helper stub isn't hit on every path, mark it `lenient()`.

### What to test for a new service method

- The happy path, with a real mapper/validator.
- Every authorization branch (self, assigned, not-assigned, wrong role) — assert `UnauthorizedOperationException`.
- Every validation failure path you added.
- Any calculation, with a hand-computed expected value (see `FatigueServiceImpl`/`OneRepMaxServiceImpl` test classes for the pattern of asserting derived numeric fields).

## Continuous integration

`.github/workflows/ci.yml` runs on every push and pull request: checkout → JDK 21 (Temurin, Maven dependency cache) → `./mvnw compile test-compile` → `./mvnw test` (against a `mongo:7.0` service container, since repository/integration tests need a real MongoDB) → `./mvnw package -DskipTests`. Any non-zero exit fails the job. JaCoCo's report is uploaded as a build artifact; surefire reports are uploaded on failure for debugging.

To run the same checks locally before opening a PR:

```bash
chmod +x mvnw            # if needed
./mvnw -B compile test-compile
./mvnw -B test
./mvnw -B package -DskipTests
```

## Contribution workflow

1. Create a branch off `main`.
2. Make focused commits — one logical change per commit, following the existing style (imperative summary line).
3. Run `./mvnw test` before opening a PR; all tests must pass (CI will also run this — see above).
4. Open a PR against `main` with a summary of what changed and why, plus a test plan.
5. Keep PRs scoped — this project has consistently been built one module/concern at a time; a PR that mixes an unrelated refactor with a feature is harder to review and revert.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `UnsupportedClassVersionError` or JaCoCo `Unsupported class file major version` warnings during tests | Running on a JDK newer than the ones JaCoCo/Mockito's bundled ASM fully recognize | Usually benign — check the surefire report's `Tests run`/`Failures`/`Errors` line rather than the console warnings; real coverage is still produced |
| A generated mapper throws `NullPointerException` on `objectIdMapper` in a unit test | The mapper was `new`'d directly instead of obtained from Spring | `ReflectionTestUtils.setField(mapper, "objectIdMapper", new ObjectIdMapperImpl())` |
| `UnnecessaryStubbingException` after refactoring a service method | A `when(...)` stub in the test is no longer reached by the (changed) code path | Remove the stub, or mark it `lenient()` if it's still needed by other tests sharing the same setup |
| A Vaadin static asset (icon, manifest) 404s for an unauthenticated user | `SecurityConstants.PUBLIC_ROUTES` doesn't include that static path | This is a known, low-priority gap — most-visited paths (`/`, `/login`, `/error`, `/static/**`) are covered; extend `PUBLIC_ROUTES` if you hit a real broken asset |
| Login "succeeds" in a test but `SecurityContextHolder` is empty afterward | Forgot `token.setAuthenticated(true)` | The 2-arg `TestingAuthenticationToken` constructor defaults to unauthenticated |
| `./mvnw` fails with a `$'\r': command not found`-style error on Linux/CI/Docker | `mvnw` was checked out with CRLF line endings (e.g. `core.autocrlf=true` on Windows without `.gitattributes` being respected) | Shouldn't happen — `.gitattributes` forces `mvnw` to LF; if it does, re-checkout the file (`git checkout -- mvnw`) after confirming `.gitattributes` is present |
| Docker build fails inside `./mvnw` trying to download Maven | Base image lacks `curl`/`wget`/`unzip`, which the wrapper's "only-script" distribution needs to fetch Maven itself | Already handled in the provided `Dockerfile` build stage (`apk add --no-cache curl unzip`); if you change the base image, keep this |
