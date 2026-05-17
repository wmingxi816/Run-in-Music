package com.runinmusic.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.runinmusic.app.core.model.SongCandidate

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val bpm: Double,
    val bpmSource: String,
    val bpmConfidence: Double,
    val energy: Int,
    val popularity: Int,
    val tagsCsv: String,
    val qqUrl: String?,
    val neteaseUrl: String?,
) {
    fun toCandidate(): SongCandidate = SongCandidate(
        id = id,
        title = title,
        artist = artist,
        bpm = bpm,
        bpmSource = bpmSource,
        bpmConfidence = bpmConfidence,
        energy = energy,
        popularity = popularity,
        tags = tagsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet(),
        qqUrl = qqUrl,
        neteaseUrl = neteaseUrl,
    )
}
