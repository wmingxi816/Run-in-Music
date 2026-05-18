# Alpha 0.2 Run, AI Music, and Sync Closure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the next practical Alpha 0.2 slice: pause/resume run tracking, serve generated AI music from the backend, play server-hosted music inside Android, and improve catalog sync status without user-entered LAN addresses.

**Architecture:** Backend remains the source of truth for generated music files and catalog metadata. Android stores only song metadata in Room, streams audio from the backend when available, and falls back to external music links for platform songs. Run tracking stays in the foreground service, with pause/resume modeled in the pure Kotlin controller so elapsed time and distance are testable.

**Tech Stack:** Android Kotlin + Compose + Room + Android `MediaPlayer`; Python FastAPI + SQLAlchemy + pytest; generated audio files under `backend/generated_music/`.

---

### Task 1: P0 Run Tracking Closure

**Files:**
- Modify: `app/src/main/java/com/runinmusic/app/location/RunTrackingState.kt`
- Modify: `app/src/main/java/com/runinmusic/app/location/RunTrackingController.kt`
- Modify: `app/src/main/java/com/runinmusic/app/location/RunTrackingStore.kt`
- Modify: `app/src/main/java/com/runinmusic/app/location/RunTrackingService.kt`
- Modify: `app/src/main/java/com/runinmusic/app/feature/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/runinmusic/app/feature/home/HomeViewModel.kt`
- Test: `app/src/test/java/com/runinmusic/app/location/RunTrackingControllerTest.kt`

- [x] Add failing tests for pause/resume elapsed-time behavior and ignored GPS updates while paused.
- [x] Implement `Paused` status, active elapsed tracking, pause/resume store methods, and service actions.
- [x] Replace the pause placeholder button with Pause/Continue controls.
- [x] Show recent run sessions on the run page.

### Task 2: P1 Backend AI Music Hosting

**Files:**
- Create: `backend/app/ai_music/generate_batch.py`
- Modify: `backend/app/ai_music/import_generated.py`
- Modify: `backend/app/api/routes.py`
- Modify: `backend/app/services/catalog.py`
- Test: `backend/tests/test_ai_music_generate_batch.py`
- Test: `backend/tests/test_ai_music_import_generated.py`
- Test: `backend/tests/test_catalog.py`

- [x] Add tests for deterministic sample WAV generation metadata.
- [x] Add tests that imported generated tracks use `/audio/generated/{track_id}` URLs.
- [x] Add a FastAPI route that serves files from `backend/generated_music/`.
- [x] Keep real ACE-Step integration as the next adapter; this slice creates a local WAV stub so the full player/catalog loop can be tested.

### Task 3: P1 Android Built-In Player

**Files:**
- Modify: `app/src/main/java/com/runinmusic/app/core/model/SongCandidate.kt`
- Modify: `app/src/main/java/com/runinmusic/app/data/local/SongEntity.kt`
- Modify: `app/src/main/java/com/runinmusic/app/data/local/RunInMusicDatabase.kt`
- Modify: `app/src/main/java/com/runinmusic/app/data/repository/BackendCatalogClient.kt`
- Create: `app/src/main/java/com/runinmusic/app/playback/MusicPlaybackController.kt`
- Modify: `app/src/main/java/com/runinmusic/app/MainActivity.kt`
- Modify: `app/src/main/java/com/runinmusic/app/feature/home/HomeScreen.kt`
- Test: `app/src/test/java/com/runinmusic/app/data/repository/BackendCatalogClientTest.kt`

- [x] Add failing parser tests for `platform_urls.generated`.
- [x] Add Room `streamUrl` migration.
- [x] Add a lightweight `MediaPlayer` controller for URL streaming.
- [x] Show now-playing state and make generated songs use “播放” instead of “去听歌”.

### Task 4: P2 Catalog Sync Experience

**Files:**
- Modify: `app/src/main/java/com/runinmusic/app/feature/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/runinmusic/app/feature/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/runinmusic/app/data/repository/MusicRepository.kt`

- [x] Track local recommendable song count from Room.
- [x] Track last sync time in UI state for the current session.
- [x] Update copy to say the App reads from the configured server/project catalog, not from user-entered LAN addresses.

### Task 5: Verification and Docs

**Files:**
- Modify: `docs/DEVELOPMENT_STATUS.md`
- Modify: `docs/AI_MUSIC_MVP_PLAN.md`
- Modify: `README.md`

- [x] Run backend pytest.
- [x] Run Android unit tests.
- [x] Run Android debug assemble if dependencies remain local-only.
- [x] Update status docs.
