# AI Music Pipeline PoC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a backend PoC pipeline that can create an AI music generation plan, analyze generated audio files, and import accepted analysis results into the existing song catalog.

**Architecture:** Keep the pipeline as small Python modules under `backend/app/ai_music`. Generation planning is pure data logic, audio analysis delegates to existing BPM service logic, and database import reuses existing SQLAlchemy models. The first generator is a dry-run plan writer; actual ACE-Step invocation is intentionally deferred.

**Tech Stack:** Python, FastAPI backend modules, SQLAlchemy, librosa via existing `services.bpm`, pytest.

---

### Task 1: Generation Plan

**Files:**
- Create: `backend/app/ai_music/__init__.py`
- Create: `backend/app/ai_music/generation_plan.py`
- Test: `backend/tests/test_ai_music_generation_plan.py`

- [ ] **Step 1: Write failing tests**

Test that `build_generation_plan(count=100)` returns 100 deterministic tasks with unique IDs, target BPM in 65-115, duration 180-300 seconds, prompts containing running and steady beat wording, and useful style tags.

- [ ] **Step 2: Run RED**

Run: `backend\.venv\Scripts\python.exe -m pytest backend\tests\test_ai_music_generation_plan.py -q`

Expected: import failure because `app.ai_music.generation_plan` does not exist.

- [ ] **Step 3: Implement plan builder**

Define `GenerationTask` dataclass and `build_generation_plan(count=100, seed_start=1000)`. Use deterministic cycling over BPM buckets, scenarios, styles, and energy labels.

- [ ] **Step 4: Run GREEN**

Run: `backend\.venv\Scripts\python.exe -m pytest backend\tests\test_ai_music_generation_plan.py -q`

Expected: tests pass.

### Task 2: Batch Analysis

**Files:**
- Create: `backend/app/ai_music/analyze_batch.py`
- Test: `backend/tests/test_ai_music_analyze_batch.py`

- [ ] **Step 1: Write failing tests**

Test that `analyze_generated_tracks` accepts metadata records and an injected analyzer function, then returns BPM, confidence, candidate BPMs, duration, energy, status, and reject reason for invalid files.

- [ ] **Step 2: Run RED**

Run: `backend\.venv\Scripts\python.exe -m pytest backend\tests\test_ai_music_analyze_batch.py -q`

Expected: import failure because `analyze_batch` does not exist.

- [ ] **Step 3: Implement analyzer wrapper**

Do not require real audio in tests. Accept an analyzer dependency that returns `(bpm, confidence, candidates)` and use `wave` or metadata duration for duration when available.

- [ ] **Step 4: Run GREEN**

Run: `backend\.venv\Scripts\python.exe -m pytest backend\tests\test_ai_music_analyze_batch.py -q`

Expected: tests pass.

### Task 3: Database Import

**Files:**
- Create: `backend/app/ai_music/import_generated.py`
- Test: `backend/tests/test_ai_music_import_generated.py`

- [ ] **Step 1: Write failing tests**

Test that accepted analysis rows create `Song`, `PlatformTrack(provider="generated")`, and `AudioAnalysis` rows; rejected rows are skipped.

- [ ] **Step 2: Run RED**

Run: `backend\.venv\Scripts\python.exe -m pytest backend\tests\test_ai_music_import_generated.py -q`

Expected: import failure because `import_generated` does not exist.

- [ ] **Step 3: Implement importer**

Use existing `Song`, `PlatformTrack`, and `AudioAnalysis`. Use `generated://<track_id>` URLs to avoid needing a web audio host in the first PoC.

- [ ] **Step 4: Run GREEN**

Run: `backend\.venv\Scripts\python.exe -m pytest backend\tests\test_ai_music_import_generated.py -q`

Expected: tests pass.

### Task 4: Documentation and Status

**Files:**
- Modify: `docs/AI_MUSIC_MVP_PLAN.md`
- Modify: `docs/DEVELOPMENT_STATUS.md`

- [ ] **Step 1: Document PoC status**

Record that backend AI music pipeline has plan/analyze/import modules, but real ACE-Step invocation remains a future step.

- [ ] **Step 2: Verify all backend tests**

Run: `backend\.venv\Scripts\python.exe -m pytest`

Expected: all backend tests pass.

- [ ] **Step 3: Commit and push**

Run:

```powershell
git add backend docs
git commit -m "Add AI music pipeline PoC"
git push
```
