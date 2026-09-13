package com.daybreak.animelauncher.ui.components

import android.net.Uri
import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
    val context = LocalContext.current
    val isImage = if (!backgroundUri.isNullOrBlank()) {
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

