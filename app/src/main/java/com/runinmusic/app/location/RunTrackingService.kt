package com.runinmusic.app.location

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.runinmusic.app.R
import com.runinmusic.app.core.run.RunLocationSample
import com.runinmusic.app.data.local.RunInMusicDatabase
import com.runinmusic.app.data.local.RunSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RunTrackingService : Service() {
    private lateinit var client: FusedLocationProviderClient
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { location ->
                RunTrackingStore.addLocation(
                    RunLocationSample(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                        timeMillis = if (location.time > 0L) location.time else System.currentTimeMillis(),
                    ),
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        client = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRun()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, notification())
        if (!RunTrackingStore.state.value.isRunning) {
            RunTrackingStore.start()
        }
        requestLocationUpdatesIfAllowed()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        client.removeLocationUpdates(callback)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun requestLocationUpdatesIfAllowed() {
        val fineGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
            .setMinUpdateIntervalMillis(1_000L)
            .setMinUpdateDistanceMeters(3f)
            .build()
        client.requestLocationUpdates(request, callback, mainLooper)
    }

    private fun stopRun() {
        client.removeLocationUpdates(callback)
        val finished = RunTrackingStore.finish()
        val startedAtMillis = finished.startedAtMillis
        val endedAtMillis = finished.endedAtMillis
        if (startedAtMillis == null || endedAtMillis == null || finished.elapsedMillis <= 0L) {
            finishService()
            return
        }

        serviceScope.launch {
            RunInMusicDatabase.get(applicationContext).songDao().insertRunSession(
                RunSessionEntity(
                    startedAtMillis = startedAtMillis,
                    endedAtMillis = endedAtMillis,
                    distanceMeters = finished.distanceMeters,
                    averagePaceSecondsPerKm = finished.averagePaceSecondsPerKm,
                    measuredSpm = null,
                    targetBpm = null,
                ),
            )
            mainHandler.post { finishService() }
        }
    }

    private fun finishService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "跑步记录",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun notification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_runner)
        .setContentTitle("Run in Music 正在记录跑步")
        .setContentText("GPS 距离记录已开启，音乐推荐仍由你手动选择。")
        .setOngoing(true)
        .build()

    companion object {
        private const val ACTION_START = "com.runinmusic.app.location.START_RUN"
        private const val ACTION_STOP = "com.runinmusic.app.location.STOP_RUN"
        private const val CHANNEL_ID = "run_tracking"
        private const val NOTIFICATION_ID = 42

        fun startIntent(context: Context): Intent {
            return Intent(context, RunTrackingService::class.java).setAction(ACTION_START)
        }

        fun stopIntent(context: Context): Intent {
            return Intent(context, RunTrackingService::class.java).setAction(ACTION_STOP)
        }
    }
}
