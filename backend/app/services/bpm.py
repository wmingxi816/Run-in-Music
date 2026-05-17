from __future__ import annotations

from pathlib import Path

import numpy as np


def candidate_bpms(bpm: float) -> list[float]:
    if bpm <= 0:
        return []
    values = [bpm, bpm / 2.0, bpm * 2.0]
    return sorted({round(value, 2) for value in values if 40.0 <= value <= 220.0})


def normalize_bpm_for_running(bpm: float, target_bpm: float) -> float:
    candidates = candidate_bpms(bpm)
    if not candidates:
        raise ValueError("bpm must be positive")
    return min(candidates, key=lambda value: abs(value - target_bpm))


def analyze_audio_file(path: str | Path) -> tuple[float, float, list[float]]:
    import librosa

    y, sr = librosa.load(str(path), mono=True)
    tempo_raw = librosa.feature.tempo(y=y, sr=sr)
    tempo = float(np.atleast_1d(tempo_raw)[0])
    onset_env = librosa.onset.onset_strength(y=y, sr=sr)
    confidence = float(min(1.0, max(0.0, np.mean(onset_env) / 10.0)))
    return tempo, confidence, candidate_bpms(tempo)
