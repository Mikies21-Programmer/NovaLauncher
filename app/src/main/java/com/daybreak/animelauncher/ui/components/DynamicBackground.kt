package com.daybreak.animelauncher.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@Composable
fun DynamicBackground(
    backgroundUri: String?,
    modifier: Modifier = Modifier,
    pageIndex: Int = 0
) {
    // ── P2-02B: DYNAMIC-BG-ENTER ─────────────────────────────────────────────
    // SideEffect se ejecuta después de cada composición exitosa del árbol.
    // Permite medir cuándo DynamicBackground fue compuesto por primera vez.
    // No modifica lógica ni árbol de composición.
    SideEffect {
        android.os.Trace.beginSection("DYNAMIC-BG-ENTER")
        android.os.Trace.endSection()
    }

    val context = LocalContext.current

    val safeBackgroundUri = if (!backgroundUri.isNullOrBlank() && LauncherMediaUriValidator.isAllowedMediaUri(backgroundUri)) {
        backgroundUri
    } else {
        null
    }

    // Optimización: la llamada a ContentResolver.getType() se aísla en remember(safeBackgroundUri)
    // para no ejecutar operaciones IPC/IO en el hilo principal durante recomposiciones
    val isImage = remember(safeBackgroundUri) {
        if (!safeBackgroundUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(safeBackgroundUri)
                val type = context.contentResolver.getType(uri)
                if (type != null) {
                    type.startsWith("image/")
                } else {
                    val lower = safeBackgroundUri.lowercase()
                    lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".bmp")
                }
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    // Historical bundled wallpaper URIs remain persisted for backwards compatibility,
    // but their unlicensed source files are deliberately no longer packaged.
    val isRetiredBundledWallpaper = safeBackgroundUri?.startsWith(
        "file:///android_asset/fondos/",
        ignoreCase = true
    ) == true

    if (isRetiredBundledWallpaper || safeBackgroundUri.isNullOrBlank()) {
        StaticDefaultBackground(modifier)
    } else if (isImage) {
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(context)
                .data(Uri.parse(safeBackgroundUri))
                .crossfade(true)
                .allowHardware(true)
                .build()
        )
        Image(
            painter = painter,
            contentDescription = "Fondo",
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize()
        )
    } else {
        VideoBackground(
            videoUri = safeBackgroundUri,
            pageIndex = pageIndex,
            modifier = modifier
        )
    }
}

/** Code-drawn fallback with no bitmap or media resource dependency. */
@Composable
private fun StaticDefaultBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF05050B),
                        Color(0xFF10102A),
                        Color(0xFF08151C)
                    )
                )
            )
    )
}

sealed class DynamicBackgroundConsumer {
    data class CoilImage(val uriString: String) : DynamicBackgroundConsumer()
    data class VideoPlayer(val uriString: String?) : DynamicBackgroundConsumer()
}

internal fun resolveDynamicBackgroundConsumer(
    backgroundUri: String?,
    isImageDecider: (String) -> Boolean
): DynamicBackgroundConsumer {
    val safeUri = if (!backgroundUri.isNullOrBlank() && LauncherMediaUriValidator.isAllowedMediaUri(backgroundUri)) {
        backgroundUri
    } else {
        null
    }
    return if (safeUri != null && isImageDecider(safeUri)) {
        DynamicBackgroundConsumer.CoilImage(safeUri)
    } else {
        DynamicBackgroundConsumer.VideoPlayer(safeUri)
    }
}
