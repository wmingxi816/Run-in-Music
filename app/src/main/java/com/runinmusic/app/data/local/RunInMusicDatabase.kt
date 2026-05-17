package com.runinmusic.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SongEntity::class,
        SongInteractionEntity::class,
        RunSessionEntity::class,
    ],
    version = 1,
)
abstract class RunInMusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    companion object {
        @Volatile private var instance: RunInMusicDatabase? = null

        fun get(context: Context): RunInMusicDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RunInMusicDatabase::class.java,
                    "run_in_music.db",
                ).build().also { instance = it }
            }
        }
    }
}
