package com.daybreak.animelauncher.ui.components

import android.net.Uri
import android.view.LayoutInflater
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.daybreak.animelauncher.R

@Composable
fun VideoBackground(
    @RawRes videoResId: Int,
    modifier: Modifier = Modifier,
    videoUri: String? = null
) {
    val context = LocalContext.current

    val exoPlayer = remember(videoResId, videoUri) {
        ExoPlayer.Builder(context).build().apply {
            val uri = if (!videoUri.isNullOrBlank()) {
                try {
                    Uri.parse(videoUri)
                } catch (e: Exception) {
                    Uri.parse("android.resource://${context.packageName}/$videoResId")
                }
            } else {
                Uri.parse("android.resource://${context.packageName}/$videoResId")
            }
            val mediaItem = MediaItem.fromUri(uri)
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f // Mute
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            val playerView = LayoutInflater.from(context).inflate(R.layout.video_player_view, null) as PlayerView
            playerView.player = exoPlayer
            playerView
        },
        update = { playerView ->
            if (playerView.player != exoPlayer) {
                playerView.player = exoPlayer
            }
        },
        modifier = modifier
    )
}
