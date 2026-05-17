from app.services.recommendation import RecommendationInput, recommend_songs, target_bpm_for_spm


def test_high_spm_maps_to_half_target_bpm():
    assert target_bpm_for_spm(160) == 80


def test_recommendation_filters_by_bpm_window():
    songs = [
        RecommendationInput("fit", "Fit", "Runner", 82, 0.9, 80, 60, {"电子"}),
        RecommendationInput("miss", "Miss", "Runner", 120, 0.9, 80, 60, {"电子"}),
    ]

    results = recommend_songs(target_bpm=80, songs=songs)

    assert [row["song_id"] for row in results] == ["fit"]


def test_preferred_tags_affect_sorting():
    songs = [
        RecommendationInput("plain", "Plain", "Runner", 90, 0.9, 70, 60, {"舒缓"}),
        RecommendationInput("preferred", "Preferred", "Runner", 90, 0.9, 70, 60, {"摇滚"}),
    ]

    results = recommend_songs(target_bpm=90, songs=songs, preferred_tags={"摇滚"})

    assert results[0]["song_id"] == "preferred"
