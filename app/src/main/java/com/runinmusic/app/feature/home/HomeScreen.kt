@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.runinmusic.app.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runinmusic.app.core.cadence.CadenceMapper
import com.runinmusic.app.core.model.RecommendedSong
import com.runinmusic.app.core.model.SongCandidate
import com.runinmusic.app.data.local.RunSessionEntity
import com.runinmusic.app.location.RunTrackingState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartMeasurement: () -> Unit,
    onStartRun: () -> Unit,
    onStopRun: () -> Unit,
    onPauseRun: () -> Unit,
    onResumeRun: () -> Unit,
    onExportDiagnostics: () -> Unit,
    onOpenSong: (SongCandidate) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(HomeTab.Run) }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0C1714), tonalElevation = 12.dp) {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.icon, fontSize = 20.sp) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFF6F1E6), Color(0xFFE7F2D8), Color(0xFF07110F)),
                    ),
                )
                .padding(innerPadding),
        ) {
            RhythmBackdrop()
            when (selectedTab) {
                HomeTab.Run -> RunPage(
                    state = state,
                    onStartRun = onStartRun,
                    onStopRun = onStopRun,
                    onPauseRun = onPauseRun,
                    onResumeRun = onResumeRun,
                )
                HomeTab.Music -> MusicPage(
                    state = state,
                    onStartMeasurement = onStartMeasurement,
                    onManualTap = viewModel::recordManualTap,
                    onSelectMeasurementSeconds = viewModel::selectMeasurementSeconds,
                    onExpandMeasurementPanel = viewModel::expandMeasurementPanel,
                    onOpenSong = onOpenSong,
                    onLike = viewModel::markLiked,
                    onDislike = viewModel::markDisliked,
                )
                HomeTab.Profile -> ProfilePage(
                    state = state,
                    onExportDiagnostics = onExportDiagnostics,
                    onRefreshCatalog = viewModel::refreshBackendCatalog,
                )
            }
        }
    }
}

private enum class HomeTab(val label: String, val icon: String) {
    Run("跑步", "跑"),
    Music("音乐", "乐"),
    Profile("我的", "我"),
}

