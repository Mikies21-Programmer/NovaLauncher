package com.daybreak.animelauncher.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import com.daybreak.animelauncher.AppShortcut
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

object IconCache {
    const val MAX_ENTRIES = 128
    private val lruCache = object : android.util.LruCache<String, ImageBitmap>(MAX_ENTRIES) {}
    private val failedPackages = Collections.synchronizedSet(HashSet<String>())
    private val inFlightJobs = ConcurrentHashMap<String, Deferred<ImageBitmap?>>()
    private val cacheScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Decodificador desacoplado para permitir pruebas unitarias deterministas
    internal var iconDecoder: (android.content.Context, String) -> ImageBitmap? = { ctx, pkg ->
        decodeIconInternal(ctx, pkg)
    }

    fun getCached(packageName: String): ImageBitmap? {
        synchronized(this) {
            return lruCache.get(packageName)
        }
    }

    fun isFailed(packageName: String): Boolean {
        return failedPackages.contains(packageName)
    }

    /**
     * Invalida un paquete específico tras desinstalación o actualización de una aplicación.
     */
    fun invalidate(packageName: String) {
        synchronized(this) {
            lruCache.remove(packageName)
            failedPackages.remove(packageName)
            inFlightJobs.remove(packageName)?.cancel()
        }
    }

    fun clear() {
        synchronized(this) {
            lruCache.evictAll()
            failedPackages.clear()
            inFlightJobs.clear()
        }
    }

    fun put(packageName: String, bitmap: ImageBitmap) {
        synchronized(this) {
            lruCache.put(packageName, bitmap)
            failedPackages.remove(packageName)
        }
    }

    /**
     * Método síncrono para compatibilidad hacia atrás.
     */
    fun getIcon(context: android.content.Context, packageName: String): ImageBitmap? {
        getCached(packageName)?.let { return it }
        val bitmap = iconDecoder(context.applicationContext ?: context, packageName)
        if (bitmap != null) {
            put(packageName, bitmap)
        } else {
            failedPackages.add(packageName)
        }
        return bitmap
    }

    /**
     * Carga un icono con deduplicación de solicitudes concurrentes y desacople del ciclo de vida del composable.
     * Si la fila del cajón sale de pantalla durante scroll rápido, el trabajo de decodificación continúa
     * en segundo plano en cacheScope e inserta el resultado en el LRU.
     */
    suspend fun loadIcon(context: android.content.Context, packageName: String): ImageBitmap? {
        getCached(packageName)?.let { return it }
        if (isFailed(packageName)) return null

        val appContext = context.applicationContext ?: context

        val deferred = inFlightJobs.computeIfAbsent(packageName) { pkg ->
            cacheScope.async {
                try {
                    val bitmap = iconDecoder(appContext, pkg)
                    if (bitmap != null) {
                        put(pkg, bitmap)
                    } else {
                        failedPackages.add(pkg)
                    }
                    bitmap
                } catch (e: Exception) {
                    failedPackages.add(pkg)
                    null
                } finally {
                    inFlightJobs.remove(pkg)
                }
            }
        }

        return try {
            deferred.await()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Precalienta en segundo plano los iconos de las aplicaciones del cajón.
     * Prioriza las primeras aplicaciones (orden A-Z visible al abrir el cajón)
     * respetando estrictamente el presupuesto máximo del LRU (MAX_ENTRIES = 128).
     */
    fun preloadIcons(context: android.content.Context, packageNames: List<String>) {
        val appContext = context.applicationContext ?: context
        val packagesToLoad = packageNames.take(MAX_ENTRIES).filter { pkg ->
            getCached(pkg) == null && !isFailed(pkg)
        }
        if (packagesToLoad.isEmpty()) return

        cacheScope.launch {
            for (pkg in packagesToLoad) {
                if (getCached(pkg) != null) continue
                try {
                    loadIcon(appContext, pkg)
                } catch (e: Exception) {
                    // Continuar con el siguiente paquete de forma silenciosa
                }
            }
        }
    }

    private fun decodeIconInternal(context: android.content.Context, packageName: String): ImageBitmap? {
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
            bmp.asImageBitmap()
        } catch (e: Exception) {
            null
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
            var isLoadFailed by remember(shortcut.packageName) { mutableStateOf(IconCache.isFailed(shortcut.packageName)) }
            
            if (appIconBitmap == null && !isLoadFailed) {
                LaunchedEffect(shortcut.packageName) {
                    val loaded = IconCache.loadIcon(context, shortcut.packageName)
                    if (loaded != null) {
                        appIconBitmap = loaded
                    } else if (IconCache.isFailed(shortcut.packageName)) {
                        isLoadFailed = true
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
            } else if (isLoadFailed) {
                // Fallback explícito únicamente ante fallo real comprobado de carga
                Icon(Icons.Outlined.Android, contentDescription = shortcut.name, tint = defaultTint, modifier = Modifier.fillMaxSize())
            } else {
                // Estado de carga controlado: placeholder sutil que evita el parpadeo del icono genérico blanco
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                )
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

