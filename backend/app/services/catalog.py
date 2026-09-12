from typing import Callable

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models import CrawlJob, PlatformTrack, Song
from app.providers.base import MusicProvider, ResolvedTrack
from app.providers.registry import provider_for_url
from app.schemas.song import BatchCrawlItemResult, BatchCrawlResponse, SongOut


ProviderResolver = Callable[[str], MusicProvider]


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


async def process_batch_urls(
    session: Session,
    urls: list[str],
    resolver: ProviderResolver | None = None,
) -> BatchCrawlResponse:
    pick_provider = resolver or provider_for_url
    results: list[BatchCrawlItemResult] = []
    succeeded = 0
    failed = 0
    missing_bpm = 0
    duplicates = 0

    for url in urls:
        job = CrawlJob(input_url=url, status="running")
        session.add(job)
        session.commit()

        try:
            provider = pick_provider(url)
        except Exception as exc:
            job.status = "failed"
            job.message = str(exc)
            session.commit()
            failed += 1
            results.append(BatchCrawlItemResult(url=url, status="failed", error=str(exc)))
            continue

        try:
            resolved = await provider.resolve(url)
        except Exception as exc:
            job.provider = getattr(provider, "provider", None)
            job.status = "failed"
            job.message = str(exc)
            session.commit()
            failed += 1
            results.append(
                BatchCrawlItemResult(
                    url=url,
                    status="failed",
                    provider=getattr(provider, "provider", None),
                    error=str(exc),
                )
            )
            continue

        existing = session.scalar(
            select(PlatformTrack).where(
                PlatformTrack.provider == resolved.provider,
                PlatformTrack.provider_track_id == resolved.provider_track_id,
            ),
        )
        was_duplicate = existing is not None

        song = upsert_resolved_track(session, resolved)
        item_missing_bpm = song.bpm is None

        job.provider = resolved.provider
        job.status = "completed"
        if was_duplicate:
            job.message = "Duplicate platform track; metadata refreshed"
        elif item_missing_bpm:
            job.message = "Resolved without BPM"
        else:
            job.message = "Resolved and stored song metadata"
        session.commit()

        succeeded += 1
        if was_duplicate:
            duplicates += 1
        if item_missing_bpm:
            missing_bpm += 1

        results.append(
            BatchCrawlItemResult(
                url=url,
                status="ok",
                provider=resolved.provider,
                missing_bpm=item_missing_bpm,
                duplicate=was_duplicate,
                song=song_to_out(song),
            )
        )

    return BatchCrawlResponse(
        total=len(urls),
        succeeded=succeeded,
        failed=failed,
        missing_bpm=missing_bpm,
        duplicates=duplicates,
        results=results,
    )
