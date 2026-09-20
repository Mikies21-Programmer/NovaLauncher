package com.daybreak.animelauncher.theming

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * CompositionLocal que provee [LauncherThemeTokens] a nivel global en la jerarquía de Compose.
 * Por defecto expone [LauncherThemeTokens.DEFAULT], garantizando seguridad en vistas previas y tests.
 */
val LocalLauncherThemeTokens = staticCompositionLocalOf { LauncherThemeTokens.DEFAULT }

/**
 * Acceso ergonómico en Compose a [LauncherThemeTokens.accentColor] como [Color].
 */
val LauncherThemeTokens.accent: Color
    get() = Color(accentColor)

/**
 * Acceso ergonómico en Compose a [LauncherThemeTokens.surfaceColor] como [Color].
 */
val LauncherThemeTokens.surface: Color
    get() = Color(surfaceColor)

/**
 * Acceso ergonómico en Compose a [LauncherThemeTokens.textPrimaryColor] como [Color].
 */
val LauncherThemeTokens.textPrimary: Color
    get() = Color(textPrimaryColor)

/**
 * Acceso ergonómico en Compose a [LauncherThemeTokens.textSecondaryColor] como [Color].
 */
val LauncherThemeTokens.textSecondary: Color
    get() = Color(textSecondaryColor)

/**
 * Acceso ergonómico en Compose a [LauncherThemeTokens.borderColor] como [Color].
 */
val LauncherThemeTokens.border: Color
    get() = Color(borderColor)
