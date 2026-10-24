package com.urbanblade.mobile.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.urbanblade.mobile.MainActivity
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.e2e.FakeUrbanApi.Companion.EMAIL
import com.urbanblade.mobile.e2e.FakeUrbanApi.Companion.PASSWORD
import com.urbanblade.mobile.e2e.FakeUrbanApi.Companion.TOKEN
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Prueba instrumentada del inicio de sesión (T145).
 *
 * Corre en el emulador con la app real: el robot abre la app sin sesión, entra a "Iniciar sesión",
 * escribe correo y contraseña y toca "Entrar a UrbanBlade". La API es FakeUrbanApi, así que se revisa
 * tanto lo que muestra la pantalla como lo que la app envió al servidor.
 */
@RunWith(AndroidJUnit4::class)
class LoginFlowTest {

    private val api = FakeUrbanApi(loggedIn = false)
    private val compose = createAndroidComposeRule<MainActivity>()

    // Primero se enciende la API falsa y se limpia la sesión; después se abre la app.
    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(api).around(compose)

    private fun login(email: String, password: String) {
        compose.tapText("Iniciar sesión")
        compose.typeIntoField(0, email)    // Correo electrónico
        compose.typeIntoField(1, password) // Contraseña
        compose.tapWhenEnabled("Entrar a UrbanBlade")
    }

    @Test
    fun conCredencialesCorrectas_entraYLasSiguientesPeticionesLlevanElToken() {
        login(EMAIL, PASSWORD)

        // Entró: aparece la navegación del cliente.
        compose.waitForText("Mis citas")

        // La app envió el correo y la contraseña escritos; el login en sí no lleva token.
        val request = api.requestsTo("POST", "auth/login").single()
        val body = JSONObject(request.text())
        assertEquals(EMAIL, body.getString("email"))
        assertEquals(PASSWORD, body.getString("password"))
        assertNull(request.getHeader("Authorization"))

        // La sesión quedó guardada y se usa: las peticiones siguientes llevan "Bearer <token>".
        compose.waitUntil(TIMEOUT_MS) {
            api.requests.any { it.path() != "auth/login" && it.getHeader("Authorization") == "Bearer $TOKEN" }
        }
        assertEquals(TOKEN, runBlocking { AppContainer.sessionManager.currentToken() })
    }

    @Test
    fun conContrasenaIncorrecta_muestraUnErrorSinDetallesYNoEntra() {
        login(EMAIL, "otra-contrasena")

        // El servidor responde 422 y la app lo explica sin decir si falló el correo o la contraseña.
        compose.waitForText("Revisa tu correo y tu contraseña e inténtalo de nuevo.")

        // Sigue en la pantalla de acceso, sin navegación de cliente y sin token guardado.
        assertFalse(compose.hasNodeWithText("Mis citas"))
        assertEquals(1, api.requestsTo("POST", "auth/login").size)
        assertNull(runBlocking { AppContainer.sessionManager.currentToken() })
    }
}
