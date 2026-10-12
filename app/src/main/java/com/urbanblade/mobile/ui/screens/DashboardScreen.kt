package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.JsonObject
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    user: AuthUser,
    onAppointments: () -> Unit,
    onBook: () -> Unit,
    onWallet: () -> Unit = {},
    onStore: () -> Unit = {},
    onExplore: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    vm: DashboardViewModel = viewModel()
) {
    // El administrador y el cliente tienen su propio inicio; el resto de roles conserva el tablero.
    val primaryRole = com.urbanblade.mobile.core.access.UserExperience.primaryRole(user.roles)
    if (primaryRole == "administrador") {
        AdminHomeScreen(user, onNavigate)
        return
    }
    // Recepción: el pulso del día y lo que hay que atender en el mostrador.
    if (primaryRole == "recepcionista") {
        ReceptionHomeScreen(user, onNavigate)
        return
    }
    // El ingeniero es de solo lectura: su inicio es la salud del servidor y sus módulos de análisis.
    if (primaryRole == "ingeniero") {
        EngineerHomeScreen(user, onNavigate)
        return
    }
    // El barbero ve "Mi día": su siguiente cita y lo que falta confirmar, no el tablero del negocio.
    if (primaryRole == "barbero") {
        BarberHomeScreen(user, onNavigate)
        return
    }
    // El cliente tiene su propio inicio (próxima cita y atajos); el personal conserva el tablero.
    if (primaryRole == "cliente") {
        ClientHomeScreen(user, onAppointments, onBook, onWallet, onStore, onExplore, onNavigate)
        return
    }

    val dashboard by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    val firstName = user.name.substringBefore(' ')
    val canBook = com.urbanblade.mobile.core.access.UserExperience.canBook(user.roles)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = com.urbanblade.mobile.ui.components.BladebotClearance),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            UrbanPageHeader(
                title = "Hola, $firstName",
                subtitle = "Tu jornada UrbanBlade está lista.",
                eyebrow = primaryRole.ifBlank { "UrbanBlade" },
                trailing = { UrbanAvatar(user.name, imageUrl = user.avatarUrl) }
            )
        }

        item {
            UrbanPremiumCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(UrbanColors.current.label.uppercase(), style = MaterialTheme.typography.labelMedium, color = UrbanColors.GoldText)
                        Spacer(Modifier.height(6.dp))
                        Text("Todo el negocio,\nen tu bolsillo.", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Agenda, clientes, pagos y operación, siempre a la mano.",
                            style = MaterialTheme.typography.bodySmall,
                            color = UrbanColors.Muted
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.size(74.dp)) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = MaterialTheme.shapes.large,
                            color = UrbanColors.Gold.copy(alpha = 0.12f)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                Icon(Icons.Default.ContentCut, null, tint = UrbanColors.Gold, modifier = Modifier.size(34.dp))
                            }
                        }
                    }
                }
            }
        }

        if (loading) item { UrbanSkeletonList(3) }
        error?.let { item { UrbanErrorBanner(it) } }

        dashboard?.let { response ->
            val kpis = extractKpis(response.data)
            if (kpis.isNotEmpty()) {
                item {
                    UrbanSectionTitle(
                        title = "Resumen",
                        subtitle = "Tus indicadores clave"
                    )
                }
                items(kpis.chunked(2)) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (label, value) ->
                            UrbanMetricCard(
                                label = label,
                                value = value,
                                icon = iconForKpi(label),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            UrbanSectionTitle(
                title = "Acciones rápidas",
                subtitle = "Lo que más usas, a un toque."
            )
        }
        item {
            UrbanQuickAction(
                title = "Mis citas",
                subtitle = "Consulta próximas y anteriores",
                icon = Icons.Default.CalendarMonth,
                onClick = onAppointments,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (canBook) {
            item {
                UrbanQuickAction(
                    title = "Reservar cita",
                    subtitle = "Solo horarios realmente disponibles",
                    icon = Icons.Default.ContentCut,
                    onClick = onBook,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item { Spacer(Modifier.height(4.dp)) }
    }
}

private fun iconForKpi(label: String): ImageVector = when {
    label.contains("ingreso", ignoreCase = true) -> Icons.Default.Payments
    label.contains("cliente", ignoreCase = true) -> Icons.Default.People
    label.contains("cita", ignoreCase = true) -> Icons.Default.EventAvailable
    else -> Icons.Default.Insights
}

private fun extractKpis(data: JsonObject): List<Pair<String, String>> {
    val source = data.getAsJsonObject("kpis") ?: data
    val friendly = mapOf(
        "appointments_today" to "Citas hoy",
        "appointments_week" to "Citas esta semana",
        "revenue_today" to "Ingresos hoy",
        "revenue_month" to "Ingresos del mes",
        "clients_total" to "Clientes",
        "total_clients" to "Clientes",
        "pending_appointments" to "Citas pendientes",
        "appointments_month" to "Citas del mes",
        "total_appointments" to "Citas totales",
        "completion_rate" to "Tasa de finalización",
        "cancellation_rate" to "Tasa de cancelación",
        "no_show_rate" to "Tasa de inasistencia",
        "income_today" to "Ingresos hoy",
        "income_week" to "Ingresos de la semana",
        "income_month" to "Ingresos del mes",
        "income_growth" to "Crecimiento de ingresos",
        "top_barber_name" to "Barbero destacado",
        "top_barber_total" to "Ventas del barbero destacado",
        "recurring_clients" to "Clientes recurrentes",
        "active_clients" to "Clientes activos",
        "retention_rate" to "Retención de clientes",
        "low_stock_count" to "Productos con stock bajo",
        "appointments" to "Citas",
        "appointment_growth" to "Crecimiento de citas",
        "appointments_growth" to "Crecimiento de citas",
        "completed_appointments" to "Citas completadas",
        "cancelled_appointments" to "Citas canceladas",
        "no_shows" to "Inasistencias",
        "revenue" to "Ingresos",
        "revenue_week" to "Ingresos de la semana",
        "clients" to "Clientes",
        "new_clients" to "Clientes nuevos",
        "barbers" to "Barberos",
        "products" to "Productos",
        "low_stock" to "Stock bajo",
        "orders" to "Pedidos",
        "orders_today" to "Pedidos hoy",
        "payments" to "Pagos",
        "growth" to "Crecimiento",
        "rating" to "Calificación",
        "average_rating" to "Calificación promedio"
    )
    val result = mutableListOf<Pair<String, String>>()
    for ((key, value) in source.entrySet()) {
        if (!value.isJsonPrimitive) continue
        val primitive = value.asJsonPrimitive
        if (!primitive.isNumber && !primitive.isString) continue
        // La API puede mandar la clave con mayúsculas o espacios ("Appointments"); se normaliza
        // para que la traducción no dependa de cómo venga escrita.
        val normalized = key.trim().lowercase().replace(' ', '_')
        val label = friendly[normalized] ?: key.replace('_', ' ').replaceFirstChar { it.uppercase() }
        // Tasas y crecimientos vienen como número suelto; con el símbolo se entienden solos.
        val isPercent = primitive.isNumber && (normalized.endsWith("_rate") || normalized.endsWith("_growth"))
        result += label to (primitive.asString + if (isPercent) "%" else "")
        if (result.size == 4) break
    }
    return result
}
