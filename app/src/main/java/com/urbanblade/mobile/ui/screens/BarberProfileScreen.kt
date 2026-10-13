package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.urbanblade.mobile.data.model.BarberProfileResponse
import com.urbanblade.mobile.data.model.BarberReviewItem
import com.urbanblade.mobile.data.model.BarberWork
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.BarberProfileViewModel

/**
 * Ficha del barbero para el cliente (como /equipo/{slug} en la web): foto, especialidades,
 * "sobre mí", números, portafolio y reseñas. Si tuvo una cita completada con él, puede
 * calificarlo con estrellas eligiendo el servicio que recibió.
 */
@Composable
fun BarberProfileScreen(
    slug: String,
    canBook: Boolean,
    onBack: () -> Unit,
    onBook: (barberId: String) -> Unit,
    vm: BarberProfileViewModel = viewModel()
) {
    val profile by vm.profile.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(slug) {
        vm.load(slug)
        com.urbanblade.mobile.core.analytics.UrbanAnalytics.log("perfil_barbero_visto")
    }

    Scaffold(containerColor = Color.Transparent, topBar = { UrbanTopBar("", onBack) { IconButton(onClick = { vm.load(slug) }, enabled = !loading) { Icon(Icons.Default.Refresh, "Actualizar perfil") } } }) { padding ->
        val data = profile
        when {
            data == null && loading -> Box(Modifier.padding(padding).padding(18.dp)) { UrbanSkeletonList(4) }
            data == null && error != null -> Box(Modifier.padding(padding).padding(18.dp)) {
                UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar este perfil", error, "Reintentar") { vm.load(slug) }
            }
            data != null -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                error?.let { item { UrbanErrorBanner(it) } }
                if (loading) item { UrbanInfoBanner("Actualizando perfil…") }
                item { ProfileHeader(data, canBook, onBook) }
                item { ProfileStats(data) }
                data.barber.descripcion?.takeIf { it.isNotBlank() }?.let { bio ->
                    item {
                        UrbanCard(Modifier.fillMaxWidth()) {
                            Text("Sobre mí", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text(bio, style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
                        }
                    }
                }
                item { UrbanSectionTitle("Portafolio", if (data.works.isEmpty()) null else UrbanFormat.count(data.works.size, "trabajo", "trabajos")) }
                item {
                    if (data.works.isEmpty()) {
                        UrbanEmptyState("Todavía no publica trabajos", null, Icons.Default.PhotoLibrary)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(data.works, key = { it.id }) { WorkTile(it) }
                        }
                    }
                }
                if (data.canReview) item { ReviewForm(data, vm, slug) }
                if (data.alreadyReviewed) item {
                    UrbanInfoBanner("Ya calificaste a este barbero. ¡Gracias por tu opinión!", Icons.Default.Verified)
                }
                item {
                    UrbanSectionTitle(
                        "Reseñas",
                        if (data.totalReviews == 0) "Aún no tiene reseñas" else UrbanFormat.count(data.totalReviews, "reseña", "reseñas")
                    )
                }
                items(data.reviews, key = { it.id }) { ReviewCard(it) }
            }
        }
    }
}

