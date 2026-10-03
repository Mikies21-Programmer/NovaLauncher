package com.daybreak.animelauncher.ui.components

import android.view.LayoutInflater
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.ui.PlayerView
import com.daybreak.animelauncher.R

@Composable
fun VideoBackground(
    @RawRes videoResId: Int,
    modifier: Modifier = Modifier,
    videoUri: String? = null,
    pageIndex: Int = 0
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Vista única por nodo. SIN onReset: con onReset presente AndroidView activa recycling y,
    // tras un rebuild del nodo del pager (flip de dynamicBeyondViewport), la vista reutilizada
    // del pool se muestra en ventana mientras el registro apuntaba a la instancia nueva —
    // resultado: player decodificando hacia una vista no adjunta (último frame congelado).
    // Sin recycling, factory/update/SideEffect/onDispose operan siempre sobre ESTA vista.
    val playerView = remember(videoResId) {
        LayoutInflater.from(context).inflate(R.layout.video_player_view, null) as PlayerView
    }

    AndroidView(
        factory = { playerView },
        update = { vp -> VideoWallpaperManager.registerPlayerView(pageIndex, vp, videoResId, videoUri) },
        modifier = modifier
    )

    // Re-afirma el registro después de cada composición exitosa: si una generación previa de la
    // misma página se desregistró tarde (onDispose diferido tras un rebuild), el binding se
    // restaura en la siguiente pasada sin depender de una recomposición posterior.
    SideEffect {
        VideoWallpaperManager.registerPlayerView(pageIndex, playerView, videoResId, videoUri)
    }

    DisposableEffect(lifecycleOwner, pageIndex) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    VideoWallpaperManager.onResume(context)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    VideoWallpaperManager.onPause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Identidad exacta: el dispose de una generación vieja no retira el binding de la
            // generación nueva de la misma página.
            VideoWallpaperManager.unregisterPlayerView(pageIndex, playerView, source = "onDispose")
        }
    }
}
