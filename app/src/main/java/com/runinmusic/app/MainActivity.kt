package com.runinmusic.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runinmusic.app.data.local.RunInMusicDatabase
import com.runinmusic.app.data.repository.MusicRepository
import com.runinmusic.app.feature.home.HomeScreen
import com.runinmusic.app.feature.home.HomeViewModel
import com.runinmusic.app.sensor.CadenceMeasurer
import com.runinmusic.app.ui.theme.RunInMusicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = RunInMusicDatabase.get(applicationContext)
        val repository = MusicRepository(applicationContext, database)
        val cadenceMeasurer = CadenceMeasurer(applicationContext)

        setContent {
            RunInMusicTheme {
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.factory(repository, cadenceMeasurer),
                )
                val activityPermissions = remember {
                    buildList {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            add(Manifest.permission.ACTIVITY_RECOGNITION)
                        }
                        add(Manifest.permission.ACCESS_FINE_LOCATION)
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

                HomeScreen(
                    viewModel = viewModel,
                    onStartMeasurement = { permissionLauncher.launch(activityPermissions) },
                    onOpenSong = { song ->
                        viewModel.recordOpened(song.id)
                        MusicLinkOpener.open(applicationContext, song)
                    },
                )
            }
        }
    }
}