@Composable
private fun RunPage(
    state: HomeUiState,
    onStartRun: () -> Unit,
    onStopRun: () -> Unit,
    onPauseRun: () -> Unit,
    onResumeRun: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { RunHeroCard(state.runTrackingState) }
        item {
            RunTrackingCard(
                runState = state.runTrackingState,
                latestRunSession = state.latestRunSession,
                message = state.runStatusMessage,
                onStartRun = onStartRun,
                onStopRun = onStopRun,
                onPauseRun = onPauseRun,
                onResumeRun = onResumeRun,
            )
        }
        item { RunHistorySection(state.recentRunSessions) }
        item { RunPlanSection() }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun MusicPage(
    state: HomeUiState,
    onStartMeasurement: () -> Unit,
    onManualTap: () -> Unit,
    onSelectMeasurementSeconds: (Int) -> Unit,
    onExpandMeasurementPanel: () -> Unit,
    onOpenSong: (SongCandidate) -> Unit,
    onLike: (String) -> Unit,
    onDislike: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            MeasurementCard(
                state = state,
                onStartMeasurement = onStartMeasurement,
                onManualTap = onManualTap,
                onSelectMeasurementSeconds = onSelectMeasurementSeconds,
                onExpandMeasurementPanel = onExpandMeasurementPanel,
            )
        }
        item { NowPlayingCard(state) }
        item {
            Text(
                text = "节奏音乐推荐",
                color = Color(0xFFFDF8EC),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
            )
        }
        if (state.recommendations.isEmpty()) {
            items(DemoMusicRecommendations, key = { it.title }) { recommendation ->
                DemoSongCard(recommendation)
            }
        } else {
            items(state.recommendations, key = { it.song.id }) { recommendation ->
                SongCard(
                    recommendation = recommendation,
                    onOpen = { onOpenSong(recommendation.song) },
                    onLike = { onLike(recommendation.song.id) },
                    onDislike = { onDislike(recommendation.song.id) },
                )
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun NowPlayingCard(state: HomeUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE7F2D8)), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "内置播放器", color = Color(0xFF101E1A), fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(text = state.playbackStatusMessage, color = Color(0xFF2D483E), fontSize = 13.sp)
            state.nowPlayingTitle?.let {
                Text(text = "当前：$it", color = Color(0xFFFF7A1A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfilePage(
    state: HomeUiState,
    onExportDiagnostics: () -> Unit,
    onRefreshCatalog: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ProfileSummaryCard(state) }
        item { DiagnosticExportCard(state = state, onExportDiagnostics = onExportDiagnostics) }
        item { CatalogSyncCard(state = state, onRefreshCatalog = onRefreshCatalog) }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun RunHeroCard(runState: RunTrackingState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101E1A)),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(text = "Run in Music", color = Color(0xFFFF7A1A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text = if (runState.isRunning) "保持节奏，继续推进" else "今天，从一段轻跑开始",
                color = Color(0xFFFDF8EC),
                fontSize = 34.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Black,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricPill(label = "时长", value = formatElapsed(runState.elapsedMillis))
                MetricPill(label = "距离", value = formatDistance(runState.distanceMeters))
                MetricPill(label = "配速", value = formatPace(runState.averagePaceSecondsPerKm))
            }
        }
    }
}

@Composable
private fun DiagnosticExportCard(
    state: HomeUiState,
    onExportDiagnostics: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101E1A)),
        shape = RoundedCornerShape(28.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "诊断日志",
                    color = Color(0xFFFFD36E),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = state.diagnosticStatusMessage,
                    color = Color(0xFFCFE8D2),
                    fontSize = 13.sp,
                )
            }
            Button(
                enabled = !state.isExportingDiagnostics,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD36E), contentColor = Color(0xFF101E1A)),
                shape = RoundedCornerShape(14.dp),
                onClick = onExportDiagnostics,
            ) {
                Text(if (state.isExportingDiagnostics) "整理中" else "导出日志")
            }
        }
    }
}

