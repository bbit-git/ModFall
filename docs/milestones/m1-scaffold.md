# M1 — Project Scaffold & Core Data Models

**Status:** Completed
**Depends on:** —

## Scope

- Android project setup: `MainActivity`, portrait lock, Compose entry point, theme
- Manifest config (`supportsRtl`, `minSdk 28`)
- Splash screen using `androidx.core:core-splashscreen` with **placeholder icon** on deep purple-black background
- **Placeholder adaptive icon** (simple solid-colour square); real icon created manually later
- All data models: tetromino shapes, SRS rotation/kick tables, `BoardState`, `GameState` enum
- `GameConstants` object with all tunable values
- `CandyPalette` colour definitions + HSL derivation
- `strings.xml` baseline (English) + 17 empty locale placeholders
- Package structure in place with placeholder files
- `GameViewModel` skeleton with lifecycle observer (auto-pause on `ON_STOP`)
- DI wiring: `ViewModelFactory` with manual constructor injection

## Exit criteria

App builds, launches splash screen (placeholder icon), shows an empty Compose screen. All core data classes compile and are unit-testable.

## Work breakdown

- [x] Create the Android app scaffold: `MainActivity`, Compose app entry point, portrait lock, theme, and package layout.
- [x] Set base platform config: `minSdk = 28`, `android:supportsRtl="true"`, and splash-screen dependency/config.
- [x] Add a placeholder adaptive icon and placeholder splash treatment that matches the planned background direction.
- [x] Define core models: tetromino types, shape definitions, rotation state data, SRS kick tables, `GameState`, and `BoardState`.
- [x] Add `GameConstants` with all tunable numeric values called out in `plan.md`.
- [x] Add `CandyPalette` with base colours and derived highlight/shadow/glow/bevel helpers.
- [x] Create `strings.xml` in English plus placeholder locale resources for the remaining 17 locales.
- [x] Add `GameViewModel` skeleton, lifecycle pause/resume hooks, and manual DI via `ViewModelFactory`.
- [x] Add unit-testable seams for core models so M2 can build logic without reworking the scaffold.

## Deliverables for handoff to M2

- Empty app screen launches through splash into Compose without runtime errors.
- Core model package is stable enough for movement/collision implementation.
- Resource and package structure is in place so later milestones add behaviour instead of reorganising files.

## Verification

- `./gradlew testDebugUnitTest` passes.
- Manual runtime launch on device/emulator is still pending.
