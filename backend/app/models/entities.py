from datetime import datetime
from uuid import uuid4

from sqlalchemy import DateTime, Float, ForeignKey, Integer, String, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base


def new_id(prefix: str) -> str:
    return f"{prefix}_{uuid4().hex[:16]}"


class Song(Base):
    __tablename__ = "songs"

    id: Mapped[str] = mapped_column(String(48), primary_key=True, default=lambda: new_id("song"))
    title: Mapped[str] = mapped_column(String(255), index=True)
    artist: Mapped[str] = mapped_column(String(255), index=True)
    duration_seconds: Mapped[int | None] = mapped_column(Integer, nullable=True)
    bpm: Mapped[float | None] = mapped_column(Float, nullable=True)
    bpm_source: Mapped[str | None] = mapped_column(String(48), nullable=True)
    bpm_confidence: Mapped[float] = mapped_column(Float, default=0.0)
    energy: Mapped[int] = mapped_column(Integer, default=70)
    popularity: Mapped[int] = mapped_column(Integer, default=50)
    tags_csv: Mapped[str] = mapped_column(Text, default="")
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    platform_tracks: Mapped[list["PlatformTrack"]] = relationship(back_populates="song", cascade="all, delete-orphan")
    analyses: Mapped[list["AudioAnalysis"]] = relationship(back_populates="song", cascade="all, delete-orphan")


class PlatformTrack(Base):
    __tablename__ = "platform_tracks"
    __table_args__ = (UniqueConstraint("provider", "provider_track_id", name="uq_provider_track"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    song_id: Mapped[str] = mapped_column(ForeignKey("songs.id"))
    provider: Mapped[str] = mapped_column(String(48), index=True)
    provider_track_id: Mapped[str] = mapped_column(String(128), index=True)
    url: Mapped[str] = mapped_column(Text)

    song: Mapped[Song] = relationship(back_populates="platform_tracks")


class AudioAnalysis(Base):
    __tablename__ = "audio_analysis"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    song_id: Mapped[str] = mapped_column(ForeignKey("songs.id"))
    bpm: Mapped[float] = mapped_column(Float)
    confidence: Mapped[float] = mapped_column(Float, default=0.0)
    source: Mapped[str] = mapped_column(String(48), default="analysis")
    candidate_bpms_csv: Mapped[str] = mapped_column(Text, default="")
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    song: Mapped[Song] = relationship(back_populates="analyses")


class CrawlJob(Base):
    __tablename__ = "crawl_jobs"

    id: Mapped[str] = mapped_column(String(48), primary_key=True, default=lambda: new_id("crawl"))
    input_url: Mapped[str] = mapped_column(Text)
    provider: Mapped[str | None] = mapped_column(String(48), nullable=True)
    status: Mapped[str] = mapped_column(String(32), default="pending")
    message: Mapped[str] = mapped_column(Text, default="")
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)


class SongTag(Base):
    __tablename__ = "song_tags"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    song_id: Mapped[str] = mapped_column(ForeignKey("songs.id"))
    tag: Mapped[str] = mapped_column(String(64), index=True)
    source: Mapped[str] = mapped_column(String(48), default="provider")
