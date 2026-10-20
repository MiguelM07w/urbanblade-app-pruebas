package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import com.urbanblade.mobile.data.repository.UrbanRepository

class StoreViewModel @JvmOverloads constructor(private val repo: UrbanRepository = AppContainer.urbanRepository) : ViewModel() {
    private val _products = MutableStateFlow<List<ProductItem>>(emptyList())
    val products = _products.asStateFlow()
    private val _knownProducts = MutableStateFlow<Map<String, ProductItem>>(emptyMap())
    val knownProducts = _knownProducts.asStateFlow()
    private val _cart = MutableStateFlow<Map<String, Int>>(emptyMap())
    val cart = _cart.asStateFlow()
    val estimatedTotal: Double get() = _cart.value.entries.sumOf { (id, quantity) -> (_knownProducts.value[id]?.precioVenta ?: 0.0) * quantity }
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    private var loadRequest: Job? = null
    private var checkoutRequest: Job? = null

    fun load(q: String? = null) {
        if (checkoutRequest?.isActive == true) return
        loadRequest?.cancel()
        _busy.value = true; _error.value = null
        loadRequest = viewModelScope.launch {
            try {
                val loaded = repo.products(q)
                ensureActive()
                _products.value = loaded
                _knownProducts.value = _knownProducts.value + loaded.associateBy { it.id }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar la tienda.") }
            finally { if (coroutineContext[Job]?.isActive == true) _busy.value = false }
        }
    }
    fun add(id: String) {
        if (_busy.value) return
        val product = _knownProducts.value[id] ?: return
        val quantity = _cart.value[id] ?: 0
        if (quantity >= product.stockActual) return
        _cart.value = _cart.value + (id to quantity + 1)
    }
    fun remove(id: String) {
        if (_busy.value) return
        val quantity = (_cart.value[id] ?: 0) - 1
        _cart.value = if (quantity <= 0) _cart.value - id else _cart.value + (id to quantity)
    }
    fun clear() { _cart.value = emptyMap() }
    fun checkout(onDone: () -> Unit) {
        if (_busy.value || checkoutRequest?.isActive == true) return
        if (_cart.value.isEmpty()) { _error.value = "Tu carrito está vacío."; return }
        val body = OrderRequest(_cart.value.map { OrderItemRequest(it.key, it.value) })
        _busy.value = true; _error.value = null
        checkoutRequest = viewModelScope.launch {
            try {
                val response = repo.createOrder(body)
                _message.value = response.message ?: "Pedido creado."
                clear(); onDone()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo crear el pedido.") }
            finally { _busy.value = false }
        }
    }
}

class NotificationsViewModel @JvmOverloads constructor(private val repo: UrbanRepository = AppContainer.urbanRepository) : ViewModel() {
    private val _data = MutableStateFlow<JsonObject?>(null); val data = _data.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    private var loadRequest: Job? = null
    private var readRequest: Job? = null

    fun load() {
        if (readRequest?.isActive == true) return
        loadRequest?.cancel()
        _busy.value = true; _error.value = null
        loadRequest = viewModelScope.launch {
            try {
                val response = repo.notifications()
                ensureActive()
                _data.value = response
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar las notificaciones.") }
            finally { if (coroutineContext[Job]?.isActive == true) _busy.value = false }
        }
    }

    fun readAll() = markRead(null)
    fun read(id: String) = markRead(id)

    private fun markRead(id: String?) {
        if (_busy.value) return
        _busy.value = true; _error.value = null
        readRequest = viewModelScope.launch {
            try {
                if (id == null) repo.markAllNotificationsRead() else repo.markNotificationRead(id)
                val response = repo.notifications()
                ensureActive()
                _data.value = response
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo actualizar el estado de tus avisos. Reintenta la consulta.") }
            finally { _busy.value = false }
        }
    }
}

class BarberScheduleViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _schedules = MutableStateFlow<List<BarberScheduleDay>>(emptyList()); val schedules = _schedules.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _saving = MutableStateFlow(false); val saving = _saving.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _schedules.value = repo.barberSchedule() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar tu horario.") }
        finally { _busy.value = false }
    }

    fun save(schedules: List<BarberScheduleDay>) = viewModelScope.launch {
        _saving.value = true; _error.value = null; _message.value = null
        try {
            _schedules.value = repo.updateBarberSchedule(schedules)
            _message.value = "Horario actualizado."
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo guardar tu horario.") }
        finally { _saving.value = false }
    }
}

class CashCloseViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _preview = MutableStateFlow(CashClosePreview()); val preview = _preview.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _closing = MutableStateFlow(false); val closing = _closing.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _preview.value = repo.cashClosePreview() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar el corte de caja.") }
        finally { _busy.value = false }
    }

    fun close(efectivoContado: Double, notas: String?) = viewModelScope.launch {
        _closing.value = true; _error.value = null; _message.value = null
        try {
            repo.registerCashClose(efectivoContado, notas)
            _message.value = "Caja cerrada correctamente."
            load()
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cerrar la caja.") }
        finally { _closing.value = false }
    }
}

class AdminMetricsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _metrics = MutableStateFlow(AdminMetrics()); val metrics = _metrics.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    fun load() = viewModelScope.launch { _busy.value = true; _error.value = null; try { _metrics.value = repo.adminMetrics() } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar las métricas.") } finally { _busy.value = false } }
}

class SystemStatusViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _status = MutableStateFlow<SystemStatusResponse?>(null); val status = _status.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    fun load() = viewModelScope.launch { _busy.value = true; _error.value = null; try { _status.value = repo.systemStatus() } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar el estado del sistema.") } finally { _busy.value = false } }
}

class RafflesViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _raffles = MutableStateFlow(RaffleResponse()); val raffles = _raffles.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    fun load() = viewModelScope.launch { _busy.value = true; _error.value = null; try { _raffles.value = repo.raffles() } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar los sorteos.") } finally { _busy.value = false } }
}

class ClientsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _clients = MutableStateFlow(ClientsResponse()); val clients = _clients.asStateFlow()
    private val _detail = MutableStateFlow<ClientDetail?>(null); val detail = _detail.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _saving = MutableStateFlow(false); val saving = _saving.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    private val _hasMore = MutableStateFlow(false); val hasMore = _hasMore.asStateFlow()
    private val _loadingMore = MutableStateFlow(false); val loadingMore = _loadingMore.asStateFlow()
    private var page = 1
    private var lastSearch: String? = null
    private var lastSegment: String? = null

    fun load(search: String? = null, segment: String? = null) = viewModelScope.launch {
        _busy.value = true; _error.value = null
        lastSearch = search; lastSegment = segment
        try {
            val response = repo.clients(search, segment, 1)
            page = 1
            _clients.value = response
            _hasMore.value = (response.currentPage ?: 1) < (response.lastPage ?: 1)
        }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar los clientes.") }
        finally { _busy.value = false }
    }

    /** El servidor entrega 15 clientes por página: esto trae la siguiente y la agrega al final. */
    fun loadMore() = viewModelScope.launch {
        if (_loadingMore.value || !_hasMore.value) return@launch
        _loadingMore.value = true
        try {
            val response = repo.clients(lastSearch, lastSegment, page + 1)
            page += 1
            val known = _clients.value.data.map { it.id }.toSet()
            _clients.value = _clients.value.copy(data = _clients.value.data + response.data.filter { it.id !in known })
            _hasMore.value = (response.currentPage ?: page) < (response.lastPage ?: page)
        }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar más clientes.") }
        finally { _loadingMore.value = false }
    }

    fun loadDetail(id: String) = viewModelScope.launch {
        _busy.value = true; _error.value = null; _detail.value = null
        try { _detail.value = repo.clientDetail(id) }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar el cliente.") }
        finally { _busy.value = false }
    }

    fun create(name: String, email: String, telefono: String?, password: String, onDone: () -> Unit) = viewModelScope.launch {
        _saving.value = true; _error.value = null
        try {
            repo.createClient(name, email, telefono, password)
            _message.value = "Cliente creado."
            onDone()
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo crear el cliente.") }
        finally { _saving.value = false }
    }

    fun update(id: String, name: String?, email: String?, telefono: String?, notas: String?) = viewModelScope.launch {
        _saving.value = true; _error.value = null
        try {
            repo.updateClient(id, name, email, telefono, notas)
            _message.value = "Cliente actualizado."
            loadDetail(id)
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo actualizar el cliente.") }
        finally { _saving.value = false }
    }

    fun delete(id: String, onDone: () -> Unit) = viewModelScope.launch {
        _saving.value = true; _error.value = null
        try {
            repo.deleteClient(id)
            onDone()
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo eliminar el cliente.") }
        finally { _saving.value = false }
    }
}

class InventoryViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _products = MutableStateFlow(InventoryResponse()); val products = _products.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _saving = MutableStateFlow(false); val saving = _saving.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _products.value = repo.inventoryProducts() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar el inventario.") }
        finally { _busy.value = false }
    }

    fun createProduct(context: android.content.Context, body: CreateProductRequest, imageUri: android.net.Uri?, onDone: () -> Unit) = viewModelScope.launch {
        _saving.value = true; _error.value = null
        try { repo.createProduct(context, body, imageUri); _message.value = "Producto creado."; onDone(); load() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo crear el producto.") }
        finally { _saving.value = false }
    }

    fun updateProduct(context: android.content.Context, id: String, body: CreateProductRequest, imageUri: android.net.Uri?, onDone: () -> Unit) = viewModelScope.launch {
        _saving.value = true; _error.value = null
        try { repo.updateProduct(context, id, body, imageUri); _message.value = "Producto actualizado."; onDone(); load() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo actualizar el producto.") }
        finally { _saving.value = false }
    }

    fun deleteProduct(id: String) = viewModelScope.launch {
        _saving.value = true; _error.value = null
        try { repo.deleteProduct(id); load() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo eliminar el producto.") }
        finally { _saving.value = false }
    }

    fun registerMovement(productId: String, tipo: String, cantidad: Int, motivo: String?, onDone: () -> Unit) = viewModelScope.launch {
        _saving.value = true; _error.value = null
        try { repo.registerMovement(productId, tipo, cantidad, motivo); _message.value = "Movimiento registrado."; onDone(); load() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo registrar el movimiento.") }
        finally { _saving.value = false }
    }
}

class PortfolioViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _portfolio = MutableStateFlow(PortfolioResponse()); val portfolio = _portfolio.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _uploading = MutableStateFlow(false); val uploading = _uploading.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _portfolio.value = repo.portfolio() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar tu portafolio.") }
        finally { _busy.value = false }
    }

    fun upload(context: android.content.Context, title: String, description: String?, media: List<android.net.Uri>, onDone: () -> Unit) = viewModelScope.launch {
        _uploading.value = true; _error.value = null
        try {
            val res = repo.createWork(context, title, description, media)
            _message.value = res.message
            onDone()
            load()
        } catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo subir el trabajo.") }
        finally { _uploading.value = false }
    }

    fun delete(id: String) = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { repo.deleteWork(id); load() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo eliminar el trabajo.") }
        finally { _busy.value = false }
    }
}


class BarberAgendaViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _agenda = MutableStateFlow(BarberAgendaResponse()); val agenda = _agenda.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _updating = MutableStateFlow<String?>(null); val updating = _updating.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    fun load(period: String = "day", estado: String? = null, offset: Int = 0) = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _agenda.value = repo.barberAgenda(period, estado, offset) }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar tu agenda.") }
        finally { _busy.value = false }
    }

    private val _ticket = MutableStateFlow<ServiceTicket?>(null); val ticket = _ticket.asStateFlow()
    private val _notice = MutableStateFlow<String?>(null); val notice = _notice.asStateFlow()
    /** Agregar tiempo choca con la siguiente cita: espera la confirmación del barbero (código, minutos, motivo). */
    private val _forceExtend = MutableStateFlow<ForceExtend?>(null); val forceExtend = _forceExtend.asStateFlow()

    data class ForceExtend(val code: String, val minutos: Int, val message: String)

    fun dismissTicket() { _ticket.value = null }
    fun dismissNotice() { _notice.value = null }
    fun dismissForceExtend() { _forceExtend.value = null }

    fun updateStatus(code: String, estado: String, period: String, filtroEstado: String?, offset: Int) = viewModelScope.launch {
        _updating.value = code; _error.value = null
        try {
            val res = repo.updateAppointmentStatus(code, estado)
            // Al terminar el servicio la API trae el ticket para mostrarlo de inmediato.
            if (estado == "completada") _ticket.value = res.ticket
            load(period, filtroEstado, offset)
        }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo actualizar la cita.") }
        finally { _updating.value = null }
    }

    /** Agrega [minutos] al servicio en curso. Si choca con la siguiente cita pide confirmar y repite con `forzar`. */
    fun extend(code: String, minutos: Int, period: String, filtroEstado: String?, offset: Int, forzar: Boolean = false) = viewModelScope.launch {
        _updating.value = code; _error.value = null
        try {
            repo.extendAppointment(code, minutos, forzar)
            _notice.value = "Se agregaron $minutos minutos. Avisamos al cliente."
            load(period, filtroEstado, offset)
        } catch (e: Exception) {
            val body = (e as? retrofit2.HttpException)?.takeIf { it.code() == 422 }?.response()?.errorBody()?.string()
            val serverMessage = body?.let { b -> runCatching { com.google.gson.JsonParser.parseString(b).asJsonObject.get("message").asString }.getOrNull() }
            if (!forzar && com.urbanblade.mobile.core.appointments.canForceFromBody(body)) {
                _forceExtend.value = ForceExtend(code, minutos, serverMessage ?: "Agregar ese tiempo choca con la siguiente cita.")
            } else {
                _error.value = serverMessage ?: e.toFriendlyMessage("No se pudo agregar tiempo.")
            }
        }
        finally { _updating.value = null }
    }

    /** Abre el ticket de un servicio ya terminado (GET /appointments/{code}/ticket). */
    fun openTicket(code: String) = viewModelScope.launch {
        _updating.value = code; _error.value = null
        try { _ticket.value = repo.appointmentTicket(code) }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("Esta cita todavía no tiene ticket.") }
        finally { _updating.value = null }
    }
}

class ReportsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _manifest = MutableStateFlow(ReportManifest()); val manifest = _manifest.asStateFlow()
    private val _report = MutableStateFlow<ReportData?>(null); val report = _report.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()

    fun loadManifest() = viewModelScope.launch {
        try { _manifest.value = repo.reportManifest() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar los tipos de reporte.") }
    }

    fun generate(type: String, startDate: String?, endDate: String?) = viewModelScope.launch {
        _busy.value = true; _error.value = null; _report.value = null
        try { _report.value = repo.exportReport(type, "json", startDate, endDate) }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo generar el reporte.") }
        finally { _busy.value = false }
    }
}


class SocialFeedViewModel @JvmOverloads constructor(private val repo: UrbanRepository = AppContainer.urbanRepository) : ViewModel() {
    private val _feed = MutableStateFlow(SocialFeedResponse()); val feed = _feed.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    private val _pending = MutableStateFlow<Set<String>>(emptySet()); val pending = _pending.asStateFlow()
    private val _bookingIds = MutableStateFlow<Map<String, String>>(emptyMap()); val bookingIds = _bookingIds.asStateFlow()
    private val _hasMore = MutableStateFlow(false); val hasMore = _hasMore.asStateFlow()
    private val _loadingMore = MutableStateFlow(false); val loadingMore = _loadingMore.asStateFlow()
    private var page = 1
    private fun lastPage(response: SocialFeedResponse) = runCatching { response.meta?.get("last_page")?.asInt }.getOrNull() ?: 1

