package com.daybreak.animelauncher.theming

import androidx.annotation.ColorInt

/**
 * Modelo inmutable de datos que representa la paleta de colores extraída
 * del análisis del wallpaper para el sistema de Adaptive Wallpaper Theming.
 *
 * @property dominantColor Color dominante de la imagen en formato ARGB Int.
 * @property vibrantColor Color vibrante principal extraído de la imagen (o su fallback más próximo).
 * @property mutedColor Color atenuado/apagado extraído de la imagen (o su fallback más próximo).
 * @property isDark Indica si el wallpaper analizado es predominantemente oscuro (luminancia < 0.5f).
 * @property averageLuminance Luminancia relativa promedio de la imagen en el rango [0.0f, 1.0f].
 */
data class ThemePalette(
    @get:ColorInt val dominantColor: Int,
    @get:ColorInt val vibrantColor: Int,
    @get:ColorInt val mutedColor: Int,
    val isDark: Boolean,
    val averageLuminance: Float
) {
    companion object {
        /**
         * Paleta de reserva neutra por defecto (oscura) con tonos característicos de NovaLauncher,
         * utilizada en caso de error de análisis o bitmap nulo/inválido.
         */
        val DEFAULT = ThemePalette(
            dominantColor = 0xFF121212.toInt(),
            vibrantColor = 0xFF00F0FF.toInt(), // Cyan cyberpunk característico
            mutedColor = 0xFF757575.toInt(),
            isDark = true,
            averageLuminance = 0.08f
        )
    }
}
