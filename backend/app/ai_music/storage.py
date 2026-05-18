from __future__ import annotations

import re
from pathlib import Path


TRACK_ID_PATTERN = re.compile(r"^[A-Za-z0-9_-]+$")


def generated_audio_path(track_id: str, generated_music_dir: Path) -> Path:
    if not TRACK_ID_PATTERN.fullmatch(track_id):
        raise ValueError("Invalid generated track id")
    return generated_music_dir / f"{track_id}.wav"
