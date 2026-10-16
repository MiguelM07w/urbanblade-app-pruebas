package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.LogsViewModel
import com.urbanblade.mobile.ui.viewmodel.ReviewsViewModel

@Composable
fun ReviewsScreen(onBack: () -> Unit, vm: ReviewsViewModel = viewModel()) {
    val reviews by vm.reviews.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    var ratingFilter by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(ratingFilter) { vm.load(rating = ratingFilter) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { UrbanTopBar("", onBack) { IconButton(onClick = { vm.load(rating = ratingFilter) }) { Icon(Icons.Default.Refresh, "Actualizar") } } }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { UrbanPageHeader(title = "Reseñas", subtitle = "Lo que opinan tus clientes de cada barbero.", eyebrow = "Calidad") }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            error?.let { item { UrbanErrorBanner(it) } }

            item {
                UrbanStatStrip(
                    listOf(
                        Triple("Total", reviews.stats.total.toString(), UrbanColors.Ink),
                        Triple("Promedio", "%.1f★".format(reviews.stats.promedio), UrbanColors.Gold),
                        Triple("Bajas (≤2★)", reviews.stats.bajas.toString(), if (reviews.stats.bajas > 0) UrbanColors.Danger else UrbanColors.Ink)
                    )
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = ratingFilter == null, onClick = { ratingFilter = null }, label = { Text("Todas") }, colors = urbanFilterChipColors())
                    (5 downTo 1).forEach { r ->
                        FilterChip(selected = ratingFilter == r, onClick = { ratingFilter = if (ratingFilter == r) null else r }, label = { Text("$r★") }, colors = urbanFilterChipColors())
                    }
                }
            }

            if (reviews.data.isEmpty() && !busy) {
                item {
                    UrbanMascotState(
                        UrbanStateKind.EMPTY,
                        if (ratingFilter == null) "Aún no hay reseñas" else "Sin reseñas de ${ratingFilter}★",
                        if (ratingFilter == null) "Se piden al cliente dos horas después de completar su cita." else "Prueba con otra calificación."
                    )
                }
            }

            items(reviews.data) { review ->
                UrbanCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(review.barber?.name ?: "Barbero", style = MaterialTheme.typography.titleMedium)
                        Text("${review.rating}★", style = MaterialTheme.typography.titleMedium, color = UrbanColors.GoldText)
                    }
                    Text(review.client?.name ?: "Cliente anónimo", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                    review.comment?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

/** Etiqueta y color de cada evento de la bitácora (activitylog usa created/updated/deleted). */
internal fun logEventStyle(event: String?): Pair<String, androidx.compose.ui.graphics.Color> = when (event?.lowercase()) {
    "created" -> "Creó" to UrbanColors.Success
    "updated" -> "Editó" to UrbanColors.Info
    "deleted" -> "Eliminó" to UrbanColors.Danger
    null, "" -> "Actividad" to UrbanColors.Muted
    else -> event.replaceFirstChar { it.uppercase() } to UrbanColors.Gold
}

@Composable
fun LogsScreen(onBack: () -> Unit, vm: LogsViewModel = viewModel()) {
    val logs by vm.logs.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    var search by remember { mutableStateOf("") }
    var eventFilter by remember { mutableStateOf<String?>(null) }
    val reload = { vm.load(search.takeIf { it.isNotBlank() }, event = eventFilter) }

    LaunchedEffect(eventFilter) { reload() }

    UrbanModuleScreen(
        eyebrow = "ANÁLISIS",
        title = "Logs de actividad",
        subtitle = "Quién hizo qué y cuándo en el sistema.",
        onBack = onBack,
        onRefresh = { reload() },
        refreshing = busy
    ) {
        item {
            UrbanHeroCard {
                UrbanHeroLabel("Actividad de hoy")
                Spacer(Modifier.height(6.dp))
                Text(
                    UrbanFormat.count(logs.stats.hoy, "movimiento", "movimientos"),
                    style = MaterialTheme.typography.displaySmall,
                    color = UrbanColors.Ink
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = UrbanColors.Ink.copy(alpha = 0.22f))
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    UrbanHeroStat("Creados", logs.stats.creates.toString(), Icons.Default.AddCircle, Modifier.weight(1f), tone = UrbanColors.Success)
                    UrbanHeroStat("Eliminados", logs.stats.deletes.toString(), Icons.Default.RemoveCircle, Modifier.weight(1f), tone = UrbanColors.Danger)
                }
            }
        }

        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Buscar en la descripción…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { reload() }),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search)
            )
        }

        if (logs.events.isNotEmpty()) {
            item {
                Row(
                    Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = eventFilter == null, onClick = { eventFilter = null }, label = { Text("Todos") })
                    logs.events.forEach { ev ->
                        FilterChip(
                            selected = eventFilter == ev,
                            onClick = { eventFilter = if (eventFilter == ev) null else ev },
                            label = { Text(logEventStyle(ev).first) }
                        )
                    }
                }
            }
        }

        when {
            error != null && logs.data.isEmpty() -> item {
                UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar la bitácora", error, "Reintentar") { reload() }
            }
            logs.data.isEmpty() && !busy -> item {
                UrbanMascotState(UrbanStateKind.EMPTY, "Sin registros", "No hay actividad que coincida con la búsqueda o el filtro.")
            }
            else -> {
                error?.let { item { UrbanErrorBanner(it) } }
                items(logs.data) { log ->
                    val (label, tone) = logEventStyle(log.event)
                    UrbanAttentionRow(
                        icon = when (log.event?.lowercase()) {
                            "created" -> Icons.Default.AddCircle
                            "updated" -> Icons.Default.Edit
                            "deleted" -> Icons.Default.RemoveCircle
                            else -> Icons.Default.History
                        },
                        text = log.description ?: log.logName ?: "—",
                        subtitle = listOfNotNull(log.causer?.name ?: "Sistema", log.createdAt).joinToString(" · "),
                        tone = tone,
                        trailing = { SimpleStatusPill(label, tone) }
                    )
                }
            }
        }
    }
}
