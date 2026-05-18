package com.runinmusic.app.location

import com.runinmusic.app.core.run.RunLocationSample
import com.runinmusic.app.core.run.RunMetricsSnapshot
import com.runinmusic.app.core.run.RunMetricsTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RunTrackingController {
    private val _state = MutableStateFlow(RunTrackingState())
    val state: StateFlow<RunTrackingState> = _state.asStateFlow()

    private var tracker: RunMetricsTracker? = null

    fun start(startedAtMillis: Long) {
        tracker = RunMetricsTracker(startedAtMillis)
        _state.value = RunTrackingState(
            status = RunTrackingStatus.Running,
            startedAtMillis = startedAtMillis,
        )
    }

    fun addLocation(sample: RunLocationSample) {
        if (_state.value.status != RunTrackingStatus.Running) return
        val snapshot = tracker?.addSample(sample) ?: return
        _state.value = snapshot.toState(RunTrackingStatus.Running, endedAtMillis = null)
    }

    fun tick(nowMillis: Long) {
        val snapshot = tracker?.snapshotAt(nowMillis) ?: return
        _state.value = snapshot.toState(_state.value.status, endedAtMillis = null)
    }

    fun pause(pausedAtMillis: Long) {
        if (_state.value.status != RunTrackingStatus.Running) return
        val snapshot = tracker?.pause(pausedAtMillis) ?: return
        _state.value = snapshot.toState(RunTrackingStatus.Paused, endedAtMillis = null)
    }

    fun resume(resumedAtMillis: Long) {
        if (_state.value.status != RunTrackingStatus.Paused) return
        val snapshot = tracker?.resume(resumedAtMillis) ?: return
        _state.value = snapshot.toState(RunTrackingStatus.Running, endedAtMillis = null)
    }

    fun finish(endedAtMillis: Long): RunTrackingState {
        val snapshot = tracker?.snapshotAt(endedAtMillis) ?: return _state.value
        tracker = null
        return snapshot
            .toState(RunTrackingStatus.Finished, endedAtMillis = endedAtMillis)
            .also { _state.value = it }
    }

    fun reset() {
        tracker = null
        _state.value = RunTrackingState()
    }

    private fun RunMetricsSnapshot.toState(
        status: RunTrackingStatus,
        endedAtMillis: Long?,
    ): RunTrackingState = RunTrackingState(
        status = status,
        startedAtMillis = startedAtMillis,
        endedAtMillis = endedAtMillis,
        elapsedMillis = elapsedMillis,
        distanceMeters = distanceMeters,
        averagePaceSecondsPerKm = averagePaceSecondsPerKm,
    )
}
