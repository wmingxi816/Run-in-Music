package com.runinmusic.app.core.cadence

import kotlin.math.abs

object CadenceMapper {
    const val DEFAULT_MEASUREMENT_SECONDS = 10
    const val DEFAULT_TOLERANCE_BPM = 10.0

    fun stepsToSpm(steps: Int, seconds: Int = DEFAULT_MEASUREMENT_SECONDS): Double {
        require(seconds > 0) { "seconds must be positive" }
        require(steps >= 0) { "steps must not be negative" }
        return steps * 60.0 / seconds
    }

    fun targetBpmForSpm(spm: Double): Double {
        require(spm >= 0) { "spm must not be negative" }
        return if (spm >= 120.0) spm / 2.0 else spm
    }

    fun candidateBpms(originalBpm: Double): List<Double> {
        if (originalBpm <= 0.0) return emptyList()
        return listOf(originalBpm, originalBpm / 2.0, originalBpm * 2.0)
            .filter { it in 40.0..220.0 }
            .distinctBy { it.toInt() }
    }

    fun closestCandidateBpm(originalBpm: Double, targetBpm: Double): Double? {
        return candidateBpms(originalBpm).minByOrNull { abs(it - targetBpm) }
    }

    fun isWithinTolerance(originalBpm: Double, targetBpm: Double, tolerance: Double = DEFAULT_TOLERANCE_BPM): Boolean {
        val closest = closestCandidateBpm(originalBpm, targetBpm) ?: return false
        return abs(closest - targetBpm) <= tolerance
    }
}
