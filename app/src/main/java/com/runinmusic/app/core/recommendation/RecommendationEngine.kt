package com.runinmusic.app.core.recommendation

import com.runinmusic.app.core.cadence.CadenceMapper
import com.runinmusic.app.core.model.RecommendedSong
import com.runinmusic.app.core.model.SongCandidate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class RecommendationEngine(
    private val toleranceBpm: Double = CadenceMapper.DEFAULT_TOLERANCE_BPM,
) {
    fun recommend(
        targetBpm: Double,
        songs: List<SongCandidate>,
        preferredTags: Set<String> = emptySet(),
        dislikedSongIds: Set<String> = emptySet(),
        limit: Int = 12,
    ): List<RecommendedSong> {
        return songs
            .asSequence()
            .filterNot { it.id in dislikedSongIds }
            .mapNotNull { song ->
                val matchedBpm = CadenceMapper.closestCandidateBpm(song.bpm, targetBpm) ?: return@mapNotNull null
                val delta = abs(matchedBpm - targetBpm)
                if (delta > toleranceBpm) return@mapNotNull null

                val bpmScore = 1.0 - (delta / toleranceBpm)
                val preferenceScore = if (preferredTags.isEmpty()) {
                    0.5
                } else {
                    song.tags.intersect(preferredTags).size.toDouble() / preferredTags.size.coerceAtLeast(1)
                }
                val energyScore = min(1.0, max(0.0, song.energy / 100.0))
                val popularityScore = min(1.0, max(0.0, song.popularity / 100.0))
                val confidenceScore = min(1.0, max(0.0, song.bpmConfidence))

                val score = bpmScore * 0.45 +
                    preferenceScore * 0.20 +
                    energyScore * 0.15 +
                    popularityScore * 0.10 +
                    confidenceScore * 0.10

                RecommendedSong(
                    song = song,
                    score = score,
                    matchedBpm = matchedBpm,
                    reason = buildReason(targetBpm, matchedBpm, song.tags),
                )
            }
            .sortedWith(compareByDescending<RecommendedSong> { it.score }.thenBy { abs(it.matchedBpm - targetBpm) })
            .take(limit)
            .toList()
    }

    private fun buildReason(targetBpm: Double, matchedBpm: Double, tags: Set<String>): String {
        val tagText = tags.take(3).joinToString(" / ").ifBlank { "节奏稳定" }
        return "匹配 ${targetBpm.toInt()} BPM，按 ${matchedBpm.toInt()} BPM 跑感推荐 · $tagText"
    }
}
