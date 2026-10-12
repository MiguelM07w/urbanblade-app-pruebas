package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.background
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.rememberPaymentLauncher
import com.urbanblade.mobile.BuildConfig
import com.urbanblade.mobile.core.payment.rememberStripeReady
import com.urbanblade.mobile.ui.viewmodel.BookingPayMethod
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import coil.compose.AsyncImage
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.BarberItem
import com.urbanblade.mobile.data.model.ServiceItem
import com.urbanblade.mobile.data.model.ProductItem
import com.urbanblade.mobile.data.model.SlotItem
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.BookingViewModel
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private enum class BookingStep(val label: String, val title: String, val hint: String) {
    SERVICE("Servicio", "Elige tu servicio", "Precio y duración reales de UrbanBlade."),
    SCHEDULE("Horario", "Barbero, día y hora", "Solo ves horarios libres de verdad."),
    EXTRAS("Extras", "¿Algo más para tu visita?", "Productos opcionales y notas para tu barbero."),
    PAY("Pago", "Revisa y reserva", "Elige cómo pagar tu servicio.")
}

/**
 * Reserva en cuatro pasos cortos (propuesta A, 25-sep): servicio -> barbero, día y hora
 * (horarios reales; con un solo barbero se asigna solo) -> extras (productos opcionales que
 * se pagan en el salón, y notas) -> pago. Una barra fija muestra el total de la visita. `initialServiceId`/
 * `initialBarberId` llegan de un tap en el catálogo (autenticado, por
 * argumento de ruta) o de PendingBooking (invitado que acaba de iniciar
 * sesión) -- ver UrbanBladeRoot.kt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    initialServiceId: String? = null,
    initialBarberId: String? = null,
    onBack: () -> Unit,
    onCreated: () -> Unit,
    vm: BookingViewModel = viewModel()
) {
    val services by vm.services.collectAsState()
    val barbers by vm.barbers.collectAsState()
    val slots by vm.slots.collectAsState()
    val slotsLoading by vm.slotsLoading.collectAsState()
    val slotsError by vm.slotsError.collectAsState()
    val slotsFor by vm.slotsFor.collectAsState()
    val catalogLoading by vm.catalogLoading.collectAsState()
    val catalogError by vm.catalogError.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    val waitlistJoined by vm.waitlistJoined.collectAsState()
    val products by vm.products.collectAsState()
    val productsNote by vm.productsNote.collectAsState()
    // rememberSaveable: si Android recrea la pantalla (volver del navegador o de Stripe, rotar,
    // cambiar tema o tamaño de letra) el cliente sigue en el mismo paso con lo que ya eligió.
    // Productos de la visita: id -> cantidad (se pagan en el salón, como en la web).
    var cart by rememberSaveable { mutableStateOf(mapOf<String, Int>()) }

    // Si se llega con el servicio ya elegido (catálogo, Bladebot, "Reservar de nuevo"), se empieza
    // en el horario: antes volvía a pedir el servicio y había que tocarlo dos veces.
    val firstStep = if (initialServiceId.isNullOrBlank()) BookingStep.SERVICE else BookingStep.SCHEDULE
    var step by rememberSaveable { mutableStateOf(firstStep) }
    var firstVisitedStep by rememberSaveable { mutableStateOf(firstStep) }
    fun previousStep() {
        if (busy) return
        step = BookingStep.entries[step.ordinal - 1]
        if (step.ordinal < firstVisitedStep.ordinal) firstVisitedStep = step
    }
    // Embudo de reserva en Analytics: de dónde llegó y hasta qué paso avanza (sin datos personales).
    LaunchedEffect(Unit) {
        com.urbanblade.mobile.core.analytics.UrbanAnalytics.log(
            "reserva_inicio",
            "origen" to when {
                !initialServiceId.isNullOrBlank() -> "servicio_elegido"
                !initialBarberId.isNullOrBlank() -> "barbero_elegido"
                else -> "directo"
            }
        )
    }
    LaunchedEffect(step) { com.urbanblade.mobile.core.analytics.UrbanAnalytics.log("reserva_paso", "paso" to step.name.lowercase()) }
    // Remote Config: el pago con tarjeta se puede apagar desde Firebase (p. ej. si Stripe falla).
    val cardEnabledRemotely by com.urbanblade.mobile.core.config.UrbanRemoteConfig.cardPayments.collectAsState()
    var serviceId by rememberSaveable { mutableStateOf(initialServiceId.orEmpty()) }
    var barberId by rememberSaveable { mutableStateOf(initialBarberId.orEmpty()) }
    var date by rememberSaveable { mutableStateOf(vm.defaultDate()) }
    var time by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var slotsRequested by rememberSaveable { mutableStateOf(false) }
    var confirmed by rememberSaveable { mutableStateOf(false) }
    // El cliente acepta el cargo por inasistencia en el último paso (el backend guarda cuándo lo aceptó).
    var acceptsNoShowFee by rememberSaveable { mutableStateOf(false) }
    val pay = rememberBookingPaymentState()
    var localError by remember { mutableStateOf<String?>(null) }
    val paymentNote by vm.paymentNote.collectAsState()
    val cardConfirm by vm.cardConfirm.collectAsState()
    val transferInfo by vm.transferInfo.collectAsState()
    val savedCards by vm.savedCards.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val cardAvailable = rememberStripeReady()
    val pickReceipt = rememberSingleImagePicker { pay.receiptUri = it }
    val paymentLauncher = rememberPaymentLauncher(BuildConfig.STRIPE_PUBLISHABLE_KEY) { result ->
        when (result) {
            is PaymentResult.Completed -> vm.onCardResult(true, false, null)
            is PaymentResult.Canceled -> vm.onCardResult(false, true, null)
            is PaymentResult.Failed -> vm.onCardResult(false, false, result.throwable.localizedMessage)
        }
    }
    // Con la cita ya creada, confirma el cobro con la tarjeta guardada o con lo que el cliente escribió.
    LaunchedEffect(cardConfirm) {
        val confirm = cardConfirm ?: return@LaunchedEffect
        val savedId = confirm.paymentMethodId
        if (savedId != null) {
            paymentLauncher.confirm(ConfirmPaymentIntentParams.createWithPaymentMethodId(savedId, confirm.clientSecret))
        } else {
            val params = pay.paymentMethodParams()
            if (params != null) {
                paymentLauncher.confirm(ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(params, confirm.clientSecret))
            } else {
                vm.onCardResult(false, false, "no se pudieron leer los datos de la tarjeta")
            }
        }
    }

    LaunchedEffect(Unit) { vm.loadPaymentOptions() }
    LaunchedEffect(Unit) { vm.loadCatalog() }
    // Combinación para la que se eligió la hora: si la pantalla se recrea con la misma, la hora se conserva.
    var slotsKey by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(barberId, serviceId, date) {
        val key = "$barberId|$serviceId|$date"
        if (key != slotsKey) {
            time = ""
            slotsKey = key
        }
        slotsRequested = false
        if (barberId.isNotBlank() && serviceId.isNotBlank() && date.isNotBlank()) {
            vm.loadSlots(barberId, serviceId, date)
            slotsRequested = true
        }
    }

    val selectedService = services.firstOrNull { it.id == serviceId }
    // Si llega desde el Muro de Inspiración trae el id del usuario del barbero: se traduce al de su perfil.
    LaunchedEffect(barbers) {
        if (barberId.isBlank()) barbers.firstOrNull { it.esFavorito }?.let { barberId = it.id }
        resolveBarberId(barbers, barberId)?.let { resolved -> if (resolved != barberId) barberId = resolved }
    }
    val selectedBarber = barbers.firstOrNull { it.id == barberId }
    // Con un solo barbero no hay nada que elegir: se asigna solo.
    LaunchedEffect(barbers) {
        if (barberId.isBlank() && barbers.size == 1) barberId = barbers.first().id
    }
    val cartLines = products.mapNotNull { p -> cart[p.id]?.takeIf { it > 0 }?.let { p to it } }
    val productsTotal = cartLines.sumOf { (p, qty) -> p.precioVenta * qty }
    val servicePrice = selectedService?.precio ?: 0.0
    val tip = if (step == BookingStep.PAY) pay.tipFor(servicePrice) else 0.0
    // "Reservar" queda donde estaba "Continuar": se activa un momento después de llegar a Pago para
    // que un doble toque en "Continuar" no confirme la reserva sin que el cliente la haya revisado.
    var reserveArmed by remember { mutableStateOf(false) }
    LaunchedEffect(step) {
        reserveArmed = false
        if (step == BookingStep.PAY) {
            kotlinx.coroutines.delay(700)
            reserveArmed = true
        }
    }
    val visitTotal = servicePrice + productsTotal + tip

    val validSlot = selectedService != null && selectedBarber != null &&
        slotsFor == "$barberId|$serviceId|$date" && !slotsLoading && slotsError == null &&
        time.isNotBlank() && slots.any { it.time == time }
    val canAdvance = !catalogLoading && catalogError == null && when (step) {
        BookingStep.SERVICE -> selectedService != null
        else -> validSlot
    }
    // El "atrás" del teléfono regresa un paso; desde el paso en que se entró sale de la reserva.
    androidx.activity.compose.BackHandler(enabled = !confirmed && step.ordinal > firstVisitedStep.ordinal) {
        previousStep()
    }

    // Momento de éxito antes de salir del flujo (antes se saltaba de pantalla sin confirmar nada).
    if (confirmed) {
        UrbanSuccessScreen(
            title = "¡Cita reservada!",
            message = listOfNotNull(paymentNote ?: "Te avisaremos cuando el barbero la confirme.", productsNote).joinToString("\n\n"),
            details = listOfNotNull(
                selectedService?.let { "Servicio" to it.nombre },
                selectedBarber?.user?.name?.let { "Barbero" to it },
                cartLines.takeIf { it.isNotEmpty() }?.let { lines -> "Productos" to lines.joinToString(", ") { (p, q) -> if (q > 1) "${p.nombre} ×$q" else p.nombre } },
                "Fecha" to UrbanFormat.date(date),
                "Hora" to UrbanFormat.time(time)
            ),
            primaryText = "Ver mis citas",
            onPrimary = onCreated
        )
        return
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { UrbanTopBar(title = "", onBack = {
            if (step.ordinal > firstVisitedStep.ordinal) previousStep()
            else onBack()
        }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            BookingHeader(
                step = step,
                // Lo que ya eligió, visible en los pasos siguientes para no perder el hilo. En Pago no
                // hace falta: la tarjeta "Tu cita" ya lo muestra completo.
                summary = if (step == BookingStep.PAY) null else listOfNotNull(
                    selectedService?.takeIf { step != BookingStep.SERVICE }?.nombre,
                    selectedBarber?.takeIf { step.ordinal > BookingStep.SCHEDULE.ordinal }?.user?.name,
                    time.takeIf { it.isNotBlank() && step.ordinal > BookingStep.SCHEDULE.ordinal }?.let { "${UrbanFormat.dateShort(date)} · ${UrbanFormat.time(it)}" }
                ).joinToString("  ·  ").ifBlank { null }
            )
            Spacer(Modifier.height(8.dp))

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                if (step == BookingStep.SCHEDULE && slotsLoading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Consultando horarios…", color = UrbanColors.Muted)
                }
                if (step == BookingStep.SCHEDULE) slotsError?.let {
                    UrbanErrorBanner(it)
                    UrbanOutlineButton("Reintentar horarios", { vm.loadSlots(barberId, serviceId, date) },
                        enabled = !slotsLoading, modifier = Modifier.fillMaxWidth())
                }
                if (step.ordinal > BookingStep.SCHEDULE.ordinal && !validSlot) {
                    UrbanInfoBanner(if (slotsLoading) "Volviendo a comprobar tu horario…" else "Revisa el horario antes de continuar.", Icons.Default.Schedule)
                    UrbanOutlineButton("Revisar horario", { step = BookingStep.SCHEDULE }, modifier = Modifier.fillMaxWidth())
                }
                when {
                    catalogLoading && services.isEmpty() -> UrbanSkeletonList(3)
                    catalogError != null -> UrbanMascotState(
                        UrbanStateKind.ERROR, "No pudimos cargar los servicios y barberos", catalogError,
                        "Reintentar", onAction = { vm.loadCatalog() }
                    )
                    else -> when (step) {
                        BookingStep.SERVICE -> Column {
                            if (initialBarberId != null && selectedBarber != null && barbers.size > 1) {
                                UrbanInfoBanner(
                                    "Reservando con ${selectedBarber.user?.name ?: "tu barbero"}. Puedes cambiarlo en el paso 2.",
                                    Icons.Default.Person
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                            ServiceStep(services, serviceId) { serviceId = it }
                        }
                        BookingStep.SCHEDULE -> CalendarStep(
                            header = { BarberPicker(barbers, barberId) { barberId = it } },
                            date = date,
                            onDateChange = { date = it },
                            slots = slots,
                            time = time,
                            onTimeChange = { time = it },
                            showEmptyState = slotsRequested && !slotsLoading && slotsError == null && slots.isEmpty(),
                            loading = slotsLoading,
                            waitlistJoined = waitlistJoined,
                            onJoinWaitlist = { vm.joinWaitlist(barberId, serviceId, date) }
                        )
                        BookingStep.EXTRAS -> ExtrasStep(
                            products = products,
                            cart = cart,
                            onQuantity = { id, qty -> cart = if (qty <= 0) cart - id else cart + (id to qty) },
                            notes = notes,
                            onNotesChange = { notes = it }
                        )
                        BookingStep.PAY -> ReviewStep(
                            service = selectedService,
                            barber = selectedBarber,
                            date = date,
                            time = time,
                            extras = cartLines,
                            notes = notes
                        ) {
                            BookingPaymentSection(
                                state = pay,
                                servicePrice = selectedService?.precio ?: 0.0,
                                transferInfo = transferInfo,
                                savedCards = savedCards,
                                cardAvailable = cardAvailable && cardEnabledRemotely,
                                testMode = BuildConfig.STRIPE_PUBLISHABLE_KEY.startsWith("pk_test_"),
                                onPickReceipt = pickReceipt
                            )
                        }
                    }
                }
            }

            (localError ?: error)?.let {
                Box(Modifier.padding(horizontal = 18.dp)) { UrbanErrorBanner(it) }
                Spacer(Modifier.height(8.dp))
            }

            // Barra fija: el total de la visita se va sumando conforme eliges.
            if (selectedService != null) {
                HorizontalDivider(color = UrbanColors.Line)
                Row(
                    Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Total estimado", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
                        if (productsTotal > 0 || tip > 0) {
                            Text(
                                listOfNotNull(
                                    "Servicio ${UrbanFormat.moneyShort(servicePrice)}",
                                    productsTotal.takeIf { it > 0 }?.let { "productos ${UrbanFormat.moneyShort(it)}" },
                                    tip.takeIf { it > 0 }?.let { "propina ${UrbanFormat.moneyShort(it)}" }
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = UrbanColors.Muted
                            )
                        }
                    }
                    Text("${UrbanFormat.moneyShort(visitTotal)}", style = MaterialTheme.typography.titleLarge, color = UrbanColors.GoldText)
                }
            }
            if (step == BookingStep.PAY) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { acceptsNoShowFee = !acceptsNoShowFee }
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = acceptsNoShowFee, onCheckedChange = { acceptsNoShowFee = it })
                    Text(
                        "Acepto el cargo por inasistencia: si no llego a mi cita sin cancelar a tiempo, se cobra un porcentaje del servicio a mi tarjeta guardada o queda como adeudo por pagar en la barbería para volver a reservar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = UrbanColors.Muted
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step != BookingStep.SERVICE) {
                    UrbanOutlineButton(
                        text = if (step == BookingStep.SCHEDULE) "Cambiar servicio" else "Atrás",
                        onClick = { previousStep() },
                        enabled = !busy,
                        icon = Icons.Default.ArrowBack,
                        modifier = Modifier.weight(1f)
                    )
                }
                UrbanPrimaryButton(
                    text = when (step) {
                        BookingStep.PAY -> "Reservar"
                        BookingStep.EXTRAS -> if (cartLines.isEmpty() && notes.isBlank()) "Omitir" else "Continuar"
                        else -> "Continuar"
                    },
                    onClick = {
                        if (step == BookingStep.PAY) {
                            localError = null
                            val savedId = pay.savedCardToUse(savedCards)
                            if (pay.method == BookingPayMethod.TARJETA && savedId == null && pay.newCardProblem() != null) {
                                localError = pay.newCardProblem()
                                // Lleva al formulario para que se vea qué falta (antes quedaba abajo, fuera de la vista).
                                pay.cardAttention++
                            } else {
                                vm.reserve(
                                    context, barberId, serviceId, date, time, notes,
                                    pay.method, pay.tipFor(selectedService?.precio ?: 0.0), pay.receiptUri,
                                    savedCardId = savedId,
                                    saveCard = pay.saveCard,
                                    productos = cartLines.map { (p, qty) -> com.urbanblade.mobile.data.model.OrderItemRequest(p.id, qty) },
                                    aceptaCargo = acceptsNoShowFee
                                ) {
                                    confirmed = true
                                    com.urbanblade.mobile.core.analytics.UrbanAnalytics.log(
                                        "reserva_confirmada",
                                        "metodo_pago" to pay.method.name.lowercase(),
                                        "value" to (selectedService?.precio ?: 0.0),
                                        "currency" to "MXN",
                                        "con_productos" to cartLines.isNotEmpty()
                                    )
                                }
                            }
                        } else {
                            step = BookingStep.entries[step.ordinal + 1]
                        }
                    },
                    enabled = canAdvance && (step != BookingStep.PAY || (reserveArmed && acceptsNoShowFee)),
                    loading = busy && step == BookingStep.PAY,
                    icon = if (step == BookingStep.PAY) Icons.Default.CheckCircle else Icons.Default.ArrowForward,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Encabezado del paso con el patrón de la app: eyebrow con el paso, título serif, avance y resumen. */
