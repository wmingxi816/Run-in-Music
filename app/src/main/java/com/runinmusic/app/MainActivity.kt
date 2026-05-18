package com.runinmusic.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runinmusic.app.data.local.RunInMusicDatabase
import com.runinmusic.app.data.repository.MusicRepository
import com.runinmusic.app.diagnostics.DiagnosticExportService
import com.runinmusic.app.diagnostics.DiagnosticShareLauncher
import com.runinmusic.app.feature.home.HomeScreen
import com.runinmusic.app.feature.home.HomeViewModel
import com.runinmusic.app.location.RunTrackingService
import com.runinmusic.app.sensor.CadenceMeasurer
import com.runinmusic.app.ui.theme.RunInMusicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = RunInMusicDatabase.get(applicationContext)
        val repository = MusicRepository(applicationContext, database)
        val cadenceMeasurer = CadenceMeasurer(applicationContext)
        val diagnosticExportService = DiagnosticExportService(applicationContext, repository)

        setContent {
            RunInMusicTheme {
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.factory(repository, cadenceMeasurer, diagnosticExportService),
                )
                val activityPermissions = remember {
                    buildList {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            add(Manifest.permission.ACTIVITY_RECOGNITION)
                        }
                    }.toTypedArray()
                }
                val runPermissions = remember {
                    buildList {
                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }.toTypedArray()
                }
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                ) { result ->
                    val activityGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                        result[Manifest.permission.ACTIVITY_RECOGNITION] == true
                    if (activityGranted) {
                        viewModel.startSensorMeasurement()
                    } else {
                        viewModel.startManualMeasurement("未获得运动识别权限，已切换为手动点拍。")
                    }
                }
                val runPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                ) { result ->
                    val locationGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    if (locationGranted) {
                        viewModel.showRunTrackingMessage("GPS 跑步记录已开始。")
                        ContextCompat.startForegroundService(
                            applicationContext,
                            RunTrackingService.startIntent(applicationContext),
                        )
                    } else {
                        viewModel.showRunTrackingMessage("未获得定位权限，暂时不能记录跑步距离。")
                    }
                }

                HomeScreen(
                    viewModel = viewModel,
                    onStartMeasurement = {
                        if (activityPermissions.isEmpty()) {
                            viewModel.startSensorMeasurement()
                        } else {
                            permissionLauncher.launch(activityPermissions)
                        }
                    },
                    onStartRun = { runPermissionLauncher.launch(runPermissions) },
                    onStopRun = {
                        viewModel.showRunTrackingMessage("正在结束并保存本次跑步。")
                        startService(RunTrackingService.stopIntent(applicationContext))
                    },
                    onExportDiagnostics = {
                        viewModel.exportDiagnostics { file ->
                            DiagnosticShareLauncher.share(this, file)
                        }
                    },
                    onOpenSong = { song ->
                        viewModel.recordOpened(song.id)
                        MusicLinkOpener.open(applicationContext, song)
                    },
                )
            }
        }
    }
}
