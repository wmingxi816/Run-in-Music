# Run Tracking Alpha 0.2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first runnable GPS tracking loop: start a run, accumulate GPS distance, calculate elapsed time and average pace, stop the run, and persist a `RunSessionEntity`.

**Architecture:** Keep GPS math in pure Kotlin under `core/run` so it can be tested without Android framework dependencies. `RunTrackingService` owns foreground location updates and persistence, while `RunTrackingStore` exposes a lightweight process-local `StateFlow` for the home screen.

**Tech Stack:** Kotlin, Android foreground service, Google Play services location, Room, Jetpack Compose, JUnit.

---

### Task 1: Pure Run Metrics

**Files:**
- Create: `app/src/main/java/com/runinmusic/app/core/run/RunLocationSample.kt`
- Create: `app/src/main/java/com/runinmusic/app/core/run/RunMetricsSnapshot.kt`
- Create: `app/src/main/java/com/runinmusic/app/core/run/RunMetricsTracker.kt`
- Test: `app/src/test/java/com/runinmusic/app/core/run/RunMetricsTrackerTest.kt`

- [x] **Step 1: Write failing tests**

```kotlin
@Test
fun firstGpsSampleDoesNotAddDistance()

@Test
fun accumulatesDistanceBetweenGpsSamples()

@Test
fun computesAveragePaceSecondsPerKm()
```

- [x] **Step 2: Run tests to verify RED**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.runinmusic.app.core.run.RunMetricsTrackerTest"`

Expected: compile failure because `RunMetricsTracker` does not exist.

- [x] **Step 3: Implement minimal pure Kotlin tracker**

Create immutable sample/snapshot data classes and a tracker that starts at `startedAtMillis`, ignores the first point for distance, uses haversine distance between accepted points, and calculates pace as `elapsedSeconds / (distanceMeters / 1000.0)`.

- [x] **Step 4: Run tests to verify GREEN**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.runinmusic.app.core.run.RunMetricsTrackerTest"`

Expected: tests pass.

### Task 2: Run Session Persistence

**Files:**
- Modify: `app/src/main/java/com/runinmusic/app/data/local/SongDao.kt`
- Modify: `app/src/main/java/com/runinmusic/app/data/repository/MusicRepository.kt`

- [x] **Step 1: Add DAO query**

Add `observeLatestRunSession(): Flow<RunSessionEntity?>` sorted by `startedAtMillis DESC`.

- [x] **Step 2: Add repository methods**

Add `latestRunSession` and `saveRunSession(...)` so UI/service code does not call DAO directly except inside the service persistence boundary.

- [x] **Step 3: Run unit tests**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: existing tests remain green.

### Task 3: Foreground Service Tracking Store

**Files:**
- Create: `app/src/main/java/com/runinmusic/app/location/RunTrackingState.kt`
- Create: `app/src/main/java/com/runinmusic/app/location/RunTrackingStore.kt`
- Modify: `app/src/main/java/com/runinmusic/app/location/RunTrackingService.kt`

- [x] **Step 1: Add tracking state**

Define `Idle`, `Running`, and `Finished` state through fields on `RunTrackingState`: `isRunning`, `startedAtMillis`, `endedAtMillis`, `elapsedMillis`, `distanceMeters`, and `averagePaceSecondsPerKm`.

- [x] **Step 2: Wire service actions**

Add `ACTION_START` and `ACTION_STOP`. Start requests location updates, update store on each GPS point, stop removes updates and persists a finished `RunSessionEntity` when duration is positive.

- [x] **Step 3: Build**

Run: `.\gradlew.bat assembleDebug`

Expected: debug APK builds.

### Task 4: Home Screen Controls

**Files:**
- Modify: `app/src/main/java/com/runinmusic/app/MainActivity.kt`
- Modify: `app/src/main/java/com/runinmusic/app/feature/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/runinmusic/app/feature/home/HomeScreen.kt`

- [x] **Step 1: Split permissions**

Measurement requests only `ACTIVITY_RECOGNITION`. Run tracking requests location and notification permissions when the user taps start run.

- [x] **Step 2: Observe run state**

`HomeViewModel` collects `RunTrackingStore.state` and `MusicRepository.latestRunSession`.

- [x] **Step 3: Add run card**

Show start/stop controls, elapsed time, distance in km, average pace, and latest saved run summary.

- [x] **Step 4: Verify**

Run: `.\gradlew.bat testDebugUnitTest` and `.\gradlew.bat assembleDebug`.

Expected: tests and build pass.

### Task 5: Status Documentation

**Files:**
- Modify: `docs/DEVELOPMENT_STATUS.md`

- [x] **Step 1: Update current work item**

Mark Alpha 0.2 GPS tracking slice as in progress or completed, depending on verification.

- [ ] **Step 2: Commit and push branch**

Run:

```powershell
git add app docs
git commit -m "Add GPS run tracking loop"
git push -u origin codex/run-tracking-alpha-0.2
```
