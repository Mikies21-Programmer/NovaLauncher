package com.daybreak.animelauncher.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daybreak.animelauncher.AdvancedStyleConfig
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedTextField
import com.daybreak.animelauncher.theming.AdaptiveColorOverrides
import com.daybreak.animelauncher.theming.LauncherThemeTokens
import com.daybreak.animelauncher.theming.LocalLauncherThemeTokens
import com.daybreak.animelauncher.theming.ThemeMode
import com.daybreak.animelauncher.ui.components.DynamicBackground
import com.daybreak.animelauncher.ui.theme.parseColorSafe

@Composable
fun ColorSettingItem(
    title: String,
    description: String,
    currentColorHex: String,
    onColorChange: (String) -> Unit
) {
    val presets = listOf(
        "#00F0FF", "#00FF66", "#FF007F", "#FFD700", "#9D00FF",
        "#FF3300", "#FFFFFF", "#08080C", "#050508", "#00B8D4"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Normal)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.LightGray, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(currentColorHex.parseColorSafe())
                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                )
                Text(text = currentColorHex.uppercase(), color = Color(0xFF00F0FF), fontSize = 12.sp, fontWeight = FontWeight.Light)
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presets) { hex ->
                val isSelected = currentColorHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(hex.parseColorSafe())
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onColorChange(hex) }
                )
            }
        }
    }
}

@Composable
fun SliderSettingItem(
    title: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    displayValue: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Normal)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.LightGray, fontSize = 11.sp)
            }
            Text(text = displayValue, color = Color(0xFF00F0FF), fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00F0FF),
                activeTrackColor = Color(0xFF00F0FF),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

/**
 * UI-1 Subfase 8B: Componente de excepción de color adaptativo por elemento.
 *
 * Semántica:
 * - null = elemento utiliza su color adaptativo según el fondo.
 * - HEX = excepción explícita que reemplaza el color generado en AUTO_WITH_OVERRIDES.
 * - onResetToAuto restablece el campo estrictamente a null.
 */
