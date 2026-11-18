package com.github.nsk90.composesample

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview


private val LightColors = lightColorScheme(
    primary = Color(0xFF0277BD),
    primaryContainer = Color(0xFF004C8C),
    secondary = Color(0xFF00897B),
    secondaryContainer = Color(0xFF005B4F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    primaryContainer = Color(0xFF0288D1),
    secondary = Color(0xFF4DB6AC),
    surface = Color(0xFF1E1E1E),
    background = Color(0xFF121212),
)

@Composable
@Preview
fun App() {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors) {
        StickManGameScreen()
    }
}
