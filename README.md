# Kettlebell 🏋️

A beautiful, offline-first Android app for kettlebell training. Browse a library of
beginner → advanced exercises, run guided workouts with sets and rest timers, log your
lifts, and get smart weight recommendations that progress every session. No account
required — install and go.

## Features

- **Start a workout** — pick a guided routine (Beginner Foundations, Intermediate
  Strength, Advanced Power), target a specific body part (chest, core, legs, back,
  arms, shoulders), or start an empty session and add exercises as you go.
- **Exercises with sets & rest timers** — each exercise holds multiple working sets with
  weight/rep steppers, a one-tap "set complete" toggle, and an automatic rest countdown
  between sets (with skip / +15s controls).
- **Exercise library** — 18 kettlebell exercises across Beginner, Intermediate and
  Advanced levels, filterable by difficulty. Each has:
  - an expandable description of the movement and the muscles it works,
  - step-by-step "how to perform" instructions,
  - a **Watch demo on YouTube** link.
- **Smart progression** — a double-progression engine recommends your next weight and rep
  target each session, snapping to standard kettlebell sizes.
- **History** — every finished workout is saved with volume, sets and duration, expandable
  to see each exercise.
- **Local & private** — all data is stored on-device in a SQLite database via Room.
- **Settings → Debug log** — an in-app log that captures exceptions (caught and uncaught)
  so issues can be inspected and shared.

## Tech stack

- Kotlin + Jetpack Compose (Material 3)
- Room (SQLite) for persistence
- Navigation Compose, ViewModel + StateFlow
- `minSdk` 26, `targetSdk` 35

## Building

Requires JDK 17 and the Android SDK.

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Continuous integration

The [`Build APK`](.github/workflows/build-apk.yml) GitHub Actions workflow compiles the
app on every push and uploads the debug APK as a workflow artifact
(`kettlebell-debug-apk`), retained for **1 day**.

## Project layout

```
app/src/main/java/com/kettlebell/app/
├── data/            # Room entities, DAOs, repository, exercise catalogue, progression engine
├── debug/           # In-app exception logger
└── ui/              # Compose theme, navigation, screens, components, view model
```
