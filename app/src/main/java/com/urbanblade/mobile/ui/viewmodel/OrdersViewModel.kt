package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.OrderRow
import com.urbanblade.mobile.data.model.OrderStats
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Estado del pedido tal como lo guarda el servidor. */
enum class OrderFilter(val label: String, val param: String?) {
    Todos("Todos", null),
    Pendientes("Pendientes", "pendiente"),
    Entregados("Entregados", "entregado"),
    Cancelados("Cancelados", "cancelado")
}

/** Formas de cobro al entregar un pedido (mismas tres que el resto de la app). */
val ORDER_PAYMENT_METHODS = listOf("efectivo", "tarjeta", "transferencia")

data class OrdersState(
    val items: List<OrderRow> = emptyList(),
    val stats: OrderStats? = null,
    val total: Int = 0,
    val page: Int = 1,
    val lastPage: Int = 1,
    val query: String = "",
    val filter: OrderFilter = OrderFilter.Todos,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    /** Pedido sobre el que se está entregando o cancelando, para bloquear sus botones. */
    val busyOrderId: String? = null,
    val error: String? = null,
    val notice: String? = null
) {
    val hasMore: Boolean get() = page < lastPage
}

/** Pedidos de la tienda: el personal los gestiona todos; el cliente ve los suyos. */
class OrdersViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(OrdersState())
    val state: StateFlow<OrdersState> = _state.asStateFlow()

    private var listRequest: Job? = null
    private var pageRequest: Job? = null

    fun load() {
        pageRequest?.cancel()
        listRequest?.cancel()
        listRequest = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, loadingMore = false, error = null)
            try {
                val s = _state.value
                val response = repo.orders(1, s.filter.param, s.query.trim().ifEmpty { null })
                ensureActive()
                val meta = response.meta
                _state.value = _state.value.copy(
                    items = response.data,
                    stats = meta?.stats,
                    total = meta?.total ?: response.data.size,
                    page = meta?.currentPage ?: 1,
                    lastPage = meta?.lastPage ?: 1,
                    loading = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.toFriendlyMessage("No se pudieron cargar los pedidos."))
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || s.loading || listRequest?.isActive == true || pageRequest?.isActive == true || !s.hasMore) return
        pageRequest = viewModelScope.launch {
            _state.value = _state.value.copy(loadingMore = true, error = null)
            try {
                val response = repo.orders(s.page + 1, s.filter.param, s.query.trim().ifEmpty { null })
                ensureActive()
                val known = _state.value.items.map { it.id }.toSet()
                _state.value = _state.value.copy(
                    items = _state.value.items + response.data.filter { it.id !in known },
                    page = response.meta?.currentPage ?: (s.page + 1),
                    lastPage = response.meta?.lastPage ?: s.lastPage,
                    loadingMore = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(loadingMore = false, error = e.toFriendlyMessage("No se pudieron cargar más pedidos."))
            }
        }
    }

    fun setQuery(value: String) {
        _state.value = _state.value.copy(query = value)
    }

    fun setFilter(filter: OrderFilter) {
        if (filter == _state.value.filter) return
        _state.value = _state.value.copy(filter = filter)
        load()
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, notice = null)
    }

    /** Cancela el pedido; el servidor devuelve el stock al inventario. */
    fun cancel(id: String) = act(id, "No se pudo cancelar el pedido.", "Pedido cancelado. El stock volvió al inventario.") {
        repo.cancelOrder(id)
    }

    /** Entrega el pedido y registra el cobro con el método indicado. */
    fun deliver(id: String, method: String) = act(id, "No se pudo entregar el pedido.", "Pedido entregado y cobrado.") {
        repo.deliverOrder(id, method)
    }

    private fun act(id: String, failure: String, success: String, call: suspend () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busyOrderId = id, error = null, notice = null)
            try {
                call()
                _state.value = _state.value.copy(busyOrderId = null, notice = success)
                load()
            } catch (e: HttpException) {
                _state.value = _state.value.copy(
                    busyOrderId = null,
                    error = if (e.code() == 422) e.serverMessage() ?: failure else e.toFriendlyMessage(failure)
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(busyOrderId = null, error = e.toFriendlyMessage(failure))
            }
        }
    }
}
