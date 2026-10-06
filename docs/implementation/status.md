# ByteBite Android — implementation status

A resumable checkpoint of the Android implementation work. Update at meaningful
milestones and before any session limit.

- **Repository:** `Ismail-Abu/ByteBite-archive` (private), remote `archive`.
- **Working branch:** `archive-improvements` (tracks `archive/main`).
- **Baseline commit (per brief):** `5411c3c` (planning). Prior in-progress work
  on this branch up to `e431ebc` (accessibility labels + model/sample tests).
- **This session's head:** see `git log` (commits listed under "Done" below).

## Build environment (verified this session)

- JDK: Android Studio **JBR 21** at `C:\Program Files\Android\Android Studio\jbr`.
  The system default JDK is 25, which Gradle 8.14.2 does **not** support — builds
  must run with `JAVA_HOME` pointed at the JBR (or any JDK 17–21).
- Android SDK at `%LOCALAPPDATA%\Android\Sdk` (platforms 36 / 36.1, build-tools
  35 & 36). `android/local.properties` is generated locally (git-ignored) with
  `sdk.dir` pointing there.
- Emulator: AVD `Medium_Phone` (system image android-37.1) boots headless and
  runs instrumented tests.
- Toolchain: AGP 8.11.0, Gradle 8.14.2, Kotlin 2.2.0, Compose BOM 2024.12.01,
  KSP 2.2.0-2.0.2, Room 2.7.2.

### Commands

From `android/`, with `JAVA_HOME` set to the JBR:

```
./gradlew testDebugUnitTest        # JVM unit tests
./gradlew assembleDebug            # debug APK
./gradlew lintDebug                # Android lint
./gradlew connectedDebugAndroidTest  # instrumented tests (emulator/device)
```

## Done (this session)

Milestone 2 — persistence + domain foundation, replacing the prototype's
scattered in-memory `SampleData` with a real, tested data layer:

1. **Meal domain** (`com.example.guione.meal`): `Nutrition` (five figures, every
   field nullable so unknown never becomes a silent zero), `Meal` /
   `MealRevision` (UUID identity, occurrence instant + zone offset, revision
   chain preserving the original estimate), `NutritionSource`.
2. **Manual-entry validation** (`MealInput`): clock-injected, pure. Blank vs
   explicit zero, Unicode/long names, decimal + locale (comma) input,
   negative/enormous/NaN/infinity/malformed/half-typed numbers, at-least-one-value
   rule, future-time policy with skew tolerance.
3. **Repository seam** (`MealRepository`) with two implementations held to the
   same behaviour: `InMemoryMealRepository` (JVM test double / pre-Room store)
   and `RoomMealRepository` (durable). Idempotent upsert on caller-minted ids
   (no duplicate on double-tap/retry/recreate); transactional save; correction
   appends a revision and keeps the original; delete cascades to a meal's own
   revisions only.
4. **Room** (`com.example.guione.meal.db`): entities with nullable nutrient
   columns and a `CASCADE` foreign key; DAO; `MealDatabase` (version 1, schema
   export on at `app/schemas`, **no destructive-migration fallback**); pure
   mappers; `MigrationTestHelper` harness wired via androidTest assets.

## Checks run (this session)

- `testDebugUnitTest`: **PASS** — 57 tests (baseline 22 + 35 new). New:
  `MealInputTest` (23), `MealRepositoryTest` (9), `db/MealMappersTest` (3).
- `assembleDebug`: **PASS** — `app/build/outputs/apk/debug/app-debug.apk`
  (~66 MB; size dominated by the LiteRT native libraries).
- `connectedDebugAndroidTest` on `Medium_Phone` (android-37): **PASS** — 11 run,
  0 failures. New `RoomMealRepositoryInstrumentedTest` (7, incl. the FK cascade
  and idempotent save on real SQLite) and `MealMigrationTest` (1) genuinely
  verified; pre-existing `ModelFixtureTest` (3) skipped via assumption because
  the model assets are absent (expected — a skipped fixture is not a release
  pass for ML readiness).
- `lintDebug`: **PASS** (no errors). 27 pre-existing warnings, all version /
  obsolete-SdkInt / unused-resource suggestions; none introduced by the data
  layer and none from its logic.

## Remaining (next milestones, in brief order)

3. **Wire the UI to the repository** and consolidate navigation: a single app
   (Fusion as the base), real Home/History/Details/Edit/Settings reading the
   repository, honest empty states, remove the prototype chooser from the
   production flow. **Not started** — the existing three-experience Compose UI
   still runs on in-memory `SampleData`.
4. **Capture pipeline as an explicit state machine** (quality → food-gate →
   inference → validation → review → save) with request tracking; move the
   `ScanStore` sealed-state idea into a proper view model over the repository.
5. **Food acceptance service** — honest `Accepted/NonFood/PoorQuality/Unsupported/
   Uncertain/Unavailable` boundary; keep auto estimation gated off in production
   until a validated classifier exists. A `photocheck/` food/non-food fixture
   set is staged under `androidTest/assets` (uncommitted) for the eval harness.
6. **Glucose boundary** — versioned `GlucosePredictor` interface with explicit
   unavailable/missing-input states; no guessed medical formula.
7. Integration/Compose flow tests, accessibility, performance/storage
   measurement, documentation, CI.

## External blockers

- **Model assets** not committed (22 MB `.tflite` + sidecar + fixtures). Without
  them nutrition inference and `ModelFixtureTest` cannot run; the app runs on
  sample data and says so. Needed: run `notebooks/bytebite_android_export.ipynb`.
- **Glucose model + contract** not in this repo. Needed before any real forecast:
  ordered features, units, normalization, history/cadence, freshness, output
  shape, target population, distribution permission.
- **Food-gate classifier** artifact + licensing + held-out eval set not yet
  established.
- **`gh` CLI not authenticated** in this environment; `git` fetch to the private
  `archive` remote works. PR creation may need auth or the web UI — see handoff.

## Design decisions / tradeoffs

- Introduced a repository interface with an in-memory impl as the spec/test
  double and a Room impl as the production store, rather than reaching for a DI
  framework — simple constructor injection suits the project size.
- Time stored as UTC epoch-ms + offset-seconds; ms resolution is ample for a
  logged meal and keeps the schema primitive (no type converters).
- Kept the existing prototype building and running untouched this session; the
  new data layer is additive, so nothing regressed while the foundation lands.
