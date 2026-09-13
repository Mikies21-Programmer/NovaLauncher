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
                        IconButton(onClick = onReset) {
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
                                onClick = onReset,
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

                // Sección 0: Efecto Glass y Estilo Dark Premium
                item {
                    Text(
                        text = if (isEs) "EFECTO GLASS Y ESTILO PREMIUM" else "GLASS EFFECT & PREMIUM STYLE",
                        color = Color(0xFF00F0FF),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
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
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        SliderSettingItem(
                            title = if (isEs) "Transparencia de Paneles Glass" else "Glass Panel Transparency",
                            description = if (isEs) "Nivel de opacidad de tarjetas y paneles translúcidos" else "Opacity level for cards and translucent panels",
                            value = currentStyle.panelTransparency,
                            valueRange = 0.20f..1.00f,
                            displayValue = "${(currentStyle.panelTransparency * 100).toInt()}%",
                            onValueChange = { 
                                currentStyle = currentStyle.copy(panelTransparency = it)
                                onUpdateTransient(currentStyle)
                            },
                            onValueChangeFinished = { onPersist(currentStyle) }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        SliderSettingItem(
                            title = if (isEs) "Brillo de Borde Glass" else "Glass Border Glow",
                            description = if (isEs) "Intensidad del contorno cristalino" else "Intensity of crystal outline",
                            value = currentStyle.glassBorderAlpha,
                            valueRange = 0.05f..0.60f,
                            displayValue = "${(currentStyle.glassBorderAlpha * 100).toInt()}%",
                            onValueChange = { 
                                currentStyle = currentStyle.copy(glassBorderAlpha = it)
                                onUpdateTransient(currentStyle)
                            },
                            onValueChangeFinished = { onPersist(currentStyle) }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        SliderSettingItem(
                            title = if (isEs) "Redondeo de Esquinas" else "Corner Rounding",
                            description = if (isEs) "Curvatura de los paneles Glass" else "Curvature of Glass panels",
                            value = currentStyle.cornerRadius,
                            valueRange = 8f..32f,
                            displayValue = "${currentStyle.cornerRadius.toInt()} dp",
                            onValueChange = { 
                                currentStyle = currentStyle.copy(cornerRadius = it)
                                onUpdateTransient(currentStyle)
                            },
                            onValueChangeFinished = { onPersist(currentStyle) }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        SliderSettingItem(
                            title = if (isEs) "Opacidad de Widgets" else "Widget Opacity",
                            description = if (isEs) "Contraste de fondo para widgets integrados" else "Background contrast for integrated widgets",
                            value = currentStyle.widgetOpacity,
                            valueRange = 0.20f..1.00f,
                            displayValue = "${(currentStyle.widgetOpacity * 100).toInt()}%",
                            onValueChange = { 
                                currentStyle = currentStyle.copy(widgetOpacity = it)
                                onUpdateTransient(currentStyle)
                            },
                            onValueChangeFinished = { onPersist(currentStyle) }
                        )
                    }
                }

                // Sección 1: Colores de Elementos Individuales
                item {
                    Text(
                        text = if (isEs) "COLORES DE TEXTO E ICONOS" else "TEXT & ICON COLORS",
                        color = Color(0xFF00F0FF),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                item {
                    GlassCard {
                        ColorSettingItem(
                            title = if (isEs) "Botón Logo 'mi' (Fondo)" else "'mi' Logo Button (Bg)",
                            description = if (isEs) "Color del círculo superior en la barra lateral" else "Top circle color in sidebar",
                            currentColorHex = currentStyle.miButtonColor,
                            onColorChange = { 
                                currentStyle = currentStyle.copy(miButtonColor = it)
                                onPersist(currentStyle)
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        ColorSettingItem(
                            title = if (isEs) "Texto Logo 'mi'" else "'mi' Logo Text",
                            description = if (isEs) "Color de las letras dentro del botón" else "Letter color inside the button",
                            currentColorHex = currentStyle.miTextColor,
                            onColorChange = { 
                                currentStyle = currentStyle.copy(miTextColor = it)
                                onPersist(currentStyle)
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        ColorSettingItem(
                            title = if (isEs) "Reloj / Hora" else "Clock / Time",
                            description = if (isEs) "Color de la hora digital en la barra lateral" else "Digital clock color in sidebar",
                            currentColorHex = currentStyle.clockColor,
                            onColorChange = { 
                                currentStyle = currentStyle.copy(clockColor = it)
                                onPersist(currentStyle)
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        ColorSettingItem(
                            title = if (isEs) "Batería (Icono y Porcentaje)" else "Battery (Icon & %)",
                            description = if (isEs) "Color del indicador de batería" else "Battery indicator color",
                            currentColorHex = currentStyle.batteryColor,
                            onColorChange = { 
                                currentStyle = currentStyle.copy(batteryColor = it)
                                onPersist(currentStyle)
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        ColorSettingItem(
                            title = if (isEs) "Texto 'Mensajes'" else "'Messages' Text",
                            description = if (isEs) "Color del contador de mensajes" else "Message counter text color",
                            currentColorHex = currentStyle.messagesColor,
                            onColorChange = { 
                                currentStyle = currentStyle.copy(messagesColor = it)
                                onPersist(currentStyle)
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
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

                // Sección 2: Barras Laterales y Diagonales
                item {
                    Text(
                        text = if (isEs) "BARRAS LATERALES Y DIAGONALES" else "SIDEBARS & DIAGONAL BARS",
                        color = Color(0xFF00F0FF),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
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
                            valueRange = 0.10f..1.00f,
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
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Text(
                            text = if (isEs) "Barra Diagonal y Accesos" else "Diagonal Bar & Shortcuts",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        SliderSettingItem(
                            title = if (isEs) "Opacidad de Barra Diagonal" else "Diagonal Bar Opacity",
                            description = if (isEs) "Transparencia de la barra diagonal" else "Diagonal bar transparency",
                            value = currentStyle.diagonalBarOpacity,
                            valueRange = 0.10f..1.00f,
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

                // Sección 3: Triángulos y Cuadrícula de Apps
                item {
                    Text(
                        text = if (isEs) "TRIÁNGULO Y CAJÓN DE APPS (VISTA 2)" else "TRIANGLE & APP DRAWER (VIEW 2)",
                        color = Color(0xFF00F0FF),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
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
                            valueRange = 0.50f..1.00f,
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
                            valueRange = 0.10f..1.00f,
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
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Text(
                            text = if (isEs) "Cajón de Aplicaciones" else "App Drawer",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        SliderSettingItem(
                            title = if (isEs) "Opacidad de Fondo" else "Background Opacity",
                            description = if (isEs) "Transparencia del cajón de aplicaciones" else "App drawer transparency",
                            value = currentStyle.appDrawerBgOpacity,
                            valueRange = 0.00f..1.00f,
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
