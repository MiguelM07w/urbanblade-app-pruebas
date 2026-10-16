package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.StripeIntentResponseData
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.kotlin.*
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class BenefitPurchaseViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo: UrbanRepository = mock()
    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { Dispatchers.resetMain() }

    @Test fun `doble toque crea un solo intento y cancelar permite reintentar`() = runTest(dispatcher) {
        whenever(repo.buyPackage("pack")).thenReturn(StripeIntentResponseData("secret", "intent"))
        val vm = BenefitPurchaseViewModel(repo)
        vm.buyPackage("pack"); vm.buyPackage("pack")
        advanceUntilIdle()
        verify(repo, times(1)).buyPackage("pack")
        assertTrue(vm.busy.value)
        assertEquals("secret", vm.secret.value)
        vm.consumeSecret()
        vm.paymentResult(false, true)
        assertFalse(vm.busy.value)
        assertNull(vm.secret.value)
        vm.buyPackage("pack")
        advanceUntilIdle()
        verify(repo, times(2)).buyPackage("pack")
    }

    @Test fun `fallo de red desbloquea compra y no abre Stripe`() = runTest(dispatcher) {
        whenever(repo.buyGiftCard(100.0, null)).thenAnswer { throw IOException("offline") }
        val vm = BenefitPurchaseViewModel(repo)
        vm.buyGiftCard(100.0, null)
        advanceUntilIdle()
        assertFalse(vm.busy.value)
        assertNull(vm.secret.value)
        assertNotNull(vm.error.value)
    }

    @Test fun `pago enviado no declara beneficio activado`() = runTest(dispatcher) {
        val vm = BenefitPurchaseViewModel(repo)
        vm.paymentResult(true, false)
        assertTrue(vm.message.value!!.contains("cuando el servidor confirme"))
        assertFalse(vm.busy.value)
    }
}
