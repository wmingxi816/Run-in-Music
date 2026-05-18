package com.runinmusic.app.data.repository

import android.content.Context
import com.runinmusic.app.core.model.SongInteractionType
import com.runinmusic.app.data.local.RunInMusicDatabase
import com.runinmusic.app.data.local.SongEntity
import com.runinmusic.app.data.local.SongInteractionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray

class MusicRepository(
    private val context: Context,
    private val database: RunInMusicDatabase,
    private val backendCatalogClient: BackendCatalogClient = BackendCatalogClient(),
) {
    val songs = database.songDao().observeSongs().map { rows -> rows.map { it.toCandidate() } }

    suspend fun seedCatalogIfEmpty() {
        if (database.songDao().countSongs() > 0) return
        database.songDao().upsertSongs(readSeedCatalog())
    }

    suspend fun refreshCatalogFromBackend(): CatalogRefreshResult = withContext(Dispatchers.IO) {
        val parsed = backendCatalogClient.fetchCatalog()
        if (parsed.songs.isNotEmpty()) {
            database.songDao().upsertSongs(parsed.songs)
        }
        CatalogRefreshResult(
            importedCount = parsed.songs.size,
            skippedWithoutBpm = parsed.skippedWithoutBpm,
            sourceUrl = BackendCatalogClient.DEFAULT_BASE_URL,
        )
    }

    suspend fun recordInteraction(songId: String, type: SongInteractionType) {
        database.songDao().insertInteraction(
            SongInteractionEntity(
                songId = songId,
                action = type.name,
            ),
        )
    }

    suspend fun preferredTags(): Set<String> {
        val songsById = database.songDao().getSongs().associateBy { it.id }
        val likedSongIds = database.songDao().getInteractions()
            .filter { it.action == SongInteractionType.Liked.name || it.action == SongInteractionType.Opened.name }
            .map { it.songId }
            .toSet()

        return likedSongIds
            .mapNotNull { songsById[it] }
            .flatMap { it.tagsCsv.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(6)
            .map { it.key }
            .toSet()
    }

    suspend fun dislikedSongIds(): Set<String> {
        return database.songDao().getInteractions()
            .filter { it.action == SongInteractionType.Disliked.name || it.action == SongInteractionType.Skipped.name }
            .map { it.songId }
            .toSet()
    }

    private fun readSeedCatalog(): List<SongEntity> {
        val json = context.assets.open("catalog.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    SongEntity(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        artist = item.getString("artist"),
                        durationSeconds = item.optInt("durationSeconds", 0),
                        bpm = item.getDouble("bpm"),
                        bpmSource = item.optString("bpmSource", "seed"),
                        bpmConfidence = item.optDouble("bpmConfidence", 0.7),
                        energy = item.optInt("energy", 70),
                        popularity = item.optInt("popularity", 50),
                        tagsCsv = item.optJSONArray("tags")?.let { tags ->
                            (0 until tags.length()).joinToString(",") { tags.getString(it) }
                        }.orEmpty(),
                        qqUrl = item.optString("qqUrl").takeIf { it.isNotBlank() },
                        neteaseUrl = item.optString("neteaseUrl").takeIf { it.isNotBlank() },
                    ),
                )
            }
        }
    }
}
