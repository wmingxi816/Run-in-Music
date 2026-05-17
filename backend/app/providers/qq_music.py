import re

import httpx

from app.core.config import get_settings
from app.providers.base import ResolvedTrack


SONG_MID_PATTERNS = [
    re.compile(r"songDetail/(?P<mid>[A-Za-z0-9]+)"),
    re.compile(r"[?&]songmid=(?P<mid>[A-Za-z0-9]+)"),
]


def extract_songmid(url: str) -> str | None:
    for pattern in SONG_MID_PATTERNS:
        match = pattern.search(url)
        if match:
            return match.group("mid")
    return None


class QQMusicProvider:
    provider = "qq"

    def can_handle(self, url: str) -> bool:
        return ("y.qq.com" in url or "qq.com" in url) and extract_songmid(url) is not None

    async def resolve(self, url: str) -> ResolvedTrack:
        songmid = extract_songmid(url)
        if not songmid:
            raise ValueError("QQ Music URL does not contain a songmid")

        settings = get_settings()
        detail_url = (
            "https://c.y.qq.com/v8/fcg-bin/fcg_play_single_song.fcg"
            f"?songmid={songmid}&tpl=yqq_song_detail&format=json"
        )
        async with httpx.AsyncClient(timeout=settings.http_timeout_seconds) as client:
            response = await client.get(detail_url, headers={"Referer": "https://y.qq.com/", "User-Agent": "Mozilla/5.0"})
            response.raise_for_status()
            payload = response.json()

        data = payload.get("data") or []
        if not data:
            raise ValueError("QQ Music did not return song data")
        item = data[0]
        singers = item.get("singer") or []
        tags = []
        genre = item.get("genre")
        if isinstance(genre, str) and genre:
            tags.append(genre)
        elif isinstance(genre, list):
            tags.extend(str(value) for value in genre if value)

        bpm = item.get("bpm")
        return ResolvedTrack(
            provider=self.provider,
            provider_track_id=songmid,
            title=item.get("name") or item.get("songname") or "Unknown title",
            artist=(singers[0].get("name") if singers else "Unknown artist"),
            duration_seconds=item.get("interval"),
            url=f"https://y.qq.com/n/ryqq/songDetail/{songmid}",
            bpm=float(bpm) if bpm else None,
            bpm_source="provider" if bpm else None,
            bpm_confidence=0.85 if bpm else 0.0,
            energy=75 if bpm else 70,
            popularity=60,
            tags=tags,
        )
