from pydantic import BaseModel, Field


class ResolveLinkRequest(BaseModel):
    url: str


class CrawlJobRequest(BaseModel):
    url: str


class AnalysisJobRequest(BaseModel):
    song_id: str
    audio_url: str | None = None


class SongOut(BaseModel):
    id: str
    title: str
    artist: str
    duration_seconds: int | None
    bpm: float | None
    bpm_source: str | None
    bpm_confidence: float
    energy: int
    popularity: int
    tags: list[str]
    platform_urls: dict[str, str]


class ResolveLinkResponse(BaseModel):
    provider: str
    provider_track_id: str
    song: SongOut


class CrawlJobResponse(BaseModel):
    id: str
    status: str
    message: str
    song: SongOut | None = None


class AnalysisJobResponse(BaseModel):
    status: str
    message: str
    bpm: float | None = None
    confidence: float | None = None
    candidate_bpms: list[float] = Field(default_factory=list)
