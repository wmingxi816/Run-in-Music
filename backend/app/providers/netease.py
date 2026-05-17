import re

import httpx

from app.core.config import get_settings
from app.providers.base import ResolvedTrack


SONG_ID_PATTERNS = [
    re.compile(r"[?&]id=(?P<id>\d+)"),
    re.compile(r"/song/(?P<id>\d+)"),
]


def extract_song_id(url: str) -> str | None:
    for pattern in SONG_ID_PATTERNS:
        match = pattern.search(url)
        if match:
            return match.group("id")
    return None


class NeteaseProvider:
    provider = "netease"

    def can_handle(self, url: str) -> bool:
        return "music.163.com" in url and extract_song_id(url) is not None

    async def resolve(self, url: str) -> ResolvedTrack:
        song_id = extract_song_id(url)
        if not song_id:
            raise ValueError("NetEase URL does not contain a song id")

        settings = get_settings()
        detail_url = f"https://music.163.com/api/song/detail/?id={song_id}&ids=[{song_id}]"
        async with httpx.AsyncClient(timeout=settings.http_timeout_seconds) as client:
            response = await client.get(detail_url, headers={"Referer": "https://music.163.com/", "User-Agent": "Mozilla/5.0"})
            response.raise_for_status()
            payload = response.json()

        songs = payload.get("songs") or []
        if not songs:
            raise ValueError("NetEase did not return song data")
        item = songs[0]
        artists = item.get("artists") or item.get("ar") or []
        duration_ms = item.get("duration") or item.get("dt")
        return ResolvedTrack(
            provider=self.provider,
            provider_track_id=song_id,
            title=item.get("name") or "Unknown title",
            artist=(artists[0].get("name") if artists else "Unknown artist"),
            duration_seconds=int(duration_ms / 1000) if duration_ms else None,
            url=f"https://music.163.com/song?id={song_id}",
            bpm=None,
            bpm_source=None,
            bpm_confidence=0.0,
            energy=70,
            popularity=55,
            tags=[],
        )
