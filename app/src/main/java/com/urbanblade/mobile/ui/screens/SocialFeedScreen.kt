package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.SocialWork
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.SocialFeedViewModel

/** Muro de trabajos reales: carruseles, perfil público, reacciones y comentarios recientes. */
@Composable
fun SocialFeedScreen(
    onBack: () -> Unit,
    onBookBarber: ((String) -> Unit)? = null,
    vm: SocialFeedViewModel = viewModel(),
    onOpenBarber: ((String) -> Unit)? = null
) {
    val feed by vm.feed.collectAsState()
    val busy by vm.busy.collectAsState()
    val loadingMore by vm.loadingMore.collectAsState()
    val error by vm.error.collectAsState()
    val pending by vm.pending.collectAsState()
    val bookingIds by vm.bookingIds.collectAsState()
    val hasMore by vm.hasMore.collectAsState()
    val canBook = onBookBarber != null
    LaunchedEffect(canBook) { vm.load(canBook) }
    UrbanModuleScreen(
        eyebrow = "URBANBLADE",
        title = "Inspiración",
        subtitle = "Trabajos del equipo. Desliza las fotos y encuentra tu próximo estilo.",
        onBack = onBack,
        onRefresh = { vm.load(canBook) },
        refreshing = busy
    ) {
        when {
            busy && feed.data.isEmpty() -> item { UrbanSkeletonList(2) }
            error != null && feed.data.isEmpty() -> item {
                UrbanMascotState(UrbanStateKind.ERROR, "No se pudo cargar el muro", error, "Reintentar") { vm.load(canBook) }
            }
            feed.data.isEmpty() -> item {
                UrbanMascotState(UrbanStateKind.EMPTY, "Todavía no hay trabajos publicados", "Los nuevos estilos del equipo aparecerán aquí.")
            }
            else -> {
                error?.let { item {
                    UrbanErrorBanner(it)
                    UrbanOutlineButton("Actualizar muro", { vm.load(canBook) }, enabled = !busy && !loadingMore && pending.isEmpty(), modifier = Modifier.fillMaxWidth())
                } }
                items(feed.data, key = { it.id }) { work ->
                    val profileId = work.barber?.id?.let(bookingIds::get)
                    WorkPost(
                        work = work,
                        enabled = !busy && work.id !in pending,
                        sending = work.id in pending,
                        error = error,
                        onReact = { vm.toggleReaction(work.id) },
                        onSave = { vm.toggleSave(work.id) },
                        onComment = { text, done -> vm.comment(work.id, text, done) },
                        onBook = profileId?.takeIf { canBook }?.let { id -> { onBookBarber?.invoke(id) } },
                        onProfile = work.barber?.slug?.takeIf { onOpenBarber != null }?.let { slug -> { onOpenBarber?.invoke(slug) } }
                    )
                }
                if (hasMore) item {
                    UrbanOutlineButton(if (loadingMore) "Cargando publicaciones…" else "Ver más trabajos", { vm.loadMore() }, enabled = !busy && !loadingMore && pending.isEmpty(), modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkPost(
    work: SocialWork,
    enabled: Boolean,
    sending: Boolean,
    error: String?,
    onReact: () -> Unit,
    onSave: () -> Unit,
    onComment: (String, () -> Unit) -> Unit,
    onBook: (() -> Unit)?,
    onProfile: (() -> Unit)?
) {
    var commentText by rememberSaveable(work.id) { mutableStateOf("") }
    var commenting by rememberSaveable(work.id) { mutableStateOf(false) }
    val barberName = work.barber?.name ?: "Equipo UrbanBlade"
    if (commenting) ModalBottomSheet(onDismissRequest = { commenting = false }, containerColor = UrbanColors.Surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).imePadding().clipToBounds().padding(horizontal = 18.dp, vertical = 12.dp)) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().clipToBounds(),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { UrbanSectionTitle("Comentarios", "${work.commentsCount} en total · muestra reciente") }
            if (work.comments.isEmpty()) item { Text("Sé el primero en comentar este trabajo.", color = UrbanColors.Muted) }
            items(work.comments) { comment ->
                Row(verticalAlignment = Alignment.Top) {
                    val author = comment.user?.name ?: "Usuario"
                    UrbanAvatar(author, Modifier.size(32.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(author, style = MaterialTheme.typography.labelLarge, color = UrbanColors.Ink)
                        Text(comment.comment.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Ink)
                    }
                }
            }
            error?.let { item { UrbanErrorBanner(it) } }
            item {
                UrbanTextField(commentText, { commentText = it }, "Tu comentario", minLines = 2, maxLines = 4,
                    helper = "${commentText.length}/500 caracteres",
                    error = if (commentText.length > 500) "Máximo 500 caracteres." else null)
            }
        }
                UrbanPrimaryButton("Publicar comentario", { onComment(commentText) { commentText = "" } },
                    enabled = enabled && commentText.trim().isNotEmpty() && commentText.trim().length <= 500,
                    loading = sending, modifier = Modifier.fillMaxWidth())
        }
    }
    Surface(shape = MaterialTheme.shapes.large, color = UrbanColors.Card, border = androidx.compose.foundation.BorderStroke(1.dp, UrbanColors.Line), modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.fillMaxWidth().then(if (onProfile != null) Modifier.clickable(onClick = onProfile) else Modifier).padding(14.dp), verticalAlignment = Alignment.Top) {
                UrbanAvatar(barberName, Modifier.size(42.dp), imageUrl = work.barber?.foto)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(barberName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
                    Text(work.workDate?.let { "Trabajo del ${UrbanFormat.dateShort(it)}" } ?: "Trabajo del equipo", style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted)
                }
                if (onProfile != null) Icon(Icons.Default.ChevronRight, "Ver perfil del barbero", tint = UrbanColors.Muted)
            }
            UrbanWorkGallery(work.media, work.title) { if (enabled && !work.isReacted) onReact() }
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onReact, enabled = enabled) {
                            Icon(if (work.isReacted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                if (work.isReacted) "Quitar me gusta" else "Me gusta", tint = if (work.isReacted) UrbanColors.Danger else UrbanColors.Ink)
                        }
                        Text(work.reactionsCount.toString(), style = MaterialTheme.typography.labelLarge, color = UrbanColors.Ink)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { commenting = true }) { Icon(Icons.Default.ChatBubbleOutline, "Ver comentarios y comentar", tint = UrbanColors.Ink) }
                        Text(work.commentsCount.toString(), style = MaterialTheme.typography.labelLarge, color = UrbanColors.Ink)
                    }
                    IconButton(onClick = onSave, enabled = enabled) {
                        Icon(if (work.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            if (work.isSaved) "Quitar de guardados" else "Guardar inspiración", tint = if (work.isSaved) UrbanColors.Gold else UrbanColors.Ink)
                    }
                }
                work.title?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = UrbanColors.Ink) }
                work.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Ink) }
                if (work.commentsCount > 0) TextButton(onClick = { commenting = true }, contentPadding = PaddingValues(0.dp)) {
                    Text("Ver comentarios recientes (${work.commentsCount})", color = UrbanColors.Muted)
                }
                if (work.isSaved) Text("Guardado en tu inspiración", style = MaterialTheme.typography.labelSmall, color = UrbanColors.GoldText)
                onBook?.let {
                    UrbanOutlineButton("Reservar con este barbero", it, modifier = Modifier.fillMaxWidth(), icon = Icons.Default.CalendarMonth, enabled = enabled)
                }
            }
        }
    }
}
