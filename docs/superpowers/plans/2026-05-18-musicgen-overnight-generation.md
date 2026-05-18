# MusicGen Overnight Generation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prepare a safe overnight MusicGen-small batch generation workflow without starting the long-running generation job.

**Architecture:** Keep MusicGen dependencies in a separate virtual environment so the FastAPI backend remains lightweight. Use `metadata.jsonl` as the resume/import ledger. Generate WAV files into `backend/generated_music/musicgen-small/`, then analyze/import them with the existing backend environment.

**Tech Stack:** PowerShell setup script, Python argparse scripts, Hugging Face `transformers`, CPU PyTorch, existing FastAPI backend modules.

---

### Task 1: Manifest Ledger

**Files:**
- Create: `backend/app/ai_music/manifest.py`
- Test: `backend/tests/test_ai_music_manifest.py`

- [x] Write failing tests for manifest entry creation and JSONL roundtrip.
- [x] Implement `GeneratedTrackManifestEntry`.
- [x] Implement append/load helpers.
- [x] Convert manifest rows back into `GeneratedTrackMetadata`.

### Task 2: MusicGen Environment and Batch Script

**Files:**
- Create: `backend/requirements-musicgen.txt`
- Create: `backend/scripts/setup_musicgen_env.ps1`
- Create: `backend/scripts/musicgen_batch.py`
- Modify: `.gitignore`

- [x] Add an isolated dependency file for non-Torch MusicGen dependencies.
- [x] Add a PowerShell setup script that creates `backend/.musicgen_venv`.
- [x] Add a resumable MusicGen batch script with `--dry-run`, `--resume`, `--count`, and `--max-new-tokens`.
- [x] Keep generated audio and the MusicGen venv out of Git.

### Task 3: Analyze and Import Script

**Files:**
- Create: `backend/scripts/analyze_import_generated.py`

- [x] Read `metadata.jsonl`.
- [x] Support `--dry-run`.
- [x] Analyze generated WAV files with existing BPM logic.
- [x] Import accepted tracks with existing catalog importer.

### Task 4: Runbook

**Files:**
- Create: `docs/MUSICGEN_OVERNIGHT_RUNBOOK.md`

- [x] Document setup command.
- [x] Document dry-run command.
- [x] Document real overnight command, without running it now.
- [x] Document analysis/import and Android sync steps.
