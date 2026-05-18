package com.runinmusic.app.diagnostics

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.runinmusic.app.core.diagnostics.DiagnosticAppInfo
import com.runinmusic.app.core.diagnostics.DiagnosticDeviceInfo
import com.runinmusic.app.core.diagnostics.DiagnosticExportBuilder
import com.runinmusic.app.core.diagnostics.DiagnosticPermissionState
import com.runinmusic.app.core.diagnostics.DiagnosticSnapshot
import com.runinmusic.app.core.diagnostics.DiagnosticZipWriter
import com.runinmusic.app.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DiagnosticExportService(
    private val context: Context,
    private val repository: MusicRepository,
    private val builder: DiagnosticExportBuilder = DiagnosticExportBuilder(),
    private val zipWriter: DiagnosticZipWriter = DiagnosticZipWriter(),
) {
    suspend fun export(): File = withContext(Dispatchers.IO) {
        val exportedAtMillis = System.currentTimeMillis()
        repository.logEvent(
            level = "info",
            module = "diagnostics",
            type = "export_started",
            message = "Diagnostic export started",
            detailsJson = null,
        )
        val snapshot = DiagnosticSnapshot(
            exportedAtMillis = exportedAtMillis,
            app = appInfo(),
            device = deviceInfo(),
            permissions = permissionStates(),
            events = repository.recentAppEvents(limit = 500)
                .map(DiagnosticRecordMapper::event),
            runSessions = repository.recentRunSessions(limit = 100)
                .map(DiagnosticRecordMapper::runSession),
            songInteractions = repository.songInteractions()
                .map(DiagnosticRecordMapper::songInteraction),
        )
        val output = zipWriter.write(
            outputDirectory = File(context.cacheDir, "diagnostics"),
            timestampMillis = exportedAtMillis,
            payload = builder.build(snapshot),
        )
        repository.logEvent(
            level = "info",
            module = "diagnostics",
            type = "export_finished",
            message = "Diagnostic export finished",
            detailsJson = """{"file":"${output.name}"}""",
        )
        output
    }

    private fun appInfo(): DiagnosticAppInfo {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        return DiagnosticAppInfo(
            packageName = context.packageName,
            versionName = packageInfo.versionName ?: "unknown",
            versionCode = versionCode,
            buildType = if (isDebuggable()) "debug" else "release",
        )
    }

    private fun deviceInfo() = DiagnosticDeviceInfo(
        manufacturer = Build.MANUFACTURER.orEmpty(),
        model = Build.MODEL.orEmpty(),
        androidVersion = Build.VERSION.RELEASE.orEmpty(),
        sdkInt = Build.VERSION.SDK_INT,
        isEmulator = isProbablyEmulator(),
    )

    private fun permissionStates(): List<DiagnosticPermissionState> {
        return buildList {
            add(permission(Manifest.permission.ACCESS_FINE_LOCATION))
            add(permission(Manifest.permission.ACCESS_COARSE_LOCATION))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(permission(Manifest.permission.ACTIVITY_RECOGNITION))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(permission(Manifest.permission.POST_NOTIFICATIONS))
            }
        }
    }

    private fun permission(name: String) = DiagnosticPermissionState(
        name = name.substringAfterLast('.'),
        granted = ContextCompat.checkSelfPermission(context, name) == PackageManager.PERMISSION_GRANTED,
    )

    private fun isProbablyEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val product = Build.PRODUCT.lowercase()
        return fingerprint.contains("generic") ||
            model.contains("emulator") ||
            model.contains("sdk_gphone") ||
            product.contains("sdk")
    }

    private fun isDebuggable(): Boolean {
        return context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
    }
}
