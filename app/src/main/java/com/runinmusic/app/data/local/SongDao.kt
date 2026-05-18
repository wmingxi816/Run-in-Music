package com.runinmusic.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY popularity DESC, title ASC")
    fun observeSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY popularity DESC, title ASC")
    suspend fun getSongs(): List<SongEntity>

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun countSongs(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongs(songs: List<SongEntity>)

    @Insert
    suspend fun insertInteraction(interaction: SongInteractionEntity)

    @Query("SELECT * FROM song_interactions ORDER BY createdAtMillis DESC")
    suspend fun getInteractions(): List<SongInteractionEntity>

    @Insert
    suspend fun insertRunSession(session: RunSessionEntity): Long

    @Query("SELECT * FROM run_sessions ORDER BY startedAtMillis DESC LIMIT 1")
    fun observeLatestRunSession(): Flow<RunSessionEntity?>
}
