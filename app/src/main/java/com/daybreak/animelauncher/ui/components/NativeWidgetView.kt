package com.daybreak.animelauncher.ui.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun NativeWidgetView(
    appWidgetId: Int,
    appWidgetHost: AppWidgetHost,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val appWidgetInfo = remember(appWidgetId) { appWidgetManager.getAppWidgetInfo(appWidgetId) }

    if (appWidgetInfo != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .padding(8.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    appWidgetHost.createView(ctx, appWidgetId, appWidgetInfo).apply {
                        setAppWidget(appWidgetId, appWidgetInfo)
                        isClickable = true
                        isFocusable = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 120.dp)
            )
            
            // Delete button in top right
            IconButton(
                onClick = {
                    try {
                        appWidgetHost.deleteAppWidgetId(appWidgetId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    onDelete()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .background(Color.Black.copy(alpha = 0.7f), shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Remover Widget",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Red.copy(alpha = 0.4f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Widget no disponible o eliminado ($appWidgetId)", color = Color.White)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Remover", tint = Color.White)
                }
            }
        }
    }
}
