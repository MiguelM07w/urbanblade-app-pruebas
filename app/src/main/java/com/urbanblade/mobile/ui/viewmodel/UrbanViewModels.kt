package com.urbanblade.mobile.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.*
import com.urbanblade.mobile.data.repository.UrbanRepository
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDate

class DashboardViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _data = MutableStateFlow<DashboardResponse?>(null)
    val data = _data.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        _loading.value = true; _error.value = null
        try { _data.value = repo.dashboard() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar el dashboard.") }
        finally { _loading.value = false }
    }
}

class AppointmentsViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _data = MutableStateFlow(AppointmentsResponse())
    val data = _data.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // Acción de reagendar tiene su propio busy -- no debe deshabilitar toda
    // la lista mientras el usuario edita en la hoja de reagendado.
    private val _rescheduling = MutableStateFlow(false)
    val rescheduling = _rescheduling.asStateFlow()

    private val _waitlistEntries = MutableStateFlow<List<WaitlistEntry>>(emptyList())
    val waitlistEntries = _waitlistEntries.asStateFlow()

    // Adeudo por inasistencia del cliente (mientras sea > 0 no puede reservar) y ticket del servicio terminado.
    private val _debt = MutableStateFlow(0.0)
    val debt = _debt.asStateFlow()
    private val _ticket = MutableStateFlow<ServiceTicket?>(null)
    val ticket = _ticket.asStateFlow()
    private val _ticketBusy = MutableStateFlow<String?>(null)
    val ticketBusy = _ticketBusy.asStateFlow()

    /** Consulta lo que el cliente debe por inasistencias; un fallo no molesta: solo no se muestra el aviso. */
    fun loadDebt() = viewModelScope.launch {
        try { _debt.value = repo.noShowFees().adeudoTotal ?: 0.0 } catch (_: Exception) { _debt.value = 0.0 }
    }

    fun openTicket(code: String) = viewModelScope.launch {
        _ticketBusy.value = code
        try { _ticket.value = repo.appointmentTicket(code) }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("Esta cita todavía no tiene ticket.") }
        finally { _ticketBusy.value = null }
    }

    fun dismissTicket() { _ticket.value = null }

    // Solo para la hoja de reagendado (mantiene el servicio de la cita,
    // permite cambiar barbero/fecha/hora) -- estado independiente del
    // wizard de nueva reserva en BookingViewModel.
    private val _barbers = MutableStateFlow<List<BarberItem>>(emptyList())
    val barbers = _barbers.asStateFlow()
    private val _rescheduleSlots = MutableStateFlow<List<SlotItem>>(emptyList())
    val rescheduleSlots = _rescheduleSlots.asStateFlow()

    // Checkout de cita: crear el intent de Stripe tiene su propio busy;
    // subir comprobante de transferencia también, para no bloquear el resto
    // de la hoja mientras cualquiera de los dos está en curso.
    private val _checkoutBusy = MutableStateFlow(false)
    val checkoutBusy = _checkoutBusy.asStateFlow()
    private val _uploadingReceipt = MutableStateFlow(false)
    val uploadingReceipt = _uploadingReceipt.asStateFlow()
    private val _stripeClientSecret = MutableStateFlow<String?>(null)
    val stripeClientSecret = _stripeClientSecret.asStateFlow()

    // La confirmación real del pago con tarjeta la hace el webhook de
    // Stripe en barber, no la respuesta del intent -- tras un
    // PaymentSheetResult.Completed hay una ventana real donde la cita
    // todavía no trae hasPayment=true. Este flag solo controla el mensaje
    // "confirmando tu pago" mientras se reintenta un refresco corto.
    private val _confirmingPayment = MutableStateFlow(false)
    val confirmingPayment = _confirmingPayment.asStateFlow()

    // Aviso informativo del checkout con tarjeta (cancelación o confirmación
    // demorada). Separado de _error porque no es un fallo: la cita solo queda
    // pagada cuando el webhook de barber recibe payment_intent.succeeded.
    private val _paymentNotice = MutableStateFlow<String?>(null)
    val paymentNotice = _paymentNotice.asStateFlow()

    // Tarjetas guardadas del cliente para el carrusel del checkout (vacío si no tiene o si falla).
    private val _savedCards = MutableStateFlow<List<SavedCard>>(emptyList())
    val savedCards = _savedCards.asStateFlow()

    // Pago con una tarjeta guardada: se confirma en la hoja con PaymentLauncher, sin abrir el PaymentSheet.
    private val _savedCardConfirm = MutableStateFlow<CardConfirm?>(null)
    val savedCardConfirm = _savedCardConfirm.asStateFlow()

    fun loadSavedCards() = viewModelScope.launch {
        _savedCards.value = try { repo.savedCards() } catch (_: Exception) { emptyList() }
    }

    fun clearSavedCardConfirm() { _savedCardConfirm.value = null }

    /**
     * Con [savedCardId] el intent se crea para el Customer del cliente (tarjeta_guardada) y se confirma
     * con esa tarjeta; sin él, se abre el PaymentSheet de Stripe para una tarjeta nueva, como antes.
     */
    fun startStripeCheckout(appointmentId: String, puntosCanjeados: Int, codigoGiftCard: String?, propina: Double, savedCardId: String? = null) =
        viewModelScope.launch {
            _checkoutBusy.value = true; _error.value = null; _paymentNotice.value = null
            try {
                val data = repo.stripeIntent(
                    StripeIntentRequest(
                        appointmentId, puntosCanjeados, codigoGiftCard?.takeIf { it.isNotBlank() }, propina,
                        tarjetaGuardada = (savedCardId != null).takeIf { it }
                    )
                )
                if (savedCardId != null) _savedCardConfirm.value = CardConfirm(data.clientSecret, savedCardId)
                else _stripeClientSecret.value = data.clientSecret
            } catch (e: HttpException) {
                _error.value = if (e.code() == 422) e.serverMessage() ?: e.toFriendlyMessage("No se pudo iniciar el pago.")
                else e.toFriendlyMessage("No se pudo iniciar el pago.")
            } catch (e: Exception) {
                _error.value = e.toFriendlyMessage("No se pudo iniciar el pago.")
            } finally { _checkoutBusy.value = false }
        }

    fun clearStripeClientSecret() { _stripeClientSecret.value = null }

    /** El cliente cerró el PaymentSheet sin pagar: Stripe no hizo ningún cargo y la cita sigue pendiente de pago. */
    fun onStripeCanceled() {
        _stripeClientSecret.value = null
        _paymentNotice.value = "Cancelaste el pago con tarjeta. No se hizo ningún cargo y tu cita sigue pendiente de pago."
    }

    /** Stripe rechazó o no pudo procesar la tarjeta. La cita no se marca como pagada (solo el webhook de barber puede hacerlo). */
    fun onStripeFailed(reason: String?) {
        _stripeClientSecret.value = null
        _paymentNotice.value = null
        val detail = reason?.trim()?.takeIf { it.isNotEmpty() }?.let { ": $it" } ?: "."
        _error.value = "El pago con tarjeta no se completó$detail Intenta con otra tarjeta o paga por transferencia."
    }

    /** Reintenta cargar las citas hasta ~10s esperando a que el webhook confirme el pago. */
    fun confirmAppointmentPayment(appointmentId: String) = viewModelScope.launch {
        _confirmingPayment.value = true
        _paymentNotice.value = null
        repeat(5) {
            delay(2000)
            try {
                val fresh = repo.appointments()
                _data.value = fresh
                if (fresh.data.any { it.id == appointmentId && it.hasPayment }) return@launch
            } catch (_: Exception) { /* se reintenta en el próximo ciclo */ }
        }
        // Stripe ya aceptó el pago pero el webhook aún no llega: avisar para que
        // el cliente no intente pagar otra vez mientras se confirma.
        _paymentNotice.value = "Stripe recibió tu pago y UrbanBlade lo está confirmando. Aparecerá como pagado en unos minutos; no es necesario pagar de nuevo."
    }.also { it.invokeOnCompletion { _confirmingPayment.value = false } }

    fun uploadPaymentReceipt(context: Context, appointmentCode: String, propina: Double, receiptUri: Uri, onDone: () -> Unit) =
        viewModelScope.launch {
            _uploadingReceipt.value = true; _error.value = null
            try {
                repo.uploadPaymentReceipt(context, appointmentCode, propina, receiptUri)
                load()
                onDone()
            } catch (e: HttpException) {
                _error.value = if (e.code() == 422) e.serverMessage() ?: e.toFriendlyMessage("No se pudo subir el comprobante.")
                else e.toFriendlyMessage("No se pudo subir el comprobante.")
            } catch (e: Exception) {
                _error.value = e.toFriendlyMessage("No se pudo subir el comprobante.")
            } finally { _uploadingReceipt.value = false }
        }

    fun loadBarbers() = viewModelScope.launch {
        try { _barbers.value = repo.barbers() } catch (_: Exception) { }
    }

    fun loadRescheduleSlots(barberId: String, serviceId: String, date: String) = viewModelScope.launch {
        if (barberId.isBlank() || serviceId.isBlank() || date.isBlank()) { _rescheduleSlots.value = emptyList(); return@launch }
        try { _rescheduleSlots.value = repo.slots(barberId, serviceId, date) }
        catch (_: Exception) { _rescheduleSlots.value = emptyList() }
    }

    private val _hasMore = MutableStateFlow(false)
    /** Hay más citas (más antiguas) por traer del servidor. */
    val hasMore = _hasMore.asStateFlow()
    private val _loadingMore = MutableStateFlow(false)
    val loadingMore = _loadingMore.asStateFlow()
    private var page = 1
    private var listRequest: Job? = null
    private var pageRequest: Job? = null

    private val _month = MutableStateFlow<List<AppointmentRow>>(emptyList())
    /** Citas del mes que muestra el calendario. */
    val month = _month.asStateFlow()
    private val _monthLoading = MutableStateFlow(false)
    val monthLoading = _monthLoading.asStateFlow()
    private var currentMonth: java.time.YearMonth? = null

    fun load() {
        pageRequest?.cancel()
        _loadingMore.value = false
        listRequest?.cancel()
        listRequest = viewModelScope.launch {
            _loading.value = true; _error.value = null
            try {
                val response = repo.appointments(page = 1, perPage = PAGE_SIZE)
                coroutineContext.ensureActive()
                page = 1
                _data.value = response
                // Un servidor sin paginación no manda `meta`: se trata como lista completa.
                _hasMore.value = response.meta?.hasMore == true
            }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar las citas.") }
            finally { _loading.value = false }
            currentMonth?.let { loadMonth(it) }
        }
    }

    /** Trae la página siguiente (citas más antiguas) y la agrega al final de la lista. */
    fun loadMore() {
        if (_loading.value || listRequest?.isActive == true || pageRequest?.isActive == true || !_hasMore.value) return
        pageRequest = viewModelScope.launch {
            _loadingMore.value = true
            _error.value = null
            try {
                val response = repo.appointments(page = page + 1, perPage = PAGE_SIZE)
                coroutineContext.ensureActive()
                page += 1
                val known = _data.value.data.map { it.id }.toSet()
                _data.value = _data.value.copy(data = _data.value.data + response.data.filter { it.id !in known })
                _hasMore.value = response.meta?.hasMore == true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.toFriendlyMessage("No se pudieron cargar más citas.")
            } finally { _loadingMore.value = false }
        }
    }

    /** Carga todas las citas de un mes para el calendario (varias páginas si hace falta). */
    fun loadMonth(month: java.time.YearMonth) = viewModelScope.launch {
        currentMonth = month
        _monthLoading.value = true
        try {
            val desde = month.atDay(1).toString()
            val hasta = month.atEndOfMonth().toString()
            val rows = mutableListOf<AppointmentRow>()
            var p = 1
            while (p <= MAX_MONTH_PAGES) {
                val response = repo.appointments(page = p, perPage = 50, desde = desde, hasta = hasta)
                rows += response.data
                if (response.meta?.hasMore != true) break
                p += 1
            }
            // Si el servidor ignora el rango, este filtro deja solo el mes pedido.
            if (currentMonth == month) _month.value = rows.filter { it.fecha.startsWith(month.toString()) }
        } catch (e: Exception) {
            _error.value = e.toFriendlyMessage("No se pudo cargar el calendario.")
        } finally { _monthLoading.value = false }
    }

    private companion object {
        const val PAGE_SIZE = 30
        const val MAX_MONTH_PAGES = 6
    }

    fun loadWaitlist() = viewModelScope.launch {
        try { _waitlistEntries.value = repo.waitlist() }
        catch (_: Exception) { /* sección secundaria, no bloquea la pantalla */ }
    }

    fun leaveWaitlist(id: String) = viewModelScope.launch {
        try { repo.leaveWaitlist(id); loadWaitlist() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo salir de la lista de espera.") }
    }

    private val _actionBusy = MutableStateFlow(false)
    /** Ocupado mientras el personal cambia el estado de una cita o registra un cobro. */
    val actionBusy = _actionBusy.asStateFlow()
    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()

    fun clearNotice() {
        _notice.value = null
    }

    /** Confirmar, iniciar, completar o marcar inasistencia; la máquina de estados la valida el servidor. */
    fun changeStatus(item: AppointmentRow, estado: String) = viewModelScope.launch {
        val code = item.code ?: return@launch
        _actionBusy.value = true; _error.value = null
        try {
            repo.updateAppointmentStatus(code, estado)
            load()
        } catch (e: HttpException) {
            _error.value = if (e.code() == 422) e.serverMessage() ?: "No se puede cambiar el estado de esta cita."
            else e.toFriendlyMessage("No se pudo cambiar el estado de la cita.")
        } catch (e: Exception) {
            _error.value = e.toFriendlyMessage("No se pudo cambiar el estado de la cita.")
        } finally { _actionBusy.value = false }
    }

    /** Registra el cobro de una cita (efectivo o transferencia); el monto real lo fija el servidor. */
    fun charge(item: AppointmentRow, metodo: String, propina: Double, onDone: () -> Unit) = viewModelScope.launch {
        _actionBusy.value = true; _error.value = null
        try {
            val informativo = item.precioCobrado ?: item.service?.precio ?: 0.0
            repo.createPayment(CreatePaymentRequest(item.id, informativo, metodo, propina))
            _notice.value = "Cobro registrado."
            onDone()
            load()
        } catch (e: HttpException) {
            _error.value = if (e.code() == 422) e.serverMessage() ?: "No se pudo registrar el cobro."
            else e.toFriendlyMessage("No se pudo registrar el cobro.")
        } catch (e: Exception) {
            _error.value = e.toFriendlyMessage("No se pudo registrar el cobro.")
        } finally { _actionBusy.value = false }
    }

    /**
     * El cliente cancela su propia cita (DELETE, con la política de horas); el personal la cancela
     * cambiando el estado (PATCH status), que es lo que el servidor le permite a recepción y además
     * avisa al cliente, devuelve lo pagado al reservar y libera el horario para la lista de espera.
     */
    fun cancel(item: AppointmentRow, asStaff: Boolean = false) = viewModelScope.launch {
        val code = item.code ?: return@launch
        if (asStaff) {
            changeStatus(item, "cancelada")
            return@launch
        }
        try { repo.cancelAppointment(code); load() }
        catch (e: HttpException) {
            // El 422 aquí es la política de cancelación (N horas de
            // anticipación) o "ya no se puede cancelar" -- mostrar el mensaje
            // real del servidor, no el genérico de toFriendlyMessage().
            _error.value = if (e.code() == 422) e.serverMessage() ?: "Esta cita ya no se puede cancelar."
            else e.toFriendlyMessage("No se pudo cancelar la cita.")
        }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cancelar la cita.") }
    }

    /**
     * Reagenda una cita propia -- PUT appointments/{code}, que en barber
     * detecta el rol cliente y delega a rescheduleAsClient() (mismo
     * AppointmentRequest que crear una cita, sin política de horas mínima,
     * solo exige que la cita siga pendiente/confirmada y futura).
     */
    fun reschedule(
        code: String,
        barberId: String,
        serviceId: String,
        fecha: String,
        horaInicio: String,
        notas: String?,
        onDone: () -> Unit
    ) {
        if (code.isBlank() || barberId.isBlank() || serviceId.isBlank() || fecha.isBlank() || horaInicio.isBlank()) {
            _error.value = "Selecciona barbero, servicio, fecha y horario."
            return
        }
        viewModelScope.launch {
            _rescheduling.value = true; _error.value = null
            try {
                repo.updateAppointment(code, AppointmentRequest(barberId, serviceId, fecha, horaInicio, notas))
                load()
                onDone()
            } catch (e: HttpException) {
                _error.value = if (e.code() == 422) e.serverMessage() ?: "Esta cita ya no se puede reagendar."
                else e.toFriendlyMessage("No se pudo reagendar la cita.")
            } catch (e: Exception) {
                _error.value = e.toFriendlyMessage("No se pudo reagendar la cita.")
            } finally { _rescheduling.value = false }
        }
    }
}

