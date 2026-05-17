from app.providers.netease import extract_song_id
from app.providers.qq_music import extract_songmid
from app.providers.registry import provider_for_url


def test_extracts_qq_songmid_from_song_detail_url():
    assert extract_songmid("https://y.qq.com/n/ryqq/songDetail/000QhABT1zNwjC") == "000QhABT1zNwjC"


def test_extracts_netease_song_id_from_share_url():
    assert extract_song_id("https://music.163.com/song?id=1901371647") == "1901371647"


def test_provider_registry_selects_supported_provider():
    assert provider_for_url("https://y.qq.com/n/ryqq/songDetail/000QhABT1zNwjC").provider == "qq"
    assert provider_for_url("https://music.163.com/song?id=1901371647").provider == "netease"
