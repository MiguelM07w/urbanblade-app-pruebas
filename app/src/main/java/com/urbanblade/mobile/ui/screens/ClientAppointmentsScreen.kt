package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.AppointmentRow
import com.urbanblade.mobile.ui.components.UrbanAttentionRow
import com.urbanblade.mobile.ui.components.UrbanAvatar
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanHeroCard
import com.urbanblade.mobile.ui.components.UrbanHeroLabel
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
import com.urbanblade.mobile.ui.components.UrbanMascotState
import com.urbanblade.mobile.ui.components.UrbanOutlineButton
import com.urbanblade.mobile.ui.components.UrbanPageHeader
import com.urbanblade.mobile.ui.components.UrbanPillTabs
import com.urbanblade.mobile.ui.components.UrbanPrimaryButton
import com.urbanblade.mobile.ui.components.ServiceTicketDialog
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.components.UrbanSkeletonList
import com.urbanblade.mobile.ui.components.UrbanStateKind
import com.urbanblade.mobile.ui.components.UrbanStatusPill
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.AppointmentsViewModel
import java.time.LocalDate

private val ACTIVE = listOf("pendiente", "confirmada", "en_proceso")

/**
 * Mis citas del cliente: "Próximas" (la siguiente destacada, lo que falta pagar y la lista de
 * espera) e "Historial" (pasadas y canceladas, con "Reservar de nuevo"). El personal usa
 * AppointmentsScreen; las hojas de reagendar y de pago son las mismas de esa pantalla.
 * "Nueva cita" vive en el encabezado: el botón flotante tapaba "Cancelar" en la última tarjeta.
 */
