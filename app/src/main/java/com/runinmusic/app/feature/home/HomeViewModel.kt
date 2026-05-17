package com.runinmusic.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.runinmusic.app.core.cadence.CadenceMapper
import com.runinmusic.app.core.model.RecommendedSong
import com.runinmusic.app.core.model.SongCandidate
import com.runinmusic.app.core.model.SongInteractionType
import com.runinmusic.app.core.recommendation.RecommendationEngine
import com.runinmusic.app.data.repository.MusicRepository
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
    private var latestSongs: List<SongCandidate> = emptyList()

    init {
        viewModelScope.launch {
            repository.seedCatalogIfEmpty()
            repository.songs.collect { songs ->
                latestSongs = songs
                recomputeRecommendations()
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

    override fun onCleared() {
        cadenceMeasurer.stop()
        timerJob?.cancel()
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
    val secondsRemaining: Int = CadenceMapper.DEFAULT_MEASUREMENT_SECONDS,
    val manualTapCount: Int = 0,
    val lastSteps: Int? = null,
    val measuredSpm: Double? = null,
    val targetBpm: Double? = null,
    val recommendations: List<RecommendedSong> = emptyList(),
    val statusMessage: String = "点击测量步频，让音乐贴住你的脚步。",
)
