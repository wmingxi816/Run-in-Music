# Diagnostic Log Export Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an in-app diagnostic log export that packages useful testing data into a shareable zip file for personal APK testing.

**Architecture:** Store structured app events in Room, generate diagnostic file contents with pure Kotlin classes under `core/diagnostics`, write a zip into app cache, and launch Android's share sheet through `FileProvider`. The first version exports summaries only and intentionally avoids raw GPS coordinates.

**Tech Stack:** Kotlin, Room, Jetpack Compose, Android `FileProvider`, `ZipOutputStream`, JUnit.

---

### Task 1: Pure Diagnostic Payload Builder

**Files:**
- Create: `app/src/main/java/com/runinmusic/app/core/diagnostics/DiagnosticModels.kt`
- Create: `app/src/main/java/com/runinmusic/app/core/diagnostics/DiagnosticExportBuilder.kt`
- Test: `app/src/test/java/com/runinmusic/app/core/diagnostics/DiagnosticExportBuilderTest.kt`

- [ ] **Step 1: Write failing tests**

Test that `DiagnosticExportBuilder` creates:
- `diagnostics.json` with app/device/permission metadata.
- `events.jsonl` with redacted event details.
- `run_sessions.csv` with run summaries.
- `song_interactions.csv` with music behavior rows.

- [ ] **Step 2: Run RED**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.runinmusic.app.core.diagnostics.DiagnosticExportBuilderTest"`

Expected: compile failure because diagnostic classes do not exist.

- [ ] **Step 3: Implement minimal builder**

Use pure data classes and `org.json` to generate deterministic output. Redact JSON keys containing `key`, `token`, `cookie`, `password`, or `secret`.

- [ ] **Step 4: Run GREEN**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.runinmusic.app.core.diagnostics.DiagnosticExportBuilderTest"`

Expected: tests pass.

### Task 2: Zip Writer

**Files:**
- Create: `app/src/main/java/com/runinmusic/app/core/diagnostics/DiagnosticZipWriter.kt`
- Test: `app/src/test/java/com/runinmusic/app/core/diagnostics/DiagnosticZipWriterTest.kt`

- [ ] **Step 1: Write failing test**

Test that zip writer creates a zip file containing `diagnostics.json` and `events.jsonl`.

- [ ] **Step 2: Run RED**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.runinmusic.app.core.diagnostics.DiagnosticZipWriterTest"`

Expected: compile failure because `DiagnosticZipWriter` does not exist.

- [ ] **Step 3: Implement minimal writer**

Write payload files with UTF-8 names/content into `run_in_music_diagnostics_<timestamp>.zip`.

- [ ] **Step 4: Run GREEN**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.runinmusic.app.core.diagnostics.DiagnosticZipWriterTest"`

Expected: tests pass.

### Task 3: Room Event Log

**Files:**
- Create: `app/src/main/java/com/runinmusic/app/data/local/AppEventEntity.kt`
- Modify: `app/src/main/java/com/runinmusic/app/data/local/SongDao.kt`
- Modify: `app/src/main/java/com/runinmusic/app/data/local/RunInMusicDatabase.kt`
- Modify: `app/src/main/java/com/runinmusic/app/data/repository/MusicRepository.kt`

- [ ] **Step 1: Add event entity and DAO methods**

Add `app_events` table with `level`, `module`, `type`, `message`, `detailsJson`, and `createdAtMillis`.

- [ ] **Step 2: Add Room migration**

Bump database version to `2` and add migration `1 -> 2` that creates `app_events`.

- [ ] **Step 3: Add repository logging/export reads**

Add methods for logging app events, reading recent events, reading recent run sessions, and reading song interactions.

- [ ] **Step 4: Run tests**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: existing unit tests pass.

### Task 4: Android Export and Share

**Files:**
- Create: `app/src/main/java/com/runinmusic/app/diagnostics/DiagnosticExportService.kt`
- Create: `app/src/main/java/com/runinmusic/app/diagnostics/DiagnosticShareLauncher.kt`
- Create: `app/src/main/res/xml/file_paths.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/runinmusic/app/MainActivity.kt`
- Modify: `app/src/main/java/com/runinmusic/app/feature/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/runinmusic/app/feature/home/HomeScreen.kt`

- [ ] **Step 1: Implement export service**

Collect metadata, events, run sessions, and interactions; build payload; write zip under `cache/diagnostics`.

- [ ] **Step 2: Add `FileProvider`**

Expose only `cache-path name="diagnostics" path="diagnostics/"`.

- [ ] **Step 3: Add UI button**

Add a `导出日志` button on the home screen diagnostic card area and show export status.

- [ ] **Step 4: Share zip**

Use `Intent.ACTION_SEND`, MIME `application/zip`, and `FLAG_GRANT_READ_URI_PERMISSION`.

- [ ] **Step 5: Build**

Run: `.\gradlew.bat assembleDebug`

Expected: debug APK builds.

### Task 5: Documentation

**Files:**
- Modify: `docs/DEVELOPMENT_STATUS.md`

- [ ] **Step 1: Update status**

Record that personal testing APK now supports one-tap diagnostic export.

- [ ] **Step 2: Commit and push**

Run:

```powershell
git add app docs
git commit -m "Add diagnostic log export"
git push
```