@Composable
fun ClientAppointmentsScreen(
    onBook: () -> Unit,
    onNavigate: (String) -> Unit,
    vm: AppointmentsViewModel = viewModel()
) {
    val response by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val notice by vm.notice.collectAsState()
    val hasMore by vm.hasMore.collectAsState()
    val loadingMore by vm.loadingMore.collectAsState()
    val waitlist by vm.waitlistEntries.collectAsState()
    val debt by vm.debt.collectAsState()
    val debtError by vm.debtError.collectAsState()
    val ticket by vm.ticket.collectAsState()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var confirmCancel by remember { mutableStateOf<AppointmentRow?>(null) }
    var rescheduling by remember { mutableStateOf<AppointmentRow?>(null) }
    var checkingOut by remember { mutableStateOf<AppointmentRow?>(null) }

    LaunchedEffect(Unit) {
        vm.load()
        vm.loadWaitlist()
        vm.loadDebt()
    }

    val todayDate = LocalDate.now()
    
    val upcoming = response.data
        .filter { 
            it.estado in ACTIVE && 
            (runCatching { LocalDate.parse(it.fecha.take(10)) }.getOrNull() ?: LocalDate.MIN) >= todayDate 
        }
        .sortedWith(
            compareBy<AppointmentRow> { runCatching { LocalDate.parse(it.fecha.take(10)) }.getOrNull() ?: LocalDate.MAX }
                .thenBy { runCatching { java.time.LocalTime.parse(it.horaInicio.take(5).padStart(5, '0')) }.getOrNull() ?: java.time.LocalTime.MAX }
        )
    val upcomingIds = upcoming.map { it.id }.toSet()
    val toPay = response.data.filter { it.isPayable() && it.id !in upcomingIds }
    val history = response.data
        .filter { it.id !in upcomingIds }
        .sortedWith(
            compareByDescending<AppointmentRow> { runCatching { LocalDate.parse(it.fecha.take(10)) }.getOrNull() ?: LocalDate.MIN }
                .thenByDescending { runCatching { java.time.LocalTime.parse(it.horaInicio.take(5).padStart(5, '0')) }.getOrNull() ?: java.time.LocalTime.MIN }
        )
    val failed = error != null && response.data.isEmpty()

    fun rebook(appt: AppointmentRow) {
        val params = listOfNotNull(appt.service?.id?.let { "serviceId=$it" }, appt.barber?.id?.let { "barberId=$it" })
        if (params.isEmpty()) onBook() else onNavigate("booking?" + params.joinToString("&"))
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = com.urbanblade.mobile.ui.components.BladebotClearance),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            UrbanPageHeader(
                title = "Mis citas",
                subtitle = "Lo que viene y lo que ya viviste.",
                eyebrow = "Agenda"
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UrbanPrimaryButton(text = "Reservar", onClick = onBook, icon = Icons.Default.Add)
                UrbanOutlineButton(
                    text = if (loading) "Actualizando…" else "Actualizar",
                    onClick = { vm.load(); vm.loadWaitlist(); vm.loadDebt() },
                    icon = Icons.Default.Refresh,
                    enabled = !loading && !loadingMore
                )
            }
        }

        notice?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }

        when {
            loading && response.data.isEmpty() -> item { UrbanSkeletonList(3) }
            failed -> item {
                UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar tus citas", error, "Reintentar") { vm.load() }
            }
            else -> {
                error?.let { item { UrbanErrorBanner(it) } }
                debtError?.let { item { UrbanInfoBanner(it) } }
                if (loading) item { UrbanInfoBanner("Actualizando tus citas…") }
                if (debt > 0) {
                    item {
                        UrbanAttentionRow(
                            Icons.Default.Payments,
                            "Tienes un adeudo por inasistencia",
                            "${UrbanFormat.money(debt)} por pagar en la barbería (efectivo o transferencia). Mientras tanto no puedes reservar.",
                            UrbanColors.Warning
                        )
                    }
                }
                item {
                    UrbanPillTabs(
                        listOf("Próximas" to Icons.Default.Event, "Historial" to Icons.Default.History),
                        tab
                    ) { tab = it }
                }

                if (tab == 0) {
                    val serverNext = response.next?.takeIf {
                        it.estado in ACTIVE &&
                            (runCatching { LocalDate.parse(it.fecha.take(10)) }.getOrNull() ?: LocalDate.MIN) >= todayDate
                    }
                    val next = serverNext ?: upcoming.firstOrNull()
                    if (next == null) {
                        item {
                            UrbanMascotState(
                                UrbanStateKind.EMPTY,
                                if (hasMore) "No hay citas próximas en las páginas cargadas" else "No tienes citas próximas",
                                if (hasMore) "Carga más citas para completar la revisión de tu agenda." else "Elige servicio, barbero y un horario disponible.",
                                actionLabel = "Reservar cita",
                                actionIcon = Icons.Default.Add,
                                onAction = onBook
                            )
                        }
                    } else {
                        item {
                            NextClientAppointment(
                                appt = next,
                                completeList = serverNext != null || !hasMore,
                                onPay = if (next.isPayable()) { { checkingOut = next } } else null,
                                onReschedule = if (next.isManageable()) { { rescheduling = next } } else null,
                                onCancel = if (next.isManageable()) { { confirmCancel = next } } else null
                            )
                        }
                    }

                    if (toPay.isNotEmpty()) {
                        item { UrbanSectionTitle("Por pagar", "Citas atendidas que aún no se cobran.") }
                        items(toPay, key = { "pay-${it.id}" }) { appt ->
                            ClientAppointmentRow(appt, onPay = { checkingOut = appt })
                        }
                    }

                    val later = upcoming.filter { it.id != next?.id }
                    if (later.isNotEmpty()) {
                        item { UrbanSectionTitle("Después", UrbanFormat.count(later.size, "cita más", "citas más")) }
                        items(later, key = { "later-${it.id}" }) { appt ->
                            ClientAppointmentRow(
                                appt,
                                onPay = if (appt.isPayable()) { { checkingOut = appt } } else null,
                                onReschedule = if (appt.isManageable()) { { rescheduling = appt } } else null,
                                onCancel = if (appt.isManageable()) { { confirmCancel = appt } } else null
                            )
                        }
                    }

                    if (waitlist.isNotEmpty()) {
                        item { UrbanSectionTitle("Lista de espera", "Te avisamos si se libera un horario.") }
                        items(waitlist, key = { "wait-${it.id}" }) { entry ->
                            WaitlistCard(entry, onLeave = { vm.leaveWaitlist(entry.id) })
                        }
                    }
                } else {
                    if (history.isEmpty()) {
                        item {
                            UrbanMascotState(UrbanStateKind.EMPTY, if (hasMore) "No hay historial en las páginas cargadas" else "Aún no hay historial", "Tus citas pasadas y canceladas aparecerán aquí.")
                        }
                    } else {
                        val completed = history.count { it.estado == "completada" }
                        item {
                            UrbanAttentionRow(
                                Icons.Default.CheckCircle,
                                UrbanFormat.count(completed, "visita completada", "visitas completadas"),
                                "Toca «Reservar de nuevo» para repetir un servicio.",
                                UrbanColors.Success
                            )
                        }
                        items(history, key = { "hist-${it.id}" }) { appt ->
                            ClientAppointmentRow(
                                appt,
                                onPay = if (appt.isPayable()) { { checkingOut = appt } } else null,
                                onRebook = if (appt.estado == "completada" || appt.estado == "cancelada") { { rebook(appt) } } else null,
                                onTicket = if (appt.estado == "completada" && appt.code != null) { { vm.openTicket(appt.code) } } else null
                            )
                        }
                    }
                }
                if (hasMore) {
                    item {
                        UrbanInfoBanner("Hay más citas por consultar. Los conteos corresponden a las páginas cargadas.")
                        Spacer(Modifier.height(8.dp))
                        UrbanOutlineButton(
                            text = if (loadingMore) "Cargando…" else "Cargar más citas",
                            onClick = { vm.loadMore() },
                            icon = Icons.Default.ExpandMore,
                            enabled = !loading && !loadingMore,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

    ticket?.let { ServiceTicketDialog(it) { vm.dismissTicket() } }
    rescheduling?.let { RescheduleSheet(appt = it, vm = vm, onDismiss = { rescheduling = null }) }
    checkingOut?.let { CheckoutSheet(appt = it, vm = vm, onDismiss = { checkingOut = null }) }
    confirmCancel?.let { appt ->
        AlertDialog(
            onDismissRequest = { confirmCancel = null },
            containerColor = UrbanColors.Card,
            title = { Text("¿Cancelar tu cita?") },
            text = {
                Text(
                    "${appt.service?.nombre ?: "Tu cita"} del ${UrbanFormat.date(appt.fecha)} a las ${UrbanFormat.time(appt.horaInicio)}. " +
                        "Si prefieres otro horario, usa «Reagendar».",
                    color = UrbanColors.Muted
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.cancel(appt); confirmCancel = null }) { Text("Sí, cancelar", color = UrbanColors.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = null }) { Text("Volver") } }
        )
    }
}

/** Qué sigue en cada estado, en lenguaje claro (el cliente no ve los botones de la agenda del barbero). */
internal fun clientStateHint(estado: String, paymentResolved: Boolean? = null): String? = when (estado) {
    "pendiente" -> "Esperando que tu barbero apruebe la cita; te avisaremos en cuanto la confirme."
    "confirmada" -> when (paymentResolved) {
        true -> "Cita aprobada y pago resuelto. Te esperamos a la hora de tu cita."
        false -> "Cita aprobada. Resuelve el pago antes del servicio; consulta las opciones aquí o en recepción."
        null -> "Cita aprobada. Consulta el estado de tu pago en tus comprobantes o en recepción."
    }
    "en_proceso" -> "Tu servicio está en curso."
    "no_asistio" -> "No registramos tu asistencia. Si hay un cargo pendiente, págalo en la barbería para volver a reservar."
    else -> null
}

private fun AppointmentRow.isManageable() = estado in listOf("pendiente", "confirmada") && code != null
private fun AppointmentRow.isPayable() = isChargeable && !hasPayment && code != null

/** La siguiente cita, en la tarjeta con foto, con sus acciones a la vista. */
@Composable
private fun NextClientAppointment(appt: AppointmentRow, completeList: Boolean, onPay: (() -> Unit)?, onReschedule: (() -> Unit)?, onCancel: (() -> Unit)?) {
    UrbanHeroCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { UrbanHeroLabel(if (completeList) "Tu próxima cita" else "Una de tus próximas citas") }
            UrbanStatusPill(appt.estado)
        }
        Spacer(Modifier.height(8.dp))
        Text(UrbanFormat.time(appt.horaInicio), style = MaterialTheme.typography.displaySmall, color = UrbanColors.Ink)
        Text(UrbanFormat.date(appt.fecha), style = MaterialTheme.typography.titleMedium, color = UrbanColors.Muted)
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = UrbanColors.Ink.copy(alpha = 0.22f))
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            UrbanAvatar(appt.barber?.user?.name ?: "Barbero", Modifier.size(42.dp), imageUrl = rememberBarberPhoto(appt.barber))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(appt.service?.nombre ?: "Servicio", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Ink)
                Text(appt.barber?.user?.name ?: "Barbero por confirmar", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            (appt.precioCobrado ?: appt.service?.precio)?.let {
                Text("${UrbanFormat.moneyShort(it)}", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Ink)
            }
        }
        clientStateHint(appt.estado, appt.pagoResuelto)?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
        }
        if (onPay != null) {
            Spacer(Modifier.height(14.dp))
            UrbanPrimaryButton(text = "Pagar cita", onClick = onPay, icon = Icons.Default.Payments, modifier = Modifier.fillMaxWidth())
        }
        if (onReschedule != null || onCancel != null) {
            Spacer(Modifier.height(10.dp))
            AppointmentActions(onReschedule = onReschedule, onCancel = onCancel)
        }
    }
}

