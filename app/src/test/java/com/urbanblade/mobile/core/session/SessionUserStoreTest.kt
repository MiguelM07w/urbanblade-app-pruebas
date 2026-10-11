package com.urbanblade.mobile.core.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.urbanblade.mobile.data.model.AuthUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * El resumen guardado es lo que permite **entrar sin conexión** en vez de mostrar la pantalla de
 * acceso. Se prueba sobre un archivo real `.preferences_pb`, el mismo formato que usa la app.
 */
class SessionUserStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val userKey = stringPreferencesKey("session_user")

    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: SessionUserStore

    @Before
    fun setUp() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(tmp.root, "urbanblade_session.preferences_pb") }
        )
        store = SessionUserStore(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `el resumen se recupera completo, con rol y perfil`() = runBlocking {
        val user = AuthUser(
            id = "7",
            name = "Ana Pérez",
            email = "ana@test.com",
            avatarUrl = "https://urbanblade.mx/ana.jpg",
            roles = listOf("cliente"),
            clientId = "c-7"
        )
        store.saveUser(user)
        assertEquals(user, store.currentUser())
    }

    @Test
    fun `sin resumen guardado no hay usuario con el que entrar`() = runBlocking {
        assertNull(store.currentUser())
    }

    @Test
    fun `un resumen ilegible no rompe el arranque`() = runBlocking {
        dataStore.edit { it[userKey] = "{esto no es json" }
        assertNull(store.currentUser())
    }

    @Test
    fun `clear borra el resumen al cerrar sesion`() = runBlocking {
        store.saveUser(AuthUser(id = "7", name = "Ana", email = "ana@test.com"))
        store.clear()
        assertNull(store.currentUser())
    }
}