/** Cómo paga el cliente al reservar: en el salón, por transferencia o con tarjeta ahora. */
enum class BookingPayMethod { EFECTIVO, TRANSFERENCIA, TARJETA }

/** PaymentIntent listo para confirmar; [paymentMethodId] es la tarjeta guardada elegida (null = tarjeta nueva). */
data class CardConfirm(val clientSecret: String, val paymentMethodId: String?)

class BookingViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {

    private val _services = MutableStateFlow<List<ServiceItem>>(emptyList())
    val services = _services.asStateFlow()
    private val _barbers = MutableStateFlow<List<BarberItem>>(emptyList())
    val barbers = _barbers.asStateFlow()
    private val _slots = MutableStateFlow<List<SlotItem>>(emptyList())
    val slots = _slots.asStateFlow()
    private val _slotsLoading = MutableStateFlow(false)
    val slotsLoading = _slotsLoading.asStateFlow()
    private val _slotsError = MutableStateFlow<String?>(null)
    val slotsError = _slotsError.asStateFlow()
    private val _slotsFor = MutableStateFlow<String?>(null)
    val slotsFor = _slotsFor.asStateFlow()
    private var slotsRequest: kotlinx.coroutines.Job? = null
    private val _catalogLoading = MutableStateFlow(false)
    val catalogLoading = _catalogLoading.asStateFlow()
    private val _catalogError = MutableStateFlow<String?>(null)
    val catalogError = _catalogError.asStateFlow()
    /** Productos con existencias para el paso "Extras" (mismo catálogo que la tienda). */
    private val _products = MutableStateFlow<List<ProductItem>>(emptyList())
    val products = _products.asStateFlow()
    /** Qué pasó con los productos de la visita, para el mensaje final. */
    private val _productsNote = MutableStateFlow<String?>(null)
    val productsNote = _productsNote.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _waitlistJoined = MutableStateFlow(false)
    val waitlistJoined = _waitlistJoined.asStateFlow()

