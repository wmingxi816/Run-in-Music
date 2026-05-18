package com.runinmusic.app.location

import com.runinmusic.app.core.run.RunLocationSample
import kotlinx.coroutines.flow.StateFlow

object RunTrackingStore {
    private val controller = RunTrackingController()

    val state: StateFlow<RunTrackingState> = controller.state

    fun start(startedAtMillis: Long = System.currentTimeMillis()) {
        controller.start(startedAtMillis)
    }

    fun addLocation(sample: RunLocationSample) {
        controller.addLocation(sample)
    }

    fun tick(nowMillis: Long = System.currentTimeMillis()) {
        controller.tick(nowMillis)
    }

    fun finish(endedAtMillis: Long = System.currentTimeMillis()): RunTrackingState {
        return controller.finish(endedAtMillis)
    }

    fun reset() {
        controller.reset()
    }
}
