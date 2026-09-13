package com.daybreak.animelauncher.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonCyanDark,
    tertiary = NeonCyan,
    background = ObsidianBlack,
    surface = ObsidianCard,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = NeonCyan,
    secondary = NeonCyanDark,
    tertiary = NeonCyan,
    background = ObsidianBlack,
    surface = ObsidianCard,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun AnimeLauncherTheme(
    darkTheme: Boolean = true, // Siempre modo oscuro ciberpunk
    dynamicColor: Boolean = false, // Desactivado para mantener el color Neón unificado
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}