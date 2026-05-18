package com.runinmusic.app.core.run

data class RunLocationSample(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val timeMillis: Long,
)
