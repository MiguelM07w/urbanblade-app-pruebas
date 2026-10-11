package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.AppointmentRow
import com.urbanblade.mobile.data.model.AuthUser
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Payments
import com.urbanblade.mobile.ui.components.UrbanAttentionRow
import com.urbanblade.mobile.ui.components.UrbanAvatar
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
import com.urbanblade.mobile.ui.components.UrbanHeroCard
import com.urbanblade.mobile.ui.components.UrbanHeroLabel
import com.urbanblade.mobile.ui.components.UrbanMascotState
import com.urbanblade.mobile.ui.components.UrbanStateKind
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanOutlineButton
import com.urbanblade.mobile.ui.components.UrbanPageHeader
import com.urbanblade.mobile.ui.components.UrbanPremiumCard
import com.urbanblade.mobile.ui.components.UrbanPrimaryButton
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.components.UrbanSkeletonList
import com.urbanblade.mobile.ui.components.UrbanStatusPill
import com.urbanblade.mobile.ui.theme.MascotMood
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.theme.mascot
import com.urbanblade.mobile.ui.viewmodel.AppointmentsViewModel
import com.urbanblade.mobile.ui.viewmodel.NotificationsViewModel
import com.urbanblade.mobile.ui.components.UrbanModuleGrid
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Replay
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Stars
import com.urbanblade.mobile.ui.viewmodel.LoyaltySummaryViewModel
import java.time.LocalTime

/**
 * Inicio del cliente: su próxima cita con "Reservar cita" a la vista (o una invitación a
 * reservar), lo que conviene resolver, repetir su última visita, su nivel (a un toque de
 * Beneficios) y lo que se descubre (Muro y Tienda). Pagos y pedidos viven en Cuenta. El personal conserva el tablero con indicadores de DashboardScreen.
 */
@Composable
fun ClientHomeScreen(
    user: AuthUser,
    onAppointments: () -> Unit,
    onBook: () -> Unit,
    onWallet: () -> Unit,
    onStore: () -> Unit,
    onExplore: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    vm: AppointmentsViewModel = viewModel(),
    notificationsVm: NotificationsViewModel = viewModel(),
    loyaltyVm: LoyaltySummaryViewModel = viewModel()
) {
    val response by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) {
        vm.load()
        notificationsVm.load()
        loyaltyVm.load()
    }
    val loyalty by loyaltyVm.loyalty.collectAsState()
    val notifications by notificationsVm.data.collectAsState()
    val unread = parseNotifications(notifications).unread

    val firstName = user.name.substringBefore(' ')
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Buenos días"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }
    val homeNotice by com.urbanblade.mobile.core.config.UrbanRemoteConfig.homeNotice.collectAsState()
    val next = response.next
    val failed = error != null && next == null
    // Citas ya aprobadas que todavía no se pagan: el cliente puede pagarlas desde Mis citas.
    val toPay = response.data.count { it.isChargeable && !it.hasPayment && it.code != null }
    val lastVisit = response.data
        .filter { it.estado == "completada" && it.service?.id != null }
        .maxByOrNull { it.fecha.take(10) + it.horaInicio }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = com.urbanblade.mobile.ui.components.BladebotClearance),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            UrbanPageHeader(
                title = firstName,
                subtitle = "¿Listo para tu próximo corte?",
                eyebrow = greeting.uppercase(),
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // El cliente no tiene la pestaña "Más": sus avisos se abren desde aquí.
                        IconButton(onClick = { onNavigate("notifications") }) {
                            BadgedBox(badge = { if (unread > 0) Badge { Text(if (unread > 9) "9+" else unread.toString()) } }) {
                                Icon(Icons.Default.Notifications, "Notificaciones", tint = UrbanColors.Gold)
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        UrbanAvatar(user.name, imageUrl = user.avatarUrl)
                    }
                }
            )
        }

        // Aviso para todos los clientes desde Firebase Remote Config (vacío = no se muestra).
        if (homeNotice.isNotBlank()) item { UrbanInfoBanner(homeNotice, Icons.Default.Campaign, Modifier.fillMaxWidth()) }

        when {
            loading && next == null -> item { UrbanSkeletonList(1) }
            failed -> item {
                UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar tus citas", error, "Reintentar") { vm.load() }
            }
            next != null -> {
                item { NextAppointmentCard(next, onClick = onAppointments) }
                item {
                    UrbanPrimaryButton(text = "Reservar cita", onClick = onBook, icon = Icons.Default.ContentCut, modifier = Modifier.fillMaxWidth())
                }
            }
            else -> item { EmptyAgendaCard(onBook = onBook) }
        }

        if (toPay > 0 || next?.estado == "pendiente") {
            item { UrbanSectionTitle("Para ti", "Lo que conviene resolver.") }
            if (toPay > 0) item {
                UrbanAttentionRow(
                    Icons.Default.Payments,
                    UrbanFormat.count(toPay, "cita por pagar", "citas por pagar"),
                    "Págala con tarjeta o transferencia desde Mis citas.",
                    UrbanColors.Warning,
                    onClick = onAppointments
                )
            }
            if (next?.estado == "pendiente") item {
                UrbanAttentionRow(
                    Icons.Default.HourglassTop,
                    "Tu barbero aún no confirma tu cita",
                    "Te avisaremos en cuanto la confirme.",
                    UrbanColors.Info,
                    onClick = onAppointments
                )
            }
        }

        // Repetir la última visita es lo que más hace un cliente frecuente: servicio y barbero ya elegidos.
        lastVisit?.let { visit ->
            item { UrbanSectionTitle("Vuelve a reservar", "Lo mismo de tu última visita.") }
            item {
                RebookCard(visit) {
                    val params = listOfNotNull(
                        visit.service?.id?.let { "serviceId=$it" },
                        visit.barber?.id?.let { "barberId=$it" }
                    )
                    if (params.isEmpty()) onBook() else onNavigate("booking?" + params.joinToString("&"))
                }
            }
        }

        // Nivel y puntos a un toque de Beneficios (ya no es pestaña; también está en Cuenta).
        loyalty?.let { l ->
            item {
                UrbanAttentionRow(
                    Icons.Default.Stars,
                    "Nivel ${l.nivelLabel ?: l.nivel ?: ""} · ${UrbanFormat.count(l.puntos, "punto", "puntos")}",
                    l.nextNivelLabel?.let { next -> "Te faltan ${UrbanFormat.count(l.citasFaltan, "cita", "citas")} para $next. Ver beneficios." }
                        ?: "Estás en el nivel más alto. Ver beneficios.",
                    UrbanColors.Gold,
                    onClick = onWallet
                )
            }
        }

        // Pagos y pedidos viven en Cuenta; aquí solo lo que se descubre.
        item { UrbanSectionTitle("Descubre", "Inspiración para tu próximo corte y productos.") }
        item {
            UrbanModuleGrid(
                tiles = listOf(
                    Triple("Muro de Inspiración", "Cortes del equipo", Icons.Default.Groups),
                    Triple("Tienda", "Productos UrbanBlade", Icons.Default.Storefront),
                ),
                routes = listOf("social", "store"),
                onNavigate = { route -> if (route == "store") onStore() else onNavigate(route) }
            )
        }
        // Bladebot ya no tiene tarjeta aquí: la burbuja flotante lo abre desde cualquier pantalla principal.
        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun NextAppointmentCard(next: AppointmentRow, onClick: () -> Unit) {
    UrbanHeroCard(Modifier.clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { UrbanHeroLabel("Tu próxima cita") }
            UrbanStatusPill(next.estado)
        }
        Spacer(Modifier.height(8.dp))
        Text(UrbanFormat.time(next.horaInicio), style = MaterialTheme.typography.displaySmall, color = UrbanColors.Ink)
        Text(UrbanFormat.date(next.fecha), style = MaterialTheme.typography.titleMedium, color = UrbanColors.Gold)
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = UrbanColors.Ink.copy(alpha = 0.22f))
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            UrbanAvatar(next.barber?.user?.name ?: "Barbero", Modifier.size(44.dp), imageUrl = rememberBarberPhoto(next.barber))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(serviceIcon(next.service?.nombre.orEmpty()), null, tint = UrbanColors.Gold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(next.service?.nombre ?: "Servicio", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Ink)
                }
                Text(
                    next.barber?.user?.name ?: "Barbero por confirmar",
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted
                )
            }
            (next.precioCobrado ?: next.service?.precio)?.let {
                Text("${UrbanFormat.moneyShort(it)}", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Gold)
            }
        }
    }
}

