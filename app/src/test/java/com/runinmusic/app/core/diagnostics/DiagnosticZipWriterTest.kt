package com.runinmusic.app.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.util.zip.ZipFile

class DiagnosticZipWriterTest {
    @Test
    fun writesPayloadFilesToZip() {
        val directory = Files.createTempDirectory("run-in-music-diagnostics").toFile()
        val payload = DiagnosticExportPayload(
            files = linkedMapOf(
                "diagnostics.json" to """{"ok":true}""",
                "events.jsonl" to """{"type":"test"}""",
            ),
        )

        val zip = DiagnosticZipWriter().write(
            outputDirectory = directory,
            timestampMillis = 10_000L,
            payload = payload,
        )

        assertTrue(zip.name.startsWith("run_in_music_diagnostics_10000"))
        assertTrue(zip.name.endsWith(".zip"))
        ZipFile(zip).use { file ->
            assertEquals("""{"ok":true}""", file.readEntry("diagnostics.json"))
            assertEquals("""{"type":"test"}""", file.readEntry("events.jsonl"))
        }
    }

    private fun ZipFile.readEntry(name: String): String {
        return getInputStream(getEntry(name)).bufferedReader().use { it.readText() }
    }
}
