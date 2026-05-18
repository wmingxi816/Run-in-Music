package com.runinmusic.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_events")
data class AppEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val level: String,
    val module: String,
    val type: String,
    val message: String,
    val detailsJson: String?,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
