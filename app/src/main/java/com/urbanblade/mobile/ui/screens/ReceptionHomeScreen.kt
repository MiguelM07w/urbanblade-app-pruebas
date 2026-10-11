package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.DashboardViewModel
import java.time.LocalDate
import java.util.Locale

/**
 * Inicio de recepción: el pulso del día (citas, cobrado, clientes nuevos), lo que requiere acción
 * (pagos por verificar, pedidos, stock) y las citas de hoy por confirmar. Antes veía el tablero
 * genérico, que no usaba nada de lo que barber ya le manda.
 */
@Composable
fun ReceptionHomeScreen(
    user: AuthUser,
    onNavigate: (String) -> Unit,
    vm: DashboardViewModel = viewModel()
) {
    val response by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    val board = parseReceptionDashboard(response?.data)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = com.urbanblade.mobile.ui.components.BladebotClearance),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            UrbanPageHeader(
                title = "Hola, ${user.name.substringBefore(' ')}",
                subtitle = UrbanFormat.date(LocalDate.now().toString()),
                eyebrow = "RECEPCIÓN",
                trailing = { UrbanAvatar(user.name, imageUrl = user.avatarUrl) }
            )
        }

        when {
            board == null && loading -> item { UrbanSkeletonList(2) }
            board == null && error != null -> item {
                UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar el día", error, "Reintentar") { vm.load() }
            }
            board != null -> {
                item {
                    UrbanHeroCard {
                        UrbanHeroLabel("Hoy en la barbería")
                        Spacer(Modifier.height(6.dp))
                        Text(
                            UrbanFormat.count(board.appointmentsToday, "cita", "citas"),
                            style = MaterialTheme.typography.displaySmall,
                            color = UrbanColors.Ink
                        )
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = UrbanColors.Ink.copy(alpha = 0.22f))
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            UrbanHeroStat("Cobrado hoy", UrbanFormat.moneyShort(board.collectedToday), Icons.Default.PointOfSale, Modifier.weight(1f))
                            UrbanHeroStat("Clientes nuevos", board.newClientsToday.toString(), Icons.Default.PersonAdd, Modifier.weight(1f))
                        }
                    }
                }

                // Resumen del día con IA local: carga aparte, no frena el inicio.
                item { com.urbanblade.mobile.ui.components.AiBriefingCard() }

                val attention = board.pendingPayments > 0 || board.pendingOrders > 0 || board.lowStock > 0
                item { UrbanSectionTitle("Requiere tu atención", if (attention) "Lo que conviene resolver primero." else null) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // pending_payments de barber = citas de hoy ya terminadas que no tienen cobro registrado.
                        if (board.pendingPayments > 0) UrbanAttentionRow(
                            Icons.Default.Payments,
                            UrbanFormat.count(board.pendingPayments, "cita terminada sin cobrar", "citas terminadas sin cobrar"),
                            "Cóbralas desde la agenda antes del corte de caja.",
                            UrbanColors.Danger,
                            onClick = { onNavigate("appointments") }
                        )
                        if (board.pendingOrders > 0) UrbanAttentionRow(
                            Icons.Default.ShoppingBag,
                            UrbanFormat.count(board.pendingOrders, "pedido por entregar", "pedidos por entregar"),
                            "Compras de la tienda esperando al cliente.",
                            UrbanColors.Warning,
                            onClick = { onNavigate("orders") }
                        )
                        if (board.lowStock > 0) UrbanAttentionRow(
                            Icons.Default.Inventory2,
                            UrbanFormat.count(board.lowStock, "producto con stock bajo", "productos con stock bajo"),
                            "Avisa para reabastecer.",
                            UrbanColors.Warning,
                            onClick = { onNavigate("inventory") }
                        )
                        if (!attention) UrbanInfoBanner("Todo al día: no hay cobros, pedidos ni inventario por atender.", Icons.Default.CheckCircle)
                    }
                }

                if (board.toConfirm.isNotEmpty()) {
                    item { UrbanSectionTitle("Por confirmar hoy", "Citas pendientes que siguen en el día.", "Ver agenda") { onNavigate("appointments") } }
                    // Una sola tarjeta con todas las filas, con separador entre ellas (antes: una
                    // tarjeta con borde por cita, que es lo que la auditoría pidió reducir).
                    item {
                        UrbanRowGroup(items = board.toConfirm) { appt ->
                            ToConfirmRow(appt) { onNavigate("appointments") }
                        }
                    }
                }
            }
        }

        response?.data.chartSeries("flowChart")?.let { flow ->
            item { UrbanDashboardChart("Citas por hora", "Cómo se reparte el día", flow) }
        }

        item { UrbanSectionTitle("Acciones rápidas", "Lo esencial del mostrador.") }
        item {
            UrbanModuleGrid(
                tiles = listOf(
                    Triple("Agenda", "Citas del día y de la semana", Icons.Default.CalendarMonth),
                    Triple("Cobros", "Pagos y comprobantes", Icons.Default.Payments),
                    Triple("Clientes", "Buscar y registrar", Icons.Default.People),
                    Triple("Lista de espera", "Clientes esperando horario", Icons.Default.HourglassTop),
                    Triple("Tarjetas regalo", "Vender y consultar saldo", Icons.Default.CardGiftcard),
                    Triple("Corte de caja", "Cierre del turno", Icons.Default.PointOfSale),
                ),
                routes = listOf("appointments", "payments", "clients", "waitlist_staff", "gift_cards", "cash"),
                onNavigate = onNavigate
            )
        }
    }
}

@Composable
private fun ToConfirmRow(appt: ReceptionAppointment, onClick: () -> Unit) {
    // Fila compacta del kit: la hora como distintivo a la izquierda y el chevron a la derecha.
    UrbanRow(
        title = appt.cliente,
        subtitle = "${appt.servicio} · ${appt.barbero}",
        onClick = onClick,
        leading = {
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(UrbanColors.Gold.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(UrbanFormat.time(appt.hora), style = MaterialTheme.typography.titleSmall, color = UrbanColors.Gold)
            }
        },
        trailing = { Icon(Icons.Default.ChevronRight, null, tint = UrbanColors.Muted) }
    )
}