@Composable
private fun ProfileHeader(data: BarberProfileResponse, canBook: Boolean, onBook: (String) -> Unit) {
    val name = data.barber.user?.name ?: "Barbero"
    UrbanHeroCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            UrbanAvatar(name, Modifier.size(104.dp), imageUrl = data.barber.foto)
            Spacer(Modifier.height(12.dp))
            Text(name, style = MaterialTheme.typography.headlineSmall, color = UrbanColors.Ink, textAlign = TextAlign.Center)
            data.avgRating?.let { avg ->
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StarRow(avg.toInt(), Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("%.1f".format(avg), style = MaterialTheme.typography.labelLarge, color = UrbanColors.GoldText)
                }
            }
            val chips = data.barber.especialidades.orEmpty().split(',').map { it.trim() }.filter { it.isNotBlank() }.take(6)
            if (chips.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    chips.forEach { chip ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = UrbanColors.CardAlt,
                            border = BorderStroke(1.dp, UrbanColors.Line)
                        ) {
                            Text(chip, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = UrbanColors.Ink)
                        }
                    }
                }
            }
            if (canBook) {
                Spacer(Modifier.height(16.dp))
                UrbanPrimaryButton(
                    // Sin nombre de pila: el primero suele ser el apellido («Reservar con Gonzalez»).
                    text = "Reservar cita",
                    onClick = { onBook(data.barber.id) },
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ProfileStats(data: BarberProfileResponse) {
    UrbanStatStrip(listOf(
        Triple("Calificación", data.avgRating?.let { "%.1f ★".format(it) } ?: "—", UrbanColors.GoldText),
        Triple("Reseñas", data.totalReviews.toString(), UrbanColors.Ink),
        Triple("Citas", data.citasCompletadas.toString(), UrbanColors.Ink)
    ))
}

@Composable
private fun WorkTile(work: BarberWork) {
    val image = work.images.firstOrNull { !it.substringBefore('?').lowercase().let { u -> u.endsWith(".mp4") || u.endsWith(".webm") || u.endsWith(".mov") } }
    Column(Modifier.width(150.dp)) {
        Box(
            Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(UrbanColors.CardAlt)
                .border(1.dp, UrbanColors.Line, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(if (image == null) Icons.Default.Videocam else Icons.Default.ContentCut, null, tint = UrbanColors.Gold.copy(alpha = 0.5f))
            if (image != null) {
                AsyncImage(model = image, contentDescription = work.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        work.title?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ReviewCard(review: BarberReviewItem) {
    UrbanCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val author = review.client?.user?.name ?: "Cliente"
            UrbanAvatar(author, Modifier.size(36.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(author, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                review.createdAt?.let { Text(UrbanFormat.dateShort(it), style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted) }
            }
            StarRow(review.rating, Modifier.size(16.dp))
        }
        review.service?.let { service ->
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(50), color = UrbanColors.CardAlt) {
                Text(service, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = UrbanColors.Ink)
            }
        }
        review.comment?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
        }
    }
}

@Composable
private fun StarRow(rating: Int, starModifier: Modifier) {
    Row(Modifier.semantics(mergeDescendants = true) { contentDescription = "$rating de 5 estrellas" }) {
        repeat(5) { i ->
            Icon(
                if (i < rating) Icons.Default.Star else Icons.Default.StarBorder,
                null,
                tint = if (i < rating) UrbanColors.Gold else UrbanColors.Muted.copy(alpha = 0.5f),
                modifier = starModifier
            )
        }
    }
}

@Composable
private fun ReviewForm(data: BarberProfileResponse, vm: BarberProfileViewModel, slug: String) {
    val sending by vm.sending.collectAsState()
    val reviewError by vm.reviewError.collectAsState()
    var rating by rememberSaveable { mutableIntStateOf(0) }
    var comment by rememberSaveable { mutableStateOf("") }
    var serviceId by rememberSaveable { mutableStateOf(data.reviewableServices.singleOrNull()?.id) }

    UrbanPremiumCard(Modifier.fillMaxWidth()) {
        Text("Califica tu visita", style = MaterialTheme.typography.titleMedium)
        Text("Tu opinión ayuda a otros clientes y suma puntos de lealtad.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)

        if (data.reviewableServices.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("¿Qué servicio calificas?", style = MaterialTheme.typography.labelLarge, color = UrbanColors.Ink)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                data.reviewableServices.forEach { s ->
                    FilterChip(
                        selected = serviceId == s.id,
                        onClick = { serviceId = if (serviceId == s.id) null else s.id },
                        label = { Text(s.nombre) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = UrbanColors.Gold, selectedLabelColor = UrbanColors.OnGold)
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("Calificación", style = MaterialTheme.typography.labelLarge, color = UrbanColors.Ink)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..5).forEach { n ->
                Icon(
                    if (n <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "$n estrella${if (n == 1) "" else "s"}",
                    tint = if (n <= rating) UrbanColors.Gold else UrbanColors.Muted,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .selectable(selected = rating == n, role = Role.RadioButton) { rating = n }
                        .padding(4.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        UrbanTextField(
            value = comment,
            onValueChange = { if (it.length <= 500) comment = it },
            label = "Comentario (opcional)",
            placeholder = "¿Cómo te fue con tu corte?",
            minLines = 3
        )
        reviewError?.let {
            Spacer(Modifier.height(8.dp))
            UrbanErrorBanner(it)
        }
        Spacer(Modifier.height(12.dp))
        UrbanPrimaryButton(
            text = if (sending) "Enviando…" else "Enviar reseña",
            onClick = { vm.review(slug, rating, comment, serviceId) },
            icon = Icons.Default.Send,
            enabled = rating > 0 && !sending,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
