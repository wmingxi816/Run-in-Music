from __future__ import annotations

import argparse
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BACKEND = ROOT / "backend"
if str(BACKEND) not in sys.path:
    sys.path.insert(0, str(BACKEND))

from app.ai_music.analyze_batch import analyze_generated_tracks
from app.ai_music.import_generated import import_generated_tracks
from app.ai_music.manifest import load_generated_track_metadata
from app.core.database import SessionLocal


def main() -> int:
    args = parse_args()
    metadata = load_generated_track_metadata(Path(args.manifest))
    if args.limit is not None:
        metadata = metadata[: args.limit]
    print(f"loaded metadata={len(metadata)} from {args.manifest}")
    if args.dry_run:
        for track in metadata[: args.preview]:
            print(f"DRY {track.track_id} {track.audio_path} target={track.target_bpm}")
        return 0

    analyses = analyze_generated_tracks(metadata)
    accepted = [row for row in analyses if row.status == "accepted"]
    rejected = [row for row in analyses if row.status != "accepted"]
    print(f"analysis accepted={len(accepted)} rejected={len(rejected)}")
    for row in rejected[: args.preview]:
        print(f"REJECTED {row.track_id}: {row.reject_reason}")

    with SessionLocal() as session:
        imported = import_generated_tracks(session, analyses)
    print(f"imported={imported}")
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Analyze generated MusicGen WAV files and import accepted tracks into the backend catalog.")
    parser.add_argument("--manifest", default="backend/generated_music/musicgen-small/metadata.jsonl")
    parser.add_argument("--limit", type=int, default=None)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--preview", type=int, default=10)
    return parser.parse_args()


if __name__ == "__main__":
    raise SystemExit(main())
