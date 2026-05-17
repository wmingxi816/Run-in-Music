package com.runinmusic.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RunInMusicColors = lightColorScheme(
    primary = Color(0xFFFF7A1A),
    onPrimary = Color(0xFF101E1A),
    secondary = Color(0xFF56B870),
    background = Color(0xFFF6F1E6),
    surface = Color(0xFFFDF8EC),
    onSurface = Color(0xFF101E1A),
)

@Composable
fun RunInMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RunInMusicColors,
        content = content,
    )
}
