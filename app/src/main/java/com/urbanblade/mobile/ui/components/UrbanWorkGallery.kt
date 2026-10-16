package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.urbanblade.mobile.data.model.SocialMedia
import com.urbanblade.mobile.ui.theme.UrbanColors

/** Carrusel común de trabajos: deslizar, ampliar fotos y reproducir videos a petición. */
@Composable
fun UrbanWorkGallery(media: List<SocialMedia>, title: String?, modifier: Modifier = Modifier, onDoubleTap: () -> Unit = {}) {
    val pages = media.filter { !it.url.isNullOrBlank() }
    var video by remember { mutableStateOf<String?>(null) }
    var photo by remember { mutableStateOf<String?>(null) }
    val doubleTap by rememberUpdatedState(onDoubleTap)
    video?.let { VideoPlayerDialog(it) { video = null } }
    photo?.let { url ->
        Dialog(onDismissRequest = { photo = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                WorkMediaThumb(url, false, title, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                IconButton(onClick = { photo = null }, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding()) {
                    Icon(Icons.Default.Close, "Cerrar fotografía", tint = Color.White)
                }
            }
        }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (pages.isEmpty()) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).background(UrbanColors.CardAlt), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Image, "Publicación sin imagen", tint = UrbanColors.Muted)
            }
        } else {
            val pager = rememberPagerState { pages.size }
            Box {
                HorizontalPager(state = pager, key = { "$it:${pages[it].url}" }, modifier = Modifier.fillMaxWidth().aspectRatio(1f)) { page ->
                    val item = pages[page]
                    Box(Modifier.fillMaxSize().semantics {
                        onClick(label = if (item.type == "video") "Reproducir video" else "Ampliar fotografía") {
                            if (item.type == "video") video = item.url else photo = item.url
                            true
                        }
                    }.pointerInput(item.url, item.type) {
                        detectTapGestures(onTap = { if (item.type == "video") video = item.url else photo = item.url }, onDoubleTap = { doubleTap() })
                    }) {
                        WorkMediaThumb(item.url, item.type == "video", title, Modifier.fillMaxSize())
                    }
                }
                if (pages.size > 1) Surface(color = Color.Black.copy(alpha = 0.65f), shape = CircleShape, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                    Text("${pager.currentPage + 1}/${pages.size}", color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
            if (pages.size > 1) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(pages.size) { index ->
                    Box(Modifier.padding(horizontal = 3.dp).size(if (index == pager.currentPage) 7.dp else 5.dp).clip(CircleShape).background(if (index == pager.currentPage) UrbanColors.Gold else UrbanColors.Line))
                }
            }
        }
    }
}
