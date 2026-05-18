package com.runinmusic.app.core.diagnostics

import org.json.JSONArray
import org.json.JSONObject

class DiagnosticExportBuilder {
    fun build(snapshot: DiagnosticSnapshot): DiagnosticExportPayload {
        return DiagnosticExportPayload(
            files = linkedMapOf(
                "diagnostics.json" to diagnosticsJson(snapshot),
                "events.jsonl" to eventsJsonLines(snapshot.events),
                "run_sessions.csv" to runSessionsCsv(snapshot.runSessions),
                "song_interactions.csv" to songInteractionsCsv(snapshot.songInteractions),
            ),
        )
    }

    private fun diagnosticsJson(snapshot: DiagnosticSnapshot): String {
        return JSONObject()
            .put("exportedAtMillis", snapshot.exportedAtMillis)
            .put(
                "app",
                JSONObject()
                    .put("packageName", snapshot.app.packageName)
                    .put("versionName", snapshot.app.versionName)
                    .put("versionCode", snapshot.app.versionCode)
                    .put("buildType", snapshot.app.buildType),
            )
            .put(
                "device",
                JSONObject()
                    .put("manufacturer", snapshot.device.manufacturer)
                    .put("model", snapshot.device.model)
                    .put("androidVersion", snapshot.device.androidVersion)
                    .put("sdkInt", snapshot.device.sdkInt)
                    .put("isEmulator", snapshot.device.isEmulator),
            )
            .put(
                "permissions",
                JSONArray(
                    snapshot.permissions.map { permission ->
                        JSONObject()
                            .put("name", permission.name)
                            .put("granted", permission.granted)
                    },
                ),
            )
            .toString(2)
    }

    private fun eventsJsonLines(events: List<DiagnosticEvent>): String {
        return events.joinToString(separator = "\n") { event ->
            JSONObject()
                .put("createdAtMillis", event.createdAtMillis)
                .put("level", event.level)
                .put("module", event.module)
                .put("type", event.type)
                .put("message", event.message)
                .put("details", redactedDetails(event.detailsJson))
                .toString()
        }
    }

    private fun runSessionsCsv(sessions: List<DiagnosticRunSession>): String {
        return buildString {
            appendLine("startedAtMillis,endedAtMillis,distanceMeters,averagePaceSecondsPerKm,measuredSpm,targetBpm")
            sessions.forEach { session ->
                appendCsvLine(
                    session.startedAtMillis,
                    session.endedAtMillis,
                    session.distanceMeters,
                    session.averagePaceSecondsPerKm,
                    session.measuredSpm,
                    session.targetBpm,
                )
            }
        }
    }

    private fun songInteractionsCsv(interactions: List<DiagnosticSongInteraction>): String {
        return buildString {
            appendLine("songId,action,createdAtMillis")
            interactions.forEach { interaction ->
                appendCsvLine(interaction.songId, interaction.action, interaction.createdAtMillis)
            }
        }
    }

    private fun StringBuilder.appendCsvLine(vararg values: Any?) {
        appendLine(values.joinToString(",") { value -> csvCell(value) })
    }

    private fun csvCell(value: Any?): String {
        val text = value?.toString().orEmpty()
        val needsQuote = text.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuote) {
            "\"${text.replace("\"", "\"\"")}\""
        } else {
            text
        }
    }

    private fun redactedDetails(detailsJson: String?): Any {
        if (detailsJson.isNullOrBlank()) return JSONObject()
        val trimmed = detailsJson.trim()
        return runCatching {
            when {
                trimmed.startsWith("{") -> redactObject(JSONObject(trimmed))
                trimmed.startsWith("[") -> redactArray(JSONArray(trimmed))
                else -> trimmed
            }
        }.getOrElse {
            JSONObject().put("unparsed", trimmed.take(500))
        }
    }

    private fun redactObject(source: JSONObject): JSONObject {
        val result = JSONObject()
        val keys = source.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = source.opt(key)
            result.put(
                key,
                if (isSensitiveKey(key)) {
                    "***"
                } else {
                    redactValue(value)
                },
            )
        }
        return result
    }

    private fun redactArray(source: JSONArray): JSONArray {
        val result = JSONArray()
        for (index in 0 until source.length()) {
            result.put(redactValue(source.opt(index)))
        }
        return result
    }

    private fun redactValue(value: Any?): Any? {
        return when (value) {
            is JSONObject -> redactObject(value)
            is JSONArray -> redactArray(value)
            else -> value
        }
    }

    private fun isSensitiveKey(key: String): Boolean {
        val normalized = key.lowercase()
        return SENSITIVE_KEY_PARTS.any { normalized.contains(it) }
    }

    companion object {
        private val SENSITIVE_KEY_PARTS = listOf("key", "token", "cookie", "password", "secret")
    }
}
