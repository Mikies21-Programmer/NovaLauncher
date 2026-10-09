package com.daybreak.animelauncher.theming

import androidx.annotation.ColorInt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daybreak.animelauncher.AdvancedStyleConfig

/** UI-1 Fase 6: modo de presentación del logo del launcher. */
enum class LogoMode {
    ICON_ONLY,
    TEXT_ONLY,
    ICON_PLUS_TEXT
}

/**
 * UI-1 Fase 6: catálogo estable de iconos predefinidos del logo.
 * Claves persistidas como String (Gson-safe); añadir entradas solo al final.
 */
object LogoCatalog {
    const val DEFAULT_ICON = "AutoAwesome"
    val KEYS = listOf(
        "AutoAwesome", "Star", "RocketLaunch", "Bolt",
        "Home", "Apps", "Palette", "Settings"
    )

    fun vector(key: String): ImageVector = when (key) {
        "Star" -> Icons.Outlined.Star
        "RocketLaunch" -> Icons.Outlined.RocketLaunch
        "Bolt" -> Icons.Outlined.Bolt
        "Home" -> Icons.Outlined.Home
        "Apps" -> Icons.Outlined.Apps
        "Palette" -> Icons.Outlined.Palette
        "Settings" -> Icons.Outlined.Settings
        else -> Icons.Outlined.AutoAwesome
    }
}

/** Longitud máxima del texto del logo. */
const val LOGO_TEXT_MAX_LENGTH = 10

/**
 * Validación del texto del logo para Settings.
 * @return null si el texto es válido; mensaje de error en caso contrario.
 * Reglas: sin wrap (máx [LOGO_TEXT_MAX_LENGTH] caracteres) y no solo espacios.
 * El campo vacío es válido (representa "sin texto").
 */
fun logoTextError(text: String, isEs: Boolean = true): String? {
    if (text.isEmpty()) return null
    if (text.isBlank()) return if (isEs) "El texto no puede contener solo espacios" else "Text cannot be only spaces"
    if (text.length > LOGO_TEXT_MAX_LENGTH) {
        return if (isEs) "Máximo $LOGO_TEXT_MAX_LENGTH caracteres" else "Max $LOGO_TEXT_MAX_LENGTH characters"
    }
    return null
}

/**
 * Sanitización runtime (defensa final en el composable): trim y recorte a 10.
 * Un texto solo-espacios se normaliza a vacío.
 */
fun sanitizeLogoText(text: String): String {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return ""
    return trimmed.take(LOGO_TEXT_MAX_LENGTH)
}

/** Tamaño tipográfico inicial (sp) según longitud — reducción progresiva (Fase 6). */
fun logoInitialFontSizeSp(length: Int): Int = when (length) {
    in 0..4 -> 14
    in 5..7 -> 12
    else -> 10
}

/** Piso tipográfico del auto-shrink: por debajo, legibilidad > encaje. */
const val LOGO_FONT_MIN_SP = 8

/**
 * UI-1 Fase 6: especificación resuelta del logo (pura, testeable).
 *
 * Colores (regla absoluta): el glifo/texto principal usa textPrimary; el círculo
 * usa accent como identidad/active state; el glifo SOBRE el círculo usa
 * textOnAccent. El accent jamás es el color del texto principal.
 */
data class LauncherLogoSpec(
    val mode: LogoMode,
    val icon: ImageVector?,
    val text: String,
    val showCircle: Boolean,
    @get:ColorInt val circleColor: Int,
    @get:ColorInt val glyphColor: Int,
    @get:ColorInt val textColor: Int
)

/**
 * UI-1 Fase 6: resolución pura del logo a partir de la configuración persistente
 * y los tokens del tema activo (MANUAL o AUTO).
 *
 * Fallback: TEXT_ONLY con texto vacío degrada a ICON_ONLY con el icono default
 * (un logo de texto invisible no es una presentación válida).
 */
