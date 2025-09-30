# TALauncher Re-Architecture Plan

## Summary
- Reorganize the project into clearly separated Android modules (app shell, feature modules, data sources, and shared core utilities) with Jetpack best practices such as single-activity navigation, Hilt dependency injection, and lifecycle-aware coroutines.
- Introduce a Clean Architecture-inspired layering (presentation → domain → data) backed by explicit use cases, repositories, and DTO-to-entity mappers to make business rules SOLID and DRY.
- Modernize platform integrations (Room, DataStore, Retrofit, WorkManager, Foreground Service APIs) and cross-cutting concerns (error handling, logging, permissions) with well-defined patterns (Repository, Use Case Command, Strategy, Facade, Adapter, Observer) so each component has a single responsibility.
- Deliver session-expiry friction flows entirely within Compose-driven surfaces that follow Android dialog best practices.
- Provide a phased delivery roadmap with guardrails for testing, migration, and developer onboarding tailored for a junior engineer to follow step by step.

## Current Architecture Assessment (What We Have Today)
1. **App shell (`MainActivity.kt`) manually wires repositories and helpers.**
   - No dependency injection framework; `MainActivity` constructs the Room database, repositories, helpers, and error handler directly.
   - The activity also controls navigation logic, pager state, permission checks, and error presentation, violating single responsibility.
2. **Presentation layer tightly couples UI and business concerns.**
   - `HomeViewModel` manages app search, session expiry, contacts, weather, and permissions in one class (~900+ lines) making it hard to test and extend.
   - Compose screens directly reference repositories or helpers through the view model without clear use case boundaries.
3. **Data layer is Room-centric and monolithic.**
   - Room entities (`AppInfo`, `LauncherSettings`, `AppSession`) double as domain models and UI models.
   - Settings that behave like preferences live in Room, making migrations verbose and increasing disk I/O on simple toggles.
   - Networking (`WeatherService`) is a synchronous HTTP client with manual JSON parsing and no caching.
4. **Services and helpers lack lifecycle boundaries.**
   - Previous countdown services relied on raw `WindowManager` operations with Compose UI inside the service; similar lifecycle gaps remain for surviving helpers.
   - Permission and contact helpers manage UI flow from non-UI layers.
5. **Testing & tooling gaps.**
   - Few dedicated unit tests for repositories/use cases; instrumentation tests are not organized per feature.
   - Error handling relies on a mutable `MainErrorHandler` singleton-like helper rather than structured result types.

## Target Architecture Overview (What We Are Building)
We will adopt a modular, Clean Architecture-aligned Compose app structured around Jetpack best practices, ensuring Android conventions override other SOLID/DRY trade-offs when conflicts arise.

### Text-Based Architecture Diagram
```
app (launcher-shell)
├── presentation (Compose Navigation host)
│   ├── feature-home (Home UI + ViewModel)
│   ├── feature-settings
│   ├── feature-onboarding
│   └── feature-insights
├── di (Hilt entry points, module wiring)
└── activity (MainActivity hosting NavHost + app-level scaffolding)

core
├── core-ui (design system, reusable Compose components)
├── core-common (Result wrappers, error models, logging facade)
└── core-testing (shared test utilities)

data
├── data-apps (Room DAO, Retrofit services, repository impl)
├── data-settings (DataStore, Room migrations if needed)
├── data-sessions (Session DAO + WorkManager sync)
└── data-weather (Retrofit client, DTOs, mappers)

domain
├── domain-model (pure Kotlin models separate from persistence)
├── domain-usecase (UseCase classes exposing suspend operator invoke)
└── domain-repository (interfaces consumed by ViewModels)
```

## Guiding Principles & Patterns
1. **Android-first practices**
   - Single-activity app with `Navigation Compose` driving destinations instead of a manual `HorizontalPager` for navigation.
   - Hilt for lifecycle-aware dependency injection (`@HiltAndroidApp` application class, entry-point injected ViewModels with `@HiltViewModel`).
   - Coroutine + Flow usage scoped to ViewModel and repository lifecycles; use `repeatOnLifecycle` in UI composables.
