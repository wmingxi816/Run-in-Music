package com.runinmusic.app.core.run

data class RunMetricsSnapshot(
    val startedAtMillis: Long,
    val updatedAtMillis: Long,
    val elapsedMillis: Long,
    val distanceMeters: Double,
    val averagePaceSecondsPerKm: Double?,
)
