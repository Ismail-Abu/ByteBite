# ByteBite Android — implementation status

A resumable checkpoint of the Android implementation work. Update at meaningful
milestones and before any session limit.

- **Repository:** `Ismail-Abu/ByteBite-archive` (private), remote `archive`.
- **Working branch:** `archive-improvements` (tracks `archive/main`).
- **Baseline commit (per brief):** `5411c3c` (planning). Prior in-progress work
  on this branch up to `e431ebc` (accessibility labels + model/sample tests).
- **This session's head:** see `git log` (commits listed under "Done" below).
- **Planning docs:** `docs/planning` was merged in from `archive/main` (it had
  not been on this branch); implementation and planning now coexist.
- **Git identity:** commits are authored/committed as
  `Ismail Abu-shanab <hdsplash06@gmail.com>` (the repo's configured identity,
  matching the GitHub account); no AI/bot attribution is added.

## Update — professional UI redesign + correctness pass

The three sample-data prototype experiences were replaced with one coherent
Material 3 app (bottom nav Today / History / Insights; Settings in the app bar;
dedicated Add/Edit and Detail screens; light + dark themes; no emojis; honest
unavailable states). A design system lives in `ui/theme`, reusable components in
`ui/components` and `meal/ui`. See `docs/design/android-ui.md` for references,
tokens, the screen map, and QA screenshots.

The eight review correctness issues were fixed with regression tests:
preserved occurrence time on edit (#1), recoverable save/load/delete failures
(#2), full draft restore via SavedStateHandle (#3), duplicate-save guard (#4),
explicit number-parsing policy (#5), partial-total handling that still counts a
meal with unknown values (#6), Today day-rollover on resume (#7), and an
observed single-query + LazyColumn history (#8).

## Update — design-system, privacy, and reliability pass

- **Color system completed**: every Material role defined for light and dark
  from the teal/slate palette, removing the unintended violet from the
  navigation indicator, dialogs, and container surfaces. Status/navigation bar
  icon contrast now follows the resolved theme.
- **Today** summary is responsive (one row, else 2x2) and holds up at 200% font
  (units no longer wrap mid-word); unknown ("—") vs known-zero ("0") vs partial
  ("Known totals") stay distinct.
- **Privacy/backup**: the meal database (and future meal images) are excluded
  from OS cloud backup and device transfer; README/README_MODEL/data-storage
  text updated to match; `BackupRulesTest` guards it.
- **Settings**: concise capability statuses with technical detail in an
  expandable, storage figure labelled "Database size" (not total app size),
  destructive delete moved to a "Danger zone"; delete-all has progress, a
  retryable failure path (data intact), success feedback, cancellation-safe
  handling, and an injectable IO dispatcher.
- **Add/Edit**: discard-changes confirmation on back once the form is dirty;
  the edited flag survives process recreation.
- **Fixtures**: `androidTest/assets/photocheck/README.md` documents the 27
  food/non-food images as held-out software-behaviour fixtures and flags that
  their per-image source/license is not yet recorded and must be confirmed
  before reliance/publication.

Verified on the emulator: Today (empty / populated / partial), Settings, and
light + dark themes, plus a 200% font-scale pass. Not yet exercised this pass:
a 320 dp narrow device, landscape, and TalkBack (reported as unverified).

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

Milestone 3 (partial) — a working, persisted meal surface reading the data
layer (the foundation is now used, not just present):

5. **`MealLogViewModel`** over the repository: saved-meal list + today summary
   (sums only known values) + manual-entry/correction form. Validation via
   `MealInput`; draft ids in `SavedStateHandle` so a save repeated after process
   recreation upserts rather than duplicates; editing routes through
   `correctMeal`. `AppGraph` is the service-locator seam (Room in production,
   overridable in tests).
6. **`MealLogScreen`** (stateless Compose, prototype's warm visual character):
   honest empty state, per-field and form-level validation errors, "Saved"
   confirmation, a saved-meal history list with an "edited" badge, and
   edit/delete with a delete confirmation dialog. `MainActivity` now starts on
   this persisted surface; the three sample-data experiences remain reachable
   behind a "Design prototype" link until they are consolidated.

## Checks run (latest)

- `testDebugUnitTest`: **PASS** — **77 tests, 0 skipped**. Domain/repo/mappers,
  `MealInputTest` (25, incl. the number-parsing policy), the view-model
  regression tests (AddEdit, Today, History, Settings), `BackupRulesTest`, and
  **Compose UI tests on the JVM via Robolectric** (Today, Add, Screens). Note:
  Robolectric tests are JVM tests, not real-device tests.
  `connectedDebugAndroidTest` (Room + migration, 11, 0 failures;
  `ModelFixtureTest` 3 skipped without model assets) re-run green on the
  emulator after this design pass.
- `assembleDebug`: **PASS** — `app/build/outputs/apk/debug/app-debug.apk`
  (~66 MB; size dominated by the LiteRT native libraries).
- `connectedDebugAndroidTest` on `Medium_Phone`: **PASS**, 11, 0 failures —
  `RoomMealRepositoryInstrumentedTest` (7) + `MealMigrationTest` (1);
  `ModelFixtureTest` (3) skipped (no model assets).
- `lintDebug`: **PASS** (no errors). ~33 warnings (version / unused-import
  suggestions); the two `LocalDate.EPOCH` NewApi errors were fixed.
- **Compose UI tests now run, not skipped.** The previous Espresso-on-API-37
  incompatibility was resolved by moving Compose UI tests to Robolectric (JVM),
  which uses its own input and runs in the ordinary unit-test task.
- **Manual on-device walkthrough** (emulator, screenshots in
  `docs/design/screenshots`): Today (empty / partial), Add meal (form,
  validation), History, Settings, and **light + dark** themes; plus the earlier
  log → force-stop → relaunch persistence check.

## Remaining (next milestones, in brief order)

3. **UI consolidation — DONE** this pass (Today / History / Detail / Add-Edit /
   Insights / Settings, bottom nav, light+dark, prototype removed). Remaining in
   this area: optional meal photo and a linked-forecast panel (depend on capture
   / glucose below).
   Earlier wording (now historical): folding the capture/glucose experiences
   onto the persisted store so the sample-data
   prototype and its chooser can be retired from the production flow.
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
