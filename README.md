# Run in Music

Run in Music 是一个 Android 跑步音乐推荐 MVP。它可以测量跑者步频，把 SPM 映射为目标 BPM，从本地曲库中推荐节奏匹配的歌曲，并通过外部音乐 App 打开选中的曲目。

仓库包含两部分：

- `app/`：Kotlin + Jetpack Compose Android App。
- `backend/`：FastAPI 后台原型，负责歌曲元数据、平台链接解析、BPM 分析和曲库导出。

## Android MVP

- 使用 `TYPE_STEP_DETECTOR` 测量步频，支持 10 / 20 / 30 / 60 秒测量。
- 设备没有计步传感器或缺少运动权限时，提供手动点拍 fallback。
- 使用 Room 保存本地曲库、歌曲交互和跑步记录。
- Android 模拟器默认从 `http://10.0.2.2:8000/catalog/export` 刷新后台曲库。
- BPM 匹配支持半速 / 倍速归一化，例如 160 SPM 可以匹配 80 BPM 歌曲。
- 使用 `Intent.ACTION_VIEW` 打开外部音乐平台链接。
- 已有前台 GPS 跑步记录服务骨架，用于记录跑步时长、距离和平均配速。
- 跑步记录支持暂停 / 继续，并显示最近跑步记录。
- 首页采用底部三栏：跑步 / 音乐 / 我的。
- AI/generated 曲目支持 App 内置播放器串流播放，音乐文件保存在服务器端，不保存到用户手机。
- 支持一键导出诊断日志 zip，便于真机测试和问题排查。

请用 Android Studio 从仓库根目录打开项目。

如果要在模拟器里同步曲库，请先在本机启动后台，然后在 App 的 `后台曲库` 卡片点击 `刷新`。没有 BPM 的歌曲会被 Android 端跳过，因为它们暂时无法按步频推荐。

## 后台 MVP

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload
```

常用接口：

- `POST /providers/resolve-link`
- `POST /crawler/jobs`
- `POST /crawler/batch`
- `POST /analysis/jobs`
- `GET /catalog/export`
- `GET /audio/generated/{track_id}`
- `GET /songs/recommend?target_bpm=80`

批量导入示例（一次提交多条 QQ/网易云链接）：

```powershell
Invoke-RestMethod -Method Post http://127.0.0.1:8000/crawler/batch `
  -ContentType "application/json" `
  -Body '{"urls":["https://y.qq.com/n/ryqq/songDetail/000QhABT1zNwjC","https://music.163.com/song?id=1901371647"]}'
```

返回字段包含 `total` / `succeeded` / `failed` / `missing_bpm` / `duplicates` 和每条链接的 `results`。单条解析失败不会影响其他链接，重复平台歌曲不会重复建歌。

从仓库根目录运行后端测试：

```powershell
backend\.venv\Scripts\python.exe -m pytest
```

AI 音乐 PoC 模块位于 `backend/app/ai_music/`。当前切片可以规划 100 首 AI 跑步音乐生成任务、生成可播放 WAV 占位音频、包装 BPM 分析结果，并把通过分析的 AI 曲目导入现有曲库表。音乐文件放在 `backend/generated_music/`，通过 `/audio/generated/{track_id}` 提供给 App 串流播放。下一步是把占位生成器替换为真实本地音乐生成器。

MusicGen-small 过夜批量生成流程见 `docs/MUSICGEN_OVERNIGHT_RUNBOOK.md`。脚本已支持独立依赖环境、dry-run、断点续跑、生成日志和生成后分析导入。

本地调用示例：

```powershell
Invoke-RestMethod -Method Post http://127.0.0.1:8000/providers/resolve-link `
  -ContentType "application/json" `
  -Body '{"url":"https://y.qq.com/n/ryqq/songDetail/000QhABT1zNwjC"}'
```

## 文档

项目总规划在 `docs/RUN_IN_MUSIC_PROJECT_PLAN.md`。新增重要决策或未来功能前，先把内容追加到文档里。

持续开发交接文档是 `docs/DEVELOPMENT_STATUS.md`，用于记录已经完成什么、还缺什么、下一步优先做什么。