2. **SOLID & DRY via Clean Architecture**
   - **S**ingle Responsibility: split `HomeViewModel` responsibilities into dedicated use cases (`ObserveVisibleApps`, `SyncInstalledApps`, `ManageSessionExpiry`, `SearchContacts`, `FetchWeather`), each backed by domain services.
   - **O**pen/Closed & Strategy pattern: provide a `FrictionStrategy` interface with implementations for `DistractingAppFriction`, `SessionExtension`, enabling easy extension of friction logic.
   - **L**iskov / **I**nterface Segregation: domain repositories expose only required functions; e.g., `AppRepository` interface for read/write operations, `SessionRepository` interface for timers, `WeatherRepository` for weather data.
   - **D**ependency Inversion: presentation depends on interfaces in the domain layer; Hilt modules bind data implementations.
   - Keep logic DRY by consolidating permission flows into reusable classes (Facade pattern) that each feature can call.
3. **Design Patterns to employ**
   - **Repository** for data abstraction (already conceptually present; formalize through interfaces in domain).
   - **Use Case (Command)** pattern for domain actions (one public `suspend operator fun invoke` per action).
   - **Strategy** for friction/permission handling to swap behaviours based on settings.
   - **Facade** for system services (e.g., `PermissionFacade`, `UsageStatsFacade`) to hide Android API complexity from ViewModels.
   - **Adapter** for bridging platform data (`PackageManager`, `Contacts`, `Location`) into domain models.
   - **Observer** (Flows) for reactive UI updates.

## Implementation Roadmap (Step-by-Step for a Junior Developer)
### Phase 0 – Foundation & Tooling
1. **Add project scaffolding**
   - Enable Gradle version catalogs if not present; ensure Kotlin, Compose, Hilt, Room, Retrofit, DataStore dependencies defined centrally.
   - Introduce `LauncherApplication` annotated with `@HiltAndroidApp`.
2. **Create module skeletons**
   - Convert existing single module into multi-module Gradle setup as per diagram: `:app`, `:core:ui`, `:core:common`, `:core:testing`, `:domain:model`, `:domain:usecase`, `:domain:repository`, `:data:apps`, `:data:settings`, `:data:sessions`, `:data:weather`.
   - Configure `app` module to depend only on `core` and `domain`; data modules depend on `core-common` and expose bindings through Hilt.
3. **Set up shared linting & static analysis**
   - Add `ktlint` or `spotless`, `detekt`, and `gradle lint` tasks to CI; ensure `run_all_tests.bat` maps to Gradle commands.

### Phase 1 – Domain & Data Separation
1. **Define domain models** (`domain-model`)
   - Create pure Kotlin data classes (`LauncherApp`, `AppSession`, `LauncherSettings`, `WeatherSnapshot`, etc.) separate from Room entities to avoid leaking persistence concerns to UI.
   - Provide mapper interfaces (`AppEntityMapper`, `SettingsMapper`) in `data` modules implementing `Adapter` pattern.
2. **Declare repository interfaces** (`domain-repository`)
   - `AppsRepository`, `SettingsRepository`, `SessionRepository`, `WeatherRepository`, `ContactsRepository`, `PermissionsRepository` (if needed).
   - Include suspend/Flow-based APIs aligned with use cases (e.g., `fun observeVisibleApps(): Flow<List<LauncherApp>>`).
3. **Implement repositories in data modules**
   - Move Room DAOs and DataSource classes into `data-*` modules, mapping to domain models.
   - Replace `WeatherService` with Retrofit + Kotlin Serialization/Moshi service; add `OkHttpClient` with logging & timeout interceptors.
   - Migrate user preference-like data from Room to `DataStore` (settings that change frequently). Keep Room for complex data (apps, sessions).
   - Introduce caching strategy for weather (Room table or DataStore with timestamp) to avoid repeated network calls.
