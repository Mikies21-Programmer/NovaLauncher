package com.daybreak.animelauncher.ui.components

import android.view.LayoutInflater
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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

    AndroidView(
        factory = { ctx ->
            val playerView = LayoutInflater.from(ctx).inflate(R.layout.video_player_view, null) as PlayerView
            VideoWallpaperManager.registerPlayerView(pageIndex, playerView, videoResId, videoUri)
            playerView
        },
        update = { playerView ->
            VideoWallpaperManager.registerPlayerView(pageIndex, playerView, videoResId, videoUri)
        },
        onReset = { playerView ->
            VideoWallpaperManager.unregisterPlayerView(pageIndex, playerView, source = "onReset")
        },
        modifier = modifier
    )

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
            VideoWallpaperManager.unregisterPlayerView(pageIndex, source = "onDispose")
        }
    }
}
