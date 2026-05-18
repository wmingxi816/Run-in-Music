from pathlib import Path

from app.ai_music.analyze_batch import GeneratedTrackMetadata, analyze_generated_tracks


def test_analyze_generated_tracks_uses_injected_analyzer(tmp_path: Path):
    audio_path = tmp_path / "track.wav"
    audio_path.write_bytes(b"fake-audio")
    metadata = GeneratedTrackMetadata(
        track_id="rim_085_easy_run_electronic_001",
        title="Neon Stride",
        audio_path=audio_path,
        target_bpm=85,
        duration_seconds=240,
        style_tags=["electronic", "easy_run", "steady_beat"],
        energy=70,
    )

    results = analyze_generated_tracks(
        [metadata],
        analyzer=lambda path: (86.0, 0.82, [43.0, 86.0, 172.0]),
    )

    assert len(results) == 1
    result = results[0]
    assert result.status == "accepted"
    assert result.track_id == metadata.track_id
    assert result.bpm == 86.0
    assert result.bpm_confidence == 0.82
    assert result.candidate_bpms == [43.0, 86.0, 172.0]
    assert result.reject_reason is None


def test_analyze_generated_tracks_rejects_missing_audio(tmp_path: Path):
    metadata = GeneratedTrackMetadata(
        track_id="missing",
        title="Missing",
        audio_path=tmp_path / "missing.wav",
        target_bpm=90,
        duration_seconds=240,
        style_tags=["tempo_run"],
        energy=80,
    )

    results = analyze_generated_tracks([metadata], analyzer=lambda path: (90.0, 0.9, [90.0]))

    assert results[0].status == "rejected"
    assert results[0].reject_reason == "audio file missing"
    assert results[0].bpm is None


def test_analyze_generated_tracks_rejects_zero_bpm(tmp_path: Path):
    audio_path = tmp_path / "silent.wav"
    audio_path.write_bytes(b"fake-audio")
    metadata = GeneratedTrackMetadata(
        track_id="silent",
        title="Silent",
        audio_path=audio_path,
        target_bpm=90,
        duration_seconds=240,
        style_tags=["tempo_run"],
        energy=80,
    )

    results = analyze_generated_tracks([metadata], analyzer=lambda path: (0.0, 0.0, []))

    assert results[0].status == "rejected"
    assert results[0].reject_reason == "bpm analysis failed"
