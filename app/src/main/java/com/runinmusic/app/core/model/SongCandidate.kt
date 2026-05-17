package com.runinmusic.app.core.model

data class SongCandidate(
    val id: String,
    val title: String,
    val artist: String,
    val bpm: Double,
    val bpmSource: String,
    val bpmConfidence: Double,
    val energy: Int,
    val popularity: Int,
    val tags: Set<String>,
    val qqUrl: String?,
    val neteaseUrl: String?,
)

data class RecommendedSong(
    val song: SongCandidate,
    val score: Double,
    val matchedBpm: Double,
    val reason: String,
)

enum class SongInteractionType {
    Opened,
    Liked,
    Disliked,
    Skipped,
}
