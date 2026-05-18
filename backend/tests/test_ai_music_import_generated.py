from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker

from app.ai_music.analyze_batch import GeneratedTrackAnalysis
from app.ai_music.import_generated import import_generated_tracks
from app.core.database import Base
from app.models import AudioAnalysis, PlatformTrack, Song


def test_import_generated_tracks_creates_catalog_rows(tmp_path):
    session = session_factory()
    audio_path = tmp_path / "track.wav"
    audio_path.write_bytes(b"fake-audio")
    row = GeneratedTrackAnalysis(
        track_id="rim_085_easy_run_electronic_001",
        title="Neon Stride",
        audio_path=audio_path,
        target_bpm=85,
        duration_seconds=240,
        style_tags=["electronic", "easy_run", "steady_beat"],
        energy=70,
        bpm=86.0,
        bpm_confidence=0.82,
        candidate_bpms=[43.0, 86.0, 172.0],
        status="accepted",
        reject_reason=None,
    )

    imported = import_generated_tracks(session, [row])

    assert imported == 1
    song = session.scalar(select(Song).where(Song.title == "Neon Stride"))
    assert song is not None
    assert song.artist == "Run in Music AI"
    assert song.bpm == 86.0
    assert song.bpm_source == "analysis"
    assert song.tags_csv == "electronic,easy_run,steady_beat"

    track = session.scalar(select(PlatformTrack).where(PlatformTrack.provider_track_id == row.track_id))
    assert track is not None
    assert track.provider == "generated"
    assert track.url == f"generated://{row.track_id}"

    analysis = session.scalar(select(AudioAnalysis).where(AudioAnalysis.song_id == song.id))
    assert analysis is not None
    assert analysis.candidate_bpms_csv == "43.0,86.0,172.0"


def test_import_generated_tracks_skips_rejected_rows(tmp_path):
    session = session_factory()
    rejected = GeneratedTrackAnalysis(
        track_id="bad",
        title="Bad",
        audio_path=tmp_path / "bad.wav",
        target_bpm=90,
        duration_seconds=240,
        style_tags=["tempo_run"],
        energy=80,
        bpm=None,
        bpm_confidence=0.0,
        candidate_bpms=[],
        status="rejected",
        reject_reason="bpm analysis failed",
    )

    imported = import_generated_tracks(session, [rejected])

    assert imported == 0
    assert session.scalar(select(Song).where(Song.title == "Bad")) is None


def session_factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(engine)
    return sessionmaker(bind=engine)()
