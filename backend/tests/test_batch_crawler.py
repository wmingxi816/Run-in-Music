import asyncio

import pytest
from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.core.database import Base
from app.models import CrawlJob, PlatformTrack, Song
from app.providers.base import ResolvedTrack
from app.schemas.song import BatchCrawlResponse
from app.services import catalog as catalog_module


class FakeProvider:
    def __init__(self, provider: str, track_id: str, *, bpm: float | None = 128.0, title: str = "Fake Title", artist: str = "Fake Artist"):
        self.provider = provider
        self._track_id = track_id
        self._bpm = bpm
        self._title = title
        self._artist = artist

    def can_handle(self, url: str) -> bool:
        return True

    async def resolve(self, url: str) -> ResolvedTrack:
        return ResolvedTrack(
            provider=self.provider,
            provider_track_id=self._track_id,
            title=self._title,
            artist=self._artist,
            duration_seconds=180,
            url=url,
            bpm=self._bpm,
            bpm_source="provider" if self._bpm else None,
            bpm_confidence=0.85 if self._bpm else 0.0,
            energy=75,
            popularity=60,
            tags=["test"],
        )


class FailingProvider:
    provider = "broken"

    def can_handle(self, url: str) -> bool:
        return True

    async def resolve(self, url: str) -> ResolvedTrack:
        raise RuntimeError("provider exploded")


@pytest.fixture
def session():
    engine = create_engine(
        "sqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    Base.metadata.create_all(engine)
    Session = sessionmaker(bind=engine, autoflush=False, autocommit=False, expire_on_commit=False)
    db = Session()
    try:
        yield db
    finally:
        db.close()


def _run(coro):
    return asyncio.get_event_loop().run_until_complete(coro) if False else asyncio.run(coro)


def test_batch_mixed_success_and_failure(session):
    resolvers = {
        "https://ok.example/qq/001": FakeProvider("qq", "001", bpm=130.0),
        "https://ok.example/netease/abc": FakeProvider("netease", "abc", bpm=None),
        "https://unsupported.example/x": None,
        "https://broken.example/y": FailingProvider(),
    }

    def resolver(url):
        provider = resolvers.get(url)
        if provider is None:
            raise ValueError("Unsupported music provider URL")
        return provider

    result: BatchCrawlResponse = asyncio.run(
        catalog_module.process_batch_urls(session, list(resolvers.keys()), resolver=resolver)
    )

    assert result.total == 4
    assert result.succeeded == 2
    assert result.failed == 2
    assert result.missing_bpm == 1
    assert result.duplicates == 0

    by_url = {item.url: item for item in result.results}
    assert by_url["https://ok.example/qq/001"].status == "ok"
    assert by_url["https://ok.example/qq/001"].missing_bpm is False
    assert by_url["https://ok.example/qq/001"].song.bpm == 130.0

    assert by_url["https://ok.example/netease/abc"].status == "ok"
    assert by_url["https://ok.example/netease/abc"].missing_bpm is True

    assert by_url["https://unsupported.example/x"].status == "failed"
    assert "Unsupported" in by_url["https://unsupported.example/x"].error

    assert by_url["https://broken.example/y"].status == "failed"
    assert "exploded" in by_url["https://broken.example/y"].error
    assert by_url["https://broken.example/y"].provider == "broken"

    songs = session.scalars(select(Song)).all()
    assert len(songs) == 2

    jobs = session.scalars(select(CrawlJob)).all()
    assert len(jobs) == 4
    statuses = {job.input_url: job.status for job in jobs}
    assert statuses["https://ok.example/qq/001"] == "completed"
    assert statuses["https://broken.example/y"] == "failed"


def test_batch_marks_duplicates_without_creating_new_song(session):
    urls = [
        "https://ok.example/qq/dup-1",
        "https://ok.example/qq/dup-2",
    ]
    provider = FakeProvider("qq", "shared-track", bpm=125.0)

    def resolver(_url):
        return provider

    result = asyncio.run(catalog_module.process_batch_urls(session, urls, resolver=resolver))

    assert result.total == 2
    assert result.succeeded == 2
    assert result.failed == 0
    assert result.duplicates == 1
    assert result.missing_bpm == 0

    songs = session.scalars(select(Song)).all()
    assert len(songs) == 1

    platform_tracks = session.scalars(select(PlatformTrack)).all()
    assert len(platform_tracks) == 1


def test_batch_endpoint_returns_400_on_empty_urls(session):
    from fastapi.testclient import TestClient

    from app.core.database import get_session
    from app.main import app

    def override():
        try:
            yield session
        finally:
            pass

    app.dependency_overrides[get_session] = override
    try:
        client = TestClient(app)
        response = client.post("/crawler/batch", json={"urls": []})
        assert response.status_code == 400
    finally:
        app.dependency_overrides.pop(get_session, None)


def test_batch_endpoint_uses_registered_provider(monkeypatch, session):
    from fastapi.testclient import TestClient

    from app.core.database import get_session
    from app.main import app

    provider = FakeProvider("qq", "endpoint-track", bpm=140.0)

    def fake_provider_for_url(_url):
        return provider

    monkeypatch.setattr(catalog_module, "provider_for_url", fake_provider_for_url)

    def override():
        try:
            yield session
        finally:
            pass

    app.dependency_overrides[get_session] = override
    try:
        client = TestClient(app)
        response = client.post(
            "/crawler/batch",
            json={"urls": ["https://y.qq.com/n/ryqq/songDetail/endpoint-track"]},
        )
        assert response.status_code == 200
        body = response.json()
        assert body["total"] == 1
        assert body["succeeded"] == 1
        assert body["results"][0]["song"]["bpm"] == 140.0
    finally:
        app.dependency_overrides.pop(get_session, None)
