package com.daybreak.animelauncher.theming

import androidx.annotation.ColorInt
import com.daybreak.animelauncher.AdvancedStyleConfig

/**
 * Modelo inmutable puro de tokens visuales runtime para NovaLauncher.
 *
 * Diseñado para operar desacoplado de Compose y del framework de Android,
 * permitiendo transformaciones seguras y deterministas en pruebas unitarias y capas de background.
 *
 * @property accentColor Color de acento / resalte primario en formato ARGB Int.
 * @property surfaceColor Color de superficie base / contenedores oscuros en formato ARGB Int.
 * @property textPrimaryColor Color de texto principal de alto contraste en formato ARGB Int.
 * @property textSecondaryColor Color de texto secundario / atenuado en formato ARGB Int.
 * @property borderColor Color de bordes y delimitadores visuales en formato ARGB Int.
 * @property textOnAccentColor UI-1 Fase 1: texto sobre superficies de acento (p. ej. botón "mi").
 * @property statusColor UI-1 Fase 1: color de los status widgets bajo el tratamiento actual (acento).
 */
data class LauncherThemeTokens(
    @get:ColorInt val accentColor: Int,
    @get:ColorInt val surfaceColor: Int,
    @get:ColorInt val textPrimaryColor: Int,
    @get:ColorInt val textSecondaryColor: Int,
    @get:ColorInt val borderColor: Int,
    @get:ColorInt val textOnAccentColor: Int = DEFAULT_TEXT_ON_ACCENT,
    @get:ColorInt val statusColor: Int = accentColor
) {
    companion object {
        /** Texto por defecto sobre superficies de acento: negro (el acento cyberpunk es luminoso). */
        const val DEFAULT_TEXT_ON_ACCENT = 0xFF000000.toInt()

        /**
         * Tokens por defecto correspondientes a la estética cyberpunk original de NovaLauncher.
         * Garantiza 100% de paridad visual con las versiones anteriores.
         */
        val DEFAULT = LauncherThemeTokens(
            accentColor = 0xFF00F0FF.toInt(),       // Cyan Neón característico (#00F0FF)
            surfaceColor = 0xFF08080C.toInt(),      // ObsidianCard (#08080C)
            textPrimaryColor = 0xFFFFFFFF.toInt(),  // Blanco puro (#FFFFFF)
            textSecondaryColor = 0xFFCCCCCC.toInt(),// Gris claro / LightGray (#CCCCCC)
            borderColor = 0xFF00F0FF.toInt()        // Cyan Neón característico (#00F0FF)
        )

        /**
         * Parsea cadenas hexadecimales (#RRGGBB, #AARRGGBB, RRGGBB) de forma segura y pura en Kotlin,
         * retornando un entero ARGB de 32 bits sin requerir APIs de Android ni Compose.
         */
        fun parseHexColor(hex: String?, fallback: Int): Int {
            if (hex.isNullOrBlank()) return fallback
            return try {
                val clean = hex.removePrefix("#").trim()
                when (clean.length) {
                    6 -> (0xFF000000 or clean.toLong(16)).toInt()
                    8 -> clean.toLong(16).toInt()
                    else -> fallback
                }
            } catch (_: Exception) {
                fallback
            }
        }

        /**
         * Sintetiza tokens de tema runtime a partir del estado actual de [AdvancedStyleConfig].
         * Mantiene retrocompatibilidad total extrayendo el acento configurado con fallback robusto.
         * UI-1 Fase 1: `textOnAccentColor` refleja el texto sobre el botón "mi" (paridad MANUAL)
         * y `statusColor` refleja el tratamiento actual de los status widgets (acento).
         */
        fun fromAdvancedStyleConfig(config: AdvancedStyleConfig): LauncherThemeTokens {
            val accent = parseHexColor(config.accentColor, DEFAULT.accentColor)
            return LauncherThemeTokens(
                accentColor = accent,
                surfaceColor = DEFAULT.surfaceColor,
                textPrimaryColor = DEFAULT.textPrimaryColor,
                textSecondaryColor = DEFAULT.textSecondaryColor,
                borderColor = accent,
                textOnAccentColor = parseHexColor(config.miTextColor, DEFAULT_TEXT_ON_ACCENT),
                statusColor = accent
            )
        }
    }
}
