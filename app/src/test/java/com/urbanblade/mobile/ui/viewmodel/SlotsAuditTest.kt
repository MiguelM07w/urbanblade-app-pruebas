package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.SlotItem
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.kotlin.*
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SlotsAuditTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo: UrbanRepository = mock()
    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { Dispatchers.resetMain() }

    @Test fun `fallo de red es error de consulta y no un dia sin horarios`() = runTest(dispatcher) {
        whenever(repo.slots("b", "s", "2026-10-12")).thenAnswer { throw IOException("offline") }
        val vm = BookingViewModel(repo)
        vm.loadSlots("b", "s", "2026-10-12")
        advanceUntilIdle()
        assertNotNull(vm.slotsError.value)
        assertFalse(vm.slotsLoading.value)
        assertTrue(vm.slots.value.isEmpty())
        assertNull(vm.slotsFor.value)
    }

    @Test fun `respuesta del dia anterior no reemplaza el nuevo`() = runTest(dispatcher) {
        whenever(repo.slots("b", "s", "2026-10-12")).doSuspendableAnswer {
            delay(1000)
            listOf(SlotItem("10:00", "10:00"))
        }
        whenever(repo.slots("b", "s", "2026-10-13")).thenReturn(listOf(SlotItem("12:00", "12:00")))
        val vm = BookingViewModel(repo)
        vm.loadSlots("b", "s", "2026-10-12")
        runCurrent()
        vm.loadSlots("b", "s", "2026-10-13")
        advanceUntilIdle()
        assertEquals("12:00", vm.slots.value.single().time)
        assertNull(vm.slotsError.value)
        assertEquals("b|s|2026-10-13", vm.slotsFor.value)
    }

    @Test fun `reconsultar invalida inmediatamente el horario anterior hasta recibir respuesta`() = runTest(dispatcher) {
        whenever(repo.slots("b", "s", "2026-10-12")).thenReturn(listOf(SlotItem("10:00", "10:00")))
        val vm = BookingViewModel(repo)
        vm.loadSlots("b", "s", "2026-10-12")
        advanceUntilIdle()
        assertEquals("b|s|2026-10-12", vm.slotsFor.value)
        vm.loadSlots("b", "s", "2026-10-12")
        assertNull(vm.slotsFor.value)
        assertTrue(vm.slots.value.isEmpty())
        advanceUntilIdle()
        assertEquals("b|s|2026-10-12", vm.slotsFor.value)
    }

    @Test fun `catalogo fallido puede reintentarse sin confundirlo con una reserva fallida`() = runTest(dispatcher) {
        whenever(repo.products()).thenReturn(emptyList())
        whenever(repo.services()).thenReturn(listOf(com.urbanblade.mobile.data.model.ServiceItem("s", "Corte")))
        whenever(repo.barbers()).thenAnswer { throw IOException("offline") }.thenReturn(listOf(com.urbanblade.mobile.data.model.BarberItem("b")))
        val vm = BookingViewModel(repo)
        vm.loadCatalog()
        advanceUntilIdle()
        assertNotNull(vm.catalogError.value)
        assertFalse(vm.catalogLoading.value)
        assertTrue(vm.services.value.isEmpty())
        assertNull(vm.error.value)
        vm.loadCatalog()
        advanceUntilIdle()
        assertNull(vm.catalogError.value)
        assertEquals("s", vm.services.value.single().id)
        assertEquals("b", vm.barbers.value.single().id)
    }
}
