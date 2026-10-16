package com.urbanblade.mobile.ui.viewmodel

import com.google.gson.JsonObject
import com.urbanblade.mobile.data.model.MessageResponse
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: UrbanRepository
    @Before fun setup() { Dispatchers.setMain(dispatcher); repo = mock() }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `reintentar limpia error y conserva avisos ante fallo`() = runTest(dispatcher) {
        val previous = JsonObject().apply { addProperty("unread_count", 2) }
        val refreshed = JsonObject().apply { addProperty("unread_count", 0) }
        whenever(repo.notifications()).thenReturn(previous).thenThrow(RuntimeException("sin red")).thenReturn(refreshed)
        val vm = NotificationsViewModel(repo)
        vm.load(); advanceUntilIdle()
        vm.load(); advanceUntilIdle()
        assertSame(previous, vm.data.value)
        assertNotNull(vm.error.value)
        vm.load()
        assertNull(vm.error.value)
        assertTrue(vm.busy.value)
        advanceUntilIdle()
        assertSame(refreshed, vm.data.value)
        assertFalse(vm.busy.value)
    }

    @Test fun `marcar todos evita envio duplicado y actualiza lista`() = runTest(dispatcher) {
        val refreshed = JsonObject().apply { addProperty("unread_count", 0) }
        whenever(repo.markAllNotificationsRead()).thenReturn(MessageResponse(message = "OK"))
        whenever(repo.notifications()).thenReturn(refreshed)
        val vm = NotificationsViewModel(repo)
        vm.readAll(); vm.readAll(); vm.load()
        advanceUntilIdle()
        verify(repo, times(1)).markAllNotificationsRead()
        verify(repo, times(1)).notifications()
        assertSame(refreshed, vm.data.value)
        assertFalse(vm.busy.value)
    }

    @Test fun `fallo al marcar conserva lista y permite recuperar consulta`() = runTest(dispatcher) {
        val previous = JsonObject().apply { addProperty("unread_count", 1) }
        whenever(repo.notifications()).thenReturn(previous)
        whenever(repo.markNotificationRead("aviso")).thenThrow(RuntimeException("sin red"))
        val vm = NotificationsViewModel(repo)
        vm.load(); advanceUntilIdle()
        vm.read("aviso"); advanceUntilIdle()
        assertSame(previous, vm.data.value)
        assertNotNull(vm.error.value)
        assertFalse(vm.busy.value)
        vm.load(); advanceUntilIdle()
        assertNull(vm.error.value)
    }
}
