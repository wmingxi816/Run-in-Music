package com.runinmusic.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackendCatalogClientTest {
    @Test
    fun parsesBackendCatalogAndSkipsSongsWithoutBpm() {
        val json = """
            [
              {
                "id": "song_1",
                "title": "Tempo Runner",
                "artist": "Run Lab",
                "duration_seconds": 201,
                "bpm": 84.0,
                "bpm_source": "analysis",
                "bpm_confidence": 0.91,
                "energy": 80,
                "popularity": 66,
                "tags": ["电子", "稳定节奏"],
                "platform_urls": {
                  "qq": "https://y.qq.com/n/ryqq/songDetail/abc",
                  "netease": "https://music.163.com/song?id=1"
                }
              },
              {
                "id": "song_2",
                "title": "No BPM Yet",
                "artist": "Run Lab",
                "bpm": null,
                "tags": []
              }
            ]
        """.trimIndent()

        val result = parseCatalogJson(json)
        val song = result.songs.single()

        assertEquals(1, result.skippedWithoutBpm)
        assertEquals("song_1", song.id)
        assertEquals("Tempo Runner", song.title)
        assertEquals(84.0, song.bpm, 0.001)
        assertEquals("analysis", song.bpmSource)
        assertEquals("电子,稳定节奏", song.tagsCsv)
        assertEquals("https://y.qq.com/n/ryqq/songDetail/abc", song.qqUrl)
        assertEquals("https://music.163.com/song?id=1", song.neteaseUrl)
    }

    @Test
    fun parsesAssetStyleCatalogWithoutPlatformUrlsObject() {
        val json = """
            [
              {
                "id": "seed",
                "title": "Seed Song",
                "artist": "Run Lab",
                "durationSeconds": 180,
                "bpm": 90,
                "bpmSource": "seed",
                "bpmConfidence": 0.7,
                "tags": ["慢跑"],
                "qqUrl": "",
                "neteaseUrl": ""
              }
            ]
        """.trimIndent()

        val song = parseCatalogJson(json).songs.single()

        assertEquals(180, song.durationSeconds)
        assertEquals("seed", song.bpmSource)
        assertNull(song.qqUrl)
        assertNull(song.neteaseUrl)
    }
}
