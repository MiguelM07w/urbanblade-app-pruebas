package com.urbanblade.mobile.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.urbanblade.mobile.MainActivity
import com.urbanblade.mobile.e2e.FakeUrbanApi.Companion.BARBER_ID
import com.urbanblade.mobile.e2e.FakeUrbanApi.Companion.SERVICE_ID
import com.urbanblade.mobile.e2e.FakeUrbanApi.Companion.SERVICE_NAME
import com.urbanblade.mobile.e2e.FakeUrbanApi.Companion.SLOT
import com.urbanblade.mobile.e2e.FakeUrbanApi.Companion.SLOT_TAKEN
import com.urbanblade.mobile.e2e.FakeUrbanApi.Companion.TOKEN
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Prueba instrumentada de la reserva de una cita (T145).
 *
 * La app arranca con la sesión iniciada y el robot recorre los cuatro pasos de la reserva:
 * servicio -> horario (barbero, día y hora) -> extras -> pago. Con un solo barbero la app lo asigna
 * sola y el día por defecto es hoy.
 */
@RunWith(AndroidJUnit4::class)
class ReservaFlowTest {

    private val api = FakeUrbanApi(loggedIn = true)
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(api).around(compose)

    /** Recorre la reserva completa y toca "Reservar cita". */
    private fun reservarCorteHoyALas12() {
        compose.waitForText("Mis citas")      // la sesión se confirmó y se ve el inicio del cliente
        compose.tapText("Reservar cita")      // botón de la tarjeta "¿Te toca corte?" del inicio

        // Paso 1: servicio.
        compose.tapText(SERVICE_NAME)
        compose.tapWhenEnabled("Elegir horario")

        // Paso 2: barbero (único, ya asignado), día (hoy) y hora libre que dio la API.
        compose.tapText(SLOT)
        compose.tapWhenEnabled("Elegir extras")

        // Paso 3: extras opcionales.
        compose.tapWhenEnabled("Continuar sin extras")

        // Paso 4: pago en el salón (opción por defecto) y aceptar la política de inasistencia,
        // sin la cual "Reservar cita" no se habilita.
        compose.tapText("Acepto el cargo por inasistencia", substring = true)
        compose.tapWhenEnabled("Reservar cita")
    }

    @Test
    fun citaValida_muestraLaConfirmacionYEnviaLosDatosCorrectos() {
        reservarCorteHoyALas12()

        compose.waitForText("¡Cita reservada!")

        // El servidor recibió una sola cita, con el token de la sesión y los datos elegidos.
        val request = api.requestsTo("POST", "appointments").single()
        assertEquals("Bearer $TOKEN", request.getHeader("Authorization"))
        val body = JSONObject(request.text())
        assertEquals(BARBER_ID, body.getString("barber_id"))
        assertEquals(SERVICE_ID, body.getString("service_id"))
        assertEquals(LocalDate.now().toString(), body.getString("fecha"))
        assertEquals(SLOT, body.getString("hora_inicio"))
        assertTrue(body.getBoolean("acepta_cargo_inasistencia"))
    }

    @Test
    fun horarioOcupado_muestraElMotivoDelServidorYNoConfirma() {
        // Otro cliente ganó el horario mientras este reservaba: la API responde 422 con el motivo.
        api.slotTaken = true

        reservarCorteHoyALas12()

        compose.waitForText(SLOT_TAKEN)
        assertFalse(compose.hasNodeWithText("¡Cita reservada!"))
        assertEquals(1, api.requestsTo("POST", "appointments").size)
    }
}
