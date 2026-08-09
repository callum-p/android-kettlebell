# Kettlebell 🏋️

[![Release](https://github.com/callum-p/android-kettlebell/actions/workflows/release.yml/badge.svg)](https://github.com/callum-p/android-kettlebell/actions/workflows/release.yml)
[![Build APK](https://github.com/callum-p/android-kettlebell/actions/workflows/build-apk.yml/badge.svg)](https://github.com/callum-p/android-kettlebell/actions/workflows/build-apk.yml)

<!-- LATEST_RELEASE -->
📦 **Latest release: [v1.6](CHANGELOG.md)** — [Download the APK](https://github.com/callum-p/android-kettlebell/releases/download/v1.6/kettlebell-v1.6.apk)
<!-- /LATEST_RELEASE -->

**Your entire kettlebell training system, in one beautiful offline app.**

Pick up the bell, hit start, and go. Kettlebell coaches you through every session with
guided routines, automatic weight progression, timed conditioning modes, and a rest timer
that keeps you moving — then quietly logs it all so you can watch yourself get stronger.
No account, no paywall, no internet required. Install and train.

> Built with Kotlin + Jetpack Compose (Material 3). Fully local, private, and offline-first —
> with optional Google Drive backup when you want it.

---

## Why you'll love it

- 🎯 **Never wonder "what weight?" again** — a double-progression engine recommends your next
  weight and reps every session, snapping to the kettlebell sizes *you actually own*.
- ⏱️ **Stay in the zone** — automatic rest timers between sets, plus EMOM, interval, and AMRAP
  modes for conditioning finishers.
- 📈 **Watch the numbers climb** — personal records, weekly volume, and a training heatmap turn
  consistency into something you can see.
- 🏅 **Earn it** — 22 achievement badges celebrate streaks, milestones, and PRs with a satisfying
  little fanfare.
- 🎨 **Make it yours** — light/dark themes, Material You dynamic colour, kg or lb, and your own
  saved routines.

---

## Features

### Train
- **Start a workout your way** — choose a guided routine (**Beginner Foundations**,
  **Intermediate Strength**, **Advanced Power**), target a body part (chest, core, legs, back,
  arms, shoulders), build your own routine, or start empty and add exercises as you go.
- **Custom routines** — create, name, reorder, and save your favourite sessions, then launch
  them in a single tap. Reorder exercises mid-workout too.
- **Sets, reps & rest timers** — weight/rep steppers, a one-tap "set complete" toggle, auto-
  collapsing finished exercises, and a rest countdown between sets with skip / +15s controls
  and a heads-up notification when rest is over.
- **Timed workouts** — dedicated **EMOM**, **interval (work/rest)**, and **AMRAP** modes with a
  full-screen timer, round tracking, and vibration cues.
- **Effort & notes** — log an RPE (rate of perceived exertion) and a free-text note on any set.

### Learn
- **Exercise library** — 23 kettlebell exercises across Beginner, Intermediate, and Advanced
  levels, filterable by difficulty. Each exercise includes:
  - an expandable description of the movement and the muscles it works,
  - step-by-step "how to perform" instructions,
  - a **Watch demo on YouTube** link — reachable even mid-workout by tapping the exercise name.

### Progress
- **Smart progression** — automatic next-session weight and rep recommendations, tailored to
  the bells you own.
- **Progress overview** — weekly training volume, a workout-frequency heatmap, and per-exercise
  **personal records** with estimated one-rep-max.
- **History** — every finished workout saved with volume, sets, and duration, expandable to see
  each exercise and set.
- **Share your session** — export a clean text recap of any workout (totals + per-exercise sets
  with RPE/notes) straight to the Android share sheet.
- **Achievements** — 22 badges for streaks, totals, volume, variety, and personal records, with
  celebratory notifications when you unlock one.
- **Home-screen widget** — glance at your total workouts and this week's count without opening
  the app.

### Personalize
- **My kettlebells** — tell the app which sizes you own so recommendations only ever suggest
  weights you can actually pick up.
- **Units** — switch freely between kilograms and pounds.
- **Appearance** — System / Light / Dark themes, plus **Material You** dynamic colour on
  Android 12+.
- **Daily reminders** — an optional nudge at a time you choose so you never skip a session.
- **What's new** — a tidy changelog appears after each update so you always know what's changed.
- **In-app updates** — checks GitHub for a newer release on launch and can download &amp; install it
  for you (no Play Store needed).

### Own your data
- **Local & private** — everything is stored on-device in a SQLite database via Room. No
  account required.
- **Backup & restore** — save or restore a backup file anywhere via the system file picker, and
  optionally connect **Google Drive** to back up after every workout and restore on launch
  (see setup below).
- **Debug log** — an in-app log in Settings captures exceptions (caught and uncaught) so issues
  can be inspected and shared.

---

## Tech stack

- Kotlin + Jetpack Compose (Material 3)
- Room (SQLite) for persistence, KSP for codegen
- Navigation Compose, ViewModel + StateFlow
- Compose Canvas charts (no third-party chart library)
- `minSdk` 26, `targetSdk` 35

## Building

Requires JDK 17 and the Android SDK.

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Continuous integration & releases

- The [`Build APK`](.github/workflows/build-apk.yml) workflow runs the JVM unit tests
  (`./gradlew testDebugUnitTest`) and then compiles the app on every push, uploading the debug
  APK as a workflow artifact (`kettlebell-debug-apk`), retained for **1 day**. A failing test
  fails the build.
- The [`Release`](.github/workflows/release.yml) workflow fires on a `v*` tag — no manual version
  edits. Push a tag and it releases the current tip of `main` as that version:

  ```bash
  git tag v1.4 && git push origin v1.4
  ```

  1. reads the version from the tag, sets `versionName`, and derives `versionCode` from it
     (`major*10000 + minor*100 + patch`),
  2. updates [`CHANGELOG.md`](CHANGELOG.md) — keeps a hand-written `## <version>` section if present,
     otherwise generates one from commit subjects since the previous tag,
  3. commits that with `[skip ci]` and pushes it,
  4. builds the signed APK from that commit and publishes a **GitHub Release** with the notes and
     the APK attached (`kettlebell-v<version>.apk`).

The in-app **"What's new"** modal and the Settings **Release notes** viewer are computed at build
time from `CHANGELOG.md` and baked into `BuildConfig`, so the app, the GitHub Release, and the repo
always tell the same story.

## Google Drive sync setup (one-time)

Local file backup/restore works out of the box. **Google Drive** sync additionally requires an
OAuth client, because Google authorizes Drive access per app identity:

1. In the [Google Cloud Console](https://console.cloud.google.com/), create a project and
   **enable the Google Drive API**.
2. Configure the **OAuth consent screen** (External is fine for personal use) and add your
   Google account under **Test users**. The app only requests the restricted-free
   `drive.appdata` scope (a private, app-specific folder).
3. Create an **OAuth client ID → Android** with:
   - Package name: `com.kettlebell.app`
   - SHA-1 of the release signing certificate:
     `C7:02:C9:46:97:E0:CA:C8:A6:36:B0:D5:0E:E9:A8:84:FE:CE:5D:A5`
     (the key is injected in CI from the `SIGNING_KEYSTORE_BASE64` secret, not committed).

No `google-services.json` is needed. Until this is set up, tapping **Connect Google Drive**
will fail gracefully and the app keeps working with local/offline storage.

The database is synced as a whole SQLite file: uploaded (after a WAL checkpoint) at the end of
each workout, and downloaded on launch before the database is opened.

## Project layout

```
app/src/main/java/com/kettlebell/app/
├── data/            # Room entities, DAOs, repository, exercise catalogue, progression engine
├── badges/          # Achievement definitions and evaluation
├── notify/          # Rest-timer, achievement, and reminder notifications + alarm scheduling
├── progress/        # Personal records, volume, and training-frequency analytics
├── share/           # Workout-summary share text
├── sync/            # Google Drive + local file backup
├── widget/          # Home-screen app widget
├── debug/           # In-app exception logger
└── ui/              # Compose theme, navigation, screens, components, view model, what's-new
```

---

*No account. No ads. No nonsense. Just you and the bell.* 💪
