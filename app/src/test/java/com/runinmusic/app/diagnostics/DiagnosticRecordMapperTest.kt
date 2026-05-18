package com.runinmusic.app.diagnostics

import com.runinmusic.app.data.local.AppEventEntity
import com.runinmusic.app.data.local.RunSessionEntity
import com.runinmusic.app.data.local.SongInteractionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticRecordMapperTest {
    @Test
    fun mapsRoomRowsToDiagnosticModels() {
        val event = DiagnosticRecordMapper.event(
            AppEventEntity(
                level = "info",
                module = "run",
                type = "start",
                message = "Run started",
                detailsJson = """{"source":"button"}""",
                createdAtMillis = 1_000L,
            ),
        )
        val run = DiagnosticRecordMapper.runSession(
            RunSessionEntity(
                startedAtMillis = 2_000L,
                endedAtMillis = 62_000L,
                distanceMeters = 500.0,
                averagePaceSecondsPerKm = 120.0,
                measuredSpm = 82.0,
                targetBpm = 82.0,
            ),
        )
        val interaction = DiagnosticRecordMapper.songInteraction(
            SongInteractionEntity(
                songId = "song-1",
                action = "Opened",
                createdAtMillis = 3_000L,
            ),
        )

        assertEquals("run", event.module)
        assertEquals("start", event.type)
        assertEquals("""{"source":"button"}""", event.detailsJson)
        assertEquals(500.0, run.distanceMeters, 0.001)
        assertEquals("song-1", interaction.songId)
        assertEquals("Opened", interaction.action)
    }
}
