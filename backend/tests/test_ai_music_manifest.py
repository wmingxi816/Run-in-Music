from pathlib import Path

from app.ai_music.generation_plan import build_generation_plan
from app.ai_music.manifest import append_manifest_entry, load_generated_track_metadata, manifest_entry_from_task


def test_manifest_entry_from_task_keeps_generation_metadata(tmp_path: Path):
    task = build_generation_plan(count=1, seed_start=77)[0]
    audio_path = tmp_path / f"{task.track_id}.wav"

    entry = manifest_entry_from_task(task=task, audio_path=audio_path, model_id="facebook/musicgen-small")

    assert entry.track_id == task.track_id
    assert entry.title == task.title
    assert entry.audio_path == str(audio_path)
    assert entry.target_bpm == task.target_bpm
    assert entry.duration_seconds == task.duration_seconds
    assert entry.style_tags == task.style_tags
    assert entry.model_id == "facebook/musicgen-small"


def test_manifest_jsonl_roundtrip_loads_generated_metadata(tmp_path: Path):
    manifest_path = tmp_path / "metadata.jsonl"
    task = build_generation_plan(count=1)[0]
    audio_path = tmp_path / f"{task.track_id}.wav"
    audio_path.write_bytes(b"fake")

    append_manifest_entry(
        manifest_path,
        manifest_entry_from_task(task=task, audio_path=audio_path, model_id="facebook/musicgen-small"),
    )

    tracks = load_generated_track_metadata(manifest_path)

    assert len(tracks) == 1
    assert tracks[0].track_id == task.track_id
    assert tracks[0].audio_path == audio_path
    assert tracks[0].target_bpm == task.target_bpm
