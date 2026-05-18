from __future__ import annotations

import argparse
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BACKEND = ROOT / "backend"
if str(BACKEND) not in sys.path:
    sys.path.insert(0, str(BACKEND))

from app.ai_music.generation_plan import build_generation_plan
from app.ai_music.manifest import append_manifest_entry, load_manifest_entries, manifest_entry_from_task


def main() -> int:
    args = parse_args()
    output_dir = Path(args.output_dir)
    manifest_path = Path(args.manifest)
    log_path = Path(args.log)
    output_dir.mkdir(parents=True, exist_ok=True)
    log_path.parent.mkdir(parents=True, exist_ok=True)

    tasks = build_generation_plan(count=args.count, seed_start=args.seed_start)[args.start_index :]
    completed = {entry.track_id for entry in load_manifest_entries(manifest_path)}
    tasks_to_run = [
        task for task in tasks
        if not args.resume or (task.track_id not in completed and not (output_dir / f"{task.track_id}.wav").exists())
    ]

    log(log_path, f"planned={len(tasks)} to_run={len(tasks_to_run)} output_dir={output_dir} model={args.model_id}")
    if args.dry_run:
        for task in tasks_to_run[: args.preview]:
            log(log_path, f"DRY {task.track_id} {task.duration_seconds}s {task.target_bpm}bpm :: {task.prompt}")
        return 0

    import torch
    from scipy.io.wavfile import write as write_wav
    from transformers import AutoProcessor, MusicgenForConditionalGeneration

    log(log_path, f"loading model {args.model_id}")
    started = time.time()
    processor = AutoProcessor.from_pretrained(args.model_id)
    model = MusicgenForConditionalGeneration.from_pretrained(args.model_id)
    model.to(args.device)
    sample_rate = int(model.config.audio_encoder.sampling_rate)
    log(log_path, f"loaded model in {time.time() - started:.1f}s sample_rate={sample_rate}")

    for index, task in enumerate(tasks_to_run, start=1):
        audio_path = output_dir / f"{task.track_id}.wav"
        log(log_path, f"[{index}/{len(tasks_to_run)}] generating {task.track_id} target={task.target_bpm} prompt={task.prompt}")
        started = time.time()
        inputs = processor(text=[task.prompt], padding=True, return_tensors="pt").to(args.device)
        generator = torch.Generator(device=args.device)
        generator.manual_seed(task.seed)
        with torch.no_grad():
            audio_values = model.generate(
                **inputs,
                max_new_tokens=args.max_new_tokens,
                do_sample=True,
                guidance_scale=args.guidance_scale,
                generator=generator,
            )
        audio = audio_values[0, 0].detach().cpu().float().numpy()
        peak = max(abs(audio).max(), 1e-6)
        audio_i16 = (audio / peak * 32767).astype("int16")
        write_wav(audio_path, sample_rate, audio_i16)
        duration_seconds = round(len(audio_i16) / sample_rate)
        append_manifest_entry(
            manifest_path,
            manifest_entry_from_task(
                task=task,
                audio_path=audio_path,
                model_id=args.model_id,
            ),
        )
        log(log_path, f"[{index}/{len(tasks_to_run)}] saved {audio_path} duration={duration_seconds}s elapsed={time.time() - started:.1f}s")
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate Run in Music tracks with Hugging Face MusicGen.")
    parser.add_argument("--model-id", default="facebook/musicgen-small")
    parser.add_argument("--count", type=int, default=100)
    parser.add_argument("--start-index", type=int, default=0)
    parser.add_argument("--seed-start", type=int, default=1000)
    parser.add_argument("--max-new-tokens", type=int, default=512)
    parser.add_argument("--guidance-scale", type=float, default=3.0)
    parser.add_argument("--device", default="cpu")
    parser.add_argument("--output-dir", default="backend/generated_music/musicgen-small")
    parser.add_argument("--manifest", default="backend/generated_music/musicgen-small/metadata.jsonl")
    parser.add_argument("--log", default="backend/generated_music/musicgen-small/run.log")
    parser.add_argument("--resume", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--preview", type=int, default=10)
    return parser.parse_args()


def log(path: Path, message: str) -> None:
    line = f"{time.strftime('%Y-%m-%d %H:%M:%S')} {message}"
    print(line, flush=True)
    with path.open("a", encoding="utf-8") as file:
        file.write(line)
        file.write("\n")


if __name__ == "__main__":
    raise SystemExit(main())
