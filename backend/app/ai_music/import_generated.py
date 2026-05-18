from __future__ import annotations

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.ai_music.analyze_batch import GeneratedTrackAnalysis
from app.models import AudioAnalysis, PlatformTrack, Song


GENERATED_PROVIDER = "generated"
GENERATED_ARTIST = "Run in Music AI"


def import_generated_tracks(session: Session, analyses: list[GeneratedTrackAnalysis]) -> int:
    imported = 0
    for analysis in analyses:
        if analysis.status != "accepted" or analysis.bpm is None:
            continue
        existing = session.scalar(
            select(PlatformTrack).where(
                PlatformTrack.provider == GENERATED_PROVIDER,
                PlatformTrack.provider_track_id == analysis.track_id,
            ),
        )
        if existing:
            continue

        song = Song(
            title=analysis.title,
            artist=GENERATED_ARTIST,
            duration_seconds=analysis.duration_seconds,
            bpm=analysis.bpm,
            bpm_source="analysis",
            bpm_confidence=analysis.bpm_confidence,
            energy=analysis.energy,
            popularity=50,
            tags_csv=",".join(analysis.style_tags),
        )
        session.add(song)
        session.flush()
        session.add(
            PlatformTrack(
                song_id=song.id,
                provider=GENERATED_PROVIDER,
                provider_track_id=analysis.track_id,
                url=f"generated://{analysis.track_id}",
            ),
        )
        session.add(
            AudioAnalysis(
                song_id=song.id,
                bpm=analysis.bpm,
                confidence=analysis.bpm_confidence,
                source="analysis",
                candidate_bpms_csv=",".join(str(value) for value in analysis.candidate_bpms),
            ),
        )
        imported += 1
    session.commit()
    return imported
