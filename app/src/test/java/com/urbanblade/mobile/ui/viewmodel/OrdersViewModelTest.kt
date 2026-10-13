package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.OrderMeta
import com.urbanblade.mobile.data.model.OrderMutationResponse
import com.urbanblade.mobile.data.model.OrderRow
import com.urbanblade.mobile.data.model.OrderStats
import com.urbanblade.mobile.data.model.OrdersResponse
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: UrbanRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mock()
        runBlocking {
            whenever(repo.orders(eq(1), anyOrNull(), anyOrNull())).thenReturn(
                OrdersResponse(listOf(OrderRow(id = "a", folio = "P-1")), OrderMeta(1, 2, 2, OrderStats(3, 7, 540.0)))
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `carga trae las estadisticas del servidor y la paginacion`() = runTest(dispatcher) {
        val vm = OrdersViewModel(repo)

        vm.load()
        advanceUntilIdle()

        assertEquals(3, vm.state.value.stats?.pendientes)
        assertEquals(540.0, vm.state.value.stats?.porCobrar ?: 0.0, 0.0)
        assertTrue(vm.state.value.hasMore)
    }

    @Test
    fun `cargar mas agrega la segunda pagina`() = runTest(dispatcher) {
        whenever(repo.orders(eq(2), anyOrNull(), anyOrNull())).thenReturn(
            OrdersResponse(listOf(OrderRow(id = "b", folio = "P-2")), OrderMeta(2, 2, 2, null))
        )
        val vm = OrdersViewModel(repo)

        vm.load()
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()

        assertEquals(listOf("a", "b"), vm.state.value.items.map { it.id })
        assertFalse(vm.state.value.hasMore)
    }

    @Test
    fun `el filtro de entregados se manda al servidor`() = runTest(dispatcher) {
        val vm = OrdersViewModel(repo)

        vm.setFilter(OrderFilter.Entregados)
        advanceUntilIdle()

        verify(repo).orders(1, "entregado", null)
    }

    @Test
    fun `entregar manda el metodo y avisa`() = runTest(dispatcher) {
        whenever(repo.deliverOrder(any(), any())).thenReturn(OrderMutationResponse("ok"))
        val vm = OrdersViewModel(repo)

        vm.deliver("a", "tarjeta")
        advanceUntilIdle()

        verify(repo).deliverOrder("a", "tarjeta")
        assertEquals("Pedido entregado y cobrado.", vm.state.value.notice)
        assertNull(vm.state.value.busyOrderId)
    }

    @Test
    fun `si el servidor rechaza la entrega muestra su mensaje`() = runTest(dispatcher) {
        val body = "{\"message\":\"Stock insuficiente.\"}".toResponseBody("application/json".toMediaType())
        whenever(repo.deliverOrder(any(), any())).thenThrow(HttpException(Response.error<Any>(422, body)))
        val vm = OrdersViewModel(repo)

        vm.deliver("a", "efectivo")
        advanceUntilIdle()

        assertEquals("Stock insuficiente.", vm.state.value.error)
        assertNull(vm.state.value.notice)
    }

    @Test
    fun `cancelar avisa que el stock volvio`() = runTest(dispatcher) {
        whenever(repo.cancelOrder(any())).thenReturn(OrderMutationResponse("ok"))
        val vm = OrdersViewModel(repo)

        vm.cancel("a")
        advanceUntilIdle()

        assertEquals("Pedido cancelado. El stock volvió al inventario.", vm.state.value.notice)
    }
    @Test
    fun `reintentar paginacion conserva filas y limpia el error`() = runTest(dispatcher) {
        whenever(repo.orders(eq(2), anyOrNull(), anyOrNull())).thenThrow(RuntimeException("sin red"))
        val vm = OrdersViewModel(repo)
        vm.load()
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()
        assertEquals(listOf("a"), vm.state.value.items.map { it.id })
        assertTrue(vm.state.value.error != null)
        doReturn(OrdersResponse(listOf(OrderRow(id = "b")), OrderMeta(2, 2, 2, null)))
            .whenever(repo).orders(eq(2), anyOrNull(), anyOrNull())
        vm.loadMore()
        advanceUntilIdle()
        assertEquals(listOf("a", "b"), vm.state.value.items.map { it.id })
        assertNull(vm.state.value.error)
    }

    @Test
    fun `recarga bloquea paginacion antes de iniciar su coroutine`() = runTest(dispatcher) {
        val vm = OrdersViewModel(repo)
        vm.load()
        advanceUntilIdle()
        // Una recarga debe bloquear paginación repetida antes de iniciar su coroutine.
        vm.load()
        vm.loadMore()
        advanceUntilIdle()
        assertEquals(listOf("a"), vm.state.value.items.map { it.id })
        assertFalse(vm.state.value.loadingMore)
    }

}