@Composable
private fun BookingHeader(step: BookingStep, summary: String?) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        UrbanPageHeader(
            title = step.title,
            subtitle = step.hint,
            eyebrow = "Reserva · paso ${step.ordinal + 1} de ${BookingStep.entries.size}"
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BookingStep.entries.forEach { s ->
                val active = s.ordinal <= step.ordinal
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(if (active) UrbanColors.Gold else UrbanColors.Line, RoundedCornerShape(2.dp))
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            BookingStep.entries.forEach { s ->
                Text(
                    s.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (s == step) UrbanColors.GoldText else UrbanColors.Muted,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        summary?.let {
            Spacer(Modifier.height(10.dp))
            Surface(shape = MaterialTheme.shapes.small, color = UrbanColors.Gold.copy(alpha = 0.10f), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = UrbanColors.Gold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ServiceStep(services: List<ServiceItem>, selectedId: String, onSelect: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<ServiceFilter?>(null) }
    val filters = remember(services) { ServiceFilter.entries.filter { f -> services.any { serviceKind(it.nombre) in f.kinds } } }
    val visible = remember(services, query, filter) { filterServices(services, query, filter) }
    Column {
        if (services.isEmpty()) {
            UrbanSkeletonList(4)
        } else {
            UrbanTextField(
                value = query,
                onValueChange = { query = it },
                label = "Buscar servicio",
                placeholder = "Fade, barba, keratina…",
                leadingIcon = Icons.Default.Search,
                imeAction = ImeAction.Done
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Todos") }, colors = bookingChipColors())
                filters.forEach { f ->
                    FilterChip(selected = filter == f, onClick = { filter = if (filter == f) null else f }, label = { Text(f.label) }, colors = bookingChipColors())
                }
            }
            Spacer(Modifier.height(10.dp))
            if (visible.isEmpty()) {
                UrbanMascotState(
                    UrbanStateKind.EMPTY,
                    "No encontramos ese servicio",
                    "Prueba con otra palabra o quita el filtro.",
                    actionLabel = "Limpiar búsqueda",
                    actionIcon = Icons.Default.Close,
                    onAction = { query = ""; filter = null }
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(visible, key = { it.id }) { service ->
                    CompactServiceRow(
                        selected = service.id == selectedId,
                        onClick = { onSelect(service.id) },
                        title = service.nombre,
                        subtitle = "${service.duracionMin} min",
                        trailing = "${UrbanFormat.moneyShort(service.precio)}",
                        imageUrl = service.imagen,
                        fallbackIcon = serviceIcon(service.nombre)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactServiceRow(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
    subtitle: String?,
    trailing: String? = null,
    imageUrl: String? = null,
    avatarName: String? = null,
    /** Sin foto del servicio, un ícono según su tipo (el mismo que en Explorar). */
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(if (selected) UrbanColors.Gold.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
                this.selected = selected
            }
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(if (avatarName != null) CircleShape else RoundedCornerShape(8.dp))
                        .background(UrbanColors.CardAlt)
                )
                Spacer(Modifier.width(12.dp))
            } else if (avatarName != null) {
                UrbanAvatar(avatarName, Modifier.size(44.dp))
                Spacer(Modifier.width(12.dp))
            } else if (fallbackIcon != null) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(UrbanColors.Gold.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(fallbackIcon, null, tint = UrbanColors.Gold, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
            }
            
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) UrbanColors.Gold else UrbanColors.Ink,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                )
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                }
            }
            
            trailing?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) UrbanColors.Gold else UrbanColors.Ink
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            if (selected) {
                Icon(Icons.Default.CheckCircle, null, tint = UrbanColors.Gold, modifier = Modifier.size(22.dp))
            } else {
                Box(Modifier.size(22.dp))
            }
        }
        HorizontalDivider(color = UrbanColors.Line.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 12.dp))
    }
}

@Composable
private fun CalendarStep(
    header: @Composable () -> Unit = {},
    date: String,
    onDateChange: (String) -> Unit,
    slots: List<SlotItem>,
    time: String,
    onTimeChange: (String) -> Unit,
    showEmptyState: Boolean,
    loading: Boolean,
    waitlistJoined: Boolean,
    onJoinWaitlist: () -> Unit
) {
    val days = remember { (0..29).map { LocalDate.now().plusDays(it.toLong()) } }
    val selectedDate = remember(date) { runCatching { LocalDate.parse(date) }.getOrNull() }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        header()
        UrbanFieldLabel("Día")
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(days) { day ->
                val selected = day == selectedDate
                Surface(
                    onClick = { onDateChange(day.toString()) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) UrbanColors.Gold else UrbanColors.Card,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) UrbanColors.Gold else UrbanColors.Line),
                    modifier = Modifier.width(58.dp)
                ) {
                    Column(
                        Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es", "MX")).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) UrbanColors.OnGold else UrbanColors.Muted
                        )
                        Text(
                            day.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) UrbanColors.OnGold else UrbanColors.Ink
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        UrbanFieldLabel("Hora")
        Spacer(Modifier.height(8.dp))
        when {
            slots.isNotEmpty() -> {
                // Agrupados por franja (como en la web): mañana < 12:00, tarde 12:00-17:59, noche desde 18:00.
                val groups = listOf(
                    Triple("Mañana", Icons.Default.WbSunny, slots.filter { (it.time.take(2).toIntOrNull() ?: 0) < 12 }),
                    Triple("Tarde", Icons.Default.WbTwilight, slots.filter { (it.time.take(2).toIntOrNull() ?: 0) in 12..17 }),
                    Triple("Noche", Icons.Default.NightsStay, slots.filter { (it.time.take(2).toIntOrNull() ?: 0) >= 18 })
                ).filter { it.third.isNotEmpty() }
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    groups.forEach { (label, icon, group) ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, null, tint = UrbanColors.Gold, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(label, style = MaterialTheme.typography.labelLarge, color = UrbanColors.Muted)
                            }
                            group.chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    row.forEach { slot ->
                                        SlotChip(slot, selected = slot.time == time, onClick = { onTimeChange(slot.time) }, modifier = Modifier.weight(1f))
                                    }
                                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
            }
            showEmptyState -> {
                UrbanMascotState(
                    UrbanStateKind.EMPTY,
                    "Sin horarios este día",
                    "Prueba otro día o únete a la lista de espera y te avisamos si se libera un horario.",
                    actionLabel = if (!waitlistJoined) "Unirme a la lista de espera" else null,
                    actionIcon = Icons.Default.NotificationsActive,
                    onAction = if (!waitlistJoined) onJoinWaitlist else null
                )
                if (waitlistJoined) {
                    UrbanInfoBanner("Ya te anotamos en la lista de espera para este día.", Icons.Default.NotificationsActive)
                }
            }
            loading -> UrbanSkeletonList(2)
            else -> Unit
        }
    }
}

/** Barbero dentro del paso de horario: si solo hay uno, se muestra ya elegido. */
@Composable
private fun BarberPicker(barbers: List<BarberItem>, selectedId: String, onSelect: (String) -> Unit) {
    if (barbers.isEmpty()) {
        UrbanSkeletonList(1)
        Spacer(Modifier.height(12.dp))
        return
    }
    UrbanFieldLabel("Barbero")
    Spacer(Modifier.height(8.dp))
    if (barbers.size == 1) {
        val only = barbers.first()
        Row(verticalAlignment = Alignment.CenterVertically) {
            UrbanAvatar(only.user?.name ?: "Barbero", Modifier.size(36.dp), imageUrl = only.foto)
            Spacer(Modifier.width(10.dp))
            Text("Con ${only.user?.name ?: "tu barbero"}", style = MaterialTheme.typography.titleSmall, color = UrbanColors.Ink)
        }
    } else {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            barbers.forEach { b ->
                FilterChip(
                    selected = b.id == selectedId,
                    onClick = { onSelect(b.id) },
                    label = { Text(b.user?.name?.substringBefore(' ') ?: "Barbero") },
                    leadingIcon = { UrbanAvatar(b.user?.name ?: "B", Modifier.size(22.dp), imageUrl = b.foto) },
                    colors = bookingChipColors()
                )
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}

/** Paso 3: productos opcionales (se pagan en el salón) y notas para el barbero. */
@Composable
private fun ExtrasStep(
    products: List<ProductItem>,
    cart: Map<String, Int>,
    onQuantity: (String, Int) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit
) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (products.isNotEmpty()) {
            UrbanFieldLabel("Productos (opcional)")
            Text("Te los apartamos para tu visita y los pagas en el salón.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            products.forEach { p ->
                val qty = cart[p.id] ?: 0
                UrbanCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!p.imagen.isNullOrBlank()) {
                            AsyncImage(p.imagen, null, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(UrbanColors.CardAlt))
                        } else {
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(UrbanColors.Gold.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ShoppingBag, null, tint = UrbanColors.Gold)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.nombre, style = MaterialTheme.typography.titleSmall, color = UrbanColors.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${UrbanFormat.moneyShort(p.precioVenta)}", style = MaterialTheme.typography.bodyMedium, color = UrbanColors.GoldText)
                        }
                        if (qty == 0) {
                            TextButton(onClick = { onQuantity(p.id, 1) }) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = UrbanColors.Gold)
                                Spacer(Modifier.width(4.dp))
                                Text("Agregar", color = UrbanColors.GoldText)
                            }
                        } else {
                            IconButton(onClick = { onQuantity(p.id, qty - 1) }) { Icon(Icons.Default.Remove, "Quitar uno", tint = UrbanColors.Gold) }
                            Text("$qty", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Ink)
                            IconButton(onClick = { onQuantity(p.id, qty + 1) }, enabled = qty < minOf(p.stockActual, 5)) {
                                Icon(Icons.Default.Add, "Agregar uno", tint = UrbanColors.Gold)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        UrbanTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = "Notas para tu barbero (opcional)",
            placeholder = "Ej. degradado bajo, barba corta…",
            leadingIcon = Icons.Default.EditNote,
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Default,
            minLines = 3
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun bookingChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = UrbanColors.Gold,
    selectedLabelColor = UrbanColors.OnGold
)

@Composable
private fun SlotChip(slot: SlotItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) UrbanColors.Gold else UrbanColors.Card,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) UrbanColors.Gold else UrbanColors.Line),
        modifier = modifier.semantics { role = Role.RadioButton; this.selected = selected }
    ) {
        Box(Modifier.padding(vertical = 12.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                slot.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) UrbanColors.OnGold else UrbanColors.Ink
            )
        }
    }
}

