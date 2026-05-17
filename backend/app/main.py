from fastapi import FastAPI

from app.api.routes import router
from app.core.database import Base, engine

app = FastAPI(
    title="Run in Music Backend",
    version="0.1.0",
    description="Song metadata, BPM analysis, and recommendation pipeline for Run in Music.",
)


@app.on_event("startup")
def create_schema() -> None:
    Base.metadata.create_all(bind=engine)


app.include_router(router)
