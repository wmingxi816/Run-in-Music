# Run in Music 项目规划文档 v0.1

## Goal

开发一个 Android 跑步音乐推荐 App：用户点击测量后，App 用手机传感器测 10 秒步频 SPM，将步频映射为歌曲 BPM，从曲库中推荐适合当前跑步节奏的歌曲，并通过平台链接跳转到 QQ 音乐、网易云等外部音乐 App 播放。

MVP 同时建设一个 Python 后台数据管线：优先抓取/解析歌曲链接与公开元数据，提取歌名、歌手、平台 ID、BPM、风格、热度等字段；能拿到音频或试听片段时再用算法分析 BPM。

## Context

- 当前仓库从空 Android Studio 工作区开始，已创建 Android App 与 FastAPI 后台两个子系统。
- Android 端使用 Kotlin + Jetpack Compose，包名 `com.runinmusic.app`。
- MVP 先做 App 闭环：测步频、算目标 BPM、推荐曲目、跳转外部音乐平台、记录用户偏好。
- 步频体验第一版只做 10 秒测量，不做跑中连续更新。
- 用户偏好第一版存 Room，不做账号系统和云同步。
- 歌曲播放第一版只做外部链接跳转，不做内置播放和自动切歌。

## Input

- Android 输入：GPS 定位、`TYPE_STEP_DETECTOR` 计步事件、手动点拍 fallback、用户点击/喜欢/不喜欢行为。
- 后台输入：QQ 音乐链接、网易云链接、后续歌单链接、音频或试听片段 URL。
- 曲库输入：平台歌曲 ID、歌名、歌手、时长、BPM、BPM 来源、置信度、风格标签、能量、热度、平台跳转链接。
- 推荐输入：10 秒步数、SPM、目标 BPM、用户本地偏好、候选歌曲标签。

## Constraints

- Android `minSdk = 26`，`targetSdk = 36`，使用 Compose + Material 3。
- 计步优先使用 `Sensor.TYPE_STEP_DETECTOR`；不可用时改用 10 秒手动点拍。
- GPS 使用 `FusedLocationProviderClient`，前台服务已预留。
- Android 权限包括 `ACTIVITY_RECOGNITION`、定位、前台服务、通知。
- 外部音乐播放使用 `Intent.ACTION_VIEW`，不承诺控制第三方音乐 App。
- 后台第一版只处理公开可访问链接/页面/接口/音频片段，不绕过登录、DRM 或加密。
- 每个 BPM 记录 `source` 和 `confidence`，推荐时支持半速/倍速归一化。

## Output

- Android App 原型：步频测量、SPM/BPM 显示、推荐列表、链接跳转、本地偏好。
- FastAPI 后台原型：链接解析、歌曲入库、BPM 分析接口、曲库导出、推荐接口。
- 测试：Android cadence/recommendation 单元测试，后台 provider/BPM/recommendation 单元测试。
- 后续开发前持续更新本文件的 Decision Log 和 Future Additions。

## Steps

1. 创建 Android Gradle/Compose 项目骨架。
2. 实现 Room 表：`SongEntity`、`SongInteractionEntity`、`RunSessionEntity`。
3. 实现 Android 10 秒步频测量和手动点拍 fallback。
4. 实现 SPM 到 BPM 映射：`SPM >= 120` 时使用 `SPM / 2`，否则直接使用 `SPM`。
5. 实现推荐算法：BPM 匹配、偏好标签、能量、热度、BPM 置信度综合评分。
6. 实现 Compose 首页、推荐列表、喜欢/不喜欢反馈、外部链接跳转。
7. 实现前台定位服务骨架，后续补充距离累计和 RunSession 写入。
8. 创建 FastAPI 后台、SQLAlchemy 模型和数据库会话。
9. 实现 QQ 音乐 provider：提取 `songmid` 并解析公开详情。
10. 实现网易云 provider：提取 `song id` 并解析基础元数据。
11. 实现 BPM 分析服务：使用 librosa 分析音频文件，并保存候选 BPM。
12. 实现后台曲库导出和推荐接口。
13. 为算法、provider、推荐排序补测试。
14. 后续逐步扩展歌单导入、截图 OCR、连续步频推荐、间歇跑模式、自动连播、账号同步。

## Notes

- 默认后端开发数据库是 SQLite，生产可通过 `RUN_IN_MUSIC_DATABASE_URL` 切换 PostgreSQL。
- Android 端当前内置 `assets/catalog.json` 作为 seed catalog，后续由后台 `/catalog/export` 替换或刷新。
- QQ provider 当前能解析 songmid 和基础公开详情；网易云 provider 默认不期待 BPM。
- BPM 分析可能出现半速/倍速误判，因此 Android 和后台都保留候选 BPM 逻辑。
- 若设备没有 step detector 或用户拒绝运动识别权限，使用手动点拍测量。

## Decision Log

- 2026-05-17: MVP 采用 Kotlin + Compose、Room、本地推荐、外部平台链接跳转。
- 2026-05-17: 后台采用 Python FastAPI + SQLAlchemy，默认 SQLite，保留 PostgreSQL 环境变量。
- 2026-05-17: 第一批 provider 为 QQ 音乐和网易云音乐。
- 2026-05-17: 第一版只做 10 秒步频测量，不做自动切歌。

## Future Additions

- 后台导出曲库后 Android 一键刷新本地 Room。
- 跑步中连续 30 秒滑动窗口步频推荐。
- 间歇跑训练模式：阶段 BPM、语音提示、手动切歌入口。
- 歌单链接导入和截图 OCR 歌曲识别。
- 后台任务队列从同步原型升级到 Redis/RQ 或 Celery worker。
- GPS 距离累计、配速计算、跑步历史详情页。
- 账号系统、云端偏好同步、跨设备推荐。
