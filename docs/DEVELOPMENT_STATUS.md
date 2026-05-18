# Run in Music 开发状态与下一步路线

> 这个文档用于后续开发交接和持续更新。每次完成一个功能、调整方向、发现风险或决定下一步时，都先更新这里，再继续实现。

## 当前阶段

- 当前版本：Alpha 0.1 MVP 原型
- 当前日期：2026-05-18
- 仓库地址：https://github.com/wmingxi816/Run-in-Music
- 当前主线分支：`main`
- 最近已推送提交：
  - `64bc333 Initial Run in Music MVP`
  - `15b91fd Add backend catalog sync`

## 已完成

### Android App

- 已创建 Kotlin + Jetpack Compose Android 项目骨架。
- 已配置 Gradle Wrapper、Android Gradle Plugin、Compose、Room、DataStore、Play Services Location。
- 已实现 10 秒步频测量逻辑，优先使用 `Sensor.TYPE_STEP_DETECTOR`。
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
  - 步频测量卡片
  - SPM / 目标 BPM / 步数显示
  - 推荐歌曲列表
  - 喜欢 / 不适合 / 去听歌操作
  - 后台曲库同步卡片
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
- 已预留 `RunTrackingService` 前台定位服务骨架。

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

### 测试与验证

- Android 单元测试已覆盖：
  - 10 秒步数转 SPM
  - 高 SPM 映射到半速 BPM
  - 推荐算法 BPM 窗口过滤
  - 偏好标签排序
  - disliked 歌曲排除
  - 后台曲库 JSON 解析
  - 缺少 BPM 歌曲跳过
- 后台 pytest 已覆盖：
  - BPM 候选生成
  - BPM 归一化
  - QQ songmid 提取
  - 网易云 song id 提取
  - provider registry
  - 推荐过滤和排序
- 已验证命令：
  - `.\gradlew.bat testDebugUnitTest`
  - `.\gradlew.bat assembleDebug`
  - `backend\.venv\Scripts\python.exe -m pytest`
- 已验证 live provider：
  - QQ 示例链接可解析歌曲和 BPM。
  - 网易云示例链接可解析歌曲基础信息，BPM 为空，符合预期。

## 当前未完成

### Android App

- GPS 距离累计未完成，目前只有前台服务和定位请求骨架。
- 跑步状态未完成，还没有 `开始跑步 / 结束跑步 / 保存记录` 的完整流程。
- `RunSessionEntity` 还没有真实写入跑步记录。
- 首页没有跑步中状态、实时距离、时长、配速展示。
- 跑步历史页未实现。
- 权限流还比较粗糙，目前运动识别、定位、通知权限的请求时机需要拆分。
- 后台曲库同步地址目前写死为模拟器地址，真机需要可配置局域网地址。
- 没有真机传感器/GPS 验收记录。

### Python 后台

- 批量链接导入未完成。
- 爬取任务还是同步原型，没有 Redis/RQ 或 Celery worker。
- BPM 分析缺少稳定音频来源、批处理、失败重试和人工审核。
- `/analysis/jobs` 当前需要调用方提供 `audio_url`。
- 后台没有管理界面或 CLI 导入工具。
- 没有导入统计、重复歌曲合并策略、失败原因报表。

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

要做：

- 完善 `RunTrackingService`，累计 GPS 距离。
- 新增跑步状态模型：`Idle`、`Running`、`Finished`。
- 实现开始跑步、结束跑步、保存跑步记录。
- 计算跑步时长、距离、平均配速。
- 写入 `RunSessionEntity`。
- 首页展示跑步中卡片和最近一次跑步摘要。
- 拆分定位权限请求，只在开始跑步时请求定位。
- 为距离累计和配速计算补单元测试。

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

- 待开始：Alpha 0.2 跑步记录闭环。

## Decision Log

- 2026-05-17：项目采用 Android Kotlin + Compose + Room。
- 2026-05-17：后台采用 FastAPI + SQLAlchemy，默认 SQLite。
- 2026-05-17：音乐播放只做外部链接跳转，不做内置播放器。
- 2026-05-17：第一批 provider 为 QQ 音乐和网易云音乐。
- 2026-05-17：Android 增加后台曲库同步，默认使用 `10.0.2.2:8000`。
- 2026-05-18：下一阶段优先做 GPS 跑步记录闭环，而不是继续扩展高级音乐功能。