@Composable
private fun RebookCard(visit: AppointmentRow, onRebook: () -> Unit) {
    UrbanCard(Modifier.fillMaxWidth(), onClick = onRebook) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(UrbanColors.CardAlt),
                contentAlignment = Alignment.Center
            ) {
                Icon(serviceIcon(visit.service?.nombre.orEmpty()), null, tint = UrbanColors.Gold, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(visit.service?.nombre ?: "Servicio", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Ink)
                Text(
                    listOfNotNull(visit.barber?.user?.name?.let { "Con $it" }, UrbanFormat.dateShort(visit.fecha)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            visit.service?.precio?.let {
                Spacer(Modifier.width(8.dp))
                Text("${UrbanFormat.moneyShort(it)}", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Ink)
            }
        }
        Spacer(Modifier.height(14.dp))
        UrbanOutlineButton(
            text = "Reservar de nuevo",
            onClick = onRebook,
            icon = Icons.Default.Replay,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EmptyAgendaCard(onBook: () -> Unit) {
    UrbanHeroCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                UrbanHeroLabel("Tu agenda está libre")
                Spacer(Modifier.height(6.dp))
                Text("¿Te toca corte?", style = MaterialTheme.typography.headlineSmall, color = UrbanColors.Ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Elige servicio, barbero y horario real en menos de un minuto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted
                )
            }
            Spacer(Modifier.width(8.dp))
            Image(
                painter = painterResource(UrbanColors.current.mascot(MascotMood.WELCOME)),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(92.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        UrbanPrimaryButton(
            text = "Reservar cita",
            onClick = onBook,
            icon = Icons.Default.ContentCut,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun HomeTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    UrbanCard(modifier.heightIn(min = 132.dp), onClick = onClick) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (highlighted) UrbanColors.Gold else UrbanColors.Gold.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                tint = if (highlighted) UrbanColors.OnGold else UrbanColors.Gold,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = UrbanColors.Ink)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 2)
    }
}
