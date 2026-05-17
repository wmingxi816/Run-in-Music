from dataclasses import dataclass, field
from typing import Protocol


@dataclass(frozen=True)
class ResolvedTrack:
    provider: str
    provider_track_id: str
    title: str
    artist: str
    duration_seconds: int | None
    url: str
    bpm: float | None = None
    bpm_source: str | None = None
    bpm_confidence: float = 0.0
    energy: int = 70
    popularity: int = 50
    tags: list[str] = field(default_factory=list)


class MusicProvider(Protocol):
    provider: str

    def can_handle(self, url: str) -> bool:
        ...

    async def resolve(self, url: str) -> ResolvedTrack:
        ...
