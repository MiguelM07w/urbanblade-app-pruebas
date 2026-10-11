package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.R
import com.google.gson.JsonObject
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.components.UrbanDashboardChart
import com.urbanblade.mobile.ui.components.chartSeries
import com.urbanblade.mobile.ui.components.UrbanAttentionRow
import com.urbanblade.mobile.ui.components.UrbanAvatar
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanHeroCard
import com.urbanblade.mobile.ui.components.UrbanHeroLabel
import com.urbanblade.mobile.ui.components.UrbanHeroStat
import com.urbanblade.mobile.ui.components.UrbanStatCard
import com.urbanblade.mobile.ui.components.UrbanTrendPill
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
import com.urbanblade.mobile.ui.components.UrbanMetricCard
import com.urbanblade.mobile.ui.components.UrbanOutlineButton
import com.urbanblade.mobile.ui.components.UrbanPageHeader
import com.urbanblade.mobile.ui.components.UrbanPremiumCard
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.components.UrbanSkeletonList
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.AdminHomeViewModel
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs

private fun JsonObject?.number(key: String): Double? =
    this?.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asDouble

private fun JsonObject?.text(key: String): String? =
    this?.get(key)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }

/**
 * Inicio del administrador: cómo va el negocio hoy, qué requiere su atención y accesos directos a
 * sus módulos. Reemplaza al tablero genérico de cuatro indicadores de citas (sin ingresos).
 */
@Composable
fun AdminHomeScreen(
    user: AuthUser,
    onNavigate: (String) -> Unit,
    vm: AdminHomeViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    val kpis = state.kpis
    val firstName = user.name.substringBefore(' ')
    val lowStock = kpis.number("low_stock_count")?.toInt() ?: 0

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = com.urbanblade.mobile.ui.components.BladebotClearance),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            UrbanPageHeader(
                title = "Hola, $firstName",
                subtitle = UrbanFormat.date(LocalDate.now().toString()),
                eyebrow = "ADMINISTRADOR",
                trailing = { UrbanAvatar(user.name, imageUrl = user.avatarUrl) }
            )
        }

        when {
            state.loading && kpis == null -> item { UrbanSkeletonList(2) }
            state.error != null && kpis == null -> item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    UrbanErrorBanner(state.error.orEmpty())
                    UrbanOutlineButton(
                        text = "Reintentar",
                        onClick = { vm.load() },
                        icon = Icons.Default.Refresh,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            kpis != null -> {
                item { TodayPulseCard(kpis, state.occupancyRate) }
                item { MonthKpis(kpis) }
                // Resumen del día con IA local: carga aparte, no frena el inicio.
                item { com.urbanblade.mobile.ui.components.AiBriefingCard() }

                item { UrbanSectionTitle("Requiere tu atención", "Lo que conviene revisar ahora.") }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (state.pendingPayments > 0) {
                            UrbanAttentionRow(
                                icon = Icons.Default.Payments,
                                text = UrbanFormat.count(state.pendingPayments, "pago por verificar", "pagos por verificar"),
                                subtitle = "Citas completadas · Revisa y confirma",
                                tone = UrbanColors.Danger,
                                onClick = { onNavigate("payments") }
                            )
                        }
                        if (lowStock > 0) {
                            UrbanAttentionRow(
                                icon = Icons.Default.Inventory2,
                                text = UrbanFormat.count(lowStock, "producto con stock bajo", "productos con stock bajo"),
                                subtitle = "Es momento de reabastecer",
                                tone = UrbanColors.Warning,
                                onClick = { onNavigate("inventory") }
                            )
                        }
                        if (state.pendingPayments == 0 && lowStock == 0) {
                            UrbanInfoBanner("Todo al día: no hay pagos ni inventario por atender.", Icons.Default.CheckCircle)
                        }
                    }
                }

            }
        }

        // Las mismas gráficas principales que el dashboard web (mismos datos de /dashboard).
        state.data.chartSeries("incomeChart")?.let { income ->
            item { UrbanDashboardChart("Ingresos por semana", "Últimas 8 semanas", income, format = UrbanFormat::moneyShort) }
        }
        state.data.chartSeries("servicesChart")?.let { services ->
            item { UrbanDashboardChart("Servicios más pedidos", "Citas por servicio", services, categories = true) }
        }

        item { UrbanSectionTitle("Acciones rápidas", "Lo esencial, siempre a la mano.") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeTile("Agenda", "Ver y gestionar citas", Icons.Default.CalendarMonth, { onNavigate("appointments") }, Modifier.weight(1f), highlighted = true)
                HomeTile("Caja", "Cobros, ingresos y cierre", Icons.Default.PointOfSale, { onNavigate("cash") }, Modifier.weight(1f))
            }
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun TodayPulseCard(kpis: JsonObject, occupancyRate: Int?) {
    val incomeToday = kpis.number("income_today") ?: 0.0
    val appointmentsToday = kpis.number("appointments_today")?.toInt() ?: 0
    val incomeWeek = kpis.number("income_week")
    val appointmentsWeek = kpis.number("appointments_week")?.toInt()

    // Misma tarjeta héroe del patrón compartido (UrbanHeroCard), no una copia local con la foto
    // y el velo: así el velo sigue el tema activo y no hay que repetir la pintura.
    UrbanHeroCard {
        UrbanHeroLabel("Ingresos de hoy")
        Spacer(Modifier.height(6.dp))
        Text(UrbanFormat.moneyShort(incomeToday), style = MaterialTheme.typography.displaySmall, color = UrbanColors.Ink)
        kpis.number("income_growth")?.let { growth ->
            Spacer(Modifier.height(6.dp))
            UrbanTrendPill(delta = growth, suffix = "vs. mes pasado")
        }
        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = UrbanColors.Ink.copy(alpha = 0.22f))
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            UrbanHeroStat("Citas hoy", appointmentsToday.toString(), Icons.Default.CalendarMonth, Modifier.weight(1f))
            UrbanHeroStat("Ocupación", occupancyRate?.let { "$it%" } ?: "—", Icons.Default.People, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            buildString {
                append("Esta semana: ")
                append(incomeWeek?.let { UrbanFormat.moneyShort(it) } ?: "—")
                if (appointmentsWeek != null) append(" · ${UrbanFormat.count(appointmentsWeek, "cita", "citas")}")
            },
            style = MaterialTheme.typography.bodySmall,
            color = UrbanColors.Muted,
        )
    }
}

