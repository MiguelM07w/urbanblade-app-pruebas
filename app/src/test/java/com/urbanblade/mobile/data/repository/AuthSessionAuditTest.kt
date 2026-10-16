package com.urbanblade.mobile.data.repository

import com.urbanblade.mobile.core.network.UrbanBladeApi
import com.urbanblade.mobile.core.session.SessionManager
import com.urbanblade.mobile.data.model.*
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class AuthSessionAuditTest {
    private val api: UrbanBladeApi = mock()
    private val session: SessionManager = mock()
    private val repo = AuthRepository(api, session)
    private val user = AuthUser("cliente-1", "Cliente", "cliente@example.test", roles = listOf("cliente"))

    @Test fun `correo registro y Google guardan el usuario desde el primer acceso`() = runTest {
        whenever(api.login(any())).thenReturn(LoginResponse(token = "test-token", user = user))
        whenever(api.register(any())).thenReturn(LoginResponse(token = "test-token", user = user))
        whenever(api.googleLogin(any())).thenReturn(LoginResponse(token = "test-token", user = user))
        repo.login(user.email, "password")
        repo.register(user.name, user.email, "password")
        repo.googleLogin("google-test")
        verify(session, times(3)).saveUser(user)
    }

    @Test fun `primer acceso permite reiniciar sin red sin consultar me antes`() = runTest {
        var cached: AuthUser? = null
        whenever(session.saveUser(any())).thenAnswer { cached = it.getArgument(0); Unit }
        whenever(session.cachedUser()).thenAnswer { cached }
        whenever(session.currentToken()).thenReturn("test-token")
        whenever(api.login(any())).thenReturn(LoginResponse(token = "test-token", user = user))
        whenever(api.me()).thenAnswer { throw IOException("offline") }
        repo.login(user.email, "password")
        assertEquals(AuthRepository.RestoredSession(user, fromCache = true), repo.restoreSession())
        verify(session, times(1)).clear() // Sustituye la sesión previa solo tras autenticación exitosa.
    }

    @Test fun `token antiguo sin cache pide reintento en lugar de acceso`() = runTest {
        whenever(session.currentToken()).thenReturn("test-token")
        whenever(api.me()).thenAnswer { throw IOException("offline") }
        assertTrue(repo.restoreSession().retryRequired)
        verify(session, never()).clear()
    }

    @Test fun `401 elimina token y cache aun cuando hay usuario guardado`() = runTest {
        whenever(session.currentToken()).thenReturn("test-token")
        whenever(session.cachedUser()).thenReturn(user)
        whenever(api.me()).thenThrow(HttpException(Response.error<Any>(401, "{}".toResponseBody())))
        assertEquals(AuthRepository.RestoredSession(null), repo.restoreSession())
        verify(session).clear()
    }
}