object LauncherLogoResolver {
    fun resolve(styleConfig: AdvancedStyleConfig, tokens: LauncherThemeTokens): LauncherLogoSpec {
        val mode = styleConfig.logoMode
        val icon = LogoCatalog.vector(styleConfig.logoIcon)
        val text = sanitizeLogoText(styleConfig.logoText)

        // Color del círculo (identidad/active state): en AUTO* es el accent del tema;
        // en MANUAL es el color histórico del usuario (miButtonColor, mismo patrón
        // que el engine aplica a elementColors.miButton) con el accent como fallback.
        val circleColor = if (styleConfig.themeMode == ThemeMode.MANUAL) {
            LauncherThemeTokens.parseHexColor(styleConfig.miButtonColor, tokens.accentColor)
        } else {
            tokens.accentColor
        }

        return when (mode) {
            LogoMode.ICON_ONLY -> LauncherLogoSpec(
                mode = mode, icon = icon, text = "", showCircle = true,
                circleColor = circleColor,
                glyphColor = tokens.textOnAccentColor,
                textColor = tokens.textPrimaryColor
            )
            LogoMode.TEXT_ONLY ->
                if (text.isEmpty()) {
                    LauncherLogoSpec(
                        mode = LogoMode.ICON_ONLY, icon = LogoCatalog.vector(LogoCatalog.DEFAULT_ICON),
                        text = "", showCircle = true,
                        circleColor = circleColor,
                        glyphColor = tokens.textOnAccentColor,
                        textColor = tokens.textPrimaryColor
                    )
                } else {
                    LauncherLogoSpec(
                        mode = mode, icon = null, text = text, showCircle = false,
                        circleColor = circleColor,
                        glyphColor = tokens.textOnAccentColor,
                        textColor = tokens.textPrimaryColor
                    )
                }
            LogoMode.ICON_PLUS_TEXT ->
                LauncherLogoSpec(
                    mode = mode, icon = icon, text = text, showCircle = true,
                    circleColor = circleColor,
                    glyphColor = tokens.textOnAccentColor,
                    textColor = tokens.textPrimaryColor
                )
        }
    }
}

/**
 * UI-1 Fase 6: composable del logo. Sustituye al elemento superior "mi" del
 * sidebar preservando geometría (círculo 45 dp en ICON_ONLY / ICON_PLUS_TEXT,
 * con el texto bajo el círculo en ICON_PLUS_TEXT).
 *
 * Colores: círculo = accent (tokens) / histórico en MANUAL, glifo = textOnAccent,
 * texto = textPrimary. El texto nunca usa accent.
 *
 * Auto-size del texto: tamaño inicial por longitud ([logoInitialFontSizeSp]) y
 * reducción progresiva si el layout reporta overflow real contra el ancho del
 * sidebar (una línea garantizada; piso [LOGO_FONT_MIN_SP] sp). El texto ya
 * llega sanitizado desde el resolver.
 */
@Composable
fun LauncherLogo(
    spec: LauncherLogoSpec,
    onClick: () -> Unit
) {
    when (spec.mode) {
        LogoMode.TEXT_ONLY -> {
            LogoAutoText(
                text = spec.text,
                color = Color(spec.textColor),
                modifier = Modifier.clickable(onClick = onClick)
            )
        }
        LogoMode.ICON_ONLY -> {
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .background(Color(spec.circleColor), shape = CircleShape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = spec.icon ?: LogoCatalog.vector(LogoCatalog.DEFAULT_ICON),
                    contentDescription = "Launcher logo",
                    tint = Color(spec.glyphColor)
                )
            }
        }
        LogoMode.ICON_PLUS_TEXT -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(spec.circleColor), shape = CircleShape)
                        .clickable(onClick = onClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = spec.icon ?: LogoCatalog.vector(LogoCatalog.DEFAULT_ICON),
                        contentDescription = "Launcher logo",
                        tint = Color(spec.glyphColor)
                    )
                }
                if (spec.text.isNotEmpty()) {
                    LogoAutoText(text = spec.text, color = Color(spec.textColor))
                }
            }
        }
    }
}

/** Texto del logo: Medium, tracking 1.5 sp, una línea, sin wrap, auto-shrink por overflow. */
@Composable
private fun LogoAutoText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    var fontSize by remember(text) {
        mutableStateOf(logoInitialFontSizeSp(text.length).sp)
    }
    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.Medium,
        fontSize = fontSize,
        letterSpacing = 1.5.sp,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSize > LOGO_FONT_MIN_SP.sp) {
                fontSize = (fontSize.value.toInt() - 1).coerceAtLeast(LOGO_FONT_MIN_SP).sp
            }
        },
        modifier = modifier
    )
}
