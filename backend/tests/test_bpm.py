from app.services.bpm import candidate_bpms, normalize_bpm_for_running


def test_candidate_bpms_include_half_and_double_values():
    assert candidate_bpms(168) == [84.0, 168]


def test_normalize_bpm_selects_closest_running_match():
    assert normalize_bpm_for_running(168, target_bpm=82) == 84.0
