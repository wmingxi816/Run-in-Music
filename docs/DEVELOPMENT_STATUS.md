# Run in Music 开发状态与下一步路线

> 这个文档用于后续开发交接和持续更新。每次完成一个功能、调整方向、发现风险或决定下一步时，都先更新这里，再继续实现。

## 当前阶段

- 当前版本：Alpha 0.2 三栏首页 + GPS 跑步记录 + 诊断日志导出 + AI 音乐管线 PoC
- 当前日期：2026-05-18
- 仓库地址：https://github.com/wmingxi816/Run-in-Music
- 当前主线分支：`main`
- 最近已推送提交：
  - `64bc333 Initial Run in Music MVP`
  - `15b91fd Add backend catalog sync`
  - `b64076c Add development status roadmap`
  - `e7c8329 Plan GPS run tracking loop`
  - `d3af205 Add run metrics tracker`
  - `7817209 Add run tracking state controller`
  - `54de90b Add GPS run tracking loop`
  - `c279529 Plan diagnostic log export`
  - `15988cb Add diagnostic export payload builder`
  - `e5dc2d9 Add structured app event storage`
  - `11856d3 Add diagnostic log export UI`

  - `cb36518 Add AI music plan and tabbed home UI`
  - `661254e Plan AI music pipeline PoC`

## 已完成

### Android App

- 已创建 Kotlin + Jetpack Compose Android 项目骨架。
- 已配置 Gradle Wrapper、Android Gradle Plugin、Compose、Room、DataStore、Play Services Location。
- 已实现 10/20/30/60 秒步频测量逻辑，优先使用 `Sensor.TYPE_STEP_DETECTOR`。
- 已实现无计步传感器或无运动权限时的手动点拍 fallback。
- 已实现 SPM 到目标 BPM 的映射：
  - `SPM >= 120` 时按 `SPM / 2` 推荐。
  - `SPM < 120` 时按 `SPM` 推荐。
- 已实现 BPM 半速/倍速候选匹配，支持例如 160 SPM 匹配 80 BPM 歌曲。
- 已实现第一版推荐算法：
  - BPM 匹配度
  - 用户偏好标签
  - 歌曲能量
  - 热门度
  - BPM 置信度
- 已实现 Compose 首页：
  - 底部固定三栏：跑步 / 音乐 / 我的
  - 跑步页：跑步记录卡片、时长 / 距离 / 配速、跑步计划推荐占位
  - 音乐页：步频测量、推荐歌曲列表、占位 AI 音乐标签
  - 我的页：节奏档案、诊断日志导出、后台曲库同步
- 已实现可选测量时长：
  - 10 秒
  - 20 秒
  - 30 秒
  - 60 秒
  - 测量完成后音乐页测量卡片自动收起，保留“再次测量”入口。
- 已实现外部音乐平台跳转，使用 `Intent.ACTION_VIEW` 打开 QQ 音乐或网易云链接。
- 已实现 Room 本地数据表：
  - `SongEntity`
  - `SongInteractionEntity`
  - `RunSessionEntity`
- 已实现内置 `assets/catalog.json` 种子曲库。
- 已实现 Android 从后台 `/catalog/export` 同步曲库：
  - 模拟器默认地址：`http://10.0.2.2:8000`
  - 自动跳过没有 BPM 的歌曲
  - 同步后写入 Room
- 已实现 GPS 跑步记录首个切片：
  - `RunMetricsTracker` 负责纯 Kotlin 距离累计、时长和平均配速计算。
  - `RunTrackingController` / `RunTrackingStore` 暴露 `Idle`、`Running`、`Finished` 状态。
  - `RunTrackingService` 支持开始 / 结束跑步，定位回调累计 GPS 距离。
  - 结束跑步后写入 `RunSessionEntity`。
  - 首页新增跑步记录卡片，展示时长、距离、配速和最近一次跑步摘要。
  - 运动识别权限和跑步定位权限已拆分请求。
- 已实现一键诊断日志导出：
  - 新增 `app_events` Room 事件表和 `1 -> 2` 数据库迁移。
  - 记录歌曲交互、步频测量、曲库同步、跑步开始 / 结束、诊断导出等关键事件。
  - 首页新增「诊断日志」卡片和「导出日志」按钮。
  - 导出 zip 到 App cache，并通过 `FileProvider` 调起系统分享面板。
  - zip 包含 `diagnostics.json`、`events.jsonl`、`run_sessions.csv`、`song_interactions.csv`。
  - 导出时会脱敏 `key`、`token`、`cookie`、`password`、`secret` 等字段。