/** Fila compacta con el riel de fecha del rediseño de citas y solo las acciones que aplican. */
@Composable
private fun ClientAppointmentRow(
    appt: AppointmentRow,
    onPay: (() -> Unit)? = null,
    onReschedule: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onRebook: (() -> Unit)? = null,
    onTicket: (() -> Unit)? = null
) {
    UrbanCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppointmentDateRail(appt, emphasized = true)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        appt.service?.nombre ?: "Servicio",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (appt.estado == "cancelada") UrbanColors.Muted else UrbanColors.Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    UrbanStatusPill(appt.estado)
                }
                Text(appt.barber?.user?.name ?: "Barbero por confirmar", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(UrbanFormat.date(appt.fecha), style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            }
        }
        clientStateHint(appt.estado, appt.pagoResuelto)?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
        }
        if (onPay != null) {
            Spacer(Modifier.height(12.dp))
            UrbanPrimaryButton(text = "Pagar cita", onClick = onPay, icon = Icons.Default.Payments, modifier = Modifier.fillMaxWidth())
        }
        if (onTicket != null) {
            Spacer(Modifier.height(10.dp))
            UrbanOutlineButton(text = "Ver ticket", onClick = onTicket, modifier = Modifier.fillMaxWidth())
        }
        if (onReschedule != null || onCancel != null || onRebook != null) {
            Spacer(Modifier.height(6.dp))
            AppointmentActions(onReschedule = onReschedule, onCancel = onCancel, onRebook = onRebook)
        }
    }
}

@Composable
private fun AppointmentActions(onReschedule: (() -> Unit)? = null, onCancel: (() -> Unit)? = null, onRebook: (() -> Unit)? = null) {
    FlowRow(Modifier.fillMaxWidth().padding(start = 2.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        onRebook?.let { ActionText("Reservar de nuevo", Icons.Default.Replay, UrbanColors.GoldText, it) }
        onReschedule?.let { ActionText("Reagendar", Icons.Default.EditCalendar, UrbanColors.GoldText, it) }
        onCancel?.let { ActionText("Cancelar", Icons.Default.Close, UrbanColors.Danger, it) }
    }
}

@Composable
private fun ActionText(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
        Icon(icon, null, modifier = Modifier.size(17.dp), tint = color)
        Spacer(Modifier.width(5.dp))
        Text(label, color = color)
    }
}
