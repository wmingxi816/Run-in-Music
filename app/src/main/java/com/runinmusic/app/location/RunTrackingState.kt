package com.runinmusic.app.location

enum class RunTrackingStatus {
    Idle,
    Running,
    Paused,
    Finished,
}

data class RunTrackingState(
    val status: RunTrackingStatus = RunTrackingStatus.Idle,
    val startedAtMillis: Long? = null,
    val endedAtMillis: Long? = null,
    val elapsedMillis: Long = 0L,
    val distanceMeters: Double = 0.0,
    val averagePaceSecondsPerKm: Double? = null,
) {
    val isRunning: Boolean
        get() = status == RunTrackingStatus.Running

    val isPaused: Boolean
        get() = status == RunTrackingStatus.Paused
}