4. **Expose repository bindings through Hilt modules**
   - Create `@Module` `@InstallIn(SingletonComponent::class)` providing DAOs, Retrofit services, DataStore instances, and binding repository implementations to interfaces.

### Phase 2 – Use Cases & Business Logic Consolidation
1. **Create use case classes** (`domain-usecase`)
   - Examples: `ObserveEssentialApps`, `ToggleAppVisibility`, `LaunchAppWithFriction`, `StartSession`, `HandleSessionExpiry`, `FetchWeatherSnapshot`, `UpdateLauncherPreferences`, `SyncInstalledApps`, `SearchContacts`.
   - Each use case should be a small class with a single `invoke` function returning a `Flow` or `Result` wrapper.
2. **Centralize error handling**
   - Replace `MainErrorHandler` and ad-hoc `showError` calls with sealed `AppError` types in `core-common` and `Result` wrappers returned by use cases.
   - Compose layer observes `UiState` containing `AppError?` to show dialogs.
3. **Refactor session timing logic**
   - Move timing responsibilities from `HomeViewModel` into `SessionManager` use cases leveraging `WorkManager` or coroutine timers scoped to repository.
   - Use `WorkManager` for background cleanup tasks (expired session cleanup, periodic sync) respecting Android lifecycle.
4. **Streamline permissions and session expiry coordination**
   - Drive friction and session-expiry surfaces through a `SessionExpiryCoordinator` that emits UI events/dialog states the main activity can render.
   - Create `PermissionCoordinator` use case to evaluate & request required permissions via events the UI can react to.

### Phase 3 – Presentation Layer Rebuild
1. **Replace pager navigation with `NavHost`**
   - Define `NavGraph` with destinations: `home`, `settings`, `insights`, `onboarding`, `frictionDialog` (as dialog destination), `sessionExpired` (dialog route).
   - Use `rememberNavController()` with `NavHost` in `MainActivity` Compose content; remove manual pager navigation side effects.
2. **Refactor `MainActivity`**
   - Inject dependencies using Hilt; `MainActivity` becomes a thin host retrieving `MainViewModel` via `hiltViewModel()`.
   - Move startup logic (database initialization, commit info) to `MainViewModel` + use cases.
   - Use `lifecycleScope` only for UI-bound side effects; rely on `ViewModel` for long-lived flows.
3. **Rebuild feature ViewModels**
   - `HomeViewModel`: orchestrates UI state by combining dedicated use cases (apps, sessions, weather). Keep it under ~200 lines by delegating to helper classes.
   - `SettingsViewModel`: interacts with `UpdateLauncherPreferences`, `ObserveSettings` use cases.
   - `OnboardingViewModel`: uses permission coordinator, session repository for initial data.
   - `InsightsViewModel`: aggregates session stats via dedicated use case (`ObserveUsageInsights`).
4. **Compose best practices**
   - Move UI-only components into `core-ui` (typography, color palettes, dialogs) to avoid duplication.
   - Provide preview parameter providers for `UiState` classes.
   - Use `collectAsStateWithLifecycle` from `androidx.lifecycle:lifecycle-runtime-compose` for Flow observation.
5. **Session expiry UI modernization**
   - Present session-expiry and friction flows inside the main activity via Compose dialogs or sheets that respect activity lifecycle constraints.
   - Ensure any background reminders rely on notifications rather than `SYSTEM_ALERT_WINDOW`, aligning with modern Android restrictions.

### Phase 4 – Feature Enhancements & Cross-Cutting Concerns
1. **Contacts & communications**
   - Introduce `ContactsRepository` with `ContentResolver` adapter, exposing flows for search. Move logic out of `HomeViewModel`.
   - Provide `CallActionUseCase` / `MessageActionUseCase` to encapsulate intents and permission checks.
