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
    }
}