    fun load(canBook: Boolean = false) {
        if (_busy.value || _loadingMore.value || _pending.value.isNotEmpty()) return
        _busy.value = true; _error.value = null
        if (!canBook) _bookingIds.value = emptyMap()
        viewModelScope.launch {
            try {
                val response = repo.socialFeed(1)
                ensureActive()
                _feed.value = response; page = 1; _hasMore.value = page < lastPage(response)
                if (canBook) {
                    val barbers = try { repo.barbers() }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { _bookingIds.value = emptyMap(); throw e }
                    ensureActive()
                    _bookingIds.value = barbers.mapNotNull { barber -> barber.user?.id?.let { it to barber.id } }.toMap()
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo actualizar el muro o preparar la reserva. Reintenta la consulta.") }
            finally { _busy.value = false }
        }
    }

    fun loadMore() {
        if (_busy.value || _loadingMore.value || !_hasMore.value || _pending.value.isNotEmpty()) return
        _loadingMore.value = true; _error.value = null
        viewModelScope.launch {
            try {
                val response = repo.socialFeed(page + 1)
                ensureActive()
                _feed.value = response.copy(data = (_feed.value.data + response.data).distinctBy { it.id })
                page += 1; _hasMore.value = page < lastPage(response)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar más publicaciones.") }
            finally { _loadingMore.value = false }
        }
    }

    private fun action(id: String, fallback: String, block: suspend () -> Unit) {
        if (_busy.value || id in _pending.value) return
        _pending.value += id; _error.value = null
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.toFriendlyMessage(fallback) }
            finally { _pending.value -= id }
        }
    }

    private fun updateWork(id: String, transform: (SocialWork) -> SocialWork) {
        _feed.value = _feed.value.copy(data = _feed.value.data.map { if (it.id == id) transform(it) else it })
    }

    fun toggleReaction(id: String) = action(id, "No se pudo reaccionar.") {
            val res = repo.reactWork(id)
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            updateWork(id) { it.copy(isReacted = res.status == "added", reactionsCount = res.count ?: it.reactionsCount) }
    }

    fun toggleSave(id: String) = action(id, "No se pudo guardar.") {
            val res = repo.saveWork(id)
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            val added = res.status == "added"
            updateWork(id) { it.copy(isSaved = added, savedCount = (it.savedCount + if (added) 1 else -1).coerceAtLeast(0)) }
    }

    private fun hideComment(workId: String, commentId: String, authorName: String? = null) = updateWork(workId) { work ->
        val kept = work.comments.filterNot { c -> c.id == commentId || (authorName != null && c.user?.name == authorName && !c.isMine) }
        work.copy(comments = kept, commentsCount = (work.commentsCount - (work.comments.size - kept.size)).coerceAtLeast(0))
    }

    /** Reporta un comentario ajeno (política de contenido de usuarios de Google Play) y lo oculta de inmediato. */
    fun reportComment(workId: String, commentId: String, reason: String, details: String? = null, onDone: () -> Unit = {}) =
        action(workId, "No se pudo enviar el reporte.") {
            repo.reportComment(commentId, reason, details)
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            hideComment(workId, commentId)
            onDone()
        }

    /** Bloquea al autor del comentario: sus comentarios dejan de mostrarse a esta cuenta. */
    fun blockCommentAuthor(workId: String, commentId: String, onDone: () -> Unit = {}) =
        action(workId, "No se pudo bloquear al usuario.") {
            val authorName = _feed.value.data.firstOrNull { it.id == workId }?.comments?.firstOrNull { it.id == commentId }?.user?.name
            repo.blockCommentAuthor(commentId)
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            hideComment(workId, commentId, authorName)
            onDone()
        }

    fun comment(id: String, text: String, onDone: () -> Unit) {
        val comment = text.trim()
        if (comment.isEmpty() || comment.length > 500) return
        action(id, "No se pudo publicar el comentario.") {
            val res = repo.commentWork(id, comment)
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            val newComment = res.data
            updateWork(id) {
                it.copy(
                    commentsCount = it.commentsCount + 1,
                    comments = if (newComment != null) (listOf(newComment) + it.comments).take(3) else it.comments
                )
            }
            onDone()
        }
    }
}

class InsightsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _data = MutableStateFlow(InsightsData()); val data = _data.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    fun load() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _data.value = repo.predictionInsights() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron generar los insights.") }
        finally { _busy.value = false }
    }
}

class AnalyticsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _data = MutableStateFlow(AnalyticsResponse()); val data = _data.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    fun load() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _data.value = repo.analytics() }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudo cargar la analítica.") }
        finally { _busy.value = false }
    }
}

class ReviewsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _reviews = MutableStateFlow(ReviewsResponse()); val reviews = _reviews.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    fun load(barberId: String? = null, rating: Int? = null) = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _reviews.value = repo.reviews(barberId, rating) }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar las reseñas.") }
        finally { _busy.value = false }
    }
}

class LogsViewModel : ViewModel() {
    private val repo = AppContainer.urbanRepository
    private val _logs = MutableStateFlow(LogsResponse()); val logs = _logs.asStateFlow()
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    fun load(search: String? = null, logName: String? = null, event: String? = null) = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try { _logs.value = repo.logs(search, logName, event) }
        catch (e: Exception) { _error.value = e.toFriendlyMessage("No se pudieron cargar los logs.") }
        finally { _busy.value = false }
    }
}


