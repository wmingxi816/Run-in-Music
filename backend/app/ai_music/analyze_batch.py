from __future__ import annotations

from collections.abc import Callable
from dataclasses import dataclass
from pathlib import Path

from app.services.bpm import analyze_audio_file


AudioAnalyzer = Callable[[Path], tuple[float, float, list[float]]]


@dataclass(frozen=True)
class GeneratedTrackMetadata:
    track_id: str
    title: str
    audio_path: Path
    target_bpm: int
    duration_seconds: int
    style_tags: list[str]
    energy: int


@dataclass(frozen=True)
class GeneratedTrackAnalysis:
    track_id: str
    title: str
    audio_path: Path
    target_bpm: int
    duration_seconds: int
    style_tags: list[str]
    energy: int
    bpm: float | None
    bpm_confidence: float
    candidate_bpms: list[float]
    status: str
    reject_reason: str | None


def analyze_generated_tracks(
    tracks: list[GeneratedTrackMetadata],
    analyzer: AudioAnalyzer = analyze_audio_file,
) -> list[GeneratedTrackAnalysis]:
    results: list[GeneratedTrackAnalysis] = []
    for track in tracks:
        if not track.audio_path.exists():
            results.append(rejected(track, "audio file missing"))
            continue

        bpm, confidence, candidates = analyzer(track.audio_path)
        if bpm <= 0 or not candidates:
            results.append(rejected(track, "bpm analysis failed"))
            continue

        results.append(
            GeneratedTrackAnalysis(
                track_id=track.track_id,
                title=track.title,
                audio_path=track.audio_path,
                target_bpm=track.target_bpm,
                duration_seconds=track.duration_seconds,
                style_tags=track.style_tags,
                energy=track.energy,
                bpm=round(float(bpm), 2),
                bpm_confidence=round(float(confidence), 4),
                candidate_bpms=[round(float(value), 2) for value in candidates],
                status="accepted",
                reject_reason=None,
            ),
        )
    return results


def rejected(track: GeneratedTrackMetadata, reason: str) -> GeneratedTrackAnalysis:
    return GeneratedTrackAnalysis(
        track_id=track.track_id,
        title=track.title,
        audio_path=track.audio_path,
        target_bpm=track.target_bpm,
        duration_seconds=track.duration_seconds,
        style_tags=track.style_tags,
        energy=track.energy,
        bpm=None,
        bpm_confidence=0.0,
        candidate_bpms=[],
        status="rejected",
        reject_reason=reason,
    )
