package com.runinmusic.app.core.diagnostics

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticExportBuilderTest {
    @Test
    fun buildsDiagnosticsFiles() {
        val payload = DiagnosticExportBuilder().build(snapshot())

        assertTrue(payload.files.containsKey("diagnostics.json"))
        assertTrue(payload.files.containsKey("events.jsonl"))
        assertTrue(payload.files.containsKey("run_sessions.csv"))
        assertTrue(payload.files.containsKey("song_interactions.csv"))

        val diagnostics = JSONObject(payload.files.getValue("diagnostics.json"))
        assertEquals("0.1.0", diagnostics.getJSONObject("app").getString("versionName"))
        assertEquals("Pixel Test", diagnostics.getJSONObject("device").getString("model"))
        assertEquals(true, diagnostics.getJSONArray("permissions").getJSONObject(0).getBoolean("granted"))
    }

    @Test
    fun redactsSensitiveEventDetails() {
        val payload = DiagnosticExportBuilder().build(snapshot())
        val events = payload.files.getValue("events.jsonl")

        assertTrue(events.contains("\"apiKey\":\"***\""))
        assertTrue(events.contains("\"token\":\"***\""))
        assertTrue(events.contains("\"visible\":\"kept\""))
        assertFalse(events.contains("secret-key"))
        assertFalse(events.contains("secret-token"))
    }

    @Test
    fun exportsRunSessionsAndInteractionsAsCsv() {
        val payload = DiagnosticExportBuilder().build(snapshot())

        assertTrue(payload.files.getValue("run_sessions.csv").contains("startedAtMillis,endedAtMillis,distanceMeters"))
        assertTrue(payload.files.getValue("run_sessions.csv").contains("1000,61000,120.5"))
        assertTrue(payload.files.getValue("song_interactions.csv").contains("songId,action,createdAtMillis"))
        assertTrue(payload.files.getValue("song_interactions.csv").contains("song-1,Liked,3000"))
    }

    private fun snapshot() = DiagnosticSnapshot(
        exportedAtMillis = 10_000L,
        app = DiagnosticAppInfo(
            packageName = "com.runinmusic.app",
            versionName = "0.1.0",
            versionCode = 1,
            buildType = "debug",
        ),
        device = DiagnosticDeviceInfo(
            manufacturer = "Google",
            model = "Pixel Test",
            androidVersion = "16",
            sdkInt = 36,
            isEmulator = true,
        ),
        permissions = listOf(
            DiagnosticPermissionState(name = "ACCESS_FINE_LOCATION", granted = true),
        ),
        events = listOf(
            DiagnosticEvent(
                level = "info",
                module = "catalog",
                type = "sync_success",
                message = "Imported catalog",
                detailsJson = """{"apiKey":"secret-key","token":"secret-token","visible":"kept"}""",
                createdAtMillis = 2_000L,
            ),
        ),
        runSessions = listOf(
            DiagnosticRunSession(
                startedAtMillis = 1_000L,
                endedAtMillis = 61_000L,
                distanceMeters = 120.5,
                averagePaceSecondsPerKm = 498.0,
                measuredSpm = 80.0,
                targetBpm = 80.0,
            ),
        ),
        songInteractions = listOf(
            DiagnosticSongInteraction(
                songId = "song-1",
                action = "Liked",
                createdAtMillis = 3_000L,
            ),
        ),
    )
}
