from app.ai_music.generation_plan import build_generation_plan


def test_build_generation_plan_returns_requested_number_of_unique_tasks():
    plan = build_generation_plan(count=100)

    assert len(plan) == 100
    assert len({task.track_id for task in plan}) == 100
    assert plan[0].track_id == "rim_065_warmup_electronic_001"


def test_generation_tasks_are_runner_focused_and_bounded():
    plan = build_generation_plan(count=100)

    assert all(65 <= task.target_bpm <= 115 for task in plan)
    assert all(180 <= task.duration_seconds <= 300 for task in plan)
    assert all("running music" in task.prompt for task in plan)
    assert all("steady beat" in task.prompt for task in plan)
    assert all("no tempo drift" in task.prompt for task in plan)
    assert {"warmup", "easy_run", "tempo_run", "interval_fast", "recovery"} <= {
        tag for task in plan for tag in task.style_tags
    }


def test_generation_plan_is_deterministic():
    first = build_generation_plan(count=12, seed_start=500)
    second = build_generation_plan(count=12, seed_start=500)

    assert first == second
    assert [task.seed for task in first[:3]] == [500, 501, 502]
