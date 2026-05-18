from __future__ import annotations

import math
import wave
from pathlib import Path

from app.ai_music.analyze_batch import GeneratedTrackMetadata
from app.ai_music.generation_plan import GenerationTask


def generate_sample_batch(
    tasks: list[GenerationTask],
    output_dir: Path,
    duration_seconds_override: int | None = None,
    sample_rate: int = 22_050,
) -> list[GeneratedTrackMetadata]:
    output_dir.mkdir(parents=True, exist_ok=True)
    generated: list[GeneratedTrackMetadata] = []
    for task in tasks:
        duration_seconds = duration_seconds_override or task.duration_seconds
        audio_path = output_dir / f"{task.track_id}.wav"
        write_stub_running_wav(
            path=audio_path,
            bpm=task.target_bpm,
            duration_seconds=duration_seconds,
            sample_rate=sample_rate,
            seed=task.seed,
        )
        generated.append(
            GeneratedTrackMetadata(
                track_id=task.track_id,
                title=task.title,
                audio_path=audio_path,
                target_bpm=task.target_bpm,
                duration_seconds=duration_seconds,
                style_tags=task.style_tags,
                energy=task.energy,
            ),
        )
    return generated


def write_stub_running_wav(
    path: Path,
    bpm: int,
    duration_seconds: int,
    sample_rate: int,
    seed: int,
) -> None:
    """Create a tiny deterministic WAV placeholder until the real AI generator is wired in."""
    total_frames = duration_seconds * sample_rate
    beat_interval = max(1, round(sample_rate * 60.0 / bpm))
    base_frequency = 180.0 + (seed % 17) * 8.0
    amplitude = 10_000

    with wave.open(str(path), "wb") as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        frames = bytearray()
        for frame_index in range(total_frames):
            beat_position = frame_index % beat_interval
            click = 1.0 if beat_position < sample_rate * 0.018 else 0.0
            tone = math.sin(2.0 * math.pi * base_frequency * frame_index / sample_rate) * 0.28
            pulse = click * math.sin(2.0 * math.pi * 900.0 * frame_index / sample_rate) * 0.72
            sample = int((tone + pulse) * amplitude)
            frames.extend(sample.to_bytes(2, byteorder="little", signed=True))
        wav_file.writeframes(bytes(frames))