### Python 后台

- 已创建 FastAPI 后台项目骨架。
- 已配置 SQLAlchemy 数据模型和数据库会话。
- 默认开发数据库为 SQLite，可通过 `RUN_IN_MUSIC_DATABASE_URL` 切换 PostgreSQL。
- 已实现核心表模型：
  - `songs`
  - `platform_tracks`
  - `audio_analysis`
  - `crawl_jobs`
  - `song_tags`
- 已实现 QQ 音乐 provider：
  - 从链接提取 `songmid`
  - 解析公开歌曲详情
  - 保存基础元数据
  - 若接口返回 BPM，则保存 BPM
- 已实现网易云 provider：
  - 从链接提取歌曲 id
  - 解析基础元数据
  - 默认不期待 BPM
- 已实现 BPM 候选归一化：
  - 原始 BPM
  - 半速 BPM
  - 倍速 BPM
- 已实现 librosa 音频 BPM 分析函数和 `/analysis/jobs` 接口骨架。
- 已实现曲库导出接口 `/catalog/export`。
- 已实现推荐接口 `/songs/recommend`。
- 已实现健康检查 `/health`。
- 已完成 AI 音乐 MVP 路线规划：
  - 生成方案优先考虑本地服务端 ACE-Step。
  - 目标为批量生成 100 首 3-5 分钟 AI 跑步音乐。
  - 服务端统一生成、BPM 分析、标签计算、导入曲库和推荐。
  - Android 保留本地 Room 缓存和离线推荐 fallback。
  - 规划文档见 `docs/AI_MUSIC_MVP_PLAN.md`。

- 已实现 AI 音乐管线 PoC 骨架：
  - `generation_plan.py` 可生成 100 条确定性的 AI 跑步音乐生成任务。
  - `analyze_batch.py` 可批量分析生成音频元数据，并输出 BPM、置信度、候选 BPM、接受 / 拒绝状态。
  - `import_generated.py` 可把通过分析的 AI 曲目导入 `songs`、`platform_tracks`、`audio_analysis`。
  - 生成曲目的平台标记为 `generated`，第一版链接格式为 `generated://<track_id>`。
  - 仓库根目录已新增 `pytest.ini`，可以直接从项目根目录运行后端 pytest。

### 测试与验证

- Android 单元测试已覆盖：
  - 10 秒步数转 SPM
  - 高 SPM 映射到半速 BPM
  - 推荐算法 BPM 窗口过滤
  - 偏好标签排序
  - disliked 歌曲排除
  - 后台曲库 JSON 解析
  - 缺少 BPM 歌曲跳过
  - GPS 距离累计
  - 平均配速计算
  - 跑步状态流转
  - 诊断导出 payload 结构
  - 诊断 zip 文件写入
  - Room 记录到诊断模型映射
- 后台 pytest 已覆盖：
  - BPM 候选生成
  - BPM 归一化
  - QQ songmid 提取
  - 网易云 song id 提取
  - provider registry
  - 推荐过滤和排序
  - AI 音乐生成任务规划
  - AI 生成音频批量分析包装
  - AI 曲目导入数据库
- 已验证命令：
  - `.\gradlew.bat testDebugUnitTest`
  - `.\gradlew.bat assembleDebug`
  - `backend\.venv\Scripts\python.exe -m pytest`
- 已验证 live provider：
  - QQ 示例链接可解析歌曲和 BPM。
  - 网易云示例链接可解析歌曲基础信息，BPM 为空，符合预期。

## 当前未完成

### Android App

- GPS 距离累计已有首版实现，但还没有真机或模拟器路线回放验收。
- 跑步记录保存已有首版实现，但还没有覆盖异常中断、服务被系统杀死后的恢复。
- 诊断日志导出已有首版实现，但还没有在真机上验证系统分享面板和实际 zip 内容。
- 诊断日志第一版只导出摘要，不导出原始 GPS 坐标轨迹。
- 跑步页暂停按钮当前是 UI 占位，尚未实现真正暂停 / 恢复 GPS 服务状态。
- 音乐页 AI 推荐卡片当前是占位数据，尚未接入服务端 AI 生成曲库。
- 跑步历史页未实现。
- 权限流已拆分运动识别和定位请求，但还需要补更细的拒绝后引导文案。
- 后台曲库同步地址目前写死为模拟器地址，真机需要可配置局域网地址。
- 没有真机传感器/GPS 验收记录。