    /**
     * Pago con tarjeta pendiente de confirmar en la pantalla: con [CardConfirm.paymentMethodId]
     * se paga con una tarjeta guardada; sin él, con lo que el cliente escribió en el formulario.
     */
    private val _cardConfirm = MutableStateFlow<CardConfirm?>(null)
    val cardConfirm = _cardConfirm.asStateFlow()

    /** Datos bancarios para transferir; null mientras cargan. */
    private val _transferInfo = MutableStateFlow<TransferInfo?>(null)
    val transferInfo = _transferInfo.asStateFlow()

    private val _savedCards = MutableStateFlow<List<SavedCard>>(emptyList())
    val savedCards = _savedCards.asStateFlow()

    /** Aviso sobre el pago tras reservar (comprobante recibido, pago pendiente, etc.). */
    private val _paymentNote = MutableStateFlow<String?>(null)
    val paymentNote = _paymentNote.asStateFlow()

    private var pendingDone: ((String?) -> Unit)? = null
    private var pendingId: String? = null

    /** Datos bancarios y tarjetas guardadas; si fallan, el pago sigue disponible (sin CLABE o sin tarjetas guardadas). */
    fun loadPaymentOptions() = viewModelScope.launch {
        _transferInfo.value = try { repo.transferInfo() } catch (_: Exception) { TransferInfo() }
        _savedCards.value = try { repo.savedCards() } catch (_: Exception) { emptyList() }
    }

