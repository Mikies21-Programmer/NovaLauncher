package com.daybreak.animelauncher.ui.theme

import androidx.compose.ui.graphics.Color

val NeonCyan = Color(0xFF00F0FF)
val NeonCyanDark = Color(0xFF00B8D4)
val ObsidianBlack = Color(0xFF050508)
val ObsidianDarkGrey = Color(0xFF101018)
val ObsidianCard = Color(0xFF08080C)

fun String.parseColorSafe(default: Color = NeonCyan): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        default
    }
}