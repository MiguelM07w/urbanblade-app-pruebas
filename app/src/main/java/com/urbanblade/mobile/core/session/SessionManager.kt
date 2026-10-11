package com.urbanblade.mobile.core.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.urbanblade.mobile.data.model.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "urbanblade_session")

class SessionManager(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")

    // El token vive cifrado (AES-256-GCM, clave en Android Keystore); ver SessionTokenStore.
    private val tokenStore = SessionTokenStore(context.dataStore, AesGcmTokenCipher(KeystoreAesKey::get))

    // Resumen del usuario: permite entrar sin conexión en vez de mandar al acceso (ver SessionUserStore).
    private val userStore = SessionUserStore(context.dataStore)

    val token: Flow<String?> = tokenStore.token
    val theme: Flow<String?> = context.dataStore.data.map { it[themeKey] }

    suspend fun currentToken(): String? = tokenStore.currentToken()

    suspend fun currentTheme(): String? = theme.first()

    suspend fun saveToken(token: String) {
        tokenStore.saveToken(token)
    }

    // El tema es una preferencia de apariencia, no de sesión -- a
    // diferencia del token, no se limpia en clear() al cerrar sesión.
    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { it[themeKey] = theme }
    }

    /** Guarda el resumen con el que se entra cuando no hay red. */
    suspend fun saveUser(user: AuthUser) {
        userStore.saveUser(user)
    }

    /** Último usuario confirmado con el servidor; null si nunca se confirmó uno. */
    suspend fun cachedUser(): AuthUser? = userStore.currentUser()

    suspend fun clear() {
        tokenStore.clear()
        userStore.clear()
    }
}
