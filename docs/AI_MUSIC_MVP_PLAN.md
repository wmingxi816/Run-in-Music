# Run in Music AI 音乐 MVP 规划

## Goal

MVP 先不依赖第三方音乐平台曲库，改为在服务端批量生成 100 首 3-5 分钟的 AI 跑步音乐，并为每首歌生成/分析标签、BPM、能量、风格、适合跑步场景等数据。Android 端通过后台曲库同步和推荐接口使用这些歌曲。

## Context

当前项目已有：

- Android 端步频测量、目标 BPM 映射、本地 Room 曲库、推荐排序。
- Python FastAPI 后台、歌曲表、BPM 分析接口、曲库导出接口。
- 诊断日志导出，方便真机测试。

下一步需要把“假设已有很多歌”的曲库，替换成一批可控的 AI 生成音乐。这样可以快速验证跑步节奏推荐闭环，不再被音乐平台链接、版权、BPM 缺失、曲库抓取稳定性卡住。

## Input

- 生成参数：风格、目标 BPM、能量、运动场景、时长、seed。
- 音频输出：`wav` 或 `flac`，后续可转码为 `mp3`/`m4a`。
- 分析输入：服务端本地音频文件。
- App 推荐输入：用户 SPM、目标 BPM、偏好标签、歌曲分析结果。

## Constraints

- 第一版只做自己测试和 MVP 验证，不做正式商用发布。
- AI 音乐生成放服务端，不放 Android 本机。
- 生成模型需要能本地运行，优先开源代码/开放权重。
- 3-5 分钟完整歌曲优先，短片段模型只作为备选。
- BPM 和标签以服务端分析结果为准，Android 不重复跑音频分析。
- Android 保持离线推荐能力：后端生成/分析/导出，手机同步后可本地推荐。
- 不默认导出或保存原始 GPS 坐标到诊断包。

## Tone

产品上强调“跑步专属 AI 节奏歌单”，不是“AI 作曲炫技”。界面文案面向跑者，少讲模型，多讲 BPM、能量、场景和适配步频。

## Style

音乐标签保持跑步语义：

- `easy_run`
- `tempo_run`
- `interval_fast`
- `recovery`
- `night_run`
- `warmup`
- `cooldown`
- `high_energy`
- `steady_beat`

歌曲命名先用内部可读格式：

```text
rim_082_easy_run_electronic_001
rim_096_tempo_run_synthwave_014
rim_108_interval_fast_rock_037
```

## Output

服务端新增一个 AI 曲库生产管线：

- `backend/app/ai_music/generation_plan.py`：生成 100 首歌曲的参数清单。
- `backend/app/ai_music/generate_batch.py`：调用本地 AI 音乐模型批量生成。
- `backend/app/ai_music/analyze_batch.py`：用 librosa 分析 BPM、节拍点、能量等。
- `backend/app/ai_music/import_generated.py`：把生成音频和分析结果写入数据库。
- `backend/generated_music/`：本地生成文件目录，默认不提交 Git。
- `GET /songs/recommend`：服务端推荐使用同一套评分算法。
- `GET /catalog/export`：Android 同步已分析完成的 AI 曲库。

Android 端保持：

- App 本地 Room 缓存曲库。
- 本地推荐作为离线 fallback。
- 音乐页展示 AI 曲库推荐卡片，后续可播放本地/服务端音频或跳转外链。

## Research Notes

### 推荐生成模型：ACE-Step

ACE-Step 的官方 README 说明它是开源音乐生成 foundation model，支持多种主流音乐风格、乐器和语言；README 还写到模型可合成最长约 4 分钟音乐，并给出硬件性能表，例如 RTX 4090 渲染 1 分钟音频约 1.74 秒（27 steps）。项目许可证为 Apache License 2.0。

适合本项目的原因：

- 更接近 3-5 分钟完整歌曲需求。
- 支持 tags、lyrics、duration 等输入，方便做跑步音乐矩阵。
- 可本地运行，支持命令行/Gradio/API 集成。
- 许可证比部分非商用模型更友好，但正式商用前仍需再审查模型权重、训练数据声明和生成内容风险。

### 备选：AudioCraft / MusicGen

AudioCraft 官方 README 说明它是 PyTorch 音频生成研究库，包含 MusicGen、AudioGen、MAGNeT 等模型；代码 MIT，模型权重 CC-BY-NC 4.0。适合作为研究和短片段生成备选，但非商用权重和长曲结构限制不适合作为首选 MVP 生产线。

### 备选：Stable Audio Open

Stable Audio Open 的模型卡访问需要 Hugging Face 授权，公开资料通常强调短音频/样本生成。可以作为音效、loop、短 intro/outro 备选，不作为 3-5 分钟完整跑步曲首选。

### BPM 分析

第一版继续使用 `librosa.beat.beat_track`。librosa 官方文档说明它是动态规划 beat tracker，会估计 tempo 并返回 beat locations；没有明显 onset strength 时会返回 0 BPM 和空 beat 列表。

## Steps

1. 新增 `backend/generated_music/` 到 `.gitignore`。
2. 新增 `backend/app/ai_music/generation_plan.py`，生成 100 条待生成任务：
   - BPM 分布覆盖 65-115。
   - 每个 BPM 桶覆盖不同风格和能量。
   - 每首歌有 `target_bpm`、`duration_seconds`、`style_tags`、`prompt`、`seed`。
3. 新增 `generate_batch.py`：
   - 第一版先封装 ACE-Step CLI 调用。
   - 支持断点续跑。
   - 输出音频文件和 `metadata.json`。
4. 新增 `analyze_batch.py`：
   - 用 librosa 分析 BPM。
   - 生成 `candidate_bpms`: `bpm`、`bpm/2`、`bpm*2`。
   - 计算能量粗指标 RMS / onset strength。
   - 标记 `bpm_confidence`。
5. 新增 `import_generated.py`：
   - 写入 `songs`、`platform_tracks` 或新增 `generated_tracks`。
   - `bpm_source=analysis`
   - `platform=generated`
6. 推荐算法位置：
   - 服务端作为权威推荐：便于统一算法、调参、记录日志、支持未来云端曲库。
   - Android 保留本地推荐 fallback：跑步时离线也能推荐。
7. Android 音乐页后续接入：
   - 曲库同步后展示 AI 歌曲。
   - 推荐卡片显示 AI 标签和 BPM。
   - 第一版可以不内置播放器，先播放本地生成音频或展示占位。
8. 验收：
   - 能生成 5 首样例音乐。
   - 能分析出 BPM 和候选 BPM。
   - 能导入数据库并从 `/catalog/export` 导出。
   - Android 音乐页能看到同步后的 AI 曲目。

## Notes

- 如果本机没有高显存 GPU，先用 5-10 首样例验证全链路，再批量生成 100 首。
- 生成 3-5 分钟歌曲时，提示词要显式写 “steady beat / running / no tempo drift”。
- BPM 分析会有半速/倍速误判，所以数据库必须保存原始 BPM 和候选 BPM。
- 生成音乐也要做质量筛选，第一版至少保留 `accepted/rejected/reason` 字段。
- 服务端推荐更适合调算法；Android 本地推荐更适合离线跑步，两者不冲突。

## Sources

- ACE-Step GitHub: https://github.com/ace-step/ACE-Step
- AudioCraft GitHub: https://github.com/facebookresearch/audiocraft
- librosa beat_track: https://librosa.org/doc/latest/generated/librosa.beat.beat_track.html
