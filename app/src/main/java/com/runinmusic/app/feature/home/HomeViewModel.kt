package com.runinmusic.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.runinmusic.app.core.cadence.CadenceMapper
import com.runinmusic.app.core.model.RecommendedSong
import com.runinmusic.app.core.model.SongCandidate
import com.runinmusic.app.core.model.SongInteractionType
import com.runinmusic.app.core.recommendation.RecommendationEngine
import com.runinmusic.app.data.local.RunSessionEntity
import com.runinmusic.app.data.repository.MusicRepository
import com.runinmusic.app.location.RunTrackingState
import com.runinmusic.app.location.RunTrackingStatus
import com.runinmusic.app.location.RunTrackingStore
import com.runinmusic.app.sensor.CadenceMeasurer
import com.runinmusic.app.sensor.CadenceResult
import com.runinmusic.app.sensor.CadenceSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: MusicRepository,
    private val cadenceMeasurer: CadenceMeasurer,
    private val recommendationEngine: RecommendationEngine = RecommendationEngine(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private var timerJob: Job? = null
    private var runTimerJob: Job? = null
    private var latestSongs: List<SongCandidate> = emptyList()

    init {
        viewModelScope.launch {
            repository.seedCatalogIfEmpty()
            repository.songs.collect { songs ->
                latestSongs = songs
                recomputeRecommendations()
            }
        }
        viewModelScope.launch {
            RunTrackingStore.state.collect { runState ->
                _uiState.update {
                    it.copy(
                        runTrackingState = runState,
                        runStatusMessage = if (runState.status == RunTrackingStatus.Finished) {
                            "本次跑步已保存到本机记录。"
                        } else {
                            it.runStatusMessage
                        },
                    )
                }
                if (runState.isRunning && runTimerJob == null) {
                    startRunTicker()
                } else if (!runState.isRunning) {
                    runTimerJob?.cancel()
                    runTimerJob = null
                }
            }
        }
        viewModelScope.launch {
            repository.latestRunSession.collect { session ->
                _uiState.update { it.copy(latestRunSession = session) }
            }
        }
    }

    fun startSensorMeasurement() {
        if (!cadenceMeasurer.isSupported) {
            startManualMeasurement("这台设备没有低延迟计步传感器，改用手动点拍。")
            return
        }

        resetMeasurementState(manualMode = false, message = "开始 10 秒步频测量，保持自然跑姿。")
        startCountdown()
        val started = cadenceMeasurer.startTenSecondMeasurement { result ->
            finishMeasurement(result)
        }
        if (!started) {
            startManualMeasurement("计步传感器暂时不可用，改用手动点拍。")
        }
    }

    fun startManualMeasurement(message: String = "手动模式：每落一步点一次按钮，持续 10 秒。") {
        resetMeasurementState(manualMode = true, message = message)
        startCountdown {
            val steps = _uiState.value.manualTapCount
            finishMeasurement(
                CadenceResult(
                    steps = steps,
                    seconds = CadenceMapper.DEFAULT_MEASUREMENT_SECONDS,
                    spm = CadenceMapper.stepsToSpm(steps),
                    source = CadenceSource.ManualTap,
                ),
            )
        }
    }

    fun recordManualTap() {
        _uiState.update { state ->
            if (state.isMeasuring && state.manualMode) {
                state.copy(manualTapCount = state.manualTapCount + 1)
            } else {
                state
            }
        }
    }

    fun markLiked(songId: String) = recordInteraction(songId, SongInteractionType.Liked)

    fun markDisliked(songId: String) = recordInteraction(songId, SongInteractionType.Disliked)

    fun recordOpened(songId: String) = recordInteraction(songId, SongInteractionType.Opened)

    fun showRunTrackingMessage(message: String) {
        _uiState.update { it.copy(runStatusMessage = message) }
    }

    fun refreshBackendCatalog() {
        if (_uiState.value.isRefreshingCatalog) return
        _uiState.update {
            it.copy(
                isRefreshingCatalog = true,
                catalogStatusMessage = "正在从后台同步曲库...",
            )
        }
        viewModelScope.launch {
            runCatching { repository.refreshCatalogFromBackend() }
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isRefreshingCatalog = false,
                            catalogStatusMessage = "已导入 ${result.importedCount} 首可推荐歌曲，跳过 ${result.skippedWithoutBpm} 首缺少 BPM 的歌曲。",
                        )
                    }
                    recomputeRecommendations()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isRefreshingCatalog = false,
                            catalogStatusMessage = "同步失败：${error.message ?: "请确认后台服务已启动"}",
                        )
                    }
                }
        }
    }

    override fun onCleared() {
        cadenceMeasurer.stop()
        timerJob?.cancel()
        runTimerJob?.cancel()
        super.onCleared()
    }

    private fun resetMeasurementState(manualMode: Boolean, message: String) {
        timerJob?.cancel()
        cadenceMeasurer.stop()
        _uiState.update {
            it.copy(
                isMeasuring = true,
                manualMode = manualMode,
                manualTapCount = 0,
                secondsRemaining = CadenceMapper.DEFAULT_MEASUREMENT_SECONDS,
                statusMessage = message,
            )
        }
    }

    private fun startCountdown(onFinished: (() -> Unit)? = null) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (remaining in CadenceMapper.DEFAULT_MEASUREMENT_SECONDS downTo 1) {
                _uiState.update { it.copy(secondsRemaining = remaining) }
                delay(1_000L)
            }
            _uiState.update { it.copy(secondsRemaining = 0) }
            onFinished?.invoke()
        }
    }

    private fun startRunTicker() {
        runTimerJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                RunTrackingStore.tick()
            }
        }
    }

    private fun finishMeasurement(result: CadenceResult) {
        timerJob?.cancel()
        val targetBpm = CadenceMapper.targetBpmForSpm(result.spm)
        _uiState.update {
            it.copy(
                isMeasuring = false,
                manualMode = false,
                lastSteps = result.steps,
                measuredSpm = result.spm,
                targetBpm = targetBpm,
                statusMessage = if (result.source == CadenceSource.StepDetector) {
                    "测量完成：已按当前步频匹配歌曲。"
                } else {
                    "手动点拍完成：已按你的节奏匹配歌曲。"
                },
            )
        }
        recomputeRecommendations()
    }

    private fun recordInteraction(songId: String, type: SongInteractionType) {
        viewModelScope.launch {
            repository.recordInteraction(songId, type)
            recomputeRecommendations()
        }
    }

    private fun recomputeRecommendations() {
        val targetBpm = _uiState.value.targetBpm ?: return
        viewModelScope.launch {
            val preferredTags = repository.preferredTags()
            val disliked = repository.dislikedSongIds()
            val recommendations = recommendationEngine.recommend(
                targetBpm = targetBpm,
                songs = latestSongs,
                preferredTags = preferredTags,
                dislikedSongIds = disliked,
            )
            _uiState.update { it.copy(recommendations = recommendations) }
        }
    }

    companion object {
        fun factory(
            repository: MusicRepository,
            cadenceMeasurer: CadenceMeasurer,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(repository, cadenceMeasurer) as T
            }
        }
    }
}

data class HomeUiState(
    val isMeasuring: Boolean = false,
    val manualMode: Boolean = false,
    val isRefreshingCatalog: Boolean = false,
    val secondsRemaining: Int = CadenceMapper.DEFAULT_MEASUREMENT_SECONDS,
    val manualTapCount: Int = 0,
    val lastSteps: Int? = null,
    val measuredSpm: Double? = null,
    val targetBpm: Double? = null,
    val recommendations: List<RecommendedSong> = emptyList(),
    val runTrackingState: RunTrackingState = RunTrackingState(),
    val latestRunSession: RunSessionEntity? = null,
    val runStatusMessage: String = "开始跑步后会记录 GPS 距离、时长和平均配速。",
    val catalogStatusMessage: String = "后台曲库同步可把爬取结果导入本机推荐。",
    val statusMessage: String = "点击测量步频，让音乐贴住你的脚步。",
)
