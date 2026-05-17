package com.runinmusic.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "run_sessions")
data class RunSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val distanceMeters: Double,
    val averagePaceSecondsPerKm: Double?,
    val measuredSpm: Double?,
    val targetBpm: Double?,
)