/**
 * Lo del mes con el KPI del kit (el mismo que la web usa como UiStatCard).
 *
 * Va sin variación a propósito: el `income_growth` que manda barber es el crecimiento de *hoy*
 * contra el mismo día del mes pasado, no el del mes; ponerlo aquí afirmaría un dato falso. El kit ya
 * cubre ese caso (delta = null → solo el valor y su leyenda). Reemplaza a MonthGrid, que estaba en el
 * archivo sin que nadie lo llamara, y a TopBarberCard, que tampoco se usaba.
 */
@Composable
private fun MonthKpis(kpis: JsonObject) {
    val monthIncome = kpis.number("income_month")
    val monthAppointments = kpis.number("appointments_month")?.toInt()
    if (monthIncome == null && monthAppointments == null) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        monthIncome?.let {
            UrbanStatCard(
                label = "Ingresos del mes",
                value = UrbanFormat.moneyShort(it),
                caption = "Este mes",
                icon = Icons.Default.AccountBalanceWallet,
                modifier = Modifier.weight(1f)
            )
        }
        monthAppointments?.let {
            UrbanStatCard(
                label = "Citas del mes",
                value = it.toString(),
                caption = "Este mes",
                icon = Icons.Default.CalendarMonth,
                modifier = Modifier.weight(1f)
            )
        }
        if (monthIncome == null || monthAppointments == null) Spacer(Modifier.weight(1f))
    }
}
