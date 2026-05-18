# MusicGen-small 过夜生成流程

## 目标

用已经烟测成功的 `facebook/musicgen-small` 先批量生成一批可播放跑步音乐样例，输出到 `backend/generated_music/musicgen-small/`，再用现有 BPM 分析和导入流程写入后台曲库。Android 端只同步元数据和播放 URL，音乐文件保存在服务器项目目录中。

## 当前选择

- 模型：`facebook/musicgen-small`
- 运行方式：CPU，最稳，不依赖 CUDA 驱动
- 输出目录：`backend/generated_music/musicgen-small/`
- 元数据账本：`backend/generated_music/musicgen-small/metadata.jsonl`
- 运行日志：`backend/generated_music/musicgen-small/run.log`
- 默认生成数量：100 首任务
- 默认生成长度：由 `--max-new-tokens` 控制，当前建议先用 `512` 或 `768`

说明：MusicGen-small 适合 MVP 自测和流程验证，但模型权重是非商用许可。正式商用前需要替换为许可更合适的模型或重新审查授权。

## 第一次准备环境

在仓库根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File backend\scripts\setup_musicgen_env.ps1
```

这个命令会创建 `backend\.musicgen_venv`，并安装：

- CPU 版 `torch`
- `transformers`
- `scipy`
- `accelerate`

## 开跑前 dry-run

dry-run 不会加载模型，也不会生成音频，只检查任务规划和日志路径：

```powershell
backend\.musicgen_venv\Scripts\python.exe backend\scripts\musicgen_batch.py `
  --count 100 `
  --max-new-tokens 512 `
  --resume `
  --dry-run
```

## 今晚真正开始跑

你明确让我开始跑之后，再执行这个命令：

```powershell
backend\.musicgen_venv\Scripts\python.exe backend\scripts\musicgen_batch.py `
  --count 100 `
  --max-new-tokens 512 `
  --resume
```

如果想生成稍长一点，可以把 `512` 改成 `768` 或 `1024`，但 CPU 会明显更慢。建议第一晚先用 `512`，确保能生成更多首，而不是卡在少数长音频上。

## 断点续跑

脚本默认配合 `--resume` 使用。已经存在的 `.wav` 或已经写入 `metadata.jsonl` 的 track 会跳过，所以中途断电或手动停止后，重新执行同一命令即可继续。

## 明早分析并导入曲库

生成完成或生成到一部分后，用后端环境分析 BPM 并导入数据库：

```powershell
backend\.venv\Scripts\python.exe backend\scripts\analyze_import_generated.py `
  --manifest backend\generated_music\musicgen-small\metadata.jsonl
```

如果想先看会导入什么，不实际分析导入：

```powershell
backend\.venv\Scripts\python.exe backend\scripts\analyze_import_generated.py `
  --manifest backend\generated_music\musicgen-small\metadata.jsonl `
  --dry-run
```

## 启动后台给 Android 同步

```powershell
cd backend
.\.venv\Scripts\Activate.ps1
uvicorn app.main:app --reload
```

Android 模拟器继续从 `http://10.0.2.2:8000/catalog/export` 同步曲库。生成音乐会通过 `/audio/generated/{track_id}` 从服务器串流播放，不会保存到手机。

## 预期文件

```text
backend/generated_music/musicgen-small/
  metadata.jsonl
  run.log
  rim_065_warmup_electronic_001.wav
  rim_070_easy_run_synthwave_002.wav
  ...
```

## 风险和建议

- CPU 生成很慢，今晚优先用 `--max-new-tokens 512` 跑稳定性。
- `facebook/musicgen-small` 不保证严格按 prompt 的 BPM 输出，所以导入前必须跑 BPM 分析。
- 如果 BPM 置信度偏低，可以先保留结果，后续增加人工审核或筛选阈值。
- 生成目录已被 `.gitignore` 忽略，不会上传 Git。
