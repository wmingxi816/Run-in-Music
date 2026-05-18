from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class GenerationTask:
    track_id: str
    title: str
    target_bpm: int
    duration_seconds: int
    style_tags: list[str]
    energy: int
    prompt: str
    seed: int


BPM_BUCKETS = [65, 70, 75, 80, 85, 90, 95, 100, 105, 110, 115]
SCENARIOS = ["warmup", "easy_run", "tempo_run", "interval_fast", "recovery", "cooldown", "night_run"]
STYLES = ["electronic", "synthwave", "indie_dance", "rock_hybrid", "ambient_pop"]
ENERGY_BY_SCENARIO = {
    "warmup": 55,
    "easy_run": 68,
    "tempo_run": 78,
    "interval_fast": 90,
    "recovery": 45,
    "cooldown": 40,
    "night_run": 72,
}


def build_generation_plan(count: int = 100, seed_start: int = 1000) -> list[GenerationTask]:
    if count <= 0:
        return []

    tasks: list[GenerationTask] = []
    for index in range(count):
        bpm = BPM_BUCKETS[index % len(BPM_BUCKETS)]
        scenario = SCENARIOS[index % len(SCENARIOS)]
        style = STYLES[index % len(STYLES)]
        serial = index + 1
        duration = 180 + (index % 5) * 30
        energy = ENERGY_BY_SCENARIO[scenario]
        tags = [style, scenario, "steady_beat", energy_label(energy)]
        track_id = f"rim_{bpm:03d}_{scenario}_{style}_{serial:03d}"
        tasks.append(
            GenerationTask(
                track_id=track_id,
                title=title_for(style, scenario, serial),
                target_bpm=bpm,
                duration_seconds=duration,
                style_tags=tags,
                energy=energy,
                prompt=prompt_for(style=style, scenario=scenario, bpm=bpm, energy=energy),
                seed=seed_start + index,
            ),
        )
    return tasks


def prompt_for(style: str, scenario: str, bpm: int, energy: int) -> str:
    readable_style = style.replace("_", " ")
    readable_scenario = scenario.replace("_", " ")
    return (
        f"instrumental {readable_style} running music for {readable_scenario}, "
        f"steady beat, target {bpm} BPM, energy {energy}/100, no tempo drift, "
        "clear drums, loop-safe structure, no vocals"
    )


def title_for(style: str, scenario: str, serial: int) -> str:
    return f"{style.replace('_', ' ').title()} {scenario.replace('_', ' ').title()} {serial:03d}"


def energy_label(energy: int) -> str:
    if energy >= 85:
        return "high_energy"
    if energy >= 60:
        return "medium_energy"
    return "low_energy"
