package com.runinmusic.app.core.diagnostics

data class DiagnosticAppInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val buildType: String,
)

data class DiagnosticDeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val isEmulator: Boolean,
)

data class DiagnosticPermissionState(
    val name: String,
    val granted: Boolean,
)

data class DiagnosticEvent(
    val level: String,
    val module: String,
    val type: String,
    val message: String,
    val detailsJson: String?,
    val createdAtMillis: Long,
)

data class DiagnosticRunSession(
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val distanceMeters: Double,
    val averagePaceSecondsPerKm: Double?,
    val measuredSpm: Double?,
    val targetBpm: Double?,
)

data class DiagnosticSongInteraction(
    val songId: String,
    val action: String,
    val createdAtMillis: Long,
)

data class DiagnosticSnapshot(
    val exportedAtMillis: Long,
    val app: DiagnosticAppInfo,
    val device: DiagnosticDeviceInfo,
    val permissions: List<DiagnosticPermissionState>,
    val events: List<DiagnosticEvent>,
    val runSessions: List<DiagnosticRunSession>,
    val songInteractions: List<DiagnosticSongInteraction>,
)

data class DiagnosticExportPayload(
    val files: Map<String, String>,
)
