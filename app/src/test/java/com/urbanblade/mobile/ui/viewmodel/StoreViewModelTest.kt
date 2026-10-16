package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.OrderRequest
import com.urbanblade.mobile.data.model.ProductItem
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class StoreViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: UrbanRepository
    @Before fun setup() { Dispatchers.setMain(dispatcher); repo = mock() }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `buscar otro producto conserva la estimacion del carrito`() = runTest(dispatcher) {
        whenever(repo.products(null)).thenReturn(listOf(ProductItem("a", "Aceite", precioVenta = 200.0, stockActual = 3)))
        whenever(repo.products("cera")).thenReturn(listOf(ProductItem("b", "Cera", precioVenta = 150.0, stockActual = 2)))
        val vm = StoreViewModel(repo)
        vm.load(); advanceUntilIdle(); vm.add("a")
        vm.load("cera"); advanceUntilIdle()
        assertEquals(listOf("b"), vm.products.value.map { it.id })
        assertEquals(mapOf("a" to 1), vm.cart.value)
        assertEquals(200.0, vm.estimatedTotal, 0.0)
        vm.add("b")
        assertEquals(350.0, vm.estimatedTotal, 0.0)
    }

    @Test fun `el carrito no permite superar las existencias conocidas`() = runTest(dispatcher) {
        whenever(repo.products(null)).thenReturn(listOf(ProductItem("a", "Aceite", precioVenta = 200.0, stockActual = 1)))
        val vm = StoreViewModel(repo)
        vm.load(); advanceUntilIdle()
        vm.add("a"); vm.add("a"); vm.add("desconocido")
        assertEquals(mapOf("a" to 1), vm.cart.value)
    }

    @Test fun `enviar pedido congela carrito y conserva seleccion si falla`() = runTest(dispatcher) {
        whenever(repo.products(null)).thenReturn(listOf(ProductItem("a", "Aceite", precioVenta = 200.0, stockActual = 3)))
        whenever(repo.createOrder(any<OrderRequest>())).thenThrow(RuntimeException("sin red"))
        val vm = StoreViewModel(repo)
        vm.load(); advanceUntilIdle(); vm.add("a")
        vm.checkout {}
        vm.add("a"); vm.remove("a")
        assertEquals(mapOf("a" to 1), vm.cart.value)
        advanceUntilIdle()
        assertEquals(mapOf("a" to 1), vm.cart.value)
        assertNotNull(vm.error.value)
        assertFalse(vm.busy.value)
    }
}
