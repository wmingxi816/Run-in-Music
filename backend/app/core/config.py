from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    database_url: str = "sqlite:///./run_in_music.db"
    http_timeout_seconds: float = 15.0

    model_config = SettingsConfigDict(
        env_file=".env",
        env_prefix="RUN_IN_MUSIC_",
        extra="ignore",
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()
