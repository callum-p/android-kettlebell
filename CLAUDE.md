# Development notes

Guidance for working on the Kettlebell Android app. Read this before changing the database,
the version, or CI.

## Project at a glance

- **Kotlin + Jetpack Compose (Material 3)**, single-Activity, MVVM with one `WorkoutViewModel`.
- **Room (SQLite)** for persistence (KSP for codegen). All data is local; no account.
- **Navigation Compose** with a 5-tab bottom bar plus full-screen destinations.
- Package map:
  - `data/` — Room entities, DAOs, repository, exercise catalogue, progression engine
  - `data/db/` — database, DAOs, entities, **migrations**
  - `badges/`, `progress/`, `share/`, `notify/`, `sync/`, `widget/` — feature logic
  - `ui/` — theme, navigation, screens, components, view model, `whatsnew/`

## Build & test

- Build the debug APK: `./gradlew assembleDebug`
- Run unit tests: `./gradlew testDebugUnitTest`
- There is no local Android SDK guarantee in every environment — CI is the source of truth.
  Before pushing, at minimum sanity-check brace/paren balance and imports.

## Testing — required for all new functionality

**Any new feature or bug fix must ship with tests**, and the tests run in CI (a failing test
fails the build). Put business logic in a form that can be unit-tested:

- Pure/derivable logic goes under `app/src/test/` as JVM unit tests (JUnit4). See the existing
  suites for `ProgressionEngine`, `Progress`, `Badges`, `ExerciseCatalog`, `Format`,
  `WorkoutShare`, `WhatsNew`/`ReleaseNotes`, and `MigrationSqlTest`.
- If logic is trapped inside a composable or an Android type, extract the testable core into a
  plain function/object (as was done for the changelog parser and the migration SQL), then test
  that.
- Prefer testing behaviour and invariants (e.g. "catalogue ids are unique", "migrations are
  additive") over implementation details.

## Versioning & releases

**To cut a release, push a tag `v<version>`** (e.g. `git tag v1.4 && git push origin v1.4`). The
`Release` workflow fires automatically and releases the current tip of the default branch as that
version — no manual input, no hand-editing versions:

1. Reads the version from the tag (strips the `v`), sets `appVersionName`, and derives
   `versionCode` from it (`major*10000 + minor*100 + patch`, e.g. `1.4` → `10400`).
2. Updates `CHANGELOG.md`: if you already wrote a `## <version>` section it keeps it, otherwise it
   prepends one from the commit subjects since the previous tag.
3. Commits that with `[skip ci]` and pushes it to the default branch.
4. Runs tests, builds the signed APK **from that commit**, and publishes the GitHub Release with
   the notes as the body and the APK attached.

So the normal release is just "tag the tip of `main`". The default branch should sit at the last
released version between releases; **don't bump `versionCode`/`versionName` by hand** — the tag
drives it. If you want curated notes instead of commit subjects, add the `## <version>` section to
`CHANGELOG.md` before tagging. Facts the workflow relies on (don't break them):

- `app/build.gradle.kts` has exactly one `versionCode = <int>` and one line
  `val appVersionName = "<x.y>"`. The app version shown in Settings reads `BuildConfig.VERSION_NAME`.
- **The release tag is `v<versionName>`**; `versionCode` is derived from the version and must keep
  increasing (Android refuses to install an APK whose `versionCode` is lower than the installed
  one) — the workflow fails the release if a version would produce a non-increasing code.
- `CHANGELOG.md` starts with a `# Changelog` title; versions are `## <version>` sections, newest at
  the top, one `- ` bullet per line (don't wrap a bullet across lines). This single file drives:
  - the in-app **What's new** modal (current version's notes, via `BuildConfig.CHANGELOG`),
  - the Settings **Release notes** viewer (full history, via `BuildConfig.CHANGELOG_FULL`),
  - the **GitHub Release** body.
- The changelog is read **at build time** in `build.gradle.kts` — never hand-copied into Kotlin.

## Database migrations — NEVER wipe user history

The app once shipped `fallbackToDestructiveMigration()`, which silently deleted every user's
workouts on each schema bump. That must never happen again.

Rules for any change to a Room entity or the schema:

1. **Bump the Room `version`** in `KettlebellDatabase` (this is the DB schema version, separate
   from the app `versionCode`).
2. **Add a `Migration` in `data/db/Migrations.kt`** and register it in `ALL_MIGRATIONS`. Expose
   the raw SQL as a `..._SQL` list (like the existing migrations) so it can be unit-tested.
3. **Migrations must be additive and preserve data** — `ALTER TABLE ... ADD COLUMN`, `CREATE
   TABLE`, `CREATE INDEX`. Never `DROP`/recreate a table holding user data, and never rebuild
   the database to satisfy a schema change.
4. **Do NOT add `fallbackToDestructiveMigration()`.** The builder intentionally only uses
   `fallbackToDestructiveMigrationOnDowngrade()` (a rare, older-APK-over-newer-DB case). A
   missing migration must fail loudly, not delete data.
5. **The migration SQL must match what Room generates** for the entity, or Room's post-migration
   validation throws at runtime. Mirror the DDL Room emits (column types, `NOT NULL`, PK,
   foreign keys, index names).
6. **Add/extend `MigrationSqlTest`** to prove the new migration is additive and that existing
   rows survive. This test runs on plain in-memory SQLite in CI (no emulator).

Restoring an older backup into a newer app also relies on these migrations, so keeping them
correct protects backup/restore too.

## CI

- `Build APK` (`.github/workflows/build-apk.yml`) runs on **push to `main`** and on **pull
  requests**: it runs `testDebugUnitTest` then `assembleDebug`, and uploads the debug APK
  artifact. A failing test fails the build.
- `Release` (`.github/workflows/release.yml`) runs on a `v*` tag (or manual dispatch): tests,
  builds the signed APK, and publishes the GitHub Release.

## Compose gotchas that have bitten us

- **Collect `uiState` inside each `composable {}` destination**, not once at NavHost
  construction — capturing it once leaves screens showing stale state.
- **Don't read a `CompositionLocal` (e.g. `LocalWeightUnit.current`) inside a non-composable
  lambda** such as `joinToString { }`; hoist it into the composable body first.
- The debug signing keystore is committed (`debug.keystore`) so every CI build is signed with
  the same key and updates install over previous builds. Don't regenerate it.