2. **Weather**
   - Add `WeatherRefreshScheduler` using `WorkManager` to fetch updates periodically and cache results.
   - Expose `Flow<WeatherState>` to UI; include offline caching and fallback states.
3. **Friction & session flows**
   - Model friction states as `sealed class FrictionState` in domain; UI shows appropriate dialogs via Compose navigation.
   - Use `Strategy` pattern to switch friction type based on settings (math challenge vs. timer vs. no friction).
4. **Error logging & analytics**
   - Implement `core-common` logging facade (Timber or `Logcat` wrapper) and optional crash reporting hooking into Hilt modules.
   - Provide `ErrorReporter` interface for future integrations; default implementation logs to Logcat.

### Phase 5 – Testing & Quality Gates
1. **Unit tests**
   - Write tests for each use case using Kotlin Coroutines test library.
   - Provide in-memory Room/`TestDispatcher` setups in `core-testing`.
2. **Integration tests**
   - Compose UI tests per feature module verifying state rendering and navigation transitions.
   - Add instrumentation tests for permission workflows and friction/session-expiry dialogs using `Mockk`/`Robolectric` where appropriate.
3. **Automation**
   - Update GitHub Actions (or equivalent) to run unit + instrumentation tests, lint, detekt, and assemble tasks.
   - Add baseline profile generation task for performance-critical code if Play Store distribution is planned.

## Data Migration Strategy
1. **Room → Domain mapping**
   - Keep existing Room schema but migrate entity classes into `data` modules; update `@Database` version as needed.
2. **Settings migration**
   - Gradually move frequently toggled preferences to `DataStore`. Provide migration logic reading existing Room rows and writing to DataStore during first app launch post-update.
   - Keep essential settings in Room until migration is complete; mark deprecated columns for removal in future versions.
3. **Weather caching**
   - Add new Room table or DataStore entry with timestamp; provide fallback to last-known value to minimize API calls.

## Developer Workflow Notes for Junior Engineers
1. **Follow feature branches per phase**
   - Create a feature branch per phase (e.g., `feature/hilt-setup`, `feature/domain-layer`). Complete and merge each before starting the next.
2. **Coding checklist**
   - Add/modify code only within the relevant module to maintain separation.
   - Ensure every new `ViewModel` constructor is annotated with `@Inject` and uses constructor-injected use cases.
   - For Compose screens, keep them stateless and receive `UiState` + event callbacks.
3. **Testing checklist**
   - Run `./gradlew lint ktlintCheck detekt` for static analysis.
   - Run `./gradlew testDebugUnitTest` for unit tests and `./gradlew connectedDebugAndroidTest` for instrumentation when applicable.
4. **Documentation**
   - Update `README.md` with module overview once refactor is complete.
   - Maintain changelog entries summarizing major architectural shifts.

## Risks & Mitigations
1. **Large refactor scope** – Mitigate by delivering in phases with feature flags and regression tests at each milestone.
2. **Data migration complexity** – Write migration tests using Room’s `MigrationTestHelper`; ensure backup/restore is validated.
3. **Session-expiry reminders on newer Android versions** – Follow current Android 14+ requirements for foreground services and prefer notifications/dialogs over `SYSTEM_ALERT_WINDOW`, keeping any user education screens within the main activity flow.
4. **Weather API rate limits** – Add caching and throttle via WorkManager; expose failure states gracefully to avoid user-facing errors.

## Acceptance Criteria for Completion
- App builds and runs using Hilt-injected dependencies with the new module structure.
- Each feature has its own ViewModel bound to use cases and domain models; Compose screens remain stateless.
- Room entities no longer leak to UI; domain models are used in ViewModels and Compose.
- Session and friction flows are orchestrated via domain use cases and strategies, not direct service calls from ViewModels.
- Automated tests cover critical flows (app launch, onboarding, home interactions, session expiry).
- Documentation updated to reflect new architecture and developer onboarding steps.