@Composable
private fun RunTrackingCard(
    runState: RunTrackingState,
    latestRunSession: RunSessionEntity?,
    message: String,
    onStartRun: () -> Unit,
    onStopRun: () -> Unit,
    onPauseRun: () -> Unit,
    onResumeRun: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8EC)),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = when {
                            runState.isRunning -> "跑步记录中"
                            runState.isPaused -> "跑步已暂停"
                            else -> "跑步记录"
                        },
                        color = Color(0xFF101E1A),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = when {
                            runState.isRunning -> "GPS 正在累计距离"
                            runState.isPaused -> "计时和定位已暂停，继续后不会计算暂停间隔。"
                            else -> message
                        },
                        color = Color(0xFF557165),
                        fontSize = 13.sp,
                    )
                }
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (runState.isRunning || runState.isPaused) Color(0xFF101E1A) else Color(0xFFFF7A1A),
                        contentColor = Color(0xFFFDF8EC),
                    ),
                    shape = RoundedCornerShape(14.dp),
                    onClick = if (runState.isRunning || runState.isPaused) onStopRun else onStartRun,
                ) {
                    Text(if (runState.isRunning || runState.isPaused) "结束" else "开跑")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RunMetricTile(label = "时长", value = formatElapsed(runState.elapsedMillis), modifier = Modifier.weight(1f))
                RunMetricTile(label = "距离", value = formatDistance(runState.distanceMeters), modifier = Modifier.weight(1f))
                RunMetricTile(label = "配速", value = formatPace(runState.averagePaceSecondsPerKm), modifier = Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    enabled = runState.isRunning || runState.isPaused,
                    shape = RoundedCornerShape(14.dp),
                    onClick = if (runState.isPaused) onResumeRun else onPauseRun,
                ) {
                    Text(if (runState.isPaused) "继续" else "暂停")
                }
                Surface(color = Color(0xFFE7F2D8), shape = CircleShape) {
                    Text(
                        text = if (runState.isPaused) "暂停中：距离不会跳算" else "暂停时会冻结计时",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = Color(0xFF2D483E),
                        fontSize = 12.sp,
                    )
                }
            }

            latestRunSession?.let { session ->
                Surface(color = Color(0xFFE7F2D8), shape = RoundedCornerShape(18.dp)) {
                    Text(
                        text = "上次跑步 ${formatDistance(session.distanceMeters)} · ${formatElapsed((session.endedAtMillis ?: session.startedAtMillis) - session.startedAtMillis)} · ${formatPace(session.averagePaceSecondsPerKm)}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = Color(0xFF2D483E),
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun RunPlanSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "推荐跑步计划", color = Color(0xFFFDF8EC), fontSize = 24.sp, fontWeight = FontWeight.Black)
        DemoRunPlans.forEach { plan ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8EC)), shape = RoundedCornerShape(24.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                        Text(text = plan.title, color = Color(0xFF101E1A), fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(text = plan.description, color = Color(0xFF557165), fontSize = 13.sp)
                    }
                    Surface(color = Color(0xFFE7F2D8), shape = CircleShape) {
                        Text(text = plan.badge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color(0xFF2D483E), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RunHistorySection(sessions: List<RunSessionEntity>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "最近跑步", color = Color(0xFFFDF8EC), fontSize = 24.sp, fontWeight = FontWeight.Black)
        if (sessions.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8EC)), shape = RoundedCornerShape(24.dp)) {
                Text(
                    text = "完成一次跑步后，这里会显示最近记录。",
                    modifier = Modifier.padding(18.dp),
                    color = Color(0xFF557165),
                    fontSize = 14.sp,
                )
            }
        } else {
            sessions.forEach { session ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8EC)), shape = RoundedCornerShape(24.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = formatDistance(session.distanceMeters), color = Color(0xFF101E1A), fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text(
                                text = "${formatElapsed((session.endedAtMillis ?: session.startedAtMillis) - session.startedAtMillis)} · ${formatPace(session.averagePaceSecondsPerKm)}",
                                color = Color(0xFF557165),
                                fontSize = 13.sp,
                            )
                        }
                        Surface(color = Color(0xFFE7F2D8), shape = CircleShape) {
                            Text(text = "已保存", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color(0xFF2D483E), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RunMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color(0xFFE7F2D8),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = value, color = Color(0xFF101E1A), fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(text = label, color = Color(0xFF557165), fontSize = 12.sp)
        }
    }
}

@Composable
private fun CatalogSyncCard(
    state: HomeUiState,
    onRefreshCatalog: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE7F2D8)),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "后台曲库",
                    color = Color(0xFF101E1A),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "本地可推荐 ${state.catalogSongCount} 首 · ${state.lastCatalogSyncAtMillis?.let { "上次同步 ${formatClockTime(it)}" } ?: "尚未手动同步"}",
                    color = Color(0xFFFF7A1A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = state.catalogStatusMessage,
                    color = Color(0xFF2D483E),
                    fontSize = 13.sp,
                )
            }
            OutlinedButton(
                enabled = !state.isRefreshingCatalog,
                shape = RoundedCornerShape(14.dp),
                onClick = onRefreshCatalog,
            ) {
                Text(if (state.isRefreshingCatalog) "同步中" else "刷新")
            }
        }
    }
}