@Composable
private fun ReviewStep(
    service: ServiceItem?,
    barber: BarberItem?,
    date: String,
    time: String,
    extras: List<Pair<ProductItem, Int>>,
    notes: String,
    payment: @Composable ColumnScope.() -> Unit
) {
    // Con el pago el paso es más largo que la pantalla: sin scroll la barra inferior tapaba las opciones.
    Column(Modifier.verticalScroll(rememberScrollState())) {
        UrbanHeroCard {
            UrbanHeroLabel("Tu cita")
            Spacer(Modifier.height(6.dp))
            Text(time.takeIf { it.isNotBlank() }?.let { UrbanFormat.time(it) } ?: "—", style = MaterialTheme.typography.displaySmall, color = UrbanColors.Ink)
            Text(UrbanFormat.date(date), style = MaterialTheme.typography.titleMedium, color = UrbanColors.GoldText)
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = UrbanColors.Ink.copy(alpha = 0.22f))
            Spacer(Modifier.height(12.dp))
            ReviewRow(serviceIcon(service?.nombre.orEmpty()), service?.nombre ?: "—", service?.let { "${it.duracionMin} min · ${UrbanFormat.moneyShort(it.precio)}" })
            Spacer(Modifier.height(10.dp))
            ReviewRow(Icons.Default.Person, barber?.user?.name ?: "—", "Te confirma la cita desde su agenda")
        }
        if (notes.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            UrbanInfoBanner("Nota para tu barbero: $notes", Icons.Default.EditNote)
        }
        if (extras.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            UrbanCard(Modifier.fillMaxWidth()) {
                Text("Productos de tu visita", style = MaterialTheme.typography.titleSmall, color = UrbanColors.Ink)
                Text("Se pagan en el salón al recogerlos, con su propio comprobante.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                Spacer(Modifier.height(8.dp))
                extras.forEach { (p, qty) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text("${p.nombre}${if (qty > 1) " ×$qty" else ""}", style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Ink, modifier = Modifier.weight(1f))
                        Text("${UrbanFormat.moneyShort(p.precioVenta * qty)}", style = MaterialTheme.typography.bodyMedium, color = UrbanColors.GoldText)
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        payment()
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun ReviewRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = UrbanColors.Gold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = UrbanColors.Ink)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted) }
        }
    }
}
