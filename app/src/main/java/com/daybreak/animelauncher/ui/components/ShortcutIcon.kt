package com.daybreak.animelauncher.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import com.daybreak.animelauncher.AppShortcut

object IconCache {
    private const val MAX_ENTRIES = 80
    private val lruCache = object : android.util.LruCache<String, ImageBitmap>(MAX_ENTRIES) {}

    fun getIcon(context: android.content.Context, packageName: String): ImageBitmap? {
        synchronized(this) {
            val cached = lruCache.get(packageName)
            if (cached != null) return cached
        }

        return try {
            val packageManager = context.packageManager
            val drawable = packageManager.getApplicationIcon(packageName)
            
            // Downsamplear iconos a 96x96 px (ahorra hasta 75% de RAM por icono decodificado)
            val targetSize = 96
            val width = drawable.intrinsicWidth.coerceAtLeast(1).coerceAtMost(targetSize)
            val height = drawable.intrinsicHeight.coerceAtLeast(1).coerceAtMost(targetSize)
            
            val bmp = if (drawable is BitmapDrawable && drawable.bitmap != null) {
                val srcBmp = drawable.bitmap
                if (srcBmp.width <= targetSize && srcBmp.height <= targetSize) {
                    srcBmp
                } else {
                    Bitmap.createScaledBitmap(srcBmp, width, height, true)
                }
            } else {
                val newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(newBmp)
                drawable.setBounds(0, 0, width, height)
                drawable.draw(canvas)
                newBmp
            }
            val imageBitmap = bmp.asImageBitmap()
            synchronized(this) {
                lruCache.put(packageName, imageBitmap)
            }
            imageBitmap
        } catch (e: Exception) {
            null
        }
    }

    fun getCached(packageName: String): ImageBitmap? {
        synchronized(this) {
            return lruCache.get(packageName)
        }
    }

    fun clear() {
        synchronized(this) {
            lruCache.evictAll()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortcutIcon(
    shortcut: AppShortcut,
    modifier: Modifier = Modifier,
    defaultTint: Color = Color.White,
    customIconTint: Color? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showAppInfoDialog by remember { mutableStateOf(false) }

    val boxModifier = if (onClick != null) {
        modifier.combinedClickable(
            onClick = { onClick() },
            onLongClick = {
                if (shortcut.packageName != null) {
                    if (onLongClick != null) {
                        onLongClick()
                    } else {
                        showAppInfoDialog = true
                    }
                }
            }
        )
    } else modifier

    Box(modifier = boxModifier, contentAlignment = Alignment.Center) {
        if (!shortcut.customIconUri.isNullOrBlank()) {
            val isAssetIcon = shortcut.customIconUri.contains("android_asset/iconos")
            val colorFilter = if (isAssetIcon) {
                ColorFilter.tint(customIconTint ?: Color.White)
            } else {
                null
            }
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(shortcut.customIconUri)
                        .allowHardware(true)
                        .build()
                ),
                contentDescription = shortcut.name,
                modifier = Modifier.fillMaxSize(),
                colorFilter = colorFilter
            )
        } else if (shortcut.packageName != null) {
            var appIconBitmap by remember(shortcut.packageName) { mutableStateOf(IconCache.getCached(shortcut.packageName)) }
            
            if (appIconBitmap == null) {
                LaunchedEffect(shortcut.packageName) {
                    withContext(Dispatchers.IO) {
                        appIconBitmap = IconCache.getIcon(context, shortcut.packageName)
                    }
                }
            }
            val currentBitmap = appIconBitmap
            if (currentBitmap != null) {
                Image(
                    bitmap = currentBitmap,
                    contentDescription = shortcut.name,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Outlined.Android, contentDescription = shortcut.name, tint = defaultTint, modifier = Modifier.fillMaxSize())
            }
        } else {
            val iconVector = when (shortcut.id) {
                "default_search" -> Icons.Outlined.Search
                "default_camera" -> Icons.Outlined.CameraAlt
                "default_chat" -> Icons.Outlined.ChatBubbleOutline
                "default_phone" -> Icons.Outlined.Smartphone
                "default_home" -> Icons.Outlined.Home
                "default_help" -> Icons.AutoMirrored.Outlined.HelpOutline
                "default_headphones" -> Icons.Outlined.Headphones
                "default_calendar" -> Icons.Outlined.CalendarToday
                else -> Icons.Outlined.Apps
            }
            Icon(iconVector, contentDescription = shortcut.name, tint = defaultTint, modifier = Modifier.fillMaxSize())
        }
    }

    if (showAppInfoDialog && shortcut.packageName != null) {
        val isEs = remember {
            java.util.Locale.getDefault().language.startsWith("es")
        }
        AlertDialog(
            onDismissRequest = { showAppInfoDialog = false },
            containerColor = Color(0xFF08080C),
            titleContentColor = Color(0xFF00F0FF),
            textContentColor = Color.White,
            title = { Text(shortcut.name) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            showAppInfoDialog = false
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", shortcut.packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00F0FF),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isEs) "ℹ️ Información de la aplicación" else "ℹ️ App info",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showAppInfoDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00F0FF))
                ) {
                    Text(if (isEs) "Cancelar" else "Cancel")
                }
            }
        )
    }
}

