package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.MembershipInvoiceRow
import com.urbanblade.mobile.data.model.MembershipInvoicesMeta
import com.urbanblade.mobile.data.model.MembershipInvoicesResponse
import com.urbanblade.mobile.data.model.OrderRow
import com.urbanblade.mobile.data.model.OrdersResponse
import com.urbanblade.mobile.data.model.PaymentRow
import com.urbanblade.mobile.data.model.PaymentsMeta
import com.urbanblade.mobile.data.model.PaymentsResponse
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class InvoicesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: UrbanRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mock()
        runBlocking {
            whenever(repo.payments()).thenReturn(
                PaymentsResponse(listOf(PaymentRow(id = "p1", monto = 250.0, propina = 25.0)), PaymentsMeta(totalPagado = 275.0))
            )
            whenever(repo.orders(any(), anyOrNull(), anyOrNull())).thenReturn(
                OrdersResponse(
                    listOf(
                        OrderRow(id = "o1", estado = "entregado", tipo = "cita", total = 180.0),
                        OrderRow(id = "o2", estado = "pendiente", tipo = "cita", total = 90.0),
                        OrderRow(id = "o3", estado = "cancelado", tipo = "tienda", total = 60.0)
                    )
                )
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `junta los pagos de citas y solo cuenta como pagados los productos entregados`() = runTest(dispatcher) {
        val vm = InvoicesViewModel(repo)

        vm.load()
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(275.0, state.totalPaid, 0.0)
        assertEquals(listOf("o1"), state.delivered.map { it.id })
        assertEquals(listOf("o2"), state.toPickUp.map { it.id })
        assertEquals(180.0, state.productsPaid, 0.0)
    }

    @Test
    fun `si los pedidos fallan se siguen mostrando las citas`() = runTest(dispatcher) {
        whenever(repo.orders(any(), anyOrNull(), anyOrNull())).thenThrow(RuntimeException("sin red"))
        val vm = InvoicesViewModel(repo)

        vm.load()
        advanceUntilIdle()

        assertNull(vm.state.value.error)
        assertEquals(1, vm.state.value.payments.size)
        assertEquals(0, vm.state.value.orders.size)
    }

    @Test
    fun `abre la liga del comprobante del pedido`() = runTest(dispatcher) {
        whenever(repo.orderReceiptUrl("o1")).thenReturn("https://example.test/pedido.pdf")
        val vm = InvoicesViewModel(repo)
        var opened: String? = null

        vm.openOrderReceipt("o1") { opened = it }
        advanceUntilIdle()

        assertEquals("https://example.test/pedido.pdf", opened)
        assertNull(vm.state.value.openingId)
        assertNull(vm.state.value.receiptError)
    }

    @Test
    fun `sin liga muestra un aviso en lugar de abrir nada`() = runTest(dispatcher) {
        whenever(repo.paymentReceiptUrl("p1")).thenReturn(null)
        val vm = InvoicesViewModel(repo)
        var opened: String? = null

        vm.openPaymentReceipt("p1") { opened = it }
        advanceUntilIdle()

        assertNull(opened)
        assertNotNull(vm.state.value.receiptError)
    }

    @Test
    fun `un pago reembolsado o por verificar no cuenta como cobrado`() = runTest(dispatcher) {
        whenever(repo.payments()).thenReturn(
            PaymentsResponse(
                listOf(
                    PaymentRow(id = "p1", monto = 200.0, estado = "verificado"),
                    PaymentRow(id = "p2", monto = 300.0, estado = "reembolsado", esDeposito = true),
                    PaymentRow(id = "p3", monto = 150.0, estado = "pendiente_verificacion"),
                    PaymentRow(id = "p4", monto = 100.0)
                )
            )
        )
        val vm = InvoicesViewModel(repo)

        vm.load()
        advanceUntilIdle()

        assertEquals(listOf("p1", "p4"), vm.state.value.collected.map { it.id })
        assertEquals(300.0, vm.state.value.totalPaid, 0.0)
    }

    @Test
    fun `los cobros de membresia se suman aparte y fallar no rompe la pantalla`() = runTest(dispatcher) {
        whenever(repo.membershipInvoices()).thenReturn(
            MembershipInvoicesResponse(listOf(MembershipInvoiceRow(id = "m1", plan = "Oro", monto = 299.0)), MembershipInvoicesMeta(299.0))
        )
        val vm = InvoicesViewModel(repo)

        vm.load()
        advanceUntilIdle()

        assertEquals(listOf("m1"), vm.state.value.memberships.map { it.id })
        assertEquals(299.0, vm.state.value.membershipPaid, 0.0)
        assertEquals(275.0, vm.state.value.totalPaid, 0.0)
    }
    @Test
    fun `consulta parcial conserva compras y membresia hasta un reintento correcto`() = runTest(dispatcher) {
        whenever(repo.membershipInvoices()).thenReturn(
            MembershipInvoicesResponse(listOf(MembershipInvoiceRow(id = "m1", monto = 299.0)), MembershipInvoicesMeta(299.0))
        )
        val vm = InvoicesViewModel(repo)
        vm.load()
        advanceUntilIdle()
        whenever(repo.orders(any(), anyOrNull(), anyOrNull())).thenThrow(RuntimeException("sin red"))
        whenever(repo.membershipInvoices()).thenThrow(RuntimeException("sin red"))
        vm.load()
        advanceUntilIdle()
        assertEquals(180.0, vm.state.value.productsPaid, 0.0)
        assertEquals(299.0, vm.state.value.membershipPaid, 0.0)
        assertNotNull(vm.state.value.partialError)
        doReturn(OrdersResponse()).whenever(repo).orders(any(), anyOrNull(), anyOrNull())
        doReturn(MembershipInvoicesResponse()).whenever(repo).membershipInvoices()
        vm.load()
        advanceUntilIdle()
        assertEquals(0.0, vm.state.value.productsPaid, 0.0)
        assertEquals(0.0, vm.state.value.membershipPaid, 0.0)
        assertNull(vm.state.value.partialError)
    }

}
