package com.daybreak.animelauncher.ui.components

import android.net.Uri
import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@Composable
fun DynamicBackground(
    @RawRes defaultVideoResId: Int,
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

    // Optimización: la llamada a ContentResolver.getType() se aísla en remember(backgroundUri)
    // para no ejecutar operaciones IPC/IO en el hilo principal durante recomposiciones
    val isImage = remember(backgroundUri) {
        if (!backgroundUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(backgroundUri)
                val type = context.contentResolver.getType(uri)
                if (type != null) {
                    type.startsWith("image/")
                } else {
                    val lower = backgroundUri.lowercase()
                    lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".bmp")
                }
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    if (isImage && !backgroundUri.isNullOrBlank()) {
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(context)
                .data(Uri.parse(backgroundUri))
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
            videoResId = defaultVideoResId,
            videoUri = backgroundUri,
            pageIndex = pageIndex,
            modifier = modifier
        )
    }
}
