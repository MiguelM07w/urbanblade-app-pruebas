package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.urbanblade.mobile.ui.components.WorkMediaThumb
import com.urbanblade.mobile.ui.components.VideoPlayerDialog
import com.urbanblade.mobile.data.model.SocialMedia
import com.urbanblade.mobile.data.model.SocialWork
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.SocialFeedViewModel

/**
 * Muro de Inspiración (así se llama en la web): los últimos trabajos de los barberos, con foto
 * grande. [onBookBarber] solo llega para quien puede reservar: de la foto que le gustó a la
 * reserva con ese barbero en un toque.
 */
@Composable
fun SocialFeedScreen(
    onBack: () -> Unit,
    onBookBarber: ((String) -> Unit)? = null,
    vm: SocialFeedViewModel = viewModel()
) {
    val feed by vm.feed.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    UrbanModuleScreen(
        eyebrow = "URBANBLADE",
        title = "Muro de Inspiración",
        subtitle = "Los últimos trabajos publicados por nuestros barberos.",
        onBack = onBack,
        onRefresh = { vm.load() },
        refreshing = busy
    ) {
        when {
            busy && feed.data.isEmpty() -> item { UrbanSkeletonList(2) }
            error != null && feed.data.isEmpty() -> item {
                UrbanMascotState(UrbanStateKind.ERROR, "No se pudo cargar el muro", error, "Reintentar") { vm.load() }
            }
            feed.data.isEmpty() -> item {
                UrbanMascotState(UrbanStateKind.EMPTY, "Todavía no hay trabajos publicados", "Los nuevos estilos del equipo aparecerán aquí.")
            }
            else -> {
                error?.let { item { UrbanErrorBanner(it) } }
                items(feed.data, key = { it.id }) { work ->
                    WorkPost(
                        work = work,
                        onReact = { vm.toggleReaction(work.id) },
                        onSave = { vm.toggleSave(work.id) },
                        onComment = { text, onDone -> vm.comment(work.id, text, onDone) },
                        onBook = work.barber?.id?.takeIf { onBookBarber != null }?.let { id -> { onBookBarber?.invoke(id) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkPost(
    work: SocialWork,
    onReact: () -> Unit,
    onSave: () -> Unit,
    onComment: (String, () -> Unit) -> Unit,
    onBook: (() -> Unit)?
) {
    var commentText by remember { mutableStateOf("") }
    // El campo de comentario se abre al tocar el ícono: abierto en cada publicación alargaba mucho el muro.
    var commenting by remember { mutableStateOf(false) }
    var playingUrl by remember { mutableStateOf<String?>(null) }
    val barberName = work.barber?.name ?: "Barbero"
    playingUrl?.let { VideoPlayerDialog(it, onDismiss = { playingUrl = null }) }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = UrbanColors.Card,
        border = androidx.compose.foundation.BorderStroke(1.dp, UrbanColors.Line),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (work.barber?.foto != null) {
                    AsyncImage(
                        model = work.barber.foto,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                    )
                } else {
                    UrbanAvatar(barberName, Modifier.size(40.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(barberName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    work.title?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }

            // Foto grande y cuadrada, como en la web: es lo que inspira. Las demás, en miniatura debajo.
            MainMedia(
                work.media.firstOrNull(),
                work.title,
                onPlay = { playingUrl = it },
                onDoubleTap = { if (!work.isReacted) onReact() }
            )
            if (work.media.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(work.media.drop(1)) { media ->
                        val isVideo = media.type == "video"
                        WorkMediaThumb(
                            url = media.url,
                            isVideo = isVideo,
                            contentDescription = null,
                            playIconSize = 24.dp,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .then(if (isVideo && media.url != null) Modifier.clickable { playingUrl = media.url } else Modifier)
                        )
                    }
                }
            }

            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onReact) {
                        Icon(
                            if (work.isReacted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            if (work.isReacted) "Quitar me gusta" else "Me gusta",
                            tint = if (work.isReacted) UrbanColors.Gold else UrbanColors.Muted
                        )
                    }
                    Text(work.reactionsCount.toString(), style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
                    Spacer(Modifier.width(14.dp))
                    IconButton(onClick = { commenting = !commenting }) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            if (commenting) "Cerrar comentario" else "Comentar",
                            tint = if (commenting) UrbanColors.Gold else UrbanColors.Muted
                        )
                    }
                    Text(work.commentsCount.toString(), style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onSave) {
                        Icon(
                            if (work.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            if (work.isSaved) "Quitar de guardados" else "Guardar",
                            tint = if (work.isSaved) UrbanColors.Gold else UrbanColors.Muted
                        )
                    }
                }

                work.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Ink)
                    Spacer(Modifier.height(6.dp))
                }

                if (onBook != null) {
                    UrbanPrimaryButton(
                        // Los nombres pueden venir con el apellido primero ("Gonzalez Ramirez Luis"): el nombre
                        // ya está arriba de la foto, el botón no necesita repetirlo.
                        text = "Reservar con este barbero",
                        onClick = onBook,
                        icon = Icons.Default.CalendarMonth,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (work.comments.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        work.comments.forEach { c ->
                            Row {
                                Text(c.user?.name ?: "Usuario", style = MaterialTheme.typography.bodySmall, color = UrbanColors.GoldText)
                                Spacer(Modifier.width(6.dp))
                                Text(c.comment ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                if (commenting) Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Escribe un comentario…") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { onComment(commentText) { commentText = "" } },
                        enabled = commentText.isNotBlank()
                    ) { Icon(Icons.Default.Send, "Enviar comentario", tint = UrbanColors.Gold) }
                }
            }
        }
    }
}

@Composable
private fun MainMedia(media: SocialMedia?, title: String?, onPlay: (String) -> Unit, onDoubleTap: () -> Unit) {
    // El detector de gestos vive mientras la foto esté en pantalla: debe llamar a la versión vigente
    // del callback (que sabe si ya tiene like), no a la del primer render.
    val latestDoubleTap by rememberUpdatedState(onDoubleTap)
    val isVideo = media?.type == "video"
    val latestTap by rememberUpdatedState<() -> Unit>({ if (isVideo) media?.url?.let(onPlay) })
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(UrbanColors.CardAlt)
            // Doble toque a la foto = me gusta, como en cualquier app de fotos (solo da like, no lo quita).
            // Un toque a un video lo reproduce.
            .pointerInput(Unit) { detectTapGestures(onTap = { latestTap() }, onDoubleTap = { latestDoubleTap() }) },
        contentAlignment = Alignment.Center
    ) {
        if (media?.url != null) {
            WorkMediaThumb(media.url, isVideo, title, Modifier.fillMaxSize(), playIconSize = 56.dp)
        } else {
            Icon(Icons.Default.Image, "Sin imagen", tint = UrbanColors.Muted, modifier = Modifier.size(40.dp))
        }
    }
}
