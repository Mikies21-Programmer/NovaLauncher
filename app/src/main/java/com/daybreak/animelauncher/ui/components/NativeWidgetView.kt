package com.daybreak.animelauncher.ui.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.daybreak.animelauncher.widget.WidgetHostManager

@Composable
fun NativeWidgetView(
    appWidgetId: Int,
    widgetHostManager: WidgetHostManager,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    onEnterEditMode: () -> Unit = {},
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val appWidgetInfo = remember(appWidgetId) { widgetHostManager.getAppWidgetInfo(appWidgetId) }

    if (appWidgetInfo != null) {
        val desiredHeight = remember(appWidgetInfo) {
            widgetHostManager.getDesiredHeightDp(appWidgetInfo, context)
        }

        var currentHostView by remember { mutableStateOf<AppWidgetHostView?>(null) }
        val lastReportedSize = remember { intArrayOf(0, 0) }

        val containerShape = RoundedCornerShape(16.dp)

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(desiredHeight)
                .then(
                    if (isEditMode) {
                        Modifier
                            .border(1.5.dp, Color(0xFF00F0FF).copy(alpha = 0.85f), containerShape)
                            .background(Color(0xFF00F0FF).copy(alpha = 0.05f), containerShape)
                            .padding(4.dp)
                    } else {
                        Modifier
                    }
                )
                .onSizeChanged { size ->
                    val widthPx = size.width
                    val heightPx = size.height
                    val density = context.resources.displayMetrics.density
                    currentHostView?.let { hostView ->
                        val updated = widgetHostManager.updateWidgetSize(
                            hostView = hostView,
                            appWidgetId = appWidgetId,
                            widthPx = widthPx,
                            heightPx = heightPx,
                            density = density,
                            lastReportedWidthDp = lastReportedSize[0],
                            lastReportedHeightDp = lastReportedSize[1]
                        )
                        if (updated != null) {
                            lastReportedSize[0] = updated.first
                            lastReportedSize[1] = updated.second
                        }
                    }
                }
        ) {
            AndroidView(
                factory = { ctx ->
                    widgetHostManager.appWidgetHost.createView(ctx, appWidgetId, appWidgetInfo).apply {
                        setAppWidget(appWidgetId, appWidgetInfo)
                        setOnLongClickListener {
                            onEnterEditMode()
                            true
                        }
                        currentHostView = this
                    }
                },
                update = { hostView ->
                    currentHostView = hostView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Botón de eliminar: ÚNICAMENTE visible cuando está en Modo Edición
            AnimatedVisibility(
                visible = isEditMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                IconButton(
                    onClick = {
                        widgetHostManager.deleteWidgetId(appWidgetId)
                        onDelete()
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF08080C).copy(alpha = 0.9f), CircleShape)
                        .border(1.dp, Color(0xFF00F0FF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Eliminar Widget",
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    } else {
        // Estado recuperable: Widget no disponible o eliminado por el sistema
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2A1015).copy(alpha = 0.8f))
                .border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Widget no disponible ($appWidgetId)",
                    color = Color.White,
                    fontSize = 13.sp
                )
                IconButton(
                    onClick = {
                        widgetHostManager.deleteWidgetId(appWidgetId)
                        onDelete()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Remover",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Sobrecarga de compatibilidad para llamadas con AppWidgetHost directo.
 */
@Composable
fun NativeWidgetView(
    appWidgetId: Int,
    appWidgetHost: AppWidgetHost,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    onEnterEditMode: () -> Unit = {},
    onDelete: () -> Unit
) {
    val context = LocalContext.current.applicationContext as android.app.Application
    val widgetHostManager = remember { WidgetHostManager(context) }
    NativeWidgetView(
        appWidgetId = appWidgetId,
        widgetHostManager = widgetHostManager,
        modifier = modifier,
        isEditMode = isEditMode,
        onEnterEditMode = onEnterEditMode,
        onDelete = onDelete
    )
}
