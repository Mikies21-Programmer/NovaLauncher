package com.daybreak.animelauncher.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daybreak.animelauncher.ram.ActivityManagerRamProvider
import com.daybreak.animelauncher.ram.RAM_POLL_INTERVAL_MS
import com.daybreak.animelauncher.ram.RamFormat
import com.daybreak.animelauncher.ram.RamProvider
import com.daybreak.animelauncher.ram.RamSnapshot
import com.daybreak.animelauncher.ram.pollRam
import com.daybreak.animelauncher.theming.LauncherThemeTokens
import kotlinx.coroutines.delay

/**
 * UI-1 Fase 7: indicador compacto de RAM GLOBAL del teléfono para el sidebar.
 *
 * Ciclo de vida del polling: [pollActive] (página visible && launcher en
 * foreground) es la key del LaunchedEffect — al dejar de ser visible la página o
 * salir de RESUMED la corutina se CANCELA (sin ticks residuales); al regresar se
 * relanza con una lectura inmediata. Intervalo [pollIntervalMs] = 5 s por defecto.
 *
 * Estilo: tokens adaptativos existentes (cifra = textPrimary, resto =
 * textSecondary) sobre la superficie Glass del sidebar; sin contenedor ni motor
 * de color nuevo. Cifras sin animación.
 */
@Composable
fun RealTimeRam(
    pollActive: Boolean,
    tokens: LauncherThemeTokens,
    language: String,
    modifier: Modifier = Modifier,
    provider: RamProvider = rememberActivityManagerRamProvider(),
    pollIntervalMs: Long = RAM_POLL_INTERVAL_MS
) {
    var snapshot by remember { mutableStateOf<RamSnapshot?>(null) }

    LaunchedEffect(pollActive) {
        if (!pollActive) return@LaunchedEffect
        pollRam(provider = provider, tick = { delay(pollIntervalMs) }) { snapshot = it }
    }

    RealTimeRamContent(snapshot = snapshot, tokens = tokens, language = language, modifier = modifier)
}

@Composable
private fun rememberActivityManagerRamProvider(): RamProvider {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) { ActivityManagerRamProvider(appContext) }
}

/** Presentación pura: tres líneas compactas centradas, una línea por Text. */
@Composable
fun RealTimeRamContent(
    snapshot: RamSnapshot?,
    tokens: LauncherThemeTokens,
    language: String,
    modifier: Modifier = Modifier
) {
    val isEs = language != "en"
    val (primary, secondary) = RamFormat.widgetColors(tokens)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = "RAM",
            color = Color(secondary),
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
            maxLines = 1,
            softWrap = false
        )
        Spacer(modifier = Modifier.height(2.dp))
        AutoShrinkRamLine(
            text = RamFormat.usedOverTotal(snapshot),
            color = Color(primary),
            initialSp = 10,
            minSp = 7,
            fontWeight = FontWeight.Medium
        )
        val detail = RamFormat.detailLine(snapshot, isEs)
        if (detail.isNotEmpty()) {
            AutoShrinkRamLine(
                text = detail,
                color = Color(secondary),
                initialSp = 8,
                minSp = 6,
                fontWeight = FontWeight.Thin
            )
        }
    }
}

/**
 * Línea de una sola fila con reducción progresiva si desborda el ancho del
 * sidebar (mismo patrón que LogoAutoText de Fase 6; piso por legibilidad).
 * Sin wrap: un texto que no cabe se encoge, nunca se sale del sidebar ni
 * cambia de línea.
 */
@Composable
private fun AutoShrinkRamLine(
    text: String,
    color: Color,
    initialSp: Int,
    minSp: Int,
    fontWeight: FontWeight
) {
    var fontSize by remember(text) { mutableStateOf(initialSp.sp) }
    Text(
        text = text,
        color = color,
        fontWeight = fontWeight,
        fontSize = fontSize,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSize.value > minSp) {
                fontSize = (fontSize.value - 0.5f).coerceAtLeast(minSp.toFloat()).sp
            }
        }
    )
}
