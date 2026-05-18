package com.runinmusic.app.diagnostics

import com.runinmusic.app.core.diagnostics.DiagnosticEvent
import com.runinmusic.app.core.diagnostics.DiagnosticRunSession
import com.runinmusic.app.core.diagnostics.DiagnosticSongInteraction
import com.runinmusic.app.data.local.AppEventEntity
import com.runinmusic.app.data.local.RunSessionEntity
import com.runinmusic.app.data.local.SongInteractionEntity

object DiagnosticRecordMapper {
    fun event(row: AppEventEntity) = DiagnosticEvent(
        level = row.level,
        module = row.module,
        type = row.type,
        message = row.message,
        detailsJson = row.detailsJson,
        createdAtMillis = row.createdAtMillis,
    )

    fun runSession(row: RunSessionEntity) = DiagnosticRunSession(
        startedAtMillis = row.startedAtMillis,
        endedAtMillis = row.endedAtMillis,
        distanceMeters = row.distanceMeters,
        averagePaceSecondsPerKm = row.averagePaceSecondsPerKm,
        measuredSpm = row.measuredSpm,
        targetBpm = row.targetBpm,
    )

    fun songInteraction(row: SongInteractionEntity) = DiagnosticSongInteraction(
        songId = row.songId,
        action = row.action,
        createdAtMillis = row.createdAtMillis,
    )
}
