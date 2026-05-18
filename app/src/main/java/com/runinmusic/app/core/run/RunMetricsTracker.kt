package com.runinmusic.app.core.run

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class RunMetricsTracker(
    private val startedAtMillis: Long,
) {
    private var lastSample: RunLocationSample? = null
    private var latestTimeMillis: Long = startedAtMillis
    private var distanceMeters: Double = 0.0

    fun addSample(sample: RunLocationSample): RunMetricsSnapshot {
        lastSample?.let { previous ->
            distanceMeters += distanceMetersBetween(previous, sample)
        }
        lastSample = sample
        latestTimeMillis = maxOf(startedAtMillis, sample.timeMillis)
        return snapshotAt(latestTimeMillis)
    }

    fun snapshotAt(timeMillis: Long): RunMetricsSnapshot {
        val updatedAtMillis = maxOf(startedAtMillis, timeMillis)
        val elapsedMillis = updatedAtMillis - startedAtMillis
        val pace = if (distanceMeters > 0.0) {
            (elapsedMillis / 1_000.0) / (distanceMeters / 1_000.0)
        } else {
            null
        }

        return RunMetricsSnapshot(
            startedAtMillis = startedAtMillis,
            updatedAtMillis = updatedAtMillis,
            elapsedMillis = elapsedMillis,
            distanceMeters = distanceMeters,
            averagePaceSecondsPerKm = pace,
        )
    }

    fun snapshot(): RunMetricsSnapshot = snapshotAt(latestTimeMillis)

    companion object {
        private const val EARTH_RADIUS_METERS = 6_371_000.0

        fun distanceMetersBetween(
            start: RunLocationSample,
            end: RunLocationSample,
        ): Double {
            val startLat = Math.toRadians(start.latitude)
            val endLat = Math.toRadians(end.latitude)
            val deltaLat = Math.toRadians(end.latitude - start.latitude)
            val deltaLon = Math.toRadians(end.longitude - start.longitude)

            val haversine = sin(deltaLat / 2).pow(2.0) +
                cos(startLat) * cos(endLat) * sin(deltaLon / 2).pow(2.0)
            val angularDistance = 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
            return EARTH_RADIUS_METERS * angularDistance
        }
    }
}
