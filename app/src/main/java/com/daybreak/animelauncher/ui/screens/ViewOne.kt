package com.daybreak.animelauncher.ui.screens

import android.appwidget.AppWidgetHost
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daybreak.animelauncher.AdvancedStyleConfig
import com.daybreak.animelauncher.LauncherViewModel
import com.daybreak.animelauncher.ViewConfig
import com.daybreak.animelauncher.ui.components.DynamicBackground
import com.daybreak.animelauncher.ui.components.NativeWidgetView
import com.daybreak.animelauncher.ui.components.RealTimeBattery
import com.daybreak.animelauncher.ui.components.RealTimeClock
import com.daybreak.animelauncher.ui.components.ShortcutIcon
import com.daybreak.animelauncher.ui.theme.parseColorSafe
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ViewOne(
    config: ViewConfig,
    onSettingsClick: () -> Unit,
    onLongPress: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    appWidgetHost: AppWidgetHost? = null,
    viewModel: LauncherViewModel? = null,
    viewIndex: Int = 0,
    language: String = "es",
    showUI: Boolean = true,
    isEditMode: Boolean = false,
    onEnterEditMode: () -> Unit = {},
    onWidgetBoundsChanged: ((List<Rect>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state = (viewModel?.state ?: kotlinx.coroutines.flow.MutableStateFlow(com.daybreak.animelauncher.LauncherState())).collectAsState().value
    val customIcons = state.customAppIcons

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onDoubleTap = { onDoubleTap() }
                )
            }
    ) {
        val style = viewModel?.state?.collectAsState()?.value?.styleConfig ?: AdvancedStyleConfig()
        val w = maxWidth
        val h = maxHeight
        val sidebarWidth = w * 0.16f
        val m = w * (1f - 0.16f) // Límite interior de la barra derecha
        val ts = m * style.triangleWidth // Lado del triángulo
        val thickness = w * 0.14f // Grosor diagonal
        val th = thickness * 1.414f // Grosor horizontal por hipotenusa (45°)

        // Dimensiones dinámicas proporcionales al ancho de pantalla (reducidas 10%)
        val iconSize = (w * 0.0855f).coerceIn(28.dp, 41.dp)
        val iconSpacing = (w * 0.065f).coerceIn(14.dp, 26.dp)

        // Background - Dynamic (Image or Video con TextureView anti pantalla negra)
        DynamicBackground(
            defaultVideoResId = com.daybreak.animelauncher.R.raw.bg_view_one,
            backgroundUri = config.backgroundUri,
            pageIndex = viewIndex,
            modifier = Modifier.fillMaxSize()
        )

        if (!showUI) return@BoxWithConstraints

        // Reutilización de objetos Path para evitar recolección de basura (GC) durante el deslizamiento
        val silverPath = remember { Path() }
        val barPath = remember { Path() }

        // Draw Custom Shapes using Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cw = size.width
            val ch = size.height
            val cm = cw * 0.84f
            val cts = cm * 0.88f
            val cth = (cw * 0.14f) * 1.414f
            
            val cxSilver = cm - cts
            val cySilver = ch - cts
            
            // 1. Triángulo inferior oscuro / neón
            silverPath.reset()
            silverPath.moveTo(cxSilver, ch)
            silverPath.lineTo(cm, cySilver)
            silverPath.lineTo(cm, ch)
            silverPath.close()
            drawPath(path = silverPath, color = style.triangleColor.parseColorSafe().copy(alpha = style.triangleOpacity))
            
            // 2. Barra Diagonal
            val cxBlack = cxSilver - cth
            val cyBlack = cySilver - cth
            
            barPath.reset()
            barPath.moveTo(cxBlack, ch)
            barPath.lineTo(cxSilver, ch)
            barPath.lineTo(cm, cySilver)
            barPath.lineTo(cm, cyBlack)
            barPath.close()
            drawPath(path = barPath, color = style.diagonalBarColor.parseColorSafe().copy(alpha = style.diagonalBarOpacity))
        }

        // Native Android AppWidgets container
        if (viewModel != null && config.nativeWidgetIds.isNotEmpty()) {
            val widgetBoundsMap = remember { mutableStateMapOf<Int, Rect>() }

            DisposableEffect(config.nativeWidgetIds) {
                widgetBoundsMap.keys.retainAll(config.nativeWidgetIds.toSet())
                val activeBounds = config.nativeWidgetIds.mapNotNull { widgetBoundsMap[it] }
                onWidgetBoundsChanged?.invoke(activeBounds)
                onDispose { }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = sidebarWidth + 16.dp, start = 16.dp, top = 40.dp, bottom = (h * 0.32f)),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(config.nativeWidgetIds, key = { it }) { widgetId ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                widgetBoundsMap[widgetId] = coordinates.boundsInRoot()
                                val activeBounds = config.nativeWidgetIds.mapNotNull { widgetBoundsMap[it] }
                                onWidgetBoundsChanged?.invoke(activeBounds)
                            }
                    ) {
                        NativeWidgetView(
                            appWidgetId = widgetId,
                            widgetHostManager = viewModel.widgetHostManager,
                            isEditMode = isEditMode,
                            onEnterEditMode = onEnterEditMode,
                            onDelete = {
                                widgetBoundsMap.remove(widgetId)
                                val activeBounds = config.nativeWidgetIds.filter { it != widgetId }.mapNotNull { widgetBoundsMap[it] }
                                onWidgetBoundsChanged?.invoke(activeBounds)
                                viewModel.removeNativeWidget(viewIndex, widgetId)
                            }
                        )
                    }
                }
            }
        } else {
            LaunchedEffect(Unit) {
                onWidgetBoundsChanged?.invoke(emptyList())
            }
        }

        // -----------------------------------------------------------------------------------------
        // ANCLAJE DE GEOMETRÍA ABSOLUTA (Sincronizado al 100% con Canvas y sin clipping)
        // Al usar BoxWithConstraints (w y h), las coordenadas coinciden milimétricamente con el Canvas
        // interno sin verse afectadas por las barras de navegación del sistema (como en Pixel 4).
        // El contenedor cuadrado (boxSize) evita el recorte por hardware en Android 15/16.
        // -----------------------------------------------------------------------------------------
        val boxSize = w * 0.75f
        val halfBox = boxSize / 2f

        // 1. Triángulo con Fecha (Centro del triángulo rosa)
        if (config.showDateWidget) {
            val distDate = ts * 0.35f
            val xDateTarget = m - distDate
            val yDateTarget = h - distDate

            Box(
                modifier = Modifier
                    .offset(x = xDateTarget - halfBox, y = yDateTarget - halfBox)
                    .size(boxSize),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.graphicsLayer { rotationZ = -45f },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val loc = if (language == "es") Locale.forLanguageTag("es-ES") else Locale.ENGLISH
                    val dayOfWeek = SimpleDateFormat("EEEE", loc).format(Date())
                    val date = if (language == "es") SimpleDateFormat("d 'de' MMM", loc).format(Date()) else SimpleDateFormat("MMM d'th'", loc).format(Date())
                    Text(
                        text = dayOfWeek,
                        color = style.dateColor.parseColorSafe(),
                        fontWeight = FontWeight.Light,
                        fontSize = (w * 0.12f).value.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = date,
                        color = style.dateColor.parseColorSafe().copy(alpha = 0.85f),
                        fontWeight = FontWeight.Thin,
                        fontSize = (w * 0.069f).value.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // 2. Iconos de Barra Diagonal (Centro matemático de la barra negra del 14%)
        val distDiag = (ts * 0.5f) + (th * 0.25f)
        val xDiagTarget = m - distDiag
        val yDiagTarget = h - distDiag

        Box(
            modifier = Modifier
                .offset(x = xDiagTarget - halfBox, y = yDiagTarget - halfBox)
                .size(boxSize),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.graphicsLayer { rotationZ = -45f },
                horizontalArrangement = Arrangement.spacedBy(iconSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                config.diagonalShortcuts.forEach { rawShortcut ->
                    val shortcut = rawShortcut.packageName?.let { pkg ->
                        customIcons[pkg]?.let { customUri ->
                            rawShortcut.copy(customIconUri = customUri)
                        }
                    } ?: rawShortcut
                    
                    ShortcutIcon(
                        shortcut = shortcut,
                        modifier = Modifier.size(iconSize),
                        defaultTint = style.triangleColor.parseColorSafe(),
                        customIconTint = style.customIconColor.parseColorSafe(),
                        onClick = {
                            if (shortcut.packageName != null) {
                                try {
                                    val intent = context.packageManager.getLaunchIntentForPackage(shortcut.packageName)
                                    if (intent != null) {
                                        if (shortcut.id.contains("/")) {
                                            intent.setClassName(shortcut.packageName, shortcut.id.substringAfter("/"))
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        val err = if (language == "en") "App not available" else "App no disponible"
                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                        onSettingsClick()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    onSettingsClick()
                                }
                            } else {
                                val smp = if (language == "en") "Sample icon. Assign your real app in Settings" else "Icono de muestra. Asigna tu app real en Ajustes"
                                Toast.makeText(context, smp, Toast.LENGTH_SHORT).show()
                                onSettingsClick()
                            }
                        }
                    )
                }
            }
        }

        // Right Sidebar (Dark Frosted Glass puro)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(sidebarWidth)
                .align(Alignment.CenterEnd)
                .background(style.sidebarColor.parseColorSafe().copy(alpha = style.sidebarOpacity))
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxHeight()
            ) {
                // Top Logo
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .background(style.miButtonColor.parseColorSafe(), shape = androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "mi", color = style.miTextColor.parseColorSafe(), fontWeight = FontWeight.Normal, fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.height(100.dp))
                
                // Rotated Status Info (Status Widget)
                if (config.showStatusWidget) {
                    Row(
                        modifier = Modifier
                            .wrapContentSize(unbounded = true)
                            .graphicsLayer { rotationZ = 90f },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        RealTimeClock(
                            color = style.clockColor.parseColorSafe(),
                            fontSize = (w * 0.09f).coerceIn(24.dp, 36.dp).value.sp
                        )
                        Column(horizontalAlignment = Alignment.Start) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RealTimeBattery(
                                    color = style.batteryColor.parseColorSafe(),
                                    fontSize = (w * 0.035f).coerceIn(12.dp, 14.dp).value.sp,
                                    iconSize = 20.dp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            val notifCount = viewModel?.notificationCount?.collectAsState()?.value ?: 0
                            val msgText = when {
                                notifCount == 0 -> if (language == "en") "0 messages" else "0 mensajes"
                                notifCount == 1 -> if (language == "en") "1 message" else "1 mensaje"
                                notifCount > 99 -> if (language == "en") "99+ messages" else "99+ mensajes"
                                else -> if (language == "en") "$notifCount messages" else "$notifCount mensajes"
                            }
                            Text(
                                text = msgText,
                                fontWeight = FontWeight.Thin,
                                fontSize = (w * 0.0318f).coerceIn(10.6.dp, 12.72.dp).value.sp,
                                color = style.messagesColor.parseColorSafe(),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.clickable {
                                    if (com.daybreak.animelauncher.NotificationMonitorService.isPermissionGranted(context)) {
                                        expandStatusBar(context)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            if (language == "en") "Grant notification access" else "Concede permiso de acceso a notificaciones",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        com.daybreak.animelauncher.NotificationMonitorService.openPermissionSettings(context)
                                    }
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Separator (horizontal line)
                Box(modifier = Modifier.width(24.dp).height(2.dp).background(style.miButtonColor.parseColorSafe().copy(alpha = 0.5f)))
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Bottom Icons (Sidebar shortcuts)
                Column(
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    config.sidebarShortcuts.forEach { rawShortcut ->
                        val shortcut = rawShortcut.packageName?.let { pkg ->
                            customIcons[pkg]?.let { customUri ->
                                rawShortcut.copy(customIconUri = customUri)
                            }
                        } ?: rawShortcut
                        
                        ShortcutIcon(
                            shortcut = shortcut,
                            modifier = Modifier.size(38.dp),
                            defaultTint = style.miButtonColor.parseColorSafe(),
                            customIconTint = style.customIconColor.parseColorSafe(),
                            onClick = {
                                if (shortcut.packageName != null) {
                                    try {
                                        val intent = context.packageManager.getLaunchIntentForPackage(shortcut.packageName)
                                        if (intent != null) {
                                            if (shortcut.id.contains("/")) {
                                                intent.setClassName(shortcut.packageName, shortcut.id.substringAfter("/"))
                                            }
                                            context.startActivity(intent)
                                        } else {
                                            val err = if (language == "en") "App not available" else "App no disponible"
                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                            onSettingsClick()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        onSettingsClick()
                                    }
                                } else {
                                    val smp = if (language == "en") "Sample icon. Assign your real app in Settings" else "Icono de muestra. Asigna tu app real en Ajustes"
                                    Toast.makeText(context, smp, Toast.LENGTH_SHORT).show()
                                    onSettingsClick()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
