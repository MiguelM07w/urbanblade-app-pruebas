package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.InventoryMovementRow
import com.urbanblade.mobile.data.model.WaitlistEntry
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanRow
import com.urbanblade.mobile.ui.components.UrbanRowGroup
import com.urbanblade.mobile.ui.components.UrbanEmptyState
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
import com.urbanblade.mobile.ui.components.UrbanStatStrip
import com.urbanblade.mobile.ui.components.UrbanOutlineButton
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.components.UrbanSkeletonList
import com.urbanblade.mobile.ui.components.UrbanTextField
import com.urbanblade.mobile.ui.components.UrbanTopBar
import com.urbanblade.mobile.ui.components.UrbanMascotState
import com.urbanblade.mobile.ui.components.UrbanStateKind
import com.urbanblade.mobile.ui.components.UrbanPageHeader
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.InventoryMovementsViewModel
import com.urbanblade.mobile.ui.viewmodel.MovementFilter
import com.urbanblade.mobile.ui.viewmodel.WaitlistFilter
import com.urbanblade.mobile.ui.viewmodel.WaitlistStaffViewModel

/** "2026-09-21T10:30:00+00:00" -> "21 sep · 10:30". Si no se puede leer, devuelve el texto original. */
internal fun formatWhen(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    val day = UrbanFormat.dateShort(iso)
    val time = if (iso.length >= 16 && iso[10] == 'T') iso.substring(11, 16) else null
    return if (time != null) "$day · $time" else day
}

/** Etiqueta en español del estado de una entrada de lista de espera. */
internal fun waitlistStatusLabel(estado: String): String = when (estado) {
    "activo" -> "En espera"
    "notificado" -> "Avisada"
    "reservado" -> "Reservó"
    "cancelado" -> "Canceló"
    "expirado" -> "Expiró"
    else -> estado.replaceFirstChar { it.uppercase() }
}

