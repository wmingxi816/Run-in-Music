package com.runinmusic.app.core.recommendation

import com.runinmusic.app.core.model.SongCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {
    private val engine = RecommendationEngine()

    @Test
    fun recommendsSongsInsideTargetBpmWindow() {
        val recommendations = engine.recommend(
            targetBpm = 80.0,
            songs = listOf(
                song(id = "fit", bpm = 82.0, tags = setOf("电子")),
                song(id = "miss", bpm = 120.0, tags = setOf("电子")),
            ),
        )

        assertEquals(listOf("fit"), recommendations.map { it.song.id })
    }

    @Test
    fun preferredTagsIncreaseRanking() {
        val recommendations = engine.recommend(
            targetBpm = 90.0,
            preferredTags = setOf("摇滚"),
            songs = listOf(
                song(id = "plain", bpm = 90.0, tags = setOf("舒缓"), popularity = 60),
                song(id = "preferred", bpm = 90.0, tags = setOf("摇滚"), popularity = 60),
            ),
        )

        assertEquals("preferred", recommendations.first().song.id)
    }

    @Test
    fun dislikedSongsAreExcluded() {
        val recommendations = engine.recommend(
            targetBpm = 90.0,
            dislikedSongIds = setOf("blocked"),
            songs = listOf(
                song(id = "blocked", bpm = 90.0),
                song(id = "allowed", bpm = 88.0),
            ),
        )

        assertTrue(recommendations.none { it.song.id == "blocked" })
    }

    private fun song(
        id: String,
        bpm: Double,
        tags: Set<String> = emptySet(),
        popularity: Int = 50,
    ) = SongCandidate(
        id = id,
        title = id,
        artist = "Test Artist",
        bpm = bpm,
        bpmSource = "test",
        bpmConfidence = 0.9,
        energy = 70,
        popularity = popularity,
        tags = tags,
        qqUrl = "https://y.qq.com/",
        neteaseUrl = null,
    )
}
