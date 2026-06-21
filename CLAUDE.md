# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository layout

The actual project lives one level deep, under [LightChecker/](LightChecker/) — the repo root only holds `.cursorrules`, `.github/`, and the VS Code workspace. Always operate from `LightChecker/` for builds and Firebase commands.

- [LightChecker/app/](LightChecker/app/) — Android application (Kotlin, Jetpack Compose, Hilt, Room, WorkManager, Glance widget).
- [LightChecker/functions/](LightChecker/functions/) — Firebase Cloud Functions (TypeScript, Node 20+). Source in `src/`, CJS tests in `test/`.
- [LightChecker/docs/](LightChecker/docs/) — design notes (e.g. [TZ_KYIV.md](LightChecker/docs/TZ_KYIV.md)) and exported Room `schemas/`.
- [LightChecker/config/detekt/detekt.yml](LightChecker/config/detekt/detekt.yml) — Detekt config (`maxIssues: 0`, applied with formatting plugin).
- Top-level [.cursorrules](.cursorrules) holds short Ukrainian-language coding rules referenced below.

This is a diploma project (bachelor's qualifying work); feature scope is bounded by documented FR/NFR specs. Don't broaden scope or add dependencies beyond what the task requires (`.cursorrules` rule 2–3).

## Common commands

All Gradle commands run from `LightChecker/` using the wrapper. The Android target is `compileSdk 36`, `minSdk 29`, `targetSdk 36`.

```powershell
# Android (run from LightChecker/)
.\gradlew :app:compileDebugKotlin     # quick build check after Kotlin/manifest/resource changes
.\gradlew :app:testDebugUnitTest      # JVM unit tests (JUnit + MockK + Turbine + Robolectric)
.\gradlew :app:testDebugUnitTest --tests "com.bowlof.lightchecker.domain.usecase.NextOutageCalculatorTest"
.\gradlew detekt                      # lint via Detekt (formatting plugin enabled)
.\gradlew :app:assembleRelease        # signed release if LIGHTCHECKER_STORE_FILE is set, else debug-signed
```

Release signing reads `LIGHTCHECKER_STORE_FILE`/`_PASSWORD`/`_KEY_ALIAS`/`_KEY_PASSWORD` Gradle properties; absent or missing keystore falls back to debug signing (see [LightChecker/app/build.gradle.kts](LightChecker/app/build.gradle.kts) and `gradle.properties.signing.example`).

```bash
# Cloud Functions (run from LightChecker/functions/)
npm ci
npm run build         # tsc -> lib/
npm run lint          # eslint src/
npm run format        # prettier
npm test              # builds, then runs node --test test/*.cjs
node --test test/normalizeIntervals.test.cjs    # single test file (build first)
```

`npm test` is `npm run build && node --test test/*.cjs` — the CJS tests import compiled JS from `lib/`, so always rebuild before re-running a single test file.

CI: [.github/workflows/functions-ci.yml](.github/workflows/functions-ci.yml) runs `npm ci && npm run build && npm run lint && npm test` on changes under `LightChecker/functions/**`. [.github/workflows/update-schedules.yml](.github/workflows/update-schedules.yml) runs hourly and executes `node scripts/populate-firestore.cjs` against the live project using `FIREBASE_SERVICE_ACCOUNT` — this is the production data ingestion path, not the deployed `onSchedule` functions in `index.ts`.

## Architecture

### High-level data flow

```
Telegram/oblenergo sources
        │  (hourly: GitHub Actions cron OR Cloud Function onSchedule)
        ▼
TS parsers in functions/src/parsers (text + OCR via tesseract.js)
        │
        ▼  applyScheduleUpdate (normalize → validate → write today/tomorrow window)
Firestore: schedules/{region}__{queue}  (format f=2, days: {YYYYMMDD: {v, s, g}})
        │
        ├─► FCM topic "lc_<region>_<queue>"  data-only {r,q,v,d}
        │           │
        │           ▼
        │   LightCheckerFirebaseMessagingService → enqueueUniqueWork(SyncScheduleWorker)
        │           │
        │           ▼  ScheduleRepository.syncIfNewerVersion (skips if cachedVersion >= remoteVersion)
        │   Room (outage_slots, sync_meta, sync_history, sync_events) + Glance widget update
        │
        └─► Direct Firestore listeners in app (ScheduleViewModel via repository flows)
```

Both Android and Functions agree on three contract points: the Firestore document layout (`f=2` multi-day), the FCM topic name, and the `d` calendar day in **Europe/Kyiv** (see [LightChecker/docs/TZ_KYIV.md](LightChecker/docs/TZ_KYIV.md)).

### Cross-layer invariants

- **Day boundary is always Europe/Kyiv.** Use [`KyivTime`](LightChecker/app/src/main/java/com/bowlof/lightchecker/domain/time/KyivTime.kt) on Android and [`kyivDate.ts`](LightChecker/functions/src/kyivDate.ts) on the server. Never trust `LocalDate.now()` without a TZ.
- **Document IDs and FCM topics are sanitized identically on both sides.** [`ScheduleDocumentIds.firestoreDocumentId`](LightChecker/app/src/main/java/com/bowlof/lightchecker/domain/ids/ScheduleDocumentIds.kt) ↔ [`functions/src/documentId.ts`](LightChecker/functions/src/documentId.ts) (`{region}__{queue}`). [`FirebaseTopicNames.forRegionQueue`](LightChecker/app/src/main/java/com/bowlof/lightchecker/domain/messaging/FirebaseTopicNames.kt) ↔ [`functions/src/topicName.ts`](LightChecker/functions/src/topicName.ts) (`lc_<region>_<queue>`). When changing one, change both.
- **Server only writes today+tomorrow.** [`applyScheduleDayWrite`](LightChecker/functions/src/multiDayScheduleWrite.ts) drops any other day key from the `days` map and skips writes (no FCM) when the new `s` array is byte-identical to the previous version. The repository's `purgeStaleCache` mirrors this by deleting Room rows older than today.
- **Schedule payload is validated on both ends.** Server: [`validateSchedulePayload`](LightChecker/functions/src/validateSlots.ts). Android: [`ValidateSchedulePayload`](LightChecker/app/src/main/java/com/bowlof/lightchecker/domain/usecase/ValidateSchedulePayload.kt). Both check `f`, monotonic `v`, day format, and the flat `[startMin, endMin, ...]` slot array.
- **Version monotonicity drives sync skipping.** FCM carries `v` and `d`; the worker calls `syncIfNewerVersion` which short-circuits when `meta.cachedVersion >= remoteVersion` for the same day (and logs a `SYNC_SKIPPED` event).

### Android module structure (`com.bowlof.lightchecker`)

Layered following the Guide to App Architecture (`.cursorrules`):

- `presentation/` — Compose screens + Hilt `ViewModel`s (`schedule`, `history`, `settings`, `onboarding`, `about`, `navigation`, `util`). Collect with `collectAsStateWithLifecycle`; state lives in ViewModel.
- `domain/` — pure Kotlin: `model/`, `repository/` interfaces, `usecase/`, `time/KyivTime`, `ids/`, `messaging/`.
- `data/` — `local/db` Room (DB version 7, `MIGRATION_5_6`, `MIGRATION_6_7` manual + `AutoMigration(4→5)`, schemas exported to `app/schemas/`), `remote/` Firestore data source + DTO mapper, `repository/` impls, `catalog/` static cities catalog, `location/`, `messaging/` `FirebaseTopicManager`.
- `di/` — Hilt modules: `DatabaseModule`, `FirebaseModule`, `RepositoryModule`, `DataStoreModule`, `TimeModule`, `CoroutineModule`, plus `GlanceWidgetEntryPoint` for the widget receiver.
- `work/SyncScheduleWorker` — `@HiltWorker` `CoroutineWorker` triggered by FCM via `enqueueUniqueWork(KEEP)` per `region_queue`, with exponential backoff. On success: refreshes Room, calls `OutageGlanceAppWidget().updateAll`, posts a per-place notification if `notificationsEnabled` and `POST_NOTIFICATIONS` granted.
- `fcm/LightCheckerFirebaseMessagingService` — translates RemoteMessage `{r,q,v?,d?}` into the worker. Data-only messages, no system notifications from FCM directly.
- `widget/OutageGlanceAppWidget` — Glance app widget using `GetWidgetDayScheduleUseCase`; updated from the worker and the repository after refresh.
- `LightCheckerApplication` — `@HiltAndroidApp`, provides `HiltWorkerFactory` via `Configuration.Provider`, creates the `schedule_updates` notification channel, gates Crashlytics behind `!BuildConfig.DEBUG`.

### Cloud Functions structure

- [index.ts](LightChecker/functions/src/index.ts) — entry points: `healthCheck` (HTTPS), `schedulePollStub` (legacy single-source `onSchedule`), `scheduleUpdateAllCities` (`onSchedule`, 1 GiB / 300 s, multi-city), `runAllCitiesHttp` and `runSchedulePipelineHttp` (key-gated HTTPS triggers using `HTTP_SCHEDULE_KEY`).
- [scheduleUpdateAll.ts](LightChecker/functions/src/scheduleUpdateAll.ts) — orchestrates the five cities; each city is processed independently so one failure doesn't block the others.
- [parsers/](LightChecker/functions/src/parsers/) — registry in `parsers/index.ts` plus city-specific parsers:
  - `cherkasyTelegramParser.ts` — text-based; reads `https://t.me/s/pat_cherkasyoblenergo`, picks message via `cherkasyPostDate.ts`.
  - `dtekTelegramParser.ts` — OCR (`tesseract.js`, `eng.traineddata`/`ukr.traineddata` shipped in the package) for Kyiv/Odesa/Dnipro; uses `dtekPostDate.ts`.
  - `lvivTelegramParser.ts` — OCR with `lvivTelegramCandidates.ts` + `lvivGraphicDate.ts`.
  - `regexTimeParser.ts` — generic fallback used by `schedulePollStub`.
- [schedulePipeline.ts](LightChecker/functions/src/schedulePipeline.ts) → [multiDayScheduleWrite.ts](LightChecker/functions/src/multiDayScheduleWrite.ts) — normalize intervals (`normalizeIntervals.ts`), validate, transactional write of one day inside the today/tomorrow window, then FCM data-only publish to the topic.
- [params.ts](LightChecker/functions/src/params.ts) — `defineString` env params: `OBLENERGO_SOURCE_URL`, `PIPELINE_REGION_ID`, `PIPELINE_QUEUE_ID`, `PIPELINE_DAY_OFFSET`, `HTTP_SCHEDULE_KEY`.
- Two production paths exist in parallel: the deployed `onSchedule` functions and the GitHub Actions cron running `scripts/populate-firestore.cjs`. Check both when changing the ingestion flow.

## Coding conventions

From [.cursorrules](.cursorrules) (apply without restatement):

- Kotlin: PascalCase / camelCase / UPPER_SNAKE_CASE; idiomatic data/sealed classes; coroutines on `viewModelScope` or appropriate scope; `kotlinx-coroutines-play-services` for Firebase awaits.
- Gradle: Kotlin DSL only; version-catalog aliases; do not add dependencies without need and align with the Firebase BoM.
- Compose: state in ViewModel, `collectAsStateWithLifecycle`, no heavy logic in composables.
- Logging: `Timber` (planted only in debug); map `FirebaseFirestoreException` to UI-friendly messages via `toScheduleUserMessage` in `presentation/util`.
- After changes touching Kotlin, manifest, resources, or Gradle, run at least `.\gradlew :app:compileDebugKotlin`.

## Testing notes

- Android unit tests use Robolectric (`testOptions.unitTests.isIncludeAndroidResources = true`) — keep them in `app/src/test/`.
- Functions tests are plain `node:test` `.cjs` files that import from `lib/`; run `npm run build` before iterating on a single file.
- Tests covering the cross-layer contract (document ID, topic name, validation, normalization) exist on both sides — keep them in sync when changing the contract.

## Things to avoid

- Don't write Firestore documents for days outside the Europe/Kyiv [today, tomorrow] window; the write helper will drop them and skip FCM anyway.
- Don't bypass `syncIfNewerVersion` from the worker — version skipping is the contract that prevents redundant notifications and Room churn.
- Don't change Room schema without bumping the DB version and adding a `Migration` (schemas are exported under `app/schemas/`).
- Don't commit `google-services.json`, `local.properties`, keystores, or `firebase-sa.json`.
