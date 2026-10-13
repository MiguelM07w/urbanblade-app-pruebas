package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.MembershipInvoiceRow
import com.urbanblade.mobile.data.model.OrderRow
import com.urbanblade.mobile.data.model.PaymentRow
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InvoicesState(
    val loading: Boolean = false,
    val error: String? = null,
    val partialError: String? = null,
    val payments: List<PaymentRow> = emptyList(),
    val totalPaid: Double = 0.0,
    val orders: List<OrderRow> = emptyList(),
    /** Cobros mensuales de la membresía (Stripe). */
    val memberships: List<MembershipInvoiceRow> = emptyList(),
    val membershipPaid: Double = 0.0,
    /** Id del comprobante que se está generando (pago u orden), para el indicador del botón. */
    val openingId: String? = null,
    val receiptError: String? = null
) {
    val collected get() = payments.filter { it.collected }
    val delivered get() = orders.filter { it.estado == "entregado" }
    val toPickUp get() = orders.filter { it.estado == "pendiente" }
    val productsPaid get() = delivered.sumOf { it.total }
}

/**
 * "Mis facturas" del cliente: comprobantes de sus citas pagadas (GET payments, PDF por
 * payments/{id}/receipt) y de sus compras de productos entregadas (orders/{id}/receipt-link).
 */
class InvoicesViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(InvoicesState())
    val state: StateFlow<InvoicesState> = _state.asStateFlow()

    private var loadRequest: Job? = null

    private suspend fun <T> readSection(fetch: suspend () -> T): Result<T> = try {
        Result.success(fetch())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun load() {
        loadRequest?.cancel()
        loadRequest = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val payments = async { repo.payments() }
                // Si los pedidos fallan se siguen mostrando las citas.
                val orders = async { readSection { repo.orders().data } }
                val memberships = async { readSection { repo.membershipInvoices() } }
                val p = payments.await()
                val mResult = memberships.await()
                val m = mResult.getOrNull()
                val o = orders.await()
                ensureActive()
                _state.update {
                    it.copy(
                        payments = p.data,
                        totalPaid = p.meta?.totalPagado ?: p.data.filter { r -> r.collected }.sumOf { r -> r.monto + r.propina },
                        orders = o.getOrNull() ?: it.orders,
                        memberships = m?.data ?: it.memberships,
                        membershipPaid = m?.meta?.totalPagado ?: m?.data?.sumOf { r -> r.monto } ?: it.membershipPaid,
                        partialError = if (o.isFailure || mResult.isFailure) "No pudimos actualizar todos los comprobantes. Conservamos los últimos datos disponibles; reintenta para completar la consulta." else null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.toFriendlyMessage("No pudimos cargar tus facturas.")) }
            } finally {
                if (coroutineContext[Job]?.isActive == true) _state.update { it.copy(loading = false) }
            }
        }
    }

    fun openPaymentReceipt(id: String, onUrl: (String) -> Unit) = open(id, onUrl) { repo.paymentReceiptUrl(id) }

    fun openOrderReceipt(id: String, onUrl: (String) -> Unit) = open(id, onUrl) { repo.orderReceiptUrl(id) }

    fun openMembershipReceipt(id: String, onUrl: (String) -> Unit) = open(id, onUrl) { repo.membershipReceiptUrl(id) }

    private fun open(id: String, onUrl: (String) -> Unit, fetch: suspend () -> String?) = viewModelScope.launch {
        if (_state.value.openingId != null) return@launch
        _state.update { it.copy(openingId = id, receiptError = null) }
        try {
            val url = fetch()
            if (url == null) _state.update { it.copy(receiptError = "No se pudo generar el comprobante. Intenta de nuevo.") } else onUrl(url)
        } catch (e: Exception) {
            _state.update { it.copy(receiptError = e.toFriendlyMessage("No se pudo abrir el comprobante. Intenta de nuevo.")) }
        } finally {
            _state.update { it.copy(openingId = null) }
        }
    }
}