    fun loadCatalog() = viewModelScope.launch {
        if (_catalogLoading.value) return@launch
        _catalogLoading.value = true
        _catalogError.value = null
        // Los productos son opcionales: si fallan, la reserva sigue sin el paso de extras con productos.
        launch { _products.value = runCatching { repo.products() }.getOrDefault(emptyList()).filter { it.stockActual > 0 } }
        try {
            val services = repo.services()
            val barbers = repo.barbers()
            _services.value = services
            _barbers.value = barbers
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            _catalogError.value = e.toFriendlyMessage("No se pudo cargar el catálogo.")
        } finally {
            _catalogLoading.value = false
        }
    }

    fun loadSlots(barberId: String, serviceId: String, date: String) {
        slotsRequest?.cancel()
        _slotsFor.value = null
        _slots.value = emptyList()
        slotsRequest = viewModelScope.launch {
            _slots.value = emptyList(); _slotsError.value = null
            if (barberId.isBlank() || serviceId.isBlank() || date.isBlank()) return@launch
            _slotsLoading.value = true
            try {
                _slots.value = repo.slots(barberId, serviceId, date)
                _slotsFor.value = "$barberId|$serviceId|$date"
            }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _slotsError.value = e.toFriendlyMessage("No pudimos consultar los horarios.") }
            finally { _slotsLoading.value = false }
        }
    }

    fun create(barberId: String, serviceId: String, date: String, time: String, notes: String, onDone: (String?) -> Unit) {
        if (barberId.isBlank() || serviceId.isBlank() || date.isBlank() || time.isBlank()) {
            _error.value = "Selecciona barbero, servicio, fecha y horario."
            return
        }
        viewModelScope.launch {
            _busy.value = true; _error.value = null; _message.value = null
            try {
                val res = repo.createAppointment(AppointmentRequest(barberId, serviceId, date, time, notes.ifBlank { null }))
                _message.value = res.message ?: "Cita reservada correctamente."
                onDone(res.data?.id)
            } catch (e: Exception) {
                _error.value = e.toFriendlyMessage("No se pudo reservar la cita.")
            } finally { _busy.value = false }
        }
    }

    /**
     * Reserva y, según el método, paga en el mismo paso (igual que la web): efectivo no cobra nada
     * (se paga en el salón), transferencia sube el comprobante y tarjeta pide el intent a barber.
     * La cita se crea primero; si el pago falla la cita queda reservada y se avisa. El monto real
     * siempre lo calcula barber, aquí solo viaja la propina.
     */
    fun reserve(
        context: Context?,
        barberId: String, serviceId: String, date: String, time: String, notes: String,
        method: BookingPayMethod, propina: Double, receiptUri: Uri?,
        savedCardId: String? = null, saveCard: Boolean = false,
        productos: List<OrderItemRequest> = emptyList(),
        aceptaCargo: Boolean = false,
        onDone: (String?) -> Unit
    ) {
        if (barberId.isBlank() || serviceId.isBlank() || date.isBlank() || time.isBlank()) {
            _error.value = "Selecciona barbero, servicio, fecha y horario."
            return
        }
        viewModelScope.launch {
            _busy.value = true; _error.value = null; _message.value = null; _paymentNote.value = null
            val res = try {
                // Igual que la web: "pagar ahora" (tarjeta, o transferencia con comprobante) le pide a barber
                // que fije el cobro completo con el descuento real del cliente; se cobra por el endpoint de depósito.
                val payNow = method == BookingPayMethod.TARJETA || (method == BookingPayMethod.TRANSFERENCIA && receiptUri != null)
                repo.createAppointment(
                    AppointmentRequest(
                        barberId, serviceId, date, time, notes.ifBlank { null },
                        pagarAhora = payNow.takeIf { it },
                        propinaSugerida = propina.takeIf { payNow && it > 0 },
                        aceptaCargoInasistencia = aceptaCargo.takeIf { it },
                        productos = productos.ifEmpty { null }
                    )
                )
            } catch (e: Exception) {
                // Un 422 trae el motivo real (una cita por día, horario ocupado): se muestra tal cual.
                _error.value = (e as? HttpException)?.takeIf { it.code() == 422 }?.serverMessage()
                    ?: e.toFriendlyMessage("No se pudo reservar la cita.")
                _busy.value = false
                return@launch
            }
            val id = res.data?.id
            val code = res.data?.code
            _productsNote.value = when {
                productos.isEmpty() -> null
                res.productosError != null -> "Tu cita quedó reservada, pero no pudimos agregar los productos: ${res.productosError}"
                else -> "Tus productos te esperan en el salón y se pagan al recogerlos."
            }
            when (method) {
                BookingPayMethod.EFECTIVO -> {
                    _paymentNote.value = "Pagas en el salón el día de tu cita."
                    _busy.value = false; onDone(id)
                }
                BookingPayMethod.TRANSFERENCIA -> {
                    // El comprobante es opcional al reservar: si no lo trae, lo sube después desde Mis citas.
                    _paymentNote.value = if (receiptUri == null) {
                        "Transfiere a la CLABE que te mostramos y sube tu comprobante desde Mis citas cuando el barbero confirme tu cita."
                    } else try {
                        if (context == null || code == null) error("sin datos")
                        repo.uploadDepositReceipt(context, code, receiptUri)
                        "Recibimos tu comprobante. Te avisaremos cuando se verifique."
                    } catch (e: Exception) {
                        "Tu cita quedó reservada, pero no se pudo subir el comprobante. Súbelo desde Mis citas."
                    }
                    _busy.value = false; onDone(id)
                }
                BookingPayMethod.TARJETA -> {
                    try {
                        if (code == null) error("sin código")
                        pendingDone = onDone; pendingId = id
                        val secret = repo.depositStripeIntent(
                            code,
                            DepositIntentRequest(
                                guardarTarjeta = (saveCard && savedCardId == null).takeIf { it },
                                tarjetaGuardada = (savedCardId != null).takeIf { it }
                            )
                        ).clientSecret
                        _cardConfirm.value = CardConfirm(secret, savedCardId)
                    } catch (e: Exception) {
                        pendingDone = null
                        _paymentNote.value = "Tu cita quedó reservada, pero no pudimos iniciar el pago con tarjeta. Págala desde Mis citas."
                        _busy.value = false; onDone(id)
                    }
                }
            }
        }
    }

    /** Resultado de confirmar la tarjeta con Stripe. El cobro real lo confirma el webhook de barber. */
    fun onCardResult(completed: Boolean, canceled: Boolean, reason: String?) {
        _cardConfirm.value = null
        _paymentNote.value = when {
            completed -> "Pago con tarjeta enviado. UrbanBlade lo está confirmando."
            canceled -> "Cancelaste el pago. Tu cita quedó reservada y puedes pagarla desde Mis citas."
            else -> "El pago con tarjeta no se completó${reason?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""}. Tu cita quedó reservada; págala desde Mis citas."
        }
        _busy.value = false
        pendingDone?.invoke(pendingId)
        pendingDone = null
    }

    fun defaultDate(): String = LocalDate.now().toString()

    fun joinWaitlist(barberId: String, serviceId: String, date: String) = viewModelScope.launch {
        if (barberId.isBlank() || serviceId.isBlank() || date.isBlank()) return@launch
        try {
            val res = repo.joinWaitlist(barberId, serviceId, date)
            _message.value = res.message ?: "Te anotamos en la lista de espera."
            _waitlistJoined.value = true
        } catch (e: Exception) {
            _error.value = e.toFriendlyMessage("No se pudo unir a la lista de espera.")
        }
    }
}

class CatalogViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _services = MutableStateFlow<List<ServiceItem>>(emptyList())
    val services = _services.asStateFlow()
    private val _barbers = MutableStateFlow<List<BarberItem>>(emptyList())
    val barbers = _barbers.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _favoriteBusy = MutableStateFlow(false)
    val favoriteBusy = _favoriteBusy.asStateFlow()

    fun favorite(barber: BarberItem) {
        if (_favoriteBusy.value) return
        _favoriteBusy.value = true
        viewModelScope.launch {
            _error.value = null
            try {
                val saved = repo.favoriteBarber(if (barber.esFavorito) null else barber.id)
                _barbers.value = _barbers.value.map { it.copy(esFavorito = it.id == saved.barberId) }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.toFriendlyMessage("No pudimos guardar tu barbero favorito.") }
            finally { _favoriteBusy.value = false }
        }
    }

    fun load() = viewModelScope.launch {
        _loading.value = true; _error.value = null
        try {
            _services.value = repo.services()
            _barbers.value = repo.barbers()
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar el catálogo.") }
        finally { _loading.value = false }
    }
}

/** Ficha del barbero para el cliente: datos, portafolio, reseñas y calificar por servicio. */
class BarberProfileViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _profile = MutableStateFlow<BarberProfileResponse?>(null)
    val profile = _profile.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _sending = MutableStateFlow(false)
    val sending = _sending.asStateFlow()
    private val _reviewError = MutableStateFlow<String?>(null)
    val reviewError = _reviewError.asStateFlow()
    private val _reviewSent = MutableStateFlow<String?>(null)
    val reviewSent = _reviewSent.asStateFlow()

    fun load(slug: String) = viewModelScope.launch {
        _loading.value = true; _error.value = null
        try { _profile.value = repo.barberProfile(slug) }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar el perfil del barbero.") }
        finally { _loading.value = false }
    }

    fun review(slug: String, rating: Int, comment: String, serviceId: String?) = viewModelScope.launch {
        _sending.value = true; _reviewError.value = null
        try {
            val res = repo.reviewBarber(slug, BarberReviewRequest(rating, comment.trim().ifBlank { null }, serviceId))
            _reviewSent.value = res.message ?: "¡Gracias por tu reseña!"
            com.urbanblade.mobile.core.analytics.UrbanAnalytics.log("resena_enviada", "calificacion" to rating, "con_servicio" to (serviceId != null), "con_comentario" to comment.isNotBlank())
            _profile.value = repo.barberProfile(slug)
        } catch (e: Exception) {
            // 422 trae el motivo real (ya la reseñaste, servicio no recibido...).
            _reviewError.value = (e as? HttpException)?.takeIf { it.code() == 422 }?.serverMessage()
                ?: e.toFriendlyMessage("No se pudo enviar tu reseña.")
        } finally { _sending.value = false }
    }
}

