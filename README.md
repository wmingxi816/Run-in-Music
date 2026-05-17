# Run in Music

Run in Music is an Android running music recommendation MVP. It measures a runner's cadence for 10 seconds, maps SPM to a target BPM, recommends matching songs from a local catalog, and opens the selected track in an external music app.

The repository contains two parts:

- `app/`: Kotlin + Jetpack Compose Android app.
- `backend/`: FastAPI song metadata, provider resolving, BPM analysis, and catalog export prototype.

## Android MVP

- 10-second cadence measurement with `TYPE_STEP_DETECTOR`.
- Manual tap fallback for devices without a step detector or missing motion permission.
- Local Room catalog and interaction storage.
- BPM matching with half/double BPM normalization.
- External link opening with `Intent.ACTION_VIEW`.
- Foreground GPS tracking service skeleton for later run-session distance tracking.

Open the project in Android Studio from this repository root.

## Backend MVP

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload
```

Useful endpoints:

- `POST /providers/resolve-link`
- `POST /crawler/jobs`
- `POST /analysis/jobs`
- `GET /catalog/export`
- `GET /songs/recommend?target_bpm=80`

## Documentation

The working implementation plan lives in `docs/RUN_IN_MUSIC_PROJECT_PLAN.md`. Add new decisions and future features there before implementing them.
