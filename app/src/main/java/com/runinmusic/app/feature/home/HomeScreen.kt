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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runinmusic.app.core.model.RecommendedSong
import com.runinmusic.app.core.model.SongCandidate
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartMeasurement: () -> Unit,
    onOpenSong: (SongCandidate) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF6F1E6), Color(0xFFE7F2D8), Color(0xFF07110F)),
                ),
            ),
    ) {
        RhythmBackdrop()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                HeroCard(
                    state = state,
                    onStartMeasurement = onStartMeasurement,
                    onManualTap = viewModel::recordManualTap,
                )
            }
            item {
                Text(
                    text = "今日节奏推荐",
                    color = Color(0xFFFDF8EC),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            if (state.recommendations.isEmpty()) {
                item { EmptyRecommendationCard() }
            } else {
                items(state.recommendations, key = { it.song.id }) { recommendation ->
                    SongCard(
                        recommendation = recommendation,
                        onOpen = { onOpenSong(recommendation.song) },
                        onLike = { viewModel.markLiked(recommendation.song.id) },
                        onDislike = { viewModel.markDisliked(recommendation.song.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    state: HomeUiState,
    onStartMeasurement: () -> Unit,
    onManualTap: () -> Unit,
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
            Text(
                text = "Run in Music",
                color = Color(0xFFFF7A1A),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "让下一首歌跟上你的步频",
                color = Color(0xFFFDF8EC),
                fontSize = 34.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Black,
            )
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

            AnimatedVisibility(visible = state.isMeasuring) {
                Text(
                    text = "剩余 ${state.secondsRemaining}s",
                    color = Color(0xFFFFD36E),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                )
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
                    onClick = onStartMeasurement,
                ) {
                    Text(if (state.isMeasuring) "测量中" else "测量 10 秒步频", fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
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
                    Text("去听歌")
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
            Text(text = "完成一次 10 秒步频测量后，这里会出现匹配当前节奏的歌曲。", color = Color(0xFF557165))
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
