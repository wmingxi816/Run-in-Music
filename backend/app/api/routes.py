from pathlib import Path
from tempfile import NamedTemporaryFile

import httpx
from fastapi import APIRouter, Depends, HTTPException, Query
from fastapi.responses import FileResponse
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.ai_music.storage import generated_audio_path
from app.core.config import get_settings
from app.core.database import get_session
from app.models import AudioAnalysis, CrawlJob, Song
from app.providers.registry import provider_for_url
from app.schemas.song import AnalysisJobRequest, AnalysisJobResponse, BatchCrawlRequest, BatchCrawlResponse, CrawlJobRequest, CrawlJobResponse, ResolveLinkRequest, ResolveLinkResponse
from app.services.bpm import analyze_audio_file, candidate_bpms
from app.services.catalog import list_catalog, process_batch_urls, song_to_out, upsert_resolved_track
from app.services.recommendation import RecommendationInput, recommend_songs

router = APIRouter()


@router.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@router.post("/providers/resolve-link", response_model=ResolveLinkResponse)
async def resolve_link(payload: ResolveLinkRequest, session: Session = Depends(get_session)) -> ResolveLinkResponse:
    try:
        provider = provider_for_url(payload.url)
        resolved = await provider.resolve(payload.url)
        song = upsert_resolved_track(session, resolved)
    except Exception as exc:  # provider failures should be surfaced clearly to the caller
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    return ResolveLinkResponse(
        provider=resolved.provider,
        provider_track_id=resolved.provider_track_id,
        song=song_to_out(song),
    )


@router.post("/crawler/jobs", response_model=CrawlJobResponse)
async def create_crawl_job(payload: CrawlJobRequest, session: Session = Depends(get_session)) -> CrawlJobResponse:
    job = CrawlJob(input_url=payload.url, status="running")
    session.add(job)
    session.commit()
    session.refresh(job)

    try:
        provider = provider_for_url(payload.url)
        resolved = await provider.resolve(payload.url)
        song = upsert_resolved_track(session, resolved)
        job.provider = resolved.provider
        job.status = "completed"
        job.message = "Resolved and stored song metadata"
        session.commit()
        return CrawlJobResponse(id=job.id, status=job.status, message=job.message, song=song_to_out(song))
    except Exception as exc:
        job.status = "failed"
        job.message = str(exc)
        session.commit()
        return CrawlJobResponse(id=job.id, status=job.status, message=job.message, song=None)


@router.post("/crawler/batch", response_model=BatchCrawlResponse)
async def create_crawl_batch(payload: BatchCrawlRequest, session: Session = Depends(get_session)) -> BatchCrawlResponse:
    if not payload.urls:
        raise HTTPException(status_code=400, detail="urls must contain at least one link")
    return await process_batch_urls(session, payload.urls)


@router.post("/analysis/jobs", response_model=AnalysisJobResponse)
async def create_analysis_job(payload: AnalysisJobRequest, session: Session = Depends(get_session)) -> AnalysisJobResponse:
    song = session.get(Song, payload.song_id)
    if not song:
        raise HTTPException(status_code=404, detail="Song not found")
    if not payload.audio_url:
        return AnalysisJobResponse(status="pending", message="No audio_url provided; song is waiting for an analyzable audio source")

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.get(payload.audio_url)
            response.raise_for_status()
        with NamedTemporaryFile(delete=False, suffix=Path(payload.audio_url).suffix or ".audio") as temp:
            temp.write(response.content)
            temp_path = Path(temp.name)
        bpm, confidence, candidates = analyze_audio_file(temp_path)
        temp_path.unlink(missing_ok=True)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"Audio analysis failed: {exc}") from exc

    analysis = AudioAnalysis(
        song_id=song.id,
        bpm=bpm,
        confidence=confidence,
        source="analysis",
        candidate_bpms_csv=",".join(str(value) for value in candidates),
    )
    session.add(analysis)
    if confidence >= song.bpm_confidence:
        song.bpm = bpm
        song.bpm_source = "analysis"
        song.bpm_confidence = confidence
    session.commit()
    return AnalysisJobResponse(status="completed", message="Audio BPM analysis completed", bpm=bpm, confidence=confidence, candidate_bpms=candidates)


@router.get("/catalog/export")
def export_catalog(session: Session = Depends(get_session)):
    return list_catalog(session)


@router.get("/audio/generated/{track_id}")
def serve_generated_audio(track_id: str):
    try:
        path = generated_audio_path(track_id, Path(get_settings().generated_music_dir))
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    if not path.exists():
        raise HTTPException(status_code=404, detail="Generated audio file not found")
    return FileResponse(path, media_type="audio/wav", filename=path.name)


@router.get("/songs/recommend")
def recommend(
    target_bpm: float = Query(..., gt=0),
    limit: int = Query(12, ge=1, le=50),
    session: Session = Depends(get_session),
):
    songs = session.scalars(select(Song).where(Song.bpm.is_not(None))).all()
    inputs = [
        RecommendationInput(
            id=song.id,
            title=song.title,
            artist=song.artist,
            bpm=float(song.bpm or 0),
            bpm_confidence=song.bpm_confidence,
            energy=song.energy,
            popularity=song.popularity,
            tags={tag for tag in song.tags_csv.split(",") if tag},
        )
        for song in songs
    ]
    return recommend_songs(target_bpm=target_bpm, songs=inputs, limit=limit)


@router.get("/analysis/candidate-bpms")
def candidate_bpm_endpoint(bpm: float = Query(..., gt=0)) -> dict[str, list[float]]:
    return {"candidate_bpms": candidate_bpms(bpm)}
