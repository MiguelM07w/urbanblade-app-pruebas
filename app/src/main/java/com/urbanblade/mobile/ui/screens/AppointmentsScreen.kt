package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.rememberPaymentLauncher
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.urbanblade.mobile.BuildConfig
import com.urbanblade.mobile.core.payment.isStripeConfigured
import com.urbanblade.mobile.core.payment.rememberUrbanPaymentSheet
import com.urbanblade.mobile.data.model.AppointmentRow
import com.urbanblade.mobile.data.model.AppointmentStats
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.data.model.SlotItem
import com.urbanblade.mobile.data.model.WaitlistEntry
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.AppointmentsViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(user: AuthUser, onBook: () -> Unit, vm: AppointmentsViewModel = viewModel()) {
    val response by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val waitlistEntries by vm.waitlistEntries.collectAsState()
    var confirmCancel by remember { mutableStateOf<AppointmentRow?>(null) }
    var rescheduling by remember { mutableStateOf<AppointmentRow?>(null) }
    var checkingOut by remember { mutableStateOf<AppointmentRow?>(null) }
    var charging by remember { mutableStateOf<AppointmentRow?>(null) }
    val notice by vm.notice.collectAsState()
    val hasMore by vm.hasMore.collectAsState()
    val loadingMore by vm.loadingMore.collectAsState()
    val monthRows by vm.month.collectAsState()
    val monthLoading by vm.monthLoading.collectAsState()
    var calendarMode by remember { mutableStateOf(false) }
    var shownMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    val staff = user.roles.any { it in listOf("administrador", "recepcionista") }
    val canBook = com.urbanblade.mobile.core.access.UserExperience.canBook(user.roles)
    val isClient = com.urbanblade.mobile.core.access.UserExperience.isClient(user.roles)

    // El personal ve las citas de todo el negocio: las estadísticas del servidor son las del propio
    // usuario (0 para un administrador), así que se calculan aquí sobre la lista.
    var staffFilter by remember { mutableStateOf("atencion") }
    val today = remember { LocalDate.now().toString() }
    val active = listOf("pendiente", "confirmada", "en_proceso")
    val stats = if (staff) {
        AppointmentStats(
            total = response.data.size,
            proximas = response.data.count { 
                it.estado in active && (runCatching { LocalDate.parse(it.fecha.take(10)) }.getOrNull() ?: LocalDate.MIN) >= LocalDate.parse(today) 
            },
            completadas = response.data.count { it.estado == "completada" },
            canceladas = response.data.count { it.estado == "cancelada" }
        )
    } else {
        response.stats
    }
    
    fun getLocalDate(d: String) = runCatching { LocalDate.parse(d.take(10)) }.getOrNull() ?: LocalDate.MAX
    fun getLocalTime(t: String) = runCatching { java.time.LocalTime.parse(t.take(5).padStart(5, '0')) }.getOrNull() ?: java.time.LocalTime.MAX
    
    val visible = if (calendarMode) {
        monthRows.filter { it.fecha.startsWith(selectedDay?.toString() ?: "-") }.sortedBy { getLocalTime(it.horaInicio) }
    } else if (!staff) {
        response.data
    } else {
        when (staffFilter) {
            "hoy" -> response.data.filter { it.fecha.take(10) == today }.sortedBy { getLocalTime(it.horaInicio) }
            "atencion", "proximas" -> response.data.filter { 
                    it.estado in active && (runCatching { LocalDate.parse(it.fecha.take(10)) }.getOrNull() ?: LocalDate.MIN) >= LocalDate.parse(today) 
                }
                .sortedWith(compareBy<AppointmentRow> { getLocalDate(it.fecha) }.thenBy { getLocalTime(it.horaInicio) })
            else -> response.data.sortedWith(compareBy<AppointmentRow> { getLocalDate(it.fecha) }.thenBy { getLocalTime(it.horaInicio) })
        }
    }
    val pendingCount = if (staff && !calendarMode) visible.count { it.estado == "pendiente" } else 0
    // Solo la siguiente decisión ocupa la tarjeta destacada. Las demás citas,
    // incluso si siguen pendientes, permanecen en la lista compacta.
    val attention = if (staff && !calendarMode) visible.filter { it.estado == "pendiente" }.take(1) else emptyList()
    val attentionIds = attention.map { it.id }.toSet()
    val later = if (staff && !calendarMode) visible.filterNot { it.id in attentionIds } else emptyList()

    LaunchedEffect(calendarMode, shownMonth) {
        if (calendarMode) vm.loadMonth(shownMonth)
    }

    LaunchedEffect(Unit) {
        vm.load()
        if (isClient) vm.loadWaitlist()
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (canBook) {
                ExtendedFloatingActionButton(
                    onClick = onBook,
                    containerColor = UrbanColors.Gold,
                    contentColor = UrbanColors.OnGold,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Nueva cita", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = com.urbanblade.mobile.ui.components.BladebotClearance),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                UrbanPageHeader(
                    title = "Citas",
                    subtitle = "Tu agenda, ordenada y siempre conectada.",
                    eyebrow = "Agenda"
                )
            }

            // Si la carga falló y no hay nada que mostrar, no se pintan contadores en 0 ni
            // "agenda libre": sería afirmar algo que no sabemos. Solo el error y "Reintentar".
            val failed = error != null && response.data.isEmpty()

            if (!failed && staff) item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        text = "${stats.proximas} próximas · ${stats.completadas} completadas",
                        style = MaterialTheme.typography.labelLarge,
                        color = UrbanColors.Muted
                    )
                }
            }

            if (loading) item { UrbanSkeletonList(3) }
            notice?.let {
                item { UrbanInfoBanner(it, Icons.Default.CheckCircle) }
            }
            error?.let { item { UrbanErrorBanner(it) } }
            if (failed && !loading) {
                item {
                    UrbanOutlineButton(
                        text = "Reintentar",
                        onClick = { vm.load() },
                        icon = Icons.Default.Refresh,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (!loading && !failed && response.data.isEmpty()) {
                item {
                    UrbanPremiumCard(Modifier.fillMaxWidth()) {
                        UrbanEmptyState(
                            title = "Tu agenda está libre",
                            subtitle = "Cuando tengas una cita aparecerá aquí.",
                            icon = Icons.Default.EventAvailable,
                            actionLabel = if (canBook) "Reservar ahora" else null,
                            onAction = if (canBook) onBook else null
                        )
                    }
                }
            }

            if (response.data.isNotEmpty()) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !calendarMode, onClick = { calendarMode = false }, label = { Text("Lista") })
                        FilterChip(
                            selected = calendarMode,
                            onClick = { calendarMode = true },
                            label = { Text("Calendario") },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp)) }
                        )
                    }
                }
                if (calendarMode) {
                    item {
                        AppointmentsCalendar(
                            month = shownMonth,
                            rows = monthRows,
                            selected = selectedDay,
                            loading = monthLoading,
                            onMonthChange = { shownMonth = it; selectedDay = null },
                            onSelect = { selectedDay = it }
                        )
                    }
                    item {
                        UrbanSectionTitle(
                            selectedDay?.let { UrbanFormat.date(it.toString()) } ?: "Elige un día",
                            if (selectedDay == null) "Toca un día del calendario" else UrbanFormat.count(visible.size, "cita", "citas")
                        )
                    }
                    if (selectedDay != null && visible.isEmpty() && !monthLoading) {
                        item { UrbanEmptyState("Sin citas este día", "Elige otro día del calendario.", Icons.Default.EventAvailable) }
                    }
                } else if (staff) {
                    item {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("atencion" to "Requieren atención", "hoy" to "Hoy", "proximas" to "Próximas", "todas" to "Todas")
                                .forEach { (key, label) ->
                                    FilterChip(selected = staffFilter == key, onClick = { staffFilter = key }, label = { Text(label) })
                                }
                        }
                    }
                    if (visible.isEmpty()) {
                        item { UrbanEmptyState("Sin citas en este filtro", "Prueba con otro filtro.", Icons.Default.EventAvailable) }
                    }
                } else {
                    item { UrbanSectionTitle("Tu agenda", UrbanFormat.count(stats.total, "cita registrada", "citas registradas")) }
                }
            }

            if (staff && !calendarMode && visible.isNotEmpty()) {
                item {
                    PrioritySectionHeader(
                        title = "Requieren atención",
                        subtitle = "Citas que necesitan una decisión tuya.",
                        count = pendingCount,
                        icon = Icons.Default.PriorityHigh
                    )
                }
                items(attention, key = { "priority-${it.id}" }) { appt ->
                    StaffPriorityAppointment(
                        appt = appt,
                        onConfirm = { vm.changeStatus(appt, "confirmada") },
                        onCancel = if (appt.code != null) { { confirmCancel = appt } } else null,
                        onCharge = if (appt.isChargeable && !appt.hasPayment) { { charging = appt } } else null
                    )
                }
                if (attention.isEmpty()) {
                    item { UrbanInfoBanner("No hay citas pendientes de confirmación.", Icons.Default.TaskAlt) }
                }
                if (later.isNotEmpty()) {
                    item {
                        PrioritySectionHeader(
                            title = "Después",
                            subtitle = "Próximas citas en tu agenda.",
                            count = later.size,
                            icon = Icons.Default.Schedule
                        )
                    }
                    items(later, key = { "later-${it.id}" }) { appt ->
                        StaffTimelineAppointment(
                            appt = appt,
                            onStatus = if (appt.code != null && appt.estado in listOf("confirmada", "en_proceso")) {
                                { estado -> vm.changeStatus(appt, estado) }
                            } else null,
                            onCancel = if (appt.code != null && appt.estado in listOf("pendiente", "confirmada")) {
                                { { confirmCancel = appt } }
                            } else null,
                            onCharge = if (appt.isChargeable && !appt.hasPayment) { { charging = appt } } else null
                        )
                    }
                }
            }

            if (!staff || calendarMode) items(visible, key = { it.id }) { appt ->
                val manageable = appt.estado in listOf("pendiente", "confirmada") && appt.code != null
                val payable = isClient && appt.isChargeable && !appt.hasPayment && appt.code != null
                AppointmentCard(
                    appt = appt,
                    clientName = if (staff) appt.client?.user?.name else null,
                    onCancel = if (manageable) { { confirmCancel = appt } } else null,
                    onReschedule = if (manageable && isClient) { { rescheduling = appt } } else null,
                    onPay = if (payable) { { checkingOut = appt } } else null,
                    onStatus = if (staff && appt.code != null && appt.estado in listOf("pendiente", "confirmada", "en_proceso")) {
                        { estado -> vm.changeStatus(appt, estado) }
                    } else null,
                    onCharge = if (staff && appt.isChargeable && !appt.hasPayment) { { charging = appt } } else null
                )
            }

            if (!calendarMode && hasMore) {
                item {
                    UrbanOutlineButton(
                        text = if (loadingMore) "Cargando…" else "Cargar más citas",
                        onClick = { vm.loadMore() },
                        enabled = !loading && !loadingMore,
                        icon = Icons.Default.ExpandMore,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (isClient && waitlistEntries.isNotEmpty()) {
                item { UrbanSectionTitle("Mi lista de espera", "Te avisamos si se libera un horario") }
                items(waitlistEntries, key = { it.id }) { entry ->
                    WaitlistCard(entry, onLeave = { vm.leaveWaitlist(entry.id) })
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    rescheduling?.let { appt ->
        RescheduleSheet(
            appt = appt,
            vm = vm,
            onDismiss = { rescheduling = null }
        )
    }

    charging?.let { appt ->
        ChargeSheet(appt = appt, vm = vm, onDismiss = { charging = null })
    }

    checkingOut?.let { appt ->
        CheckoutSheet(
            appt = appt,
            vm = vm,
            onDismiss = { checkingOut = null }
        )
    }

    confirmCancel?.let { appt ->
        AlertDialog(
            onDismissRequest = { confirmCancel = null },
            containerColor = UrbanColors.Card,
            title = { Text("Cancelar cita") },
            text = {
                Text(
                    if (staff) {
                        "¿Cancelar la cita de ${appt.client?.user?.name ?: "este cliente"} del ${UrbanFormat.date(appt.fecha)} a las " +
                            "${UrbanFormat.time(appt.horaInicio)}? Se le avisará y, si pagó al reservar, se le devolverá."
                    } else {
                        "¿Quieres cancelar tu cita del ${UrbanFormat.date(appt.fecha)} a las ${UrbanFormat.time(appt.horaInicio)}?"
                    },
                    color = UrbanColors.Muted
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.cancel(appt, asStaff = staff); confirmCancel = null }) {
                    Text("Sí, cancelar", color = UrbanColors.Danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = null }) { Text("Volver") } }
        )
    }
}

@Composable
private fun PrioritySectionHeader(title: String, subtitle: String, count: Int, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = CircleShape,
            color = UrbanColors.Gold.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, UrbanColors.Gold.copy(alpha = 0.5f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = UrbanColors.Gold, modifier = Modifier.size(19.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(count.toString(), style = MaterialTheme.typography.labelLarge, color = UrbanColors.Muted)
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
        }
    }
}

@Composable
private fun StaffPriorityAppointment(
    appt: AppointmentRow,
    onConfirm: () -> Unit,
    onCancel: (() -> Unit)?,
    onCharge: (() -> Unit)?
) {
    var stamped by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(stamped) {
        if (stamped) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            kotlinx.coroutines.delay(650)
            onConfirm()
        }
    }

    Box(contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth().alpha(if (stamped) 0.5f else 1f),
            shape = RoundedCornerShape(20.dp),
            color = UrbanColors.Card.copy(alpha = 0.86f),
            border = BorderStroke(1.dp, UrbanColors.Gold.copy(alpha = 0.72f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                AppointmentDateRail(appt, emphasized = true)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            appt.service?.nombre ?: "Servicio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        UrbanStatusPill(appt.estado)
                        StaffAppointmentMenu(onCancel = onCancel, onCharge = onCharge)
                    }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PersonLine(
                                label = appt.client?.user?.name ?: "Cliente por confirmar",
                                name = appt.client?.user?.name ?: "Cliente",
                                imageUrl = appt.client?.user?.avatarUrl,
                                color = UrbanColors.Ink
                            )
                            PersonLine(
                                label = appt.barber?.user?.name ?: "Barbero por confirmar",
                                name = appt.barber?.user?.name ?: "Barbero",
                                imageUrl = appt.barber?.fotoUrl,
                                color = UrbanColors.Muted
                            )
                            ReminderLine(appt)
                        }
                        Button(
                            onClick = { stamped = true },
                            modifier = Modifier.heightIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = UrbanColors.Gold,
                                contentColor = UrbanColors.OnGold
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Confirmar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        if (stamped) {
            com.urbanblade.mobile.ui.components.UrbanStampAnimation("CONFIRMADA")
        }
    }
}

@Composable
private fun StaffTimelineAppointment(
    appt: AppointmentRow,
    onStatus: ((String) -> Unit)?,
    onCancel: (() -> Unit)?,
    onCharge: (() -> Unit)?
) {
    var stampedAction by remember { mutableStateOf<String?>(null) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(stampedAction) {
        if (stampedAction != null) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            kotlinx.coroutines.delay(650)
            onStatus?.invoke(stampedAction!!)
            stampedAction = null
        }
    }

    Box(contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.alpha(if (stampedAction != null) 0.5f else 1f)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                AppointmentDateRail(appt, emphasized = false)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            appt.service?.nombre ?: "Servicio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        UrbanStatusPill(appt.estado)
                        StaffAppointmentMenu(
                            onStatus = if (onStatus != null) { { estado -> stampedAction = estado } } else null,
                            currentStatus = appt.estado,
                            startBlocked = appt.estado == "confirmada" && appt.puedeIniciar == false,
                            onCancel = onCancel,
                            onCharge = onCharge
                        )
                    }
                    Text(appt.client?.user?.name ?: "Cliente por confirmar", style = MaterialTheme.typography.bodyMedium)
                    Text(appt.barber?.user?.name ?: "Barbero por confirmar", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                    ReminderLine(appt)
                }
            }
            HorizontalDivider(color = UrbanColors.Line.copy(alpha = 0.65f))
        }

        when (stampedAction) {
            "completada" -> com.urbanblade.mobile.ui.components.UrbanStampAnimation("COMPLETADA", color = UrbanColors.Success)
            "no_asistio" -> com.urbanblade.mobile.ui.components.UrbanStampAnimation("NO ASISTIÓ", color = UrbanColors.Danger)
            "en_proceso" -> com.urbanblade.mobile.ui.components.UrbanStampAnimation("INICIADA", color = UrbanColors.Gold)
        }
    }
}

