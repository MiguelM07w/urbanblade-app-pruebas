package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.data.model.OrderRow
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanEmptyState
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanMascotState
import com.urbanblade.mobile.ui.components.UrbanPageHeader
import com.urbanblade.mobile.ui.components.UrbanStateKind
import com.urbanblade.mobile.ui.components.UrbanFieldLabel
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
import com.urbanblade.mobile.ui.components.UrbanKeyValue
import com.urbanblade.mobile.ui.components.UrbanOutlineButton
import com.urbanblade.mobile.ui.components.UrbanPremiumCard
import com.urbanblade.mobile.ui.components.UrbanPrimaryButton
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.components.UrbanSkeletonList
import com.urbanblade.mobile.ui.components.UrbanStatusPill
import com.urbanblade.mobile.ui.components.UrbanTextField
import com.urbanblade.mobile.ui.components.UrbanTopBar
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.ORDER_PAYMENT_METHODS
import com.urbanblade.mobile.ui.viewmodel.OrderFilter
import com.urbanblade.mobile.ui.viewmodel.OrdersViewModel

private fun money(value: Double) = UrbanFormat.money(value)
private fun moneyShort(value: Double) = UrbanFormat.moneyShort(value)

/** Pedidos de la tienda: el personal los gestiona (entregar, cobrar, cancelar); el cliente sigue los suyos. */
@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
fun OrdersScreen(user: AuthUser, onBack: () -> Unit, vm: OrdersViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val staff = user.roles.any { it == "administrador" || it == "recepcionista" }
    var delivering by remember { mutableStateOf<OrderRow?>(null) }
    var cancelling by remember { mutableStateOf<OrderRow?>(null) }
    val priorityOrders = remember(state.items, state.filter, state.query, staff) {
        if (staff && state.filter == OrderFilter.Todos && state.query.isBlank()) {
            state.items.filter { it.estado.equals("pendiente", ignoreCase = true) }
        } else emptyList()
    }
    val historyOrders = remember(state.items, priorityOrders) { state.items.filterNot { it in priorityOrders } }

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { UrbanTopBar("", onBack) { IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, "Actualizar") } } }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                UrbanPageHeader(
                    title = if (staff) "Pedidos" else "Mis pedidos",
                    subtitle = if (staff && priorityOrders.isNotEmpty()) {
                        UrbanFormat.count(priorityOrders.size, "pedido listo para entregar", "pedidos listos para entregar")
                    } else if (staff) "Entrega, cobra o cancela los pedidos de la tienda." else "Sigue tus compras de la tienda.",
                    eyebrow = if (staff) "OPERACIÓN" else "CUENTA"
                )
            }
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            state.notice?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }
            if (state.error != null && state.items.isEmpty() && !state.loading) {
                item { UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar los pedidos", state.error, "Reintentar") { vm.load() } }
            } else {
                state.error?.let { item { UrbanErrorBanner(it) } }
            }

            if (staff) {
                state.stats?.let { stats ->
                    item {
                        UrbanPremiumCard(Modifier.fillMaxWidth()) {
                            Text("POR COBRAR", style = MaterialTheme.typography.labelLarge, color = UrbanColors.Gold)
                            Text(moneyShort(stats.porCobrar), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
                            Text("en pedidos pendientes de entregar", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = UrbanColors.Line)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                OrderSummaryValue("Pendientes", stats.pendientes.toString(), Modifier.weight(1f), stats.pendientes > 0)
                                OrderSummaryValue("Entregados", stats.entregados.toString(), Modifier.weight(1f))
                            }
                        }
                    }
                }
                item {
                    UrbanTextField(
                        value = state.query,
                        onValueChange = vm::setQuery,
                        label = "Buscar por folio",
                        leadingIcon = Icons.Default.Search,
                        imeAction = ImeAction.Search,
                        onImeAction = { vm.load() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                // Los cuatro filtros a la vista: si no caben en una línea bajan a la siguiente (antes «Cancelados» quedaba cortado).
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrderFilter.entries.forEach { f ->
                        FilterChip(selected = state.filter == f, onClick = { vm.setFilter(f) }, label = { Text(f.label) })
                    }
                }
            }

            if (state.loading && state.items.isEmpty()) item { UrbanSkeletonList(3) }
            if (!state.loading && state.items.isEmpty() && state.error == null) {
                item {
                    UrbanMascotState(
                        UrbanStateKind.EMPTY,
                        if (state.filter == OrderFilter.Todos && state.query.isBlank()) "Aún no hay pedidos" else "Sin resultados",
                        if (staff) "Los pedidos de la tienda aparecerán aquí." else "Tus compras de la tienda aparecerán aquí."
                    )
                }
            }
            if (priorityOrders.isNotEmpty()) {
                item { UrbanSectionTitle("Por entregar ahora", UrbanFormat.count(priorityOrders.size, "pedido pendiente", "pedidos pendientes")) }
                items(priorityOrders, key = { it.id }) { order ->
                    OrderCard(order, staff, state.busyOrderId == order.id, { vm.clearMessages(); cancelling = order }, { vm.clearMessages(); delivering = order })
                }
            }
            if (historyOrders.isNotEmpty()) {
                item {
                    UrbanSectionTitle(
                        if (priorityOrders.isNotEmpty()) "Historial reciente" else if (staff) "Bandeja" else "Tus pedidos",
                        UrbanFormat.count(if (priorityOrders.isNotEmpty()) historyOrders.size else state.total, "pedido", "pedidos")
                    )
                }
                items(historyOrders, key = { it.id }) { order ->
                    OrderCard(order, staff, state.busyOrderId == order.id, { vm.clearMessages(); cancelling = order }, { vm.clearMessages(); delivering = order })
                }
            }
            if (state.hasMore) {
                item {
                    UrbanOutlineButton(
                        text = if (state.loadingMore) "Cargando…" else "Cargar más pedidos",
                        onClick = { vm.loadMore() },
                        icon = Icons.Default.ExpandMore,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    delivering?.let { order ->
        DeliverDialog(
            order = order,
            onDismiss = { delivering = null },
            onConfirm = { method -> vm.deliver(order.id, method); delivering = null }
        )
    }

    cancelling?.let { order ->
        AlertDialog(
            onDismissRequest = { cancelling = null },
            containerColor = UrbanColors.Card,
            title = { Text("Cancelar pedido") },
            text = {
                Text(
                    if (staff) "¿Cancelar el pedido ${order.folio ?: ""}? El stock de sus productos volverá al inventario."
                    else "¿Cancelar tu pedido ${order.folio ?: ""}?",
                    color = UrbanColors.Muted
                )
            },
            confirmButton = { TextButton(onClick = { vm.cancel(order.id); cancelling = null }) { Text("Sí, cancelar", color = UrbanColors.Danger) } },
            dismissButton = { TextButton(onClick = { cancelling = null }) { Text("Volver") } }
        )
    }
}

@Composable
private fun OrderCard(order: OrderRow, staff: Boolean, busy: Boolean, onCancel: () -> Unit, onDeliver: () -> Unit) {
    UrbanCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(order.folio ?: "Pedido", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
                Text(formatWhen(order.createdAt), style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            }
            UrbanStatusPill(order.estado)
        }
        Spacer(Modifier.height(12.dp))
        if (staff) order.client?.name?.let { UrbanKeyValue("Cliente", it) }
        order.items.take(4).forEach { line ->
            UrbanKeyValue("${line.cantidad} × ${line.nombre ?: "Producto"}", money(line.subtotal))
        }
        if (order.items.size > 4) Text("+${order.items.size - 4} productos más", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = UrbanColors.Line)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Total", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
                order.metodoPago?.let { Text("Pagado con $it", style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted) }
            }
            Text(money(order.total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = UrbanColors.Gold)
        }
        if (order.estado == "pendiente") {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UrbanOutlineButton("Cancelar", onCancel, Modifier.weight(1f))
                if (staff) {
                    UrbanPrimaryButton("Entregar y cobrar", onDeliver, Modifier.weight(1f), enabled = !busy, loading = busy)
                }
            }
        }
    }
}

@Composable
private fun OrderSummaryValue(label: String, value: String, modifier: Modifier = Modifier, attention: Boolean = false) {
    Column(modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = if (attention) UrbanColors.Warning else UrbanColors.Ink)
    }
}

/** Confirma la entrega y el método con el que se cobra: es dinero real, no un toque suelto. */
@Composable
private fun DeliverDialog(order: OrderRow, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var method by remember { mutableStateOf(ORDER_PAYMENT_METHODS.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UrbanColors.Card,
        title = { Text("Entregar pedido") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Confirma que ${order.client?.name ?: "el cliente"} pagó ${money(order.total)} por el pedido ${order.folio ?: ""}.",
                    color = UrbanColors.Muted
                )
                UrbanFieldLabel("Método de pago")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ORDER_PAYMENT_METHODS.forEach { m ->
                        FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m.replaceFirstChar { it.uppercase() }) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(method) }) { Text("Entregar y cobrar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Volver") } }
    )
}
