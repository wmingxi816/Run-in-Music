from app.providers.base import MusicProvider
from app.providers.netease import NeteaseProvider
from app.providers.qq_music import QQMusicProvider


def providers() -> list[MusicProvider]:
    return [QQMusicProvider(), NeteaseProvider()]


def provider_for_url(url: str) -> MusicProvider:
    for provider in providers():
        if provider.can_handle(url):
            return provider
    raise ValueError("Unsupported music provider URL")
