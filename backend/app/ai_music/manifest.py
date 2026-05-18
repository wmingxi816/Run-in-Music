from __future__ import annotations

import json
from dataclasses import asdict, dataclass
from pathlib import Path

from app.ai_music.analyze_batch import GeneratedTrackMetadata
from app.ai_music.generation_plan import GenerationTask


@dataclass(frozen=True)
class GeneratedTrackManifestEntry:
    track_id: str
    title: str
    audio_path: str
    target_bpm: int
    duration_seconds: int
    style_tags: list[str]
    energy: int
    prompt: str
    seed: int
    model_id: str
    status: str = "generated"


def manifest_entry_from_task(
    task: GenerationTask,
    audio_path: Path,
    model_id: str,
    status: str = "generated",
) -> GeneratedTrackManifestEntry:
    return GeneratedTrackManifestEntry(
        track_id=task.track_id,
        title=task.title,
        audio_path=str(audio_path),
        target_bpm=task.target_bpm,
        duration_seconds=task.duration_seconds,
        style_tags=task.style_tags,
        energy=task.energy,
        prompt=task.prompt,
        seed=task.seed,
        model_id=model_id,
        status=status,
    )


def append_manifest_entry(path: Path, entry: GeneratedTrackManifestEntry) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as file:
        file.write(json.dumps(asdict(entry), ensure_ascii=False))
        file.write("\n")


def load_manifest_entries(path: Path) -> list[GeneratedTrackManifestEntry]:
    if not path.exists():
        return []
    entries: list[GeneratedTrackManifestEntry] = []
    with path.open("r", encoding="utf-8") as file:
        for line in file:
            if not line.strip():
                continue
            data = json.loads(line)
            entries.append(
                GeneratedTrackManifestEntry(
                    track_id=data["track_id"],
                    title=data["title"],
                    audio_path=data["audio_path"],
                    target_bpm=int(data["target_bpm"]),
                    duration_seconds=int(data["duration_seconds"]),
                    style_tags=list(data.get("style_tags", [])),
                    energy=int(data["energy"]),
                    prompt=data.get("prompt", ""),
                    seed=int(data.get("seed", 0)),
                    model_id=data.get("model_id", "unknown"),
                    status=data.get("status", "generated"),
                ),
            )
    return entries


def load_generated_track_metadata(path: Path) -> list[GeneratedTrackMetadata]:
    return [
        GeneratedTrackMetadata(
            track_id=entry.track_id,
            title=entry.title,
            audio_path=Path(entry.audio_path),
            target_bpm=entry.target_bpm,
            duration_seconds=entry.duration_seconds,
            style_tags=entry.style_tags,
            energy=entry.energy,
        )
        for entry in load_manifest_entries(path)
        if entry.status == "generated"
    ]