### Python 后台

- 批量链接导入未完成。
- 爬取任务还是同步原型，没有 Redis/RQ 或 Celery worker。
- BPM 分析缺少稳定音频来源、批处理、失败重试和人工审核。
- `/analysis/jobs` 当前需要调用方提供 `audio_url`。
- 后台没有管理界面或 CLI 导入工具。
- 没有导入统计、重复歌曲合并策略、失败原因报表。
- AI 音乐管线仍缺少真实生成器接入，`generate_batch.py` 未实现。
- 尚未生成真实 AI 音频样例，也未把样例导入 Android 可同步曲库。

### 产品能力

- 没有间歇跑训练模式。
- 没有自动连播。
- 没有跑中连续 30 秒滑动窗口步频推荐。
- 没有歌单链接导入。
- 没有截图 OCR 导入。
- 没有账号系统和云同步。
- 没有隐私说明、用户协议、发布配置。

## 下一步优先级

### P0：Alpha 0.2 跑步记录闭环

目标：让 App 从“音乐推荐 demo”变成“能真实试跑的跑步 App”。

状态：首个实现切片已完成，下一步要做真实设备 / 模拟器 GPS 验收和体验修补。

已完成：

- 完善 `RunTrackingService`，累计 GPS 距离。
- 新增跑步状态模型：`Idle`、`Running`、`Finished`。
- 实现开始跑步、结束跑步、保存跑步记录。
- 计算跑步时长、距离、平均配速。
- 写入 `RunSessionEntity`。
- 首页展示跑步中卡片和最近一次跑步摘要。
- 拆分定位权限请求，只在开始跑步时请求定位。
- 为距离累计和配速计算补单元测试。

仍需：

- 在模拟器中用路线回放验证 GPS 更新后距离累计。
- 在真机中验证定位权限、前台通知和后台持续记录。
- 增加跑步历史页。
- 为服务异常中断和权限拒绝补更明确的用户提示。

验收标准：

- 真机或模拟器可以点击开始跑步。
- 定位权限通过后，App 显示跑步时长。
- GPS 更新后距离会累计。
- 结束跑步后 Room 中保存一条记录。
- 首页显示最近一次跑步摘要。

### P1：后台批量歌曲导入

目标：让曲库增长不再依赖单条接口手动调用。

要做：

- 新增 `POST /crawler/batch`。
- 请求体支持多个链接：`{"urls": ["..."]}`。
- 每条链接独立解析，失败不影响其他链接。
- 返回总数、成功数、失败数、缺少 BPM 数、每条结果。
- 重复平台歌曲不重复建歌。
- README 增加批量导入示例。
- 为 batch crawler 补 pytest。

验收标准：

- 一次提交多个 QQ/网易云链接。
- 成功链接进入数据库。
- 失败链接返回明确错误。
- `/catalog/export` 能导出新增曲库。
- Android 点击刷新后能看到新增可推荐歌曲。

### P1：AI 音乐生成管线 PoC 闭环

目标：让“自建 AI 曲库”从规划进入可验证样例阶段，先跑通 5 首歌，再扩展到 100 首。

已完成：

- 生成 100 首 AI 跑步音乐的任务规划。
- 批量分析包装，支持 accepted/rejected 和候选 BPM。
- 数据库导入逻辑，复用现有曲库表。
- 后端测试覆盖生成计划、批量分析和导入。

仍需：

- 接入真实本地生成器，首选 ACE-Step CLI，保留替换为其他生成器的适配层。
- 新增 `generate_batch.py`，支持 dry-run、断点续跑、输出音频和 metadata。
- 生成 5 首 3-5 分钟样例音频。
- 对样例音频运行 BPM 分析并导入数据库。
- 从 `/catalog/export` 导出 AI 曲目，并在 Android 音乐页同步查看。

验收标准：