@Composable
internal fun AppointmentDateRail(appt: AppointmentRow, emphasized: Boolean) {
    Column(
        modifier = Modifier.width(54.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (emphasized) {
            Text(appt.fecha.takeLast(2), style = MaterialTheme.typography.headlineMedium, color = UrbanColors.GoldText, fontWeight = FontWeight.Bold)
            Text(UrbanFormat.date(appt.fecha).take(3).uppercase(), style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted)
            HorizontalDivider(Modifier.padding(vertical = 3.dp), color = UrbanColors.Line)
        }
        Text(
            appt.horaInicio.take(5),
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (emphasized) UrbanColors.Ink else UrbanColors.Gold
        )
        if (!emphasized) {
            Text(
                "${appt.fecha.takeLast(2)} ${UrbanFormat.date(appt.fecha).take(3).uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = UrbanColors.Muted
            )
        }
    }
}

@Composable
private fun ReminderLine(appt: AppointmentRow) {
    val (label, color, icon) = remember(appt.reminder24hSent, appt.reminder2hSent, appt.fecha, appt.horaInicio) {
        when {
            appt.reminder2hSent -> Triple("Recordatorio 2 h enviado", UrbanColors.Muted, Icons.Default.NotificationsActive)
            appt.reminder24hSent -> Triple("Recordatorio 24 h enviado", UrbanColors.Muted, Icons.Default.NotificationsActive)
            appointmentIsFuture(appt) -> Triple("Recordatorio por enviar", UrbanColors.Muted, Icons.Default.Schedule)
            else -> Triple("Sin recordatorio enviado", UrbanColors.Danger, Icons.Default.NotificationsOff)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

private fun appointmentIsFuture(appt: AppointmentRow): Boolean = runCatching {
    val date = LocalDate.parse(appt.fecha.take(10))
    val time = LocalTime.parse(appt.horaInicio.take(5))
    LocalDateTime.of(date, time).isAfter(LocalDateTime.now())
}.getOrDefault(false)

@Composable
private fun StaffAppointmentMenu(
    onStatus: ((String) -> Unit)? = null,
    currentStatus: String? = null,
    startBlocked: Boolean = false,
    onCancel: (() -> Unit)?,
    onCharge: (() -> Unit)?
) {
    var expanded by remember { mutableStateOf(false) }
    val next = when (currentStatus) {
        "confirmada" -> "en_proceso" to "Iniciar servicio"
        "en_proceso" -> "completada" to "Completar servicio"
        else -> null
    }
    if (next != null || onCancel != null || onCharge != null) {
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Más acciones")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (next != null && onStatus != null) {
                    DropdownMenuItem(
                        text = { Text(if (startBlocked) "${next.second} (falta el pago)" else next.second) },
                        enabled = !startBlocked,
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                        onClick = { expanded = false; onStatus(next.first) }
                    )
                }
                if (onCharge != null) {
                    DropdownMenuItem(
                        text = { Text("Cobrar") },
                        leadingIcon = { Icon(Icons.Default.Payments, null) },
                        onClick = { expanded = false; onCharge() }
                    )
                }
                if (onCancel != null) {
                    DropdownMenuItem(
                        text = { Text("Cancelar cita", color = UrbanColors.Danger) },
                        leadingIcon = { Icon(Icons.Default.Close, null, tint = UrbanColors.Danger) },
                        onClick = { expanded = false; onCancel() }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppointmentCard(
    appt: AppointmentRow,
    clientName: String? = null,
    onCancel: (() -> Unit)?,
    onReschedule: (() -> Unit)? = null,
    onPay: (() -> Unit)? = null,
    onStatus: ((String) -> Unit)? = null,
    onCharge: (() -> Unit)? = null
) {
    UrbanPremiumCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Surface(
                modifier = Modifier.size(width = 66.dp, height = 72.dp),
                color = UrbanColors.Gold.copy(alpha = 0.09f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, UrbanColors.Gold.copy(alpha = 0.25f))
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(appt.fecha.takeLast(2), style = MaterialTheme.typography.headlineMedium, color = UrbanColors.GoldText)
                    Text(appt.horaInicio.take(5), style = MaterialTheme.typography.labelMedium, color = UrbanColors.Ink)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(appt.service?.nombre ?: "Servicio", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    UrbanStatusPill(appt.estado)
                }
                clientName?.let { PersonLine("Cliente: $it", it, appt.client?.user?.avatarUrl, UrbanColors.Ink) }
                PersonLine(
                    label = appt.barber?.user?.name ?: "Barbero por confirmar",
                    name = appt.barber?.user?.name ?: "Barbero",
                    imageUrl = appt.barber?.fotoUrl,
                    color = UrbanColors.Muted
                )
                Text(UrbanFormat.date(appt.fecha), style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                appt.notas?.takeIf { it.isNotBlank() }?.let {
                    Text("“$it”", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Ink)
                }
                if (onStatus != null || onCharge != null) {
                    Spacer(Modifier.height(6.dp))
                    // Siguiente paso del flujo (pendiente, confirmada, en proceso, completada) y cobro.
                    val next = when (appt.estado) {
                        "pendiente" -> "confirmada" to "Confirmar"
                        "confirmada" -> "en_proceso" to "Iniciar"
                        "en_proceso" -> "completada" to "Completar"
                        else -> null
                    }
                    // Iniciar exige que sea hoy y que el pago esté resuelto (cobrada o transferencia verificada).
                    val startBlocked = appt.estado == "confirmada" && appt.puedeIniciar == false
                    if (startBlocked && !appt.motivoNoIniciar.isNullOrBlank()) {
                        Text(appt.motivoNoIniciar, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Warning)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (next != null && onStatus != null) {
                            UrbanPrimaryButton(
                                text = next.second,
                                onClick = { onStatus(next.first) },
                                enabled = !startBlocked,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (onCharge != null) {
                            UrbanOutlineButton(
                                text = "Cobrar",
                                onClick = onCharge,
                                icon = Icons.Default.Payments,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                if (onPay != null) {
                    Spacer(Modifier.height(6.dp))
                    UrbanPrimaryButton(
                        text = "Pagar cita",
                        onClick = onPay,
                        icon = Icons.Default.Payments,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                val canNoShow = appt.estado == "confirmada" && onStatus != null
                if (onCancel != null || onReschedule != null || canNoShow) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (canNoShow) {
                            TextButton(onClick = { onStatus?.invoke("no_asistio") }, contentPadding = PaddingValues(0.dp)) {
                                Text("No asistió", color = UrbanColors.Muted)
                            }
                            Spacer(Modifier.width(16.dp))
                        }
                        if (onReschedule != null) {
                            TextButton(onClick = onReschedule, contentPadding = PaddingValues(0.dp)) {
                                Icon(Icons.Default.EditCalendar, null, modifier = Modifier.size(17.dp), tint = UrbanColors.Gold)
                                Spacer(Modifier.width(5.dp))
                                Text("Reagendar", color = UrbanColors.GoldText)
                            }
                            Spacer(Modifier.width(16.dp))
                        }
                        if (onCancel != null) {
                            TextButton(onClick = onCancel, contentPadding = PaddingValues(0.dp)) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(17.dp), tint = UrbanColors.Danger)
                                Spacer(Modifier.width(5.dp))
                                Text("Cancelar cita", color = UrbanColors.Danger)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun WaitlistCard(entry: WaitlistEntry, onLeave: () -> Unit) {
    // Salir de la lista hace perder el lugar: se confirma como las demás acciones que no se deshacen.
    var confirmLeave by remember { mutableStateOf(false) }
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            containerColor = UrbanColors.Card,
            title = { Text("¿Salir de la lista de espera?") },
            text = { Text("Ya no te avisaremos si se libera un horario para ${entry.service?.nombre ?: "este servicio"}.", color = UrbanColors.Muted) },
            confirmButton = { TextButton(onClick = { confirmLeave = false; onLeave() }) { Text("Sí, salir", color = UrbanColors.Danger) } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Volver") } }
        )
    }
    UrbanCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.service?.nombre ?: "Servicio", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${entry.barber?.name ?: "Cualquier barbero"} · ${entry.fecha ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted
                )
            }
            UrbanStatusPill(entry.estado)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { confirmLeave = true }) { Icon(Icons.Default.Close, "Salir de la lista de espera", tint = UrbanColors.Danger) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RescheduleSheet(appt: AppointmentRow, vm: AppointmentsViewModel, onDismiss: () -> Unit) {
    val barbers by vm.barbers.collectAsState()
    val slots by vm.rescheduleSlots.collectAsState()
    val busy by vm.rescheduling.collectAsState()
    val error by vm.error.collectAsState()

    var barberId by remember { mutableStateOf(appt.barber?.id.orEmpty()) }
    var date by remember { mutableStateOf(appt.fecha) }
    var time by remember { mutableStateOf("") }
    val serviceId = appt.service?.id.orEmpty()

    LaunchedEffect(Unit) { vm.loadBarbers() }
    LaunchedEffect(barberId, date) {
        time = ""
        if (barberId.isNotBlank() && date.isNotBlank()) vm.loadRescheduleSlots(barberId, serviceId, date)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = UrbanColors.Card) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
            UrbanSectionTitle("Reagendar cita", appt.service?.nombre)
            Spacer(Modifier.height(14.dp))

            Text("Profesional", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(barbers, key = { it.id }) { barber ->
                    val selected = barber.id == barberId
                    Surface(
                        onClick = { barberId = barber.id },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) UrbanColors.Gold else UrbanColors.Background,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) UrbanColors.Gold else UrbanColors.Line)
                    ) {
                        Text(
                            barber.user?.name ?: "Barbero",
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) UrbanColors.OnGold else UrbanColors.Ink
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Fecha", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
            Spacer(Modifier.height(8.dp))
            val days = remember { (0..29).map { LocalDate.now().plusDays(it.toLong()) } }
            val selectedDate = remember(date) { runCatching { LocalDate.parse(date) }.getOrNull() }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(days) { day ->
                    val selected = day == selectedDate
                    Surface(
                        onClick = { date = day.toString() },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) UrbanColors.Gold else UrbanColors.Background,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) UrbanColors.Gold else UrbanColors.Line),
                        modifier = Modifier.width(54.dp)
                    ) {
                        Column(Modifier.padding(vertical = 8.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es", "MX")).replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) UrbanColors.OnGold else UrbanColors.Muted
                            )
                            Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, color = if (selected) UrbanColors.OnGold else UrbanColors.Ink)
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Horario", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
            Spacer(Modifier.height(8.dp))
            if (slots.isEmpty()) {
                Text("Sin horarios para este día.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 180.dp)
                ) {
                    items(slots, key = { it.time }) { slot: SlotItem ->
                        val selected = slot.time == time
                        Surface(
                            onClick = { time = slot.time },
                            shape = MaterialTheme.shapes.medium,
                            color = if (selected) UrbanColors.Gold else UrbanColors.Background,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) UrbanColors.Gold else UrbanColors.Line)
                        ) {
                            Box(Modifier.padding(vertical = 10.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(slot.label, style = MaterialTheme.typography.labelLarge, color = if (selected) UrbanColors.OnGold else UrbanColors.Ink)
                            }
                        }
                    }
                }
            }

            error?.let { Spacer(Modifier.height(12.dp)); UrbanErrorBanner(it) }

            Spacer(Modifier.height(18.dp))
            UrbanPrimaryButton(
                text = "Confirmar reagendado",
                onClick = {
                    appt.code?.let { code -> vm.reschedule(code, barberId, serviceId, date, time, appt.notas, onDismiss) }
                },
                enabled = barberId.isNotBlank() && date.isNotBlank() && time.isNotBlank(),
                loading = busy,
                icon = Icons.Default.EditCalendar,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

private enum class PaymentMethodChoice { TARJETA, TRANSFERENCIA }

/**
 * Checkout de cita: tarjeta (Stripe PaymentSheet) o transferencia (subir
 * comprobante) + propina opcional -- sin efectivo, que es inherentemente
 * presencial (lo cobra recepción, no algo que el cliente "pague" desde el
 * teléfono). El monto real SIEMPRE lo calcula barber al crear el intent o
 * al recibir el comprobante; aquí solo se muestra una estimación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CheckoutSheet(appt: AppointmentRow, vm: AppointmentsViewModel, onDismiss: () -> Unit) {
    val checkoutBusy by vm.checkoutBusy.collectAsState()
    val uploadingReceipt by vm.uploadingReceipt.collectAsState()
    val clientSecret by vm.stripeClientSecret.collectAsState()
    val confirmingPayment by vm.confirmingPayment.collectAsState()
    val paymentNotice by vm.paymentNotice.collectAsState()
    val cardAvailable = remember { isStripeConfigured() }
    val response by vm.data.collectAsState()
    val error by vm.error.collectAsState()
    val context = LocalContext.current

    var method by remember { mutableStateOf<PaymentMethodChoice?>(null) }
    var tipOption by remember { mutableStateOf(0) }
    var customTip by remember { mutableStateOf("") }
    var giftCardCode by remember { mutableStateOf("") }
    var puntos by remember { mutableStateOf("") }
    var showExtras by remember { mutableStateOf(false) }
    var receiptUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val savedCards by vm.savedCards.collectAsState()
    val savedCardConfirm by vm.savedCardConfirm.collectAsState()
    // Tarjeta al centro del carrusel; null = «Otra tarjeta» (se paga con el PaymentSheet de Stripe).
    var selectedCardId by remember { mutableStateOf<String?>(null) }

    val basePrice = appt.precioCobrado ?: appt.service?.precio ?: 0.0
    val tipAmount = when (tipOption) {
        1 -> (basePrice * 0.10).let { Math.round(it * 100) / 100.0 }
        2 -> (basePrice * 0.15).let { Math.round(it * 100) / 100.0 }
        3 -> customTip.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    // La cita puede haberse marcado como pagada mientras la hoja sigue
    // abierta (confirmAppointmentPayment refresca la lista)
    val justPaid = response.data.firstOrNull { it.id == appt.id }?.hasPayment == true

    val pickReceipt = rememberSingleImagePicker { receiptUri = it }

    val presentPaymentSheet = rememberUrbanPaymentSheet { result ->
        when (result) {
            is PaymentSheetResult.Completed -> {
                vm.clearStripeClientSecret()
                vm.confirmAppointmentPayment(appt.id)
            }
            is PaymentSheetResult.Canceled -> vm.onStripeCanceled()
            is PaymentSheetResult.Failed -> vm.onStripeFailed(result.error.localizedMessage)
        }
    }

    LaunchedEffect(clientSecret) {
        clientSecret?.let { presentPaymentSheet(it) }
    }

    // Pago con una tarjeta guardada: Stripe lo confirma directo con esa tarjeta, sin pedir datos.
    val paymentLauncher = rememberPaymentLauncher(BuildConfig.STRIPE_PUBLISHABLE_KEY) { result ->
        when (result) {
            is PaymentResult.Completed -> vm.confirmAppointmentPayment(appt.id)
            is PaymentResult.Canceled -> vm.onStripeCanceled()
            is PaymentResult.Failed -> vm.onStripeFailed(result.throwable.localizedMessage)
        }
    }
    LaunchedEffect(savedCardConfirm) {
        val confirm = savedCardConfirm ?: return@LaunchedEffect
        vm.clearSavedCardConfirm()
        confirm.paymentMethodId?.let { paymentLauncher.confirm(ConfirmPaymentIntentParams.createWithPaymentMethodId(it, confirm.clientSecret)) }
    }
    LaunchedEffect(cardAvailable) { if (cardAvailable) vm.loadSavedCards() }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = UrbanColors.Card) {
        if (justPaid) {
            Box(Modifier.fillMaxWidth().padding(bottom = 32.dp), contentAlignment = Alignment.Center) {
                com.urbanblade.mobile.ui.components.UrbanReceiptAnimation(
                    serviceName = appt.service?.nombre ?: "Servicio",
                    total = basePrice + tipAmount,
                    date = "${UrbanFormat.date(appt.fecha)} • ${UrbanFormat.time(appt.horaInicio)}",
                    onAnimationEnd = onDismiss
                )
            }
        } else {
            // Con carrusel de tarjetas el contenido supera la altura de pantallas chicas: la hoja se desplaza
            // para que el botón de pagar siempre se pueda alcanzar.
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp)) {
            UrbanSectionTitle("Pagar cita", appt.service?.nombre)
            Spacer(Modifier.height(14.dp))

            UrbanKeyValue("Servicio", "${UrbanFormat.moneyShort(basePrice)}")
            if (tipAmount > 0) UrbanKeyValue("Propina", "${UrbanFormat.moneyShort(tipAmount)}")
            UrbanKeyValue("Total estimado", "${UrbanFormat.moneyShort(basePrice + tipAmount)}")
            Spacer(Modifier.height(4.dp))
            Text(
                "El monto final (con descuentos de nivel/membresía o tarjeta regalo aplicados) lo confirma UrbanBlade al procesar el pago.",
                style = MaterialTheme.typography.bodySmall,
                color = UrbanColors.Muted
            )

            Spacer(Modifier.height(16.dp))
            UrbanFieldLabel("Propina (opcional)")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Sin propina" to 0, "10%" to 1, "15%" to 2, "Otro monto" to 3).forEach { (label, value) ->
                    FilterChip(selected = tipOption == value, onClick = { tipOption = value }, label = { Text(label) })
                }
            }
            if (tipOption == 3) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    customTip, { customTip = it.filter { c -> c.isDigit() || c == '.' } },
                    placeholder = { Text("Monto en pesos") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }

            Spacer(Modifier.height(8.dp))
            // Gift card y puntos son poco frecuentes: se esconden para que el método de pago quede a la vista.
            val extrasOpen = showExtras || giftCardCode.isNotBlank() || puntos.isNotBlank()
            TextButton(onClick = { showExtras = !extrasOpen }) {
                Text(if (extrasOpen) "Ocultar tarjeta regalo y puntos" else "¿Tienes tarjeta regalo o puntos?", color = UrbanColors.GoldText)
                Icon(if (extrasOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = UrbanColors.Gold)
            }
            if (extrasOpen) {
                Spacer(Modifier.height(16.dp))
                UrbanFieldLabel("Código de tarjeta regalo (opcional)")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    giftCardCode, { giftCardCode = it },
                    placeholder = { Text("Ej. UB-XXXXXX") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(Modifier.height(16.dp))
                UrbanFieldLabel("Puntos a canjear (opcional)")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    puntos, { puntos = it.filter(Char::isDigit) },
                    placeholder = { Text("0") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

            }

            Spacer(Modifier.height(20.dp))
            UrbanFieldLabel("Método de pago")
            Spacer(Modifier.height(8.dp))
            // Mismos mosaicos que al reservar (kit): antes eran dos botones sin estado seleccionado.
            UrbanChoiceTiles(
                options = listOf(
                    UrbanChoice(PaymentMethodChoice.TARJETA, "Tarjeta", "Paga ahora", Icons.Default.CreditCard),
                    UrbanChoice(PaymentMethodChoice.TRANSFERENCIA, "Transferencia", "SPEI", Icons.Default.AccountBalance)
                ),
                selected = method,
                onSelect = { method = it }
            )
            Text(
                "¿Prefieres pagar en efectivo? Puedes hacerlo directamente en la barbería.",
                style = MaterialTheme.typography.bodySmall,
                color = UrbanColors.Muted,
                modifier = Modifier.padding(top = 6.dp)
            )

            when (method) {
                PaymentMethodChoice.TARJETA -> {
                    Spacer(Modifier.height(16.dp))
                    if (!cardAvailable) {
                        UrbanInfoBanner("El pago con tarjeta no está disponible en esta versión de la app. Paga por transferencia o en la barbería.")
                        Spacer(Modifier.height(10.dp))
                    }
                    // Antes era solo un botón al PaymentSheet: ahora las tarjetas guardadas se eligen deslizando.
                    val chosenCard = savedCards.firstOrNull { it.id == selectedCardId }
                    if (cardAvailable && savedCards.isNotEmpty()) {
                        UrbanFieldLabel("Tus tarjetas")
                        Spacer(Modifier.height(10.dp))
                        UrbanCreditCardCarousel(
                            cards = savedCards,
                            selectedCardId = selectedCardId,
                            onSelect = { selectedCardId = it }
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    UrbanPrimaryButton(
                        text = chosenCard?.let { "Pagar con •••• ${it.last4}" } ?: "Proceder al pago",
                        onClick = {
                            vm.startStripeCheckout(appt.id, puntos.toIntOrNull() ?: 0, giftCardCode, tipAmount, chosenCard?.id)
                        },
                        enabled = cardAvailable && !confirmingPayment,
                        loading = checkoutBusy,
                        icon = Icons.Default.CreditCard,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                PaymentMethodChoice.TRANSFERENCIA -> {
                    Spacer(Modifier.height(16.dp))
                    UrbanOutlineButton(
                        text = if (receiptUri != null) "Comprobante seleccionado" else "Elegir comprobante",
                        onClick = pickReceipt,
                        icon = Icons.Default.Image,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    UrbanPrimaryButton(
                        text = "Subir comprobante",
                        onClick = {
                            val uri = receiptUri
                            val code = appt.code
                            if (uri != null && code != null) {
                                vm.uploadPaymentReceipt(context, code, tipAmount, uri, onDismiss)
                            }
                        },
                        enabled = receiptUri != null,
                        loading = uploadingReceipt,
                        icon = Icons.Default.Upload,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                null -> {}
            }

            if (confirmingPayment) {
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold)
                Spacer(Modifier.height(8.dp))
                UrbanInfoBanner("Confirmando tu pago con UrbanBlade…", Icons.Default.HourglassTop)
            }

            paymentNotice?.let { Spacer(Modifier.height(12.dp)); UrbanInfoBanner(it) }

            error?.let { Spacer(Modifier.height(12.dp)); UrbanErrorBanner(it) }
            Spacer(Modifier.height(24.dp))
            } // fin del Column
        } // fin del else
    } // fin del ModalBottomSheet
}

/** Una persona de la cita (cliente o barbero) con su foto, o iniciales si no tiene. */
@Composable
private fun PersonLine(label: String, name: String, imageUrl: String?, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        UrbanAvatar(name, Modifier.size(26.dp), imageUrl = imageUrl)
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