data class WalletData(
    val loyalty: ClientLoyalty? = null,
    val membership: MyMembership? = null,
    val packages: List<MyPackage> = emptyList(),
    val giftCards: List<GiftCard> = emptyList(),
    val referrals: ReferralInfo? = null,
    /** Planes para contratar desde la app (solo se muestran si no tiene membresía). */
    val plans: List<MembershipPlan> = emptyList(),
    val savedCards: List<SavedCard> = emptyList()
) {
    val hasContent: Boolean get() = loyalty != null || membership != null || packages.isNotEmpty() ||
        giftCards.isNotEmpty() || referrals != null || plans.isNotEmpty()
}

/** Primer cobro de la membresía por confirmar con Stripe: tarjeta guardada o la que escribió. */
data class MembershipCheckout(val clientSecret: String, val savedCardId: String?)

/** Beneficios y membresía. Conserva datos previos al fallar una fuente; compras en BenefitPurchaseViewModel. */
class WalletViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _data = MutableStateFlow(WalletData())
    val data = _data.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        if (_loading.value) return@launch
        _loading.value = true; _error.value = null
        val previous = _data.value
        var failures = 0
        suspend fun <T> source(fallback: T, read: suspend () -> T): T = try { read() }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) {
            failures++
            if (_error.value == null) _error.value = e.toFriendlyMessage("No pudimos actualizar todos tus beneficios. Intenta de nuevo.")
            fallback
        }
        try {
            _data.value = WalletData(
                loyalty = source(previous.loyalty) { repo.clientLoyalty() },
                membership = source(previous.membership) { repo.myMembership() },
                packages = source(previous.packages) { repo.myPackages() },
                giftCards = source(previous.giftCards) { repo.myGiftCards() },
                referrals = source(previous.referrals) { repo.myReferrals() },
                plans = source(previous.plans) { repo.membershipPlans().orEmpty() },
                savedCards = source(previous.savedCards) { repo.savedCards().orEmpty() }
            )
            if (failures > 0 && failures < 7) _error.value = "Algunos beneficios no se pudieron actualizar. Lo anterior se conserva; vuelve a intentar."
        } finally { _loading.value = false }
    }

    private val _checkout = MutableStateFlow<MembershipCheckout?>(null)
    val checkout = _checkout.asStateFlow()

    /**
     * Contrata (o reintenta pagar) un plan: barber crea la suscripción y devuelve el client_secret
     * del primer cobro; la pantalla lo confirma con Stripe y avisa con [onCheckoutResult].
     */
    fun subscribe(planId: String, savedCardId: String?) = viewModelScope.launch {
        _busy.value = true; _error.value = null; _message.value = null
        try {
            val secret = repo.subscribeMembership(planId)?.clientSecret
            if (secret.isNullOrBlank()) {
                _error.value = "No se pudo iniciar el pago de la membresía. Intenta de nuevo."
                _busy.value = false
            } else {
                _checkout.value = MembershipCheckout(secret, savedCardId)
            }
        } catch (e: Exception) {
            _error.value = e.toFriendlyMessage("No se pudo iniciar el pago de la membresía.")
            _busy.value = false
        }
    }

    fun onCheckoutResult(ok: Boolean, canceled: Boolean, detail: String?) = viewModelScope.launch {
        _checkout.value = null
        when {
            ok -> {
                _message.value = "¡Listo! Tu membresía se está activando; en unos segundos verás tu descuento."
                // Stripe confirma el cobro por webhook: se da un momento antes de recargar.
                kotlinx.coroutines.delay(2500)
                load()
            }
            canceled -> _message.value = "Cancelaste el pago. Puedes intentarlo de nuevo cuando quieras."
            else -> _error.value = "No se pudo cobrar tu tarjeta" + (detail?.let { ": $it" } ?: ".")
        }
        _busy.value = false
    }

    fun cancelMembership() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try {
            val res = repo.cancelMembership()
            _message.value = res.message ?: "Tu membresía se cancelará al finalizar el periodo actual."
            load()
        } catch (e: Exception) {
            _error.value = e.toFriendlyMessage("No se pudo cancelar la membresía.")
        } finally { _busy.value = false }
    }
}
