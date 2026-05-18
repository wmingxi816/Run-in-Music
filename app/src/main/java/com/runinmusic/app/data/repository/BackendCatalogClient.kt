package com.runinmusic.app.data.repository

import com.runinmusic.app.data.local.SongEntity
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class BackendCatalogClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
) {
    fun fetchCatalog(): CatalogParseResult {
        val endpoint = "${baseUrl.trimEnd('/')}/catalog/export"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 12_000
        }

        return try {
            if (connection.responseCode !in 200..299) {
                error("Backend returned HTTP ${connection.responseCode}")
            }
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            parseCatalogJson(json)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8000"
    }
}

data class CatalogParseResult(
    val songs: List<SongEntity>,
    val skippedWithoutBpm: Int,
)

data class CatalogRefreshResult(
    val importedCount: Int,
    val skippedWithoutBpm: Int,
    val sourceUrl: String,
)

fun parseCatalogJson(json: String): CatalogParseResult {
    val array = JSONArray(json)
    val songs = mutableListOf<SongEntity>()
    var skippedWithoutBpm = 0

    for (index in 0 until array.length()) {
        val item = array.getJSONObject(index)
        if (!item.has("bpm") || item.isNull("bpm")) {
            skippedWithoutBpm += 1
            continue
        }
        songs += item.toSongEntity()
    }

    return CatalogParseResult(songs = songs, skippedWithoutBpm = skippedWithoutBpm)
}

private fun org.json.JSONObject.toSongEntity(): SongEntity {
    val platformUrls = optJSONObject("platform_urls")
    return SongEntity(
        id = getString("id"),
        title = getString("title"),
        artist = getString("artist"),
        durationSeconds = optNullableInt("duration_seconds") ?: optNullableInt("durationSeconds") ?: 0,
        bpm = getDouble("bpm"),
        bpmSource = optString("bpm_source").ifBlank { optString("bpmSource", "backend") },
        bpmConfidence = optDouble("bpm_confidence", optDouble("bpmConfidence", 0.7)),
        energy = optInt("energy", 70),
        popularity = optInt("popularity", 50),
        tagsCsv = optJSONArray("tags")?.let { tags ->
            (0 until tags.length()).joinToString(",") { tags.getString(it) }
        }.orEmpty(),
        qqUrl = platformUrls?.optString("qq")?.takeIf { it.isNotBlank() }
            ?: optString("qqUrl").takeIf { it.isNotBlank() },
        neteaseUrl = platformUrls?.optString("netease")?.takeIf { it.isNotBlank() }
            ?: optString("neteaseUrl").takeIf { it.isNotBlank() },
    )
}

private fun org.json.JSONObject.optNullableInt(name: String): Int? {
    return if (has(name) && !isNull(name)) optInt(name) else null
}
