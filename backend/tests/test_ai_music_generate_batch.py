import wave

from app.ai_music.generate_batch import generate_sample_batch
from app.ai_music.generation_plan import build_generation_plan


def test_generate_sample_batch_writes_wav_files_and_metadata(tmp_path):
    plan = build_generation_plan(count=2, seed_start=300)

    generated = generate_sample_batch(
        tasks=plan,
        output_dir=tmp_path,
        duration_seconds_override=1,
        sample_rate=8_000,
    )

    assert len(generated) == 2
    first = generated[0]
    assert first.track_id == plan[0].track_id
    assert first.audio_path.exists()
    assert first.duration_seconds == 1
    assert first.target_bpm == plan[0].target_bpm
    assert "steady_beat" in first.style_tags

    with wave.open(str(first.audio_path), "rb") as wav_file:
        assert wav_file.getnchannels() == 1
        assert wav_file.getframerate() == 8_000
        assert wav_file.getnframes() == 8_000
