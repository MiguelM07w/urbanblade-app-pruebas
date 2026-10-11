package com.urbanblade.mobile.core.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.urbanblade.mobile.data.model.AuthUser
import kotlinx.coroutines.flow.first

/**
 * Resumen del usuario que inició sesión, guardado en el mismo DataStore que el token.
 *
 * Sirve para **entrar sin conexión**: si al arrancar la app no se alcanza el servidor, se entra con
 * lo último que se sabía del usuario en vez de mostrar la pantalla de acceso, que haría pensar que
 * la sesión se cerró. Solo guarda lo necesario para pintar la app (id, nombre, correo, rol).
 *
 * Se borra al cerrar sesión y cuando el servidor responde 401: ahí la sesión sí terminó.
 *
 * Recibe el DataStore y el Gson por parámetro para poder probarse en la JVM sin Android.
 */
class SessionUserStore(
    private val dataStore: DataStore<Preferences>,
    private val gson: Gson = Gson()
) {
    private val userKey = stringPreferencesKey("session_user")

    suspend fun currentUser(): AuthUser? {
        val raw = dataStore.data.first()[userKey] ?: return null
        return runCatching { gson.fromJson(raw, AuthUser::class.java) }.getOrNull()
    }

    suspend fun saveUser(user: AuthUser) {
        dataStore.edit { it[userKey] = gson.toJson(user) }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(userKey) }
    }
}
