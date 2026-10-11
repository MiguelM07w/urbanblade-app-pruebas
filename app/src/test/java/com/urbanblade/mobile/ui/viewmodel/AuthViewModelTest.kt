package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.core.session.SessionEvents
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response

/**
 * Cubre P0 #4: transiciones de estado del ViewModel de auth y el mapeo de
 * códigos HTTP a mensajes amigables (401/403/422/429/503), sin backend real
 * -- AuthRepository se mockea directamente (no requiere un SessionManager
 * real, que exige Context/DataStore no disponibles en pruebas JVM puras).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: AuthRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = mock()
        // Por defecto no hay sesión guardada; las pruebas que la necesitan la re-estiban.
        // restoreSession es suspend: el stub por defecto va en runBlocking.
        runBlocking { whenever(repository.restoreSession()).thenReturn(AuthRepository.RestoredSession(null)) }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun httpException(code: Int) =
        HttpException(Response.error<Any>(code, "".toResponseBody(null)))

    @Test
    fun `login exitoso pasa a Authenticated`() = runTest(dispatcher) {
        val user = AuthUser(id = "1", name = "Ana", email = "ana@test.com")
        whenever(repository.login("ana@test.com", "secret123")).thenReturn(user)

        val vm = AuthViewModel(repository)
        vm.login("ana@test.com", "secret123")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthState.Authenticated(user), vm.state.value)
        assertFalse(vm.busy.value)
        assertEquals(null, vm.error.value)
    }

    @Test
    fun `login con 401 muestra credenciales incorrectas`() = runTest(dispatcher) {
        whenever(repository.login(any(), any())).thenThrow(httpException(401))

        val vm = AuthViewModel(repository)
        vm.login("ana@test.com", "wrong")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Credenciales incorrectas o sesión vencida.", vm.error.value)
        assertFalse(vm.busy.value)
    }

    @Test
    fun `login con 403 pide verificar correo`() = runTest(dispatcher) {
        whenever(repository.login(any(), any())).thenThrow(httpException(403))

        val vm = AuthViewModel(repository)
        vm.login("ana@test.com", "secret123")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Debes verificar tu correo para iniciar sesión.", vm.error.value)
    }

    @Test
    fun `login con 422 pide revisar datos`() = runTest(dispatcher) {
        whenever(repository.login(any(), any())).thenThrow(httpException(422))

        val vm = AuthViewModel(repository)
        vm.login("ana@test.com", "secret123")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Revisa tu correo y tu contraseña e inténtalo de nuevo.", vm.error.value)
    }

    @Test
    fun `login con 429 pide reintentar mas tarde`() = runTest(dispatcher) {
        whenever(repository.login(any(), any())).thenThrow(httpException(429))

        val vm = AuthViewModel(repository)
        vm.login("ana@test.com", "secret123")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Demasiados intentos. Intenta de nuevo más tarde.", vm.error.value)
    }

    @Test
    fun `login con 503 indica mantenimiento`() = runTest(dispatcher) {
        whenever(repository.login(any(), any())).thenThrow(httpException(503))

        val vm = AuthViewModel(repository)
        vm.login("ana@test.com", "secret123")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("UrbanBlade está en mantenimiento.", vm.error.value)
    }

    @Test
    fun `loginWithGoogle Cancelled reporta mensaje de seleccionar cuenta`() = runTest(dispatcher) {
        val vm = AuthViewModel(repository)
        vm.reportGoogleCancelled()

        assertEquals("Selecciona una cuenta de Google para continuar o inicia con correo.", vm.error.value)
    }

    @Test
    fun `loginWithGoogle Unavailable reporta mensaje de conexion`() = runTest(dispatcher) {
        val vm = AuthViewModel(repository)
        vm.reportGoogleUnavailable()

        assertEquals("No pudimos abrir Google. Revisa tu conexión o intenta de nuevo.", vm.error.value)
    }

    @Test
    fun `register con contrasenas distintas no llama al repositorio`() = runTest(dispatcher) {
        val vm = AuthViewModel(repository)
        vm.register("Ana", "ana@test.com", "secret123", "otra123")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Las contraseñas no coinciden.", vm.error.value)
        verify(repository, never()).register(any(), any(), any())
    }


    @Test
    fun `sin conexion se entra con el resumen guardado y se avisa`() = runTest(dispatcher) {
        val user = AuthUser(id = "1", name = "Ana", email = "ana@test.com")
        whenever(repository.restoreSession())
            .thenReturn(AuthRepository.RestoredSession(user, fromCache = true))

        val vm = AuthViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthState.Authenticated(user), vm.state.value)
        assertTrue("debe avisar que no hubo conexión", vm.offline.value)
    }

    @Test
    fun `al confirmar la sesion con el servidor no se avisa nada`() = runTest(dispatcher) {
        val user = AuthUser(id = "1", name = "Ana", email = "ana@test.com")
        whenever(repository.restoreSession()).thenReturn(AuthRepository.RestoredSession(user))

        val vm = AuthViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthState.Authenticated(user), vm.state.value)
        assertFalse("con el servidor confirmado no hay aviso", vm.offline.value)
    }

    @Test
    fun `un login que si funciona quita el aviso sin conexion`() = runTest(dispatcher) {
        val user = AuthUser(id = "1", name = "Ana", email = "ana@test.com")
        whenever(repository.restoreSession())
            .thenReturn(AuthRepository.RestoredSession(user, fromCache = true))

        val vm = AuthViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.offline.value)

        whenever(repository.login("ana@test.com", "secret123")).thenReturn(user)
        vm.login("ana@test.com", "secret123")
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse("tras entrar de nuevo ya hay conexión", vm.offline.value)
    }
    @Test
    fun `logout limpia el estado a Guest`() = runTest(dispatcher) {
        val vm = AuthViewModel(repository)
        vm.logout()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.state.value is AuthState.Guest)
    }

    @Test
    fun `una sesion rechazada por el servidor vuelve a invitado y lo avisa`() = runTest(dispatcher) {
        val user = AuthUser(id = "1", name = "Ana", email = "ana@test.com")
        whenever(repository.restoreSession()).thenReturn(AuthRepository.RestoredSession(user))

        val vm = AuthViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(AuthState.Authenticated(user), vm.state.value)

        SessionEvents.notifyExpired()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthState.Guest, vm.state.value)
        assertTrue(vm.sessionExpired.value)
        verify(repository).clearLocalSession()
    }

    @Test
    fun `sesion vencida sin usuario autenticado no hace nada`() = runTest(dispatcher) {
        whenever(repository.restoreSession()).thenReturn(AuthRepository.RestoredSession(null))

        val vm = AuthViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()
        SessionEvents.notifyExpired()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthState.Guest, vm.state.value)
        assertFalse(vm.sessionExpired.value)
        verify(repository, never()).clearLocalSession()
    }
}
