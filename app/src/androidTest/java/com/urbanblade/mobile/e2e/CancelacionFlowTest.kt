package com.urbanblade.mobile.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.urbanblade.mobile.MainActivity
import com.urbanblade.mobile.e2e.FakeUrbanApi.Companion.TOKEN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Prueba instrumentada de la cancelación de una cita (T145).
 *
 * La app arranca con la sesión iniciada y el cliente tiene una cita pendiente para mañana. El robot la
 * cancela desde "Mis citas", pasando por el diálogo de confirmación.
 */
@RunWith(AndroidJUnit4::class)
class CancelacionFlowTest {

    private val code = "UB-0042"
    private val manana = LocalDate.now().plusDays(1).toString()

    private val api = FakeUrbanApi(loggedIn = true).apply { addAppointment(code, manana) }
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(api).around(compose)

    /** Abre "Mis citas", toca "Cancelar" en la cita y espera el diálogo de confirmación. */
    private fun abrirDialogoDeCancelar() {
        compose.tapTab("Mis citas")
        compose.waitForText("Lo que viene y lo que ya viviste.") // encabezado de "Mis citas"
        compose.tapText("Cancelar")
        compose.waitForText("¿Cancelar tu cita?")
    }

    @Test
    fun confirmar_enviaElDeleteYLaCitaQuedaCancelada() {
        abrirDialogoDeCancelar()
        compose.tapText("Sí, cancelar")

        // El servidor recibió DELETE /appointments/{código} con el token de la sesión.
        compose.waitUntil(TIMEOUT_MS) { api.requestsTo("DELETE", "appointments/$code").isNotEmpty() }
        val request = api.requestsTo("DELETE", "appointments/$code").single()
        assertEquals("Bearer $TOKEN", request.getHeader("Authorization"))
        assertEquals("cancelada", api.appointment(code)?.estado)

        // La lista se vuelve a pedir y la cita pasa al historial como cancelada.
        compose.waitForNoText("¿Cancelar tu cita?")
        compose.tapText("Historial")
        compose.waitForText("Cancelada")
    }

    @Test
    fun volver_noEnviaNadaYLaCitaSigueActiva() {
        abrirDialogoDeCancelar()
        compose.tapText("Volver")

        // El diálogo se cierra, no viaja ninguna petición y la cita sigue con su opción de cancelar.
        compose.waitForNoText("¿Cancelar tu cita?")
        assertTrue(api.requestsTo("DELETE", "appointments/$code").isEmpty())
        assertEquals("pendiente", api.appointment(code)?.estado)
        compose.waitForText("Cancelar")
    }
}