/** Historial de entradas y salidas de stock del inventario. */
@Composable
fun InventoryMovementsScreen(onBack: () -> Unit, vm: InventoryMovementsViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { UrbanTopBar("", onBack) { IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, "Actualizar") } } }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { UrbanPageHeader(title = "Movimientos de inventario", subtitle = "Entradas, salidas y ajustes de stock.", eyebrow = "OPERACIÓN") }
            // Tres números en una sola tarjeta (UrbanStatStrip), como pide la skill: con tres
            // UrbanMetricCard angostas la etiqueta "Entradas" se partía como "Entrad / as" a 360 dp.
            item {
                UrbanStatStrip(
                    listOf(
                        Triple("Entradas", state.stats.entradas.toString(), UrbanColors.Ink),
                        Triple("Salidas", state.stats.salidas.toString(), UrbanColors.Ink),
                        Triple("Hoy", state.stats.hoy.toString(), UrbanColors.Ink)
                    )
                )
            }
            item {
                UrbanTextField(
                    value = state.query,
                    onValueChange = vm::setQuery,
                    label = "Buscar por producto o motivo",
                    leadingIcon = Icons.Default.Search,
                    imeAction = ImeAction.Search,
                    onImeAction = { vm.load() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MovementFilter.entries.forEach { f ->
                        FilterChip(selected = state.filter == f, onClick = { vm.setFilter(f) }, label = { Text(f.label) })
                    }
                }
            }
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            state.error?.let { item { UrbanErrorBanner(it) } }
            if (state.loading && state.items.isEmpty()) item { UrbanSkeletonList(4) }

            if (!state.loading && state.items.isEmpty() && state.error == null) {
                item { UrbanMascotState(UrbanStateKind.EMPTY, "Sin movimientos", "Aquí aparecerán las entradas y salidas de stock.") }
            }
            if (state.items.isNotEmpty()) {
                item { UrbanSectionTitle("Historial", UrbanFormat.count(state.total, "movimiento", "movimientos")) }
            }
            if (state.items.isNotEmpty()) {
                item { UrbanRowGroup(items = state.items, row = { MovementCard(it) }) }
            }
            if (state.hasMore) {
                item {
                    UrbanOutlineButton(
                        text = if (state.loadingMore) "Cargando…" else "Cargar más movimientos",
                        onClick = { vm.loadMore() },
                        icon = Icons.Default.ExpandMore,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun MovementCard(m: InventoryMovementRow) {
    val entrada = m.tipo == "entrada"
    val tone = if (entrada) UrbanColors.Success else UrbanColors.Danger
    // Fila compacta del kit: antes era una tarjeta con borde por movimiento.
    UrbanRow(
        title = m.product?.nombre ?: "Producto",
        subtitle = listOfNotNull(
            m.motivo?.takeIf { it.isNotBlank() },
            m.appointment?.client?.let { "Cita de $it" },
            m.user?.name
        ).joinToString(" · ").ifEmpty { "Sin motivo" },
        meta = formatWhen(m.fecha),
        value = (if (entrada) "+" else "−") + m.cantidad,
        valueTone = tone,
        leading = {
            Box(Modifier.size(40.dp).clip(CircleShape).background(tone.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(if (entrada) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null, tint = tone, modifier = Modifier.size(20.dp))
            }
        }
    )
}

/** Lista de espera de todo el negocio, para que el personal sepa a quién avisar. */
@Composable
fun WaitlistStaffScreen(onBack: () -> Unit, vm: WaitlistStaffViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { UrbanTopBar("", onBack) { IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, "Actualizar") } } }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { UrbanPageHeader(title = "Lista de espera", subtitle = "Clientes que quieren un horario que ya estaba lleno.", eyebrow = "OPERACIÓN") }
            // En barber el personal no gestiona la lista: el aviso sale solo al liberarse un horario y solo el
            // cliente puede salirse. Se explica aquí para que recepción no busque botones que no existen.
            item {
                UrbanInfoBanner(
                    "Cuando se cancela una cita, UrbanBlade avisa a todos los anotados para ese barbero, servicio y día; el primero que reserve se queda el horario. Si el cliente llama, puedes reservarle desde la agenda.",
                    Icons.Default.NotificationsActive
                )
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WaitlistFilter.entries.forEach { f ->
                        FilterChip(selected = state.filter == f, onClick = { vm.setFilter(f) }, label = { Text(f.label) })
                    }
                }
            }
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            state.error?.let { item { UrbanErrorBanner(it) } }
            if (state.loading && state.items.isEmpty()) item { UrbanSkeletonList(3) }

            if (!state.loading && state.items.isEmpty() && state.error == null) {
                item {
                    UrbanMascotState(
                        UrbanStateKind.EMPTY,
                        "Nadie en esta lista",
                        if (state.filter == WaitlistFilter.Espera) "Cuando un cliente se anote por un horario lleno aparecerá aquí." else "No hay entradas con este estado."
                    )
                }
            }
            if (state.items.isNotEmpty()) {
                item { UrbanSectionTitle("Entradas", UrbanFormat.count(state.items.size, "entrada", "entradas")) }
            }
            if (state.items.isNotEmpty()) {
                item { UrbanRowGroup(items = state.items, row = { WaitlistCard(it) }) }
            }
        }
    }
}

@Composable
private fun WaitlistCard(entry: WaitlistEntry) {
    val tone = when (entry.estado) {
        "activo" -> UrbanColors.Warning
        "notificado" -> UrbanColors.Info
        "reservado" -> UrbanColors.Success
        else -> UrbanColors.Muted
    }
    // Fila compacta del kit. El estado va a la derecha con su propia etiqueta (statusStyle no
    // cubre los términos de lista de espera: "En espera", "Avisada"…).
    UrbanRow(
        title = entry.service?.nombre ?: "Servicio",
        // Sin nombre = la entrada apunta a un cliente que ya no existe; se dice claro en vez de un genérico.
        subtitle = entry.client?.name ?: "Cliente ya no registrado",
        meta = buildString {
            append("Con ${entry.barber?.name ?: "cualquier barbero"} · para el ${entry.fecha?.let { UrbanFormat.dateShort(it) } ?: "—"}")
            entry.notificadoEn?.let { append(" · Avisada el ${formatWhen(it)}") }
        },
        trailing = {
            Text(
                waitlistStatusLabel(entry.estado),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = tone
            )
        }
    )
}