- `backend/generated_music/` 下有 5 首样例音频和对应 metadata。
- 后端能分析出 BPM、候选 BPM 和置信度。
- 数据库中出现 `provider=generated` 的曲目。
- `/catalog/export` 能导出这些 AI 曲目。
- Android 点击刷新后能看到 AI 曲库推荐卡片。

### P2：Android 曲库同步体验

目标：让后台同步对真机和非开发者更友好。

要做：

- 增加后台地址配置入口。
- 默认保留模拟器地址 `10.0.2.2:8000`。
- 支持用户输入局域网地址，例如 `http://192.168.x.x:8000`。
- 显示本地可推荐歌曲数量。
- 显示上次同步时间。
- 同步失败时显示明确解决建议。

验收标准：

- 模拟器能用默认地址同步。
- 真机能改成电脑局域网 IP 同步。
- 首页能看到本地曲库数量和同步状态。

### P3：真实设备验收与体验修复

目标：验证传感器、定位、外部 App 跳转在真实设备上的表现。

要做：

- 在至少一台 Android 真机上测试运动识别权限。
- 测 10 秒步频，记录误差和手动点拍 fallback 表现。
- 测外部 QQ/网易云链接跳转。
- 测后台同步在同一局域网下是否可用。
- 记录设备型号、Android 版本、问题和修复方案。

验收标准：

- 有一份真机测试记录。
- 关键阻塞问题进入 TODO 或被修复。

## 暂不做

- 内置音乐播放器。
- 自动控制第三方音乐 App 切歌。
- 账号系统。
- 付费、会员、版权结算。
- 完整训练计划系统。
- OCR 歌单导入。
- 上架应用商店。

## 后续更新规则

每次继续开发前：

1. 先看本文件的“下一步优先级”。
2. 选择一个 P0/P1/P2 任务，不要同时铺太多线。
3. 开发前把目标写入“当前工作项”。
4. 开发完成后更新“已完成”和“当前未完成”。
5. 如果产生技术决策，追加到“Decision Log”。
6. 测试通过后提交 Git，并在这里记录提交 hash。

## 当前工作项

- 进行中：Alpha 0.2 GPS 跑步记录和一键诊断日志已实现并通过本地构建，下一步是真机 / 模拟器 GPS 验收和日志 zip 内容验收。
- 进行中：AI 音乐生成管线 PoC 已完成后端 plan / analyze / import 骨架，下一步接入真实生成器并生成 5 首样例音频。

## Decision Log

- 2026-05-17：项目采用 Android Kotlin + Compose + Room。
- 2026-05-17：后台采用 FastAPI + SQLAlchemy，默认 SQLite。
- 2026-05-17：音乐播放只做外部链接跳转，不做内置播放器。
- 2026-05-17：第一批 provider 为 QQ 音乐和网易云音乐。
- 2026-05-17：Android 增加后台曲库同步，默认使用 `10.0.2.2:8000`。
- 2026-05-18：下一阶段优先做 GPS 跑步记录闭环，而不是继续扩展高级音乐功能。
- 2026-05-18：跑步距离和配速计算放在纯 Kotlin `core/run`，前台服务只负责定位输入和保存记录，避免 UI、GPS、Room 强耦合。
- 2026-05-18：`RunTrackingService` 前台服务类型收窄为 `location`，不再声明 `health`，减少 Android 14+ 额外权限风险。
- 2026-05-18：诊断日志默认只导出摘要和事件流水，不导出原始 GPS 坐标；如需轨迹文件，后续单独加显式开关。
- 2026-05-18：诊断包通过 `FileProvider` 暴露 cache 下的 zip，不直接暴露 Room 数据库文件。
- 2026-05-18：App 主界面改为底部三栏：跑步负责记录和训练计划，音乐负责步频测量和歌曲推荐，我的负责个人摘要、曲库同步和日志导出。
- 2026-05-18：AI 音乐 MVP 规划为服务端批量生成和分析，后端作为权威推荐源，Android 保留本地推荐作为离线 fallback。
- 2026-05-18：AI 生成曲目第一版复用现有曲库表，不新增 `generated_tracks` 表；平台统一标记为 `generated`，音频链接先使用 `generated://<track_id>` 占位，等真实音频托管方式确定后再替换。
- 2026-05-18：后端测试入口固定到仓库根目录 `pytest.ini`，避免后续从项目根运行 pytest 时找不到 `backend/app` 包。
