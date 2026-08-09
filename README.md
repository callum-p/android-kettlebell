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
- **Backup & sync** — save/restore a backup file anywhere on the device via the system file
  picker, and optionally connect **Google Drive** to back up after every workout and restore
  on launch (see setup below).
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
   - SHA-1 of the signing certificate. For the CI debug builds that is the committed
     `debug.keystore`:
     `E2:FB:F5:27:E8:1C:22:10:94:7D:42:67:5A:C2:31:4D:C1:50:71:4D`
     (or run `keytool -list -v -keystore debug.keystore -storepass android -alias androiddebugkey`).

No `google-services.json` is needed. Until this is set up, tapping **Connect Google Drive**
will fail gracefully and the app keeps working with local/offline storage.

The database is synced as a whole SQLite file: uploaded (after a WAL checkpoint) at the end of
each workout, and downloaded on launch before the database is opened.

## Project layout

```
app/src/main/java/com/kettlebell/app/
├── data/            # Room entities, DAOs, repository, exercise catalogue, progression engine
├── debug/           # In-app exception logger
└── ui/              # Compose theme, navigation, screens, components, view model
```
