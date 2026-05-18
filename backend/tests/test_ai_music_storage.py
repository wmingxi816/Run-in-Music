import pytest

from app.ai_music.storage import generated_audio_path


def test_generated_audio_path_allows_track_ids(tmp_path):
    assert generated_audio_path("rim_080_easy_run_001", tmp_path) == tmp_path / "rim_080_easy_run_001.wav"


def test_generated_audio_path_rejects_path_traversal(tmp_path):
    with pytest.raises(ValueError):
        generated_audio_path("../secret", tmp_path)