@Composable
private fun MeasurementCard(
    state: HomeUiState,
    onStartMeasurement: () -> Unit,
    onManualTap: () -> Unit,
    onSelectMeasurementSeconds: (Int) -> Unit,
    onExpandMeasurementPanel: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101E1A)),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(text = "步频测量", color = Color(0xFFFF7A1A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = "让下一首歌跟上你的脚步", color = Color(0xFFFDF8EC), fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Black)
            Text(
                text = state.statusMessage,
                color = Color(0xFFCFE8D2),
                fontSize = 15.sp,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricPill(label = "SPM", value = state.measuredSpm?.roundToInt()?.toString() ?: "--")
                MetricPill(label = "目标 BPM", value = state.targetBpm?.roundToInt()?.toString() ?: "--")
                MetricPill(label = "步数", value = state.lastSteps?.toString() ?: state.manualTapCount.toString())
            }

            AnimatedVisibility(visible = state.isMeasurementPanelExpanded || state.isMeasuring) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CadenceMapper.SUPPORTED_MEASUREMENT_SECONDS.forEach { seconds ->
                            val selected = state.selectedMeasurementSeconds == seconds
                            OutlinedButton(
                                enabled = !state.isMeasuring,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) Color(0xFFFFD36E) else Color.Transparent,
                                    contentColor = if (selected) Color(0xFF101E1A) else Color(0xFFFFD36E),
                                ),
                                onClick = { onSelectMeasurementSeconds(seconds) },
                            ) {
                                Text("${seconds}s")
                            }
                        }
                    }
                    if (state.isMeasuring) {
                        Text(text = "剩余 ${state.secondsRemaining}s", color = Color(0xFFFFD36E), fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            if (state.manualMode && state.isMeasuring) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD36E), contentColor = Color(0xFF101E1A)),
                    shape = RoundedCornerShape(18.dp),
                    onClick = onManualTap,
                ) {
                    Text("记一拍", fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
            } else {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    enabled = !state.isMeasuring,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A1A), contentColor = Color(0xFF101E1A)),
                    shape = RoundedCornerShape(18.dp),
                    onClick = if (state.isMeasurementPanelExpanded) onStartMeasurement else onExpandMeasurementPanel,
                ) {
                    Text(
                        when {
                            state.isMeasuring -> "测量中"
                            state.isMeasurementPanelExpanded -> "测量 ${state.selectedMeasurementSeconds} 秒步频"
                            else -> "再次测量"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoSongCard(recommendation: DemoMusicRecommendation) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8EC)), shape = RoundedCornerShape(26.dp)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = recommendation.title, color = Color(0xFF101E1A), fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(text = recommendation.subtitle, color = Color(0xFF557165), fontSize = 14.sp)
                }
                Text(text = "${recommendation.bpm} BPM", color = Color(0xFFFF7A1A), fontWeight = FontWeight.Black)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recommendation.tags.forEach { tag ->
                    Surface(color = Color(0xFFE7F2D8), shape = CircleShape) {
                        Text(text = tag, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color(0xFF2D483E), fontSize = 12.sp)
                    }
                }
            }
            Text(text = recommendation.reason, color = Color(0xFF2D483E), fontSize = 14.sp)
        }
    }
}

@Composable
private fun ProfileSummaryCard(state: HomeUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8EC)), shape = RoundedCornerShape(28.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "我的节奏档案", color = Color(0xFF101E1A), fontSize = 24.sp, fontWeight = FontWeight.Black)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RunMetricTile(label = "最近 SPM", value = state.measuredSpm?.roundToInt()?.toString() ?: "--", modifier = Modifier.weight(1f))
                RunMetricTile(label = "目标 BPM", value = state.targetBpm?.roundToInt()?.toString() ?: "--", modifier = Modifier.weight(1f))
                RunMetricTile(label = "上次距离", value = state.latestRunSession?.distanceMeters?.let(::formatDistance) ?: "--", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String) {
    Surface(
        color = Color(0xFF1D332C),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = value, color = Color(0xFFFDF8EC), fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(text = label, color = Color(0xFF9EC7A2), fontSize = 12.sp)
        }
    }
}

