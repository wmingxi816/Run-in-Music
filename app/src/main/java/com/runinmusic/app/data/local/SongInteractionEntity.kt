package com.runinmusic.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_interactions")
data class SongInteractionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val action: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