@Composable
fun OverrideColorItem(
    title: String,
    description: String,
    currentOverrideHex: String?,
    isEs: Boolean,
    onColorChange: (String) -> Unit,
    onResetToAuto: () -> Unit
) {
    val presets = listOf(
        "#00F0FF", "#00FF66", "#FF007F", "#FFD700", "#9D00FF",
        "#FF3300", "#FFFFFF", "#08080C", "#050508", "#00B8D4"
    )
    val isAuto = currentOverrideHex == null

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Normal)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.LightGray, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isAuto) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF00F0FF).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isEs) "Automático" else "Auto",
                            color = Color(0xFF00F0FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(currentOverrideHex!!.parseColorSafe())
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    )
                    Text(
                        text = currentOverrideHex.uppercase(),
                        color = Color(0xFF00F0FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable { onResetToAuto() }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isEs) "Restablecer" else "Reset",
                            color = Color(0xFFFF7043),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isAuto) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.08f))
                        .clickable { onResetToAuto() }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Auto",
                        color = if (isAuto) Color(0xFF050508) else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            items(presets) { hex ->
                val isSelected = !isAuto && currentOverrideHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(hex.parseColorSafe())
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onColorChange(hex) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF00F0FF),
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    styleConfig: AdvancedStyleConfig,
    backgroundUri: String,
    isEs: Boolean,
    onUpdateTransient: (AdvancedStyleConfig) -> Unit,
    onPersist: (AdvancedStyleConfig) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    var currentStyle by androidx.compose.runtime.remember(styleConfig) { androidx.compose.runtime.mutableStateOf(styleConfig) }

    val handleReset = {
        currentStyle = AdvancedStyleConfig()
        onReset()
    }

    val currentTokens = androidx.compose.runtime.remember(currentStyle) {
        LauncherThemeTokens.fromAdvancedStyleConfig(currentStyle)
    }

    CompositionLocalProvider(
        LocalAdvancedStyleConfig provides currentStyle,
        LocalLauncherThemeTokens provides currentTokens
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DynamicBackground(
                defaultVideoResId = com.daybreak.animelauncher.R.raw.bg_view_one,
                backgroundUri = backgroundUri,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050508).copy(alpha = 0.45f)))

            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (isEs) "Opciones Avanzadas de Diseño" else "Advanced Design Options",
                                color = Color.White,
                                fontWeight = FontWeight.Light,
                                fontSize = 20.sp
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00F0FF))
                            }
                        },
                        actions = {
                            IconButton(onClick = handleReset) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Reset", tint = Color(0xFF00F0FF))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    // Botón de restauración rápida
                    item {
                        GlassCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEs) "Restaurar Diseño Original" else "Reset to Original Design",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isEs) "Vuelve a los tonos Cian Neón y Obsidiana por defecto" else "Revert to default Neon Cyan and Obsidian tones",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )
                                }
                                Button(
                                    onClick = handleReset,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00F0FF).copy(alpha = 0.25f),
                                        contentColor = Color(0xFF00F0FF)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(if (isEs) "Restaurar" else "Reset", fontWeight = FontWeight.Normal)
                                }
                            }
                        }
                    }

                    // =========================================================================
                    // GRUPO 1: MODO DE TEMA
                    // =========================================================================
                    item {
                        SectionHeader(if (isEs) "1. MODO DE TEMA" else "1. THEME MODE")
                    }
                    item {
                        GlassCard {
                            Text(
                                text = if (isEs) "Selección de Modo" else "Mode Selection",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val modes = listOf(
                                    Triple(
                                        if (isEs) "Manual" else "Manual",
                                        ThemeMode.MANUAL,
                                        if (isEs) "Colores manuales fijos definidos por el usuario." else "Fixed manual colors defined by the user."
                                    ),
                                    Triple(
                                        if (isEs) "Automático" else "Adaptive",
                                        ThemeMode.AUTO,
                                        if (isEs) "Colores adaptativos derivados automáticamente del fondo con contraste WCAG." else "Adaptive colors derived from wallpaper with WCAG contrast."
                                    ),
                                    Triple(
                                        if (isEs) "Auto + Excepciones" else "Auto + Overrides",
                                        ThemeMode.AUTO_WITH_OVERRIDES,
                                        if (isEs) "Base adaptativa del fondo con excepciones manuales para elementos clave." else "Adaptive background base with explicit manual overrides for key elements."
                                    )
                                )

                                modes.forEach { (label, mode, _) ->
                                    val selected = currentStyle.themeMode == mode
                                    Text(
                                        text = label,
                                        color = if (selected) Color(0xFF050508) else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.08f))
                                            .clickable {
                                                if (currentStyle.themeMode != mode) {
                                                    currentStyle = currentStyle.copy(themeMode = mode)
                                                    onPersist(currentStyle)
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val modeDesc = when (currentStyle.themeMode) {
                                ThemeMode.MANUAL -> if (isEs) "Colores manuales fijos definidos por el usuario." else "Fixed manual colors defined by the user."
                                ThemeMode.AUTO -> if (isEs) "Colores adaptativos derivados automáticamente del fondo con contraste WCAG." else "Adaptive colors derived from wallpaper with WCAG contrast."
                                ThemeMode.AUTO_WITH_OVERRIDES -> if (isEs) "Base adaptativa del fondo con excepciones manuales para elementos clave." else "Adaptive background base with explicit manual overrides for key elements."
                            }

                            Text(
                                text = modeDesc,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // =========================================================================
                    // GRUPO 2: COLORES Y PERSONALIZACIÓN (CONDICIONAL POR THEME_MODE)
                    // =========================================================================
                    item {
                        SectionHeader(if (isEs) "2. COLORES Y PERSONALIZACIÓN" else "2. COLORS & CUSTOMIZATION")
                    }

                    when (currentStyle.themeMode) {
                        ThemeMode.MANUAL -> {
                            item {
                                GlassCard {
                                    Text(
                                        text = if (isEs) "Colores Manuales Fijos" else "Fixed Manual Colors",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                    Text(
                                        text = if (isEs) "En modo Manual, los elementos conservan exactamente los tonos fijos configurados aquí."
                                               else "In Manual mode, elements retain the exact fixed colors configured here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    ColorSettingItem(
                                        title = if (isEs) "Botón Logo 'mi' (Fondo)" else "'mi' Logo Button (Bg)",
                                        description = if (isEs) "Color del círculo superior en la barra lateral" else "Top circle color in sidebar",
                                        currentColorHex = currentStyle.miButtonColor,
                                        onColorChange = {
                                            currentStyle = currentStyle.copy(miButtonColor = it)
                                            onPersist(currentStyle)
                                        }
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                                    ColorSettingItem(
                                        title = if (isEs) "Texto Logo 'mi'" else "'mi' Logo Text",
                                        description = if (isEs) "Color de las letras dentro del botón" else "Letter color inside the button",
                                        currentColorHex = currentStyle.miTextColor,
                                        onColorChange = {
                                            currentStyle = currentStyle.copy(miTextColor = it)
                                            onPersist(currentStyle)
                                        }
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                                    ColorSettingItem(
                                        title = if (isEs) "Reloj / Hora" else "Clock / Time",
                                        description = if (isEs) "Color de la hora digital en la barra lateral" else "Digital clock color in sidebar",
                                        currentColorHex = currentStyle.clockColor,
                                        onColorChange = {
                                            currentStyle = currentStyle.copy(clockColor = it)
                                            onPersist(currentStyle)
                                        }
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                                    ColorSettingItem(
                                        title = if (isEs) "Batería (Icono y Porcentaje)" else "Battery (Icon & %)",
                                        description = if (isEs) "Color del indicador de batería" else "Battery indicator color",
                                        currentColorHex = currentStyle.batteryColor,
                                        onColorChange = {
                                            currentStyle = currentStyle.copy(batteryColor = it)
                                            onPersist(currentStyle)
                                        }
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                                    ColorSettingItem(
                                        title = if (isEs) "Texto 'Mensajes'" else "'Messages' Text",
                                        description = if (isEs) "Color del contador de mensajes" else "Message counter text color",
                                        currentColorHex = currentStyle.messagesColor,
                                        onColorChange = {
                                            currentStyle = currentStyle.copy(messagesColor = it)
                                            onPersist(currentStyle)
                                        }
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                                    ColorSettingItem(
                                        title = if (isEs) "Fecha y Día (Vista 1)" else "Date & Day (View 1)",
                                        description = if (isEs) "Color del día y fecha en el triángulo" else "Day & date color in triangle",
                                        currentColorHex = currentStyle.dateColor,
                                        onColorChange = {
                                            currentStyle = currentStyle.copy(dateColor = it)
                                            onPersist(currentStyle)
                                        }
                                    )
                                }
                            }
                        }
                        ThemeMode.AUTO -> {
                            item {
                                GlassCard {
                                    Text(
                                        text = if (isEs) "Colores Adaptativos Activos" else "Adaptive Colors Active",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    Text(
                                        text = if (isEs)
                                            "En modo Automático, los colores del reloj, batería, mensajes, fecha e indicadores se calculan dinámicamente a partir del fondo de pantalla de cada página, garantizando contraste y legibilidad según los estándares WCAG.\n\nLos selectores manuales de texto e iconos están desactivados en este modo para evitar inconsistencias de contraste. Las excepciones personalizadas que hayas configurado se conservan intactas para cuando cambies al modo Auto + Excepciones."
                                        else
                                            "In Adaptive mode, colors for clock, battery, messages, date and indicators are dynamically derived from each page's wallpaper, ensuring WCAG contrast and readability.\n\nManual text and icon pickers are disabled in this mode to avoid contrast inconsistencies. Any custom overrides you configured remain intact for when you switch to Auto + Overrides.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                        ThemeMode.AUTO_WITH_OVERRIDES -> {
                            item {
                                GlassCard {
                                    Text(
                                        text = if (isEs) "Excepciones de Color (Modo Auto + Excepciones)" else "Color Overrides (Auto + Overrides)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    Text(
                                        text = if (isEs)
                                            "Los elementos con estado 'Auto' adoptan dinámicamente el color del fondo. Si asignas un color personalizado a un elemento, anulará el cálculo automático para ese elemento específico. Ten en cuenta que los colores personalizados elegidos manualmente pueden reducir la legibilidad sobre ciertos fondos."
                                        else
                                            "Elements set to 'Auto' dynamically adopt the wallpaper color. Setting a custom color overrides automatic adaptation for that specific element. Note that manually chosen colors may reduce readability on certain wallpapers.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    OverrideColorItem(
                                        title = if (isEs) "Reloj / Hora" else "Clock / Time",
                                        description = if (isEs) "Hora digital en la barra lateral" else "Digital clock in sidebar",
                                        currentOverrideHex = currentStyle.adaptiveOverrides.clockColor,
                                        isEs = isEs,
                                        onColorChange = { hex ->
                                            currentStyle = currentStyle.copy(
                                                adaptiveOverrides = currentStyle.adaptiveOverrides.copy(clockColor = hex)
                                            )
                                            onPersist(currentStyle)
                                        },
                                        onResetToAuto = {
                                            currentStyle = currentStyle.copy(
                                                adaptiveOverrides = currentStyle.adaptiveOverrides.copy(clockColor = null)
                                            )
                                            onPersist(currentStyle)
                                        }
                                    )

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                                    OverrideColorItem(
                                        title = if (isEs) "Batería (Icono y %)" else "Battery (Icon & %)",
                                        description = if (isEs) "Indicador de batería en la barra lateral" else "Battery indicator in sidebar",
                                        currentOverrideHex = currentStyle.adaptiveOverrides.batteryColor,
                                        isEs = isEs,
                                        onColorChange = { hex ->
                                            currentStyle = currentStyle.copy(
                                                adaptiveOverrides = currentStyle.adaptiveOverrides.copy(batteryColor = hex)
                                            )
                                            onPersist(currentStyle)
                                        },
                                        onResetToAuto = {
                                            currentStyle = currentStyle.copy(
                                                adaptiveOverrides = currentStyle.adaptiveOverrides.copy(batteryColor = null)
                                            )
                                            onPersist(currentStyle)
                                        }
                                    )

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                                    OverrideColorItem(
                                        title = if (isEs) "Texto 'Mensajes'" else "'Messages' Text",
                                        description = if (isEs) "Contador de mensajes en la barra lateral" else "Messages counter in sidebar",
                                        currentOverrideHex = currentStyle.adaptiveOverrides.messagesColor,
                                        isEs = isEs,
                                        onColorChange = { hex ->
                                            currentStyle = currentStyle.copy(
                                                adaptiveOverrides = currentStyle.adaptiveOverrides.copy(messagesColor = hex)
                                            )
                                            onPersist(currentStyle)
                                        },
                                        onResetToAuto = {
                                            currentStyle = currentStyle.copy(
                                                adaptiveOverrides = currentStyle.adaptiveOverrides.copy(messagesColor = null)
                                            )
                                            onPersist(currentStyle)
                                        }
                                    )

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                                    OverrideColorItem(
                                        title = if (isEs) "Fecha y Día" else "Date & Day",
                                        description = if (isEs) "Fecha y día en el triángulo de Vista 1" else "Date and day in View 1 triangle",
                                        currentOverrideHex = currentStyle.adaptiveOverrides.dateColor,
                                        isEs = isEs,
                                        onColorChange = { hex ->
                                            currentStyle = currentStyle.copy(
                                                adaptiveOverrides = currentStyle.adaptiveOverrides.copy(dateColor = hex)
                                            )
                                            onPersist(currentStyle)
                                        },
                                        onResetToAuto = {
                                            currentStyle = currentStyle.copy(
                                                adaptiveOverrides = currentStyle.adaptiveOverrides.copy(dateColor = null)
                                            )
                                            onPersist(currentStyle)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // =========================================================================
                    // GRUPO 3: APARIENCIA DEL CRISTAL (GLASS)
                    // =========================================================================
                    item {
                        SectionHeader(if (isEs) "3. APARIENCIA DEL CRISTAL (GLASS)" else "3. GLASS APPEARANCE")
                    }
                    item {
                        GlassCard {
                            ColorSettingItem(
                                title = if (isEs) "Color de Acento Neón" else "Neon Accent Color",
                                description = if (isEs) "Tono principal para bordes, brillos y destaques" else "Primary tone for borders, glows and highlights",
                                currentColorHex = currentStyle.accentColor,
                                onColorChange = {
                                    currentStyle = currentStyle.copy(accentColor = it)
                                    onPersist(currentStyle)
                                }
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                            SliderSettingItem(
                                title = if (isEs) "Transparencia de Paneles de Ajustes" else "Settings Panel Transparency",
                                description = if (isEs) "Nivel de opacidad de tarjetas y paneles en las pantallas de configuración" else "Opacity level for cards and panels in settings screens",
                                value = currentStyle.panelTransparency,
                                valueRange = 0.20f..1.00f,
                                displayValue = "${(currentStyle.panelTransparency * 100).toInt()}%",
                                onValueChange = {
                                    currentStyle = currentStyle.copy(panelTransparency = it)
                                    onUpdateTransient(currentStyle)
                                },
                                onValueChangeFinished = { onPersist(currentStyle) }
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                            SliderSettingItem(
                                title = if (isEs) "Brillo de Borde Glass" else "Glass Border Glow",
                                description = if (isEs) "Intensidad del contorno cristalino en tarjetas y paneles Glass" else "Intensity of crystal outline in Glass cards and panels",
                                value = currentStyle.glassBorderAlpha,
                                valueRange = 0.05f..0.60f,
                                displayValue = "${(currentStyle.glassBorderAlpha * 100).toInt()}%",
                                onValueChange = {
                                    currentStyle = currentStyle.copy(glassBorderAlpha = it)
                                    onUpdateTransient(currentStyle)
                                },
                                onValueChangeFinished = { onPersist(currentStyle) }
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                            SliderSettingItem(
                                title = if (isEs) "Redondeo de Esquinas" else "Corner Rounding",
                                description = if (isEs) "Curvatura de los paneles y tarjetas Glass" else "Curvature of Glass panels and cards",
                                value = currentStyle.cornerRadius,
                                valueRange = 8f..32f,
                                displayValue = "${currentStyle.cornerRadius.toInt()} dp",
                                onValueChange = {
                                    currentStyle = currentStyle.copy(cornerRadius = it)
                                    onUpdateTransient(currentStyle)
                                },
                                onValueChangeFinished = { onPersist(currentStyle) }
                            )
                        }
                    }

                    // =========================================================================
                    // GRUPO 4: LOGO DEL LAUNCHER
                    // =========================================================================
                    item {
                        SectionHeader(if (isEs) "4. LOGO DEL LAUNCHER" else "4. LAUNCHER LOGO")
                    }
                    item {
                        GlassCard {
                            var logoTextDraft by androidx.compose.runtime.remember {
                                androidx.compose.runtime.mutableStateOf(currentStyle.logoText)
                            }
                            val logoError = com.daybreak.animelauncher.theming.logoTextError(logoTextDraft, isEs)
                            val logoCount = logoTextDraft.length

                            Text(
                                text = if (isEs) "Configuración del Logo" else "Logo Configuration",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val modes: List<Triple<String, com.daybreak.animelauncher.theming.LogoMode, androidx.compose.ui.graphics.vector.ImageVector>> = listOf(
                                    Triple("Icono", com.daybreak.animelauncher.theming.LogoMode.ICON_ONLY, Icons.Outlined.AutoAwesome),
                                    Triple("Texto", com.daybreak.animelauncher.theming.LogoMode.TEXT_ONLY, Icons.Outlined.TextFields),
                                    Triple("Ambos", com.daybreak.animelauncher.theming.LogoMode.ICON_PLUS_TEXT, Icons.Outlined.AutoAwesome)
                                )
                                modes.forEach { (label, mode, _) ->
                                    val selected = currentStyle.logoMode == mode
                                    Text(
                                        text = label,
                                        color = if (selected) Color(0xFF050508) else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                            .background(if (selected) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.08f))
                                            .clickable {
                                                currentStyle = currentStyle.copy(logoMode = mode)
                                                onPersist(currentStyle)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (currentStyle.logoMode != com.daybreak.animelauncher.theming.LogoMode.ICON_ONLY) {
                                OutlinedTextField(
                                    value = logoTextDraft,
                                    onValueChange = { input ->
                                        logoTextDraft = input
                                        if (com.daybreak.animelauncher.theming.logoTextError(input, isEs) == null) {
                                            val sanitized = com.daybreak.animelauncher.theming.sanitizeLogoText(input)
                                            currentStyle = currentStyle.copy(logoText = sanitized)
                                            onPersist(currentStyle)
                                        }
                                    },
                                    isError = logoError != null,
                                    label = { Text(if (isEs) "Texto del logo" else "Logo text") },
                                    supportingText = {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = logoError ?: "",
                                                color = Color(0xFFFF5252),
                                                fontSize = 11.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "$logoCount/10",
                                                color = if (logoCount > 10) Color(0xFFFF5252) else Color.White.copy(alpha = 0.5f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            Text(
                                text = if (isEs) "Icono" else "Icon",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                com.daybreak.animelauncher.theming.LogoCatalog.KEYS.forEach { key ->
                                    val vector = com.daybreak.animelauncher.theming.LogoCatalog.vector(key)
                                    val selected = currentStyle.logoIcon == key
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                            .background(if (selected) Color(0xFF00F0FF).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f))
                                            .clickable {
                                                currentStyle = currentStyle.copy(logoIcon = key)
                                                onPersist(currentStyle)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = vector,
                                            contentDescription = key,
                                            tint = if (selected) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // =========================================================================
                    // GRUPO 5: INDICADORES Y BARRAS DEL LAUNCHER
                    // =========================================================================
                    item {
                        SectionHeader(if (isEs) "5. INDICADORES Y BARRAS" else "5. INDICATORS & BARS")
                    }

                    // 5.1 RAM global
                    item {
                        GlassCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = if (isEs) "Indicador de RAM del dispositivo" else "Device RAM Indicator",
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (isEs) "Muestra la memoria RAM total y usada por el dispositivo en la barra lateral"
                                               else "Shows device-wide total and used RAM in the sidebar",
                                        color = Color.White.copy(alpha = 0.55f),
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = currentStyle.showRamWidget,
                                    onCheckedChange = { checked ->
                                        currentStyle = currentStyle.copy(showRamWidget = checked)
                                        onPersist(currentStyle)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = Color(0xFF00F0FF),
                                        checkedThumbColor = Color(0xFF050508)
                                    )
                                )
                            }
                        }
                    }

                    // 5.2 Barras Laterales y Diagonales
                    item {
                        GlassCard {
                            Text(
                                text = if (isEs) "Barra Lateral (Izquierda/Derecha)" else "Sidebar (Left/Right)",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            SliderSettingItem(
                                title = if (isEs) "Opacidad de Barra Lateral" else "Sidebar Opacity",
                                description = if (isEs) "Transparencia del fondo cristalino" else "Frosted glass transparency",
                                value = currentStyle.sidebarOpacity,
                                valueRange = 0.25f..1.00f,
                                displayValue = "${(currentStyle.sidebarOpacity * 100).toInt()}%",
                                onValueChange = {
                                    currentStyle = currentStyle.copy(sidebarOpacity = it)
                                    onUpdateTransient(currentStyle)
                                },
                                onValueChangeFinished = { onPersist(currentStyle) }
                            )
                            ColorSettingItem(
                                title = if (isEs) "Color de Barra Lateral" else "Sidebar Color",
                                description = if (isEs) "Color base de la barra lateral" else "Sidebar base color",
                                currentColorHex = currentStyle.sidebarColor,
                                onColorChange = {
                                    currentStyle = currentStyle.copy(sidebarColor = it)
                                    onPersist(currentStyle)
                                }
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = if (isEs) "Barra Diagonal y Accesos" else "Diagonal Bar & Shortcuts",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            SliderSettingItem(
                                title = if (isEs) "Opacidad de Barra Diagonal" else "Diagonal Bar Opacity",
                                description = if (isEs) "Transparencia de la barra diagonal" else "Diagonal bar transparency",
                                value = currentStyle.diagonalBarOpacity,
                                valueRange = 0.25f..1.00f,
                                displayValue = "${(currentStyle.diagonalBarOpacity * 100).toInt()}%",
                                onValueChange = {
                                    currentStyle = currentStyle.copy(diagonalBarOpacity = it)
                                    onUpdateTransient(currentStyle)
                                },
                                onValueChangeFinished = { onPersist(currentStyle) }
                            )
                            ColorSettingItem(
                                title = if (isEs) "Color de Barra Diagonal" else "Diagonal Bar Color",
                                description = if (isEs) "Color base de la banda diagonal" else "Diagonal bar base color",
                                currentColorHex = currentStyle.diagonalBarColor,
                                onColorChange = {
                                    currentStyle = currentStyle.copy(diagonalBarColor = it)
                                    onPersist(currentStyle)
                                }
                            )
                        }
                    }

                    // 5.3 Triángulo y Cajón de Apps (Vista 2)
                    item {
                        GlassCard {
                            Text(
                                text = if (isEs) "Triángulo Inferior" else "Bottom Triangle",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            SliderSettingItem(
                                title = if (isEs) "Tamaño del Triángulo" else "Triangle Size",
                                description = if (isEs) "Proporción del triángulo respecto al área" else "Triangle proportion relative to area",
                                value = currentStyle.triangleWidth,
                                valueRange = 0.65f..1.00f,
                                displayValue = "${(currentStyle.triangleWidth * 100).toInt()}%",
                                onValueChange = {
                                    currentStyle = currentStyle.copy(triangleWidth = it)
                                    onUpdateTransient(currentStyle)
                                },
                                onValueChangeFinished = { onPersist(currentStyle) }
                            )
                            SliderSettingItem(
                                title = if (isEs) "Opacidad del Triángulo" else "Triangle Opacity",
                                description = if (isEs) "Transparencia de la forma triangular" else "Triangle shape transparency",
                                value = currentStyle.triangleOpacity,
                                valueRange = 0.25f..1.00f,
                                displayValue = "${(currentStyle.triangleOpacity * 100).toInt()}%",
                                onValueChange = {
                                    currentStyle = currentStyle.copy(triangleOpacity = it)
                                    onUpdateTransient(currentStyle)
                                },
                                onValueChangeFinished = { onPersist(currentStyle) }
                            )
                            ColorSettingItem(
                                title = if (isEs) "Color de Acento del Triángulo" else "Triangle Accent Color",
                                description = if (isEs) "Color del triángulo e iconos diagonales" else "Color of triangle and diagonal icons",
                                currentColorHex = currentStyle.triangleColor,
                                onColorChange = {
                                    currentStyle = currentStyle.copy(triangleColor = it)
                                    onPersist(currentStyle)
                                }
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = if (isEs) "Cajón de Aplicaciones" else "App Drawer",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            SliderSettingItem(
                                title = if (isEs) "Opacidad de Fondo" else "Background Opacity",
                                description = if (isEs) "Transparencia del cajón de aplicaciones" else "App drawer transparency",
                                value = currentStyle.appDrawerBgOpacity,
                                valueRange = 0.30f..1.00f,
                                displayValue = "${(currentStyle.appDrawerBgOpacity * 100).toInt()}%",
                                onValueChange = {
                                    currentStyle = currentStyle.copy(appDrawerBgOpacity = it)
                                    onUpdateTransient(currentStyle)
                                },
                                onValueChangeFinished = { onPersist(currentStyle) }
                            )
                            ColorSettingItem(
                                title = if (isEs) "Color de Fondo" else "Background Color",
                                description = if (isEs) "Color sólido del cajón de aplicaciones" else "Solid color of the app drawer",
                                currentColorHex = currentStyle.appDrawerBgColor,
                                onColorChange = {
                                    currentStyle = currentStyle.copy(appDrawerBgColor = it)
                                    onPersist(currentStyle)
                                }
                            )
                            ColorSettingItem(
                                title = if (isEs) "Color de Texto (Nombres)" else "Text Color (App Names)",
                                description = if (isEs) "Color de las letras de las aplicaciones" else "App letters color",
                                currentColorHex = currentStyle.appDrawerTextColor,
                                onColorChange = {
                                    currentStyle = currentStyle.copy(appDrawerTextColor = it)
                                    onPersist(currentStyle)
                                }
                            )
                            ColorSettingItem(
                                title = if (isEs) "Color de Iconos Personalizados" else "Custom Icons Color",
                                description = if (isEs) "Aplica a los iconos predeterminados de la galería" else "Applies to default asset icons",
                                currentColorHex = currentStyle.customIconColor,
                                onColorChange = {
                                    currentStyle = currentStyle.copy(customIconColor = it)
                                    onPersist(currentStyle)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
