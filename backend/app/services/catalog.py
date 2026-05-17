from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models import PlatformTrack, Song
from app.providers.base import ResolvedTrack
from app.schemas.song import SongOut


def upsert_resolved_track(session: Session, resolved: ResolvedTrack) -> Song:
    existing_track = session.scalar(
        select(PlatformTrack).where(
            PlatformTrack.provider == resolved.provider,
            PlatformTrack.provider_track_id == resolved.provider_track_id,
        ),
    )
    if existing_track:
        song = existing_track.song
    else:
        song = Song(
            title=resolved.title,
            artist=resolved.artist,
            duration_seconds=resolved.duration_seconds,
            bpm=resolved.bpm,
            bpm_source=resolved.bpm_source,
            bpm_confidence=resolved.bpm_confidence,
            energy=resolved.energy,
            popularity=resolved.popularity,
            tags_csv=",".join(resolved.tags),
        )
        session.add(song)
        session.flush()
        session.add(
            PlatformTrack(
                song_id=song.id,
                provider=resolved.provider,
                provider_track_id=resolved.provider_track_id,
                url=resolved.url,
            ),
        )

    if resolved.bpm and (not song.bpm or resolved.bpm_confidence >= song.bpm_confidence):
        song.bpm = resolved.bpm
        song.bpm_source = resolved.bpm_source
        song.bpm_confidence = resolved.bpm_confidence
    if resolved.tags and not song.tags_csv:
        song.tags_csv = ",".join(resolved.tags)
    session.commit()
    session.refresh(song)
    return song


def song_to_out(song: Song) -> SongOut:
    return SongOut(
        id=song.id,
        title=song.title,
        artist=song.artist,
        duration_seconds=song.duration_seconds,
        bpm=song.bpm,
        bpm_source=song.bpm_source,
        bpm_confidence=song.bpm_confidence,
        energy=song.energy,
        popularity=song.popularity,
        tags=[tag for tag in song.tags_csv.split(",") if tag],
        platform_urls={track.provider: track.url for track in song.platform_tracks},
    )


def list_catalog(session: Session) -> list[SongOut]:
    songs = session.scalars(select(Song).order_by(Song.popularity.desc(), Song.title.asc())).unique().all()
    return [song_to_out(song) for song in songs]
