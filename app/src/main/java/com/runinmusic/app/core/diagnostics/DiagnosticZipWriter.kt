package com.runinmusic.app.core.diagnostics

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DiagnosticZipWriter {
    fun write(
        outputDirectory: File,
        timestampMillis: Long,
        payload: DiagnosticExportPayload,
    ): File {
        outputDirectory.mkdirs()
        val output = File(outputDirectory, "run_in_music_diagnostics_$timestampMillis.zip")
        ZipOutputStream(output.outputStream().buffered()).use { zip ->
            payload.files.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return output
    }
}
