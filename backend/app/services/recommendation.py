from dataclasses import dataclass
from math import fabs

from app.services.bpm import candidate_bpms


@dataclass(frozen=True)
class RecommendationInput:
    id: str
    title: str
    artist: str
    bpm: float
    bpm_confidence: float
    energy: int
    popularity: int
    tags: set[str]


def target_bpm_for_spm(spm: float) -> float:
    if spm < 0:
        raise ValueError("spm must not be negative")
    return spm / 2.0 if spm >= 120.0 else spm


def recommend_songs(
    target_bpm: float,
    songs: list[RecommendationInput],
    preferred_tags: set[str] | None = None,
    tolerance: float = 10.0,
    limit: int = 12,
) -> list[dict]:
    preferred_tags = preferred_tags or set()
    rows = []
    for song in songs:
        candidates = candidate_bpms(song.bpm)
        if not candidates:
            continue
        matched_bpm = min(candidates, key=lambda value: fabs(value - target_bpm))
        delta = fabs(matched_bpm - target_bpm)
        if delta > tolerance:
            continue

        bpm_score = 1.0 - delta / tolerance
        preference_score = 0.5 if not preferred_tags else len(song.tags & preferred_tags) / max(1, len(preferred_tags))
        score = (
            bpm_score * 0.45
            + preference_score * 0.20
            + min(1.0, song.energy / 100.0) * 0.15
            + min(1.0, song.popularity / 100.0) * 0.10
            + min(1.0, song.bpm_confidence) * 0.10
        )
        rows.append({"song_id": song.id, "title": song.title, "artist": song.artist, "score": round(score, 4), "matched_bpm": matched_bpm})
    return sorted(rows, key=lambda row: row["score"], reverse=True)[:limit]
