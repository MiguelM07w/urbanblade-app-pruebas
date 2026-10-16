package com.urbanblade.mobile.ui.components

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import com.urbanblade.mobile.ui.theme.UrbanColors
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis

/**
 * Foto o video del muro de los barberos. Para un video muestra su primer segundo como
 * miniatura (Coil no sabe leer un .mp4 como imagen sin VideoFrameDecoder) y un ícono de play.
 */
@Composable
fun WorkMediaThumb(
    url: String?,
    isVideo: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    playIconSize: Dp = 48.dp,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    var failed by remember(url, isVideo) { mutableStateOf(false) }
    var loading by remember(url, isVideo) { mutableStateOf(!url.isNullOrBlank()) }
    Box(modifier, contentAlignment = Alignment.Center) {
        AsyncImage(
            model = remember(url, isVideo) {
                ImageRequest.Builder(context).data(url).apply {
                    if (isVideo) {
                        decoderFactory(VideoFrameDecoder.Factory())
                        videoFrameMillis(1000)
                    }
                }.crossfade(true).build()
            },
            contentDescription = contentDescription,
            contentScale = contentScale,
            onLoading = { loading = true },
            onError = { failed = true; loading = false },
            onSuccess = { failed = false; loading = false },
            modifier = Modifier.fillMaxSize()
        )
        if (loading) {
            CircularProgressIndicator(Modifier.size(24.dp), color = UrbanColors.Gold, strokeWidth = 2.dp)
        } else if (failed || url.isNullOrBlank()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.BrokenImage, null, tint = UrbanColors.Muted)
                Text(if (isVideo) "Vista previa no disponible" else "Imagen no disponible", color = UrbanColors.Muted, style = MaterialTheme.typography.labelSmall)
            }
        } else if (isVideo) {
            Icon(
                Icons.Default.PlayCircle,
                "Reproducir video",
                tint = Color.White,
                modifier = Modifier.size(playIconSize)
            )
        }
    }
}

/** Reproduce un video del muro a pantalla completa; se libera al cerrarlo. */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerDialog(url: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)
            ) {
                Icon(Icons.Default.Close, "Cerrar video", tint = Color.White)
            }
        }
    }
}
