package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.core.session.SessionEvents
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface AuthState {
    data object Loading : AuthState
    data object Guest : AuthState
    data class Authenticated(val user: AuthUser) : AuthState
}

class AuthViewModel @JvmOverloads constructor(
    private val repository: AuthRepository = AppContainer.authRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _sessionExpired = MutableStateFlow(false)
    /** Verdadero cuando la sesión se cerró porque el servidor la rechazó; la bienvenida lo avisa. */
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    private val _offline = MutableStateFlow(false)
    /**
     * Verdadero cuando se entró con el resumen guardado porque no se pudo confirmar la sesión con el
     * servidor (sin red o error suyo). La sesión sigue viva: la app lo avisa y ofrece reintentar, en
     * vez de mostrar la pantalla de acceso como si se hubiera cerrado (ver SessionUserStore).
     */
    val offline: StateFlow<Boolean> = _offline.asStateFlow()

    init {
        bootstrap()
        viewModelScope.launch {
            SessionEvents.expired.collect { onSessionExpired() }
        }
    }

    private suspend fun onSessionExpired() {
        if (_state.value !is AuthState.Authenticated) return
        repository.clearLocalSession()
        _sessionExpired.value = true
        _offline.value = false
        _state.value = AuthState.Guest
    }

    fun bootstrap() {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val restored = repository.restoreSession()
            _offline.value = restored.fromCache
            _state.value = restored.user?.let { AuthState.Authenticated(it) } ?: AuthState.Guest
        }
    }

    /** Cada pantalla de acceso empieza sin el error de la anterior (el estado se comparte). */
    fun clearError() {
        _error.value = null
        _sessionExpired.value = false
    }

    fun login(email: String, password: String) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                val user = repository.login(email.trim(), password)
                _offline.value = false
                _state.value = AuthState.Authenticated(user)
            } catch (e: Exception) {
                // En el acceso, un 422 significa correo o contraseña con formato inaceptable: el genérico
                // "Revisa los datos capturados" no dice qué revisar.
                _error.value = if (e is HttpException && e.code() == 422) {
                    "Revisa tu correo y tu contraseña e inténtalo de nuevo."
                } else if (e is HttpException && e.code() == 403) {
                    // En el acceso, el único 403 es el correo sin verificar (AuthController::login).
                    e.serverMessage() ?: "Debes verificar tu correo para iniciar sesión."
                } else {
                    e.toFriendlyMessage("No se pudo iniciar sesión.")
                }
            } finally { _busy.value = false }
        }
    }

    fun loginWithGoogle(idToken: String) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                val user = repository.googleLogin(idToken)
                _offline.value = false
                _state.value = AuthState.Authenticated(user)
            } catch (e: Exception) {
                _error.value = e.toFriendlyMessage("No se pudo iniciar sesión con Google.")
            } finally { _busy.value = false }
        }
    }

    fun reportGoogleUnavailable() {
        _error.value = "No pudimos abrir Google. Revisa tu conexión o intenta de nuevo."
    }

    fun reportGoogleCancelled() {
        _error.value = "Selecciona una cuenta de Google para continuar o inicia con correo."
    }

    fun register(name: String, email: String, password: String, confirmation: String) {
        if (_busy.value) return
        if (password != confirmation) {
            _error.value = "Las contraseñas no coinciden."
            return
        }
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                val user = repository.register(name.trim(), email.trim(), password)
                _offline.value = false
                _state.value = AuthState.Authenticated(user)
            } catch (e: Exception) {
                _error.value = e.toFriendlyMessage("No se pudo crear la cuenta.")
            } finally { _busy.value = false }
        }
    }

    fun forgotPassword(email: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try { onResult(repository.forgotPassword(email.trim())) }
            catch (e: Exception) {
                // El servidor responde 400 cuando el correo no pertenece a ninguna cuenta.
                _error.value = if (e is HttpException && e.code() == 400) {
                    "No encontramos una cuenta con ese correo. Revisa que esté bien escrito."
                } else {
                    e.toFriendlyMessage("No se pudo enviar el correo.")
                }
            }
            finally { _busy.value = false }
        }
    }

    /** Tras editar la cuenta, actualiza nombre, correo y foto en toda la app sin cerrar sesión. */
    fun refreshUser() {
        viewModelScope.launch {
            if (_state.value !is AuthState.Authenticated) return@launch
            runCatching { repository.currentUser() }.onSuccess { _offline.value = false
 _state.value = AuthState.Authenticated(it) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _offline.value = false
            _state.value = AuthState.Guest
        }
    }
}

/**
 * Extrae el campo "message" real que Laravel devuelve en un 422 (p.ej. el
 * motivo exacto de un rechazo por política de cancelación/reagendado), en
 * vez del mensaje genérico de toFriendlyMessage(). errorBody() solo se puede
 * leer una vez -- se llama justo al capturar la excepción, antes de que algo
 * más la consuma.
 */
internal fun HttpException.serverMessage(): String? = try {
    val body = response()?.errorBody()?.string()
    if (body.isNullOrBlank()) null
    else com.google.gson.JsonParser.parseString(body).asJsonObject.get("message")?.asString
} catch (_: Exception) { null }

internal fun Exception.toFriendlyMessage(fallback: String): String {
    return when (this) {
        is HttpException -> when (code()) {
            401 -> "Credenciales incorrectas o sesión vencida."
            // Fuera del acceso, un 403 es falta de permiso (no sesión vencida: eso es 401). Se muestra el
            // motivo del servidor cuando lo manda, para no confundir con "inicia sesión".
            403 -> serverMessage()?.takeIf { it.isNotBlank() && it != "This action is unauthorized." }
                ?: "Tu cuenta no tiene permiso para hacer esto."
            // El servidor explica por qué no se pudo ("el pago aún no está resuelto", "tienes un adeudo…") en
            // español; los errores de validación genéricos de Laravel vienen en inglés y se siguen resumiendo.
            422 -> serverMessage()?.takeIf { it.isNotBlank() && !it.startsWith("The ") && !it.contains("(and ") }
                ?: "Revisa los datos capturados."
            429 -> "Demasiados intentos. Intenta de nuevo más tarde."
            503 -> "UrbanBlade está en mantenimiento."
            else -> fallback
        }
        // Sin red o tiempo agotado: un mensaje claro, no el "Unable to resolve host" técnico.
        is IOException -> "Sin conexión. Revisa tu red e inténtalo de nuevo."
        else -> message?.takeIf { it.isNotBlank() } ?: fallback
    }
}
