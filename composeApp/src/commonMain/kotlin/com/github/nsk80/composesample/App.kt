package com.github.nsk80.composesample

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.navigator.Navigator

private val LightColors = lightColors(
    primary = Color(0xFF0277BD),
    primaryVariant = Color(0xFF004C8C),
    secondary = Color(0xFF00897B),
    secondaryVariant = Color(0xFF005B4F),
)

private val DarkColors = darkColors(
    primary = Color(0xFF4FC3F7),
    primaryVariant = Color(0xFF0288D1),
    secondary = Color(0xFF4DB6AC),
    surface = Color(0xFF1E1E1E),
    background = Color(0xFF121212),
)

@Composable
@Preview
fun App() {
    MaterialTheme(colors = if (isSystemInDarkTheme()) DarkColors else LightColors) {
        Navigator(StickManGameScreen())
    }
}