@Composable
private fun SongCard(
    recommendation: RecommendedSong,
    onOpen: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8EC)),
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recommendation.song.title,
                        color = Color(0xFF101E1A),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = recommendation.song.artist,
                        color = Color(0xFF557165),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "${recommendation.matchedBpm.roundToInt()} BPM",
                    color = Color(0xFFFF7A1A),
                    fontWeight = FontWeight.Black,
                )
            }
            Text(text = recommendation.reason, color = Color(0xFF2D483E), fontSize = 14.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recommendation.song.tags.take(4).forEach { tag ->
                    Surface(color = Color(0xFFE7F2D8), shape = CircleShape) {
                        Text(text = tag, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color(0xFF2D483E), fontSize = 12.sp)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onOpen, shape = RoundedCornerShape(14.dp)) {
                    Text(if (recommendation.song.streamUrl != null) "播放" else "去听歌")
                }
                OutlinedButton(onClick = onLike, shape = RoundedCornerShape(14.dp)) {
                    Text("喜欢")
                }
                OutlinedButton(onClick = onDislike, shape = RoundedCornerShape(14.dp)) {
                    Text("不适合")
                }
            }
        }
    }
}

@Composable
private fun EmptyRecommendationCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8EC)),
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "还没有推荐", fontSize = 22.sp, color = Color(0xFF101E1A), fontWeight = FontWeight.Black)
            Text(text = "完成一次步频测量后，这里会出现匹配当前节奏的歌曲。", color = Color(0xFF557165))
        }
    }
}

@Composable
private fun RhythmBackdrop() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val orange = Color(0x33FF7A1A)
        val green = Color(0x3356B870)
        drawCircle(color = orange, radius = size.width * 0.35f, center = Offset(size.width * 0.92f, size.height * 0.08f))
        drawCircle(color = green, radius = size.width * 0.45f, center = Offset(size.width * 0.05f, size.height * 0.48f))
    }
}

private fun formatElapsed(elapsedMillis: Long): String {
    val totalSeconds = (elapsedMillis / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatDistance(distanceMeters: Double): String {
    return "%.2f km".format(distanceMeters / 1_000.0)
}

private fun formatPace(secondsPerKm: Double?): String {
    if (secondsPerKm == null || secondsPerKm <= 0.0) return "--"
    val totalSeconds = secondsPerKm.roundToInt()
    return "%d'%02d\"".format(totalSeconds / 60, totalSeconds % 60)
}

private fun formatClockTime(timeMillis: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMillis))
}

private data class DemoRunPlan(
    val title: String,
    val description: String,
    val badge: String,
)

private val DemoRunPlans = listOf(
    DemoRunPlan("20 分钟轻松跑", "低压力热身，适合今天只想动起来。", "新手"),
    DemoRunPlan("3 × 4 分钟节奏跑", "中等强度间歇，后续可接入自动换歌。", "节奏"),
    DemoRunPlan("30 分钟燃脂巡航", "稳定配速，推荐 80-95 BPM 音乐区间。", "耐力"),
)

private data class DemoMusicRecommendation(
    val title: String,
    val subtitle: String,
    val bpm: Int,
    val tags: List<String>,
    val reason: String,
)

private val DemoMusicRecommendations = listOf(
    DemoMusicRecommendation("Neon Stride 01", "AI Seed / Electronic Run", 82, listOf("电子", "高能", "稳定鼓点"), "适合 75-90 SPM 的轻松跑节奏。"),
    DemoMusicRecommendation("Asphalt Pulse", "AI Seed / Indie Dance", 88, listOf("律动", "城市感", "中等激情"), "鼓组清晰，适合作为热身后的第一首。"),
    DemoMusicRecommendation("Long Breath Loop", "AI Seed / Ambient Pop", 72, listOf("舒缓", "长跑", "低疲劳"), "适合恢复跑或慢跑阶段保持呼吸。"),
    DemoMusicRecommendation("Copper Sprint", "AI Seed / Rock Hybrid", 104, listOf("摇滚", "冲刺", "强节拍"), "可用于间歇跑快段的占位曲目。"),
    DemoMusicRecommendation("Night Track Marker", "AI Seed / Synthwave", 96, listOf("复古合成器", "夜跑", "推进感"), "节奏稳定，适合持续跑中段。"),
)
