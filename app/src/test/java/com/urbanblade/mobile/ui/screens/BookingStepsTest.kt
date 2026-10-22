package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.urbanblade.mobile.data.model.BarberItem
import com.urbanblade.mobile.data.model.BarberUser
import com.urbanblade.mobile.data.model.ProductItem
import com.urbanblade.mobile.data.model.ServiceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pruebas de componente de los pasos de la reserva: elegir servicio (búsqueda y selección) y revisar la
 * cita antes de pagar. Compose UI Test sobre Robolectric, sin emulador ni red. Pantalla alta para que todo
 * quepa sin desplazarse.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h3000dp")
class BookingStepsTest {
    @get:Rule
    val compose = createComposeRule()

    private val services = listOf(
        ServiceItem(id = "s1", nombre = "Corte clásico", precio = 180.0, duracionMin = 30),
        ServiceItem(id = "s2", nombre = "Barba tradicional", precio = 120.0, duracionMin = 25),
        ServiceItem(id = "s3", nombre = "Combo premium", precio = 260.0, duracionMin = 60)
    )

    private fun showServiceStep(list: List<ServiceItem> = services, onSelect: (String) -> Unit = {}) {
        compose.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize()) { ServiceStep(list, selectedId = "", onSelect = onSelect) }
            }
        }
    }

    @Test
    fun `el paso de servicio lista cada servicio con su duracion y precio`() {
        showServiceStep()

        compose.onNodeWithText("Corte clásico").assertIsDisplayed()
        compose.onNodeWithText("30 min", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Barba tradicional").assertIsDisplayed()
        compose.onNodeWithText("Combo premium").assertIsDisplayed()
    }

    @Test
    fun `tocar un servicio avisa cual se eligio`() {
        var chosen = ""
        showServiceStep(onSelect = { chosen = it })

        compose.onNodeWithText("Barba tradicional").performClick()

        assertEquals("s2", chosen)
    }

    @Test
    fun `sin coincidencias avisa y limpiar la busqueda devuelve la lista completa`() {
        showServiceStep()

        compose.onNode(hasSetTextAction()).performTextInput("xyz")
        compose.onNodeWithText("No encontramos ese servicio").assertIsDisplayed()
        compose.onAllNodesWithText("Corte clásico").assertCountEquals(0)

        compose.onNodeWithText("Limpiar búsqueda").performClick()

        compose.onAllNodesWithText("No encontramos ese servicio").assertCountEquals(0)
        compose.onNodeWithText("Corte clásico").assertIsDisplayed()
        compose.onNodeWithText("Combo premium").assertIsDisplayed()
    }

    @Test
    fun `la busqueda deja solo los servicios que coinciden`() {
        showServiceStep()

        compose.onNode(hasSetTextAction()).performTextInput("barba")

        compose.onNodeWithText("Barba tradicional").assertIsDisplayed()
        compose.onAllNodesWithText("Corte clásico").assertCountEquals(0)
        compose.onAllNodesWithText("Combo premium").assertCountEquals(0)
    }

    private val barber = BarberItem(id = "b1", slug = "nava", user = BarberUser(id = "u1", name = "Nava Panther"))

    private fun showReview(
        service: ServiceItem? = services[0],
        notes: String = "",
        extras: List<Pair<ProductItem, Int>> = emptyList()
    ): () -> Boolean {
        var paymentSlotShown = false
        compose.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize()) {
                    ReviewStep(
                        service = service,
                        barber = barber,
                        date = "2026-10-23",
                        time = "12:30",
                        extras = extras,
                        notes = notes
                    ) {
                        paymentSlotShown = true
                        Text("Aquí va el paso de pago")
                    }
                }
            }
        }
        return { paymentSlotShown }
    }

    @Test
    fun `el resumen muestra la hora, la fecha, el servicio y el barbero`() {
        showReview()

        compose.onNodeWithText("Tu cita", ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithText("12:30 PM").assertIsDisplayed()
        compose.onNodeWithText("Viernes 23 de octubre").assertIsDisplayed()
        compose.onNodeWithText("Corte clásico").assertIsDisplayed()
        compose.onNodeWithText("Nava Panther").assertIsDisplayed()
    }

    @Test
    fun `el resumen incluye el paso de pago que le pasan`() {
        val shown = showReview()

        compose.onNodeWithText("Aquí va el paso de pago").assertIsDisplayed()
        assertTrue(shown())
    }

    @Test
    fun `la nota para el barbero solo aparece si hay una`() {
        showReview(notes = "")
        compose.onAllNodesWithText("Nota para tu barbero", substring = true).assertCountEquals(0)
    }

    @Test
    fun `con nota el resumen la muestra`() {
        showReview(notes = "Degradado bajo, por favor")

        compose.onNodeWithText("Nota para tu barbero: Degradado bajo, por favor").assertIsDisplayed()
    }
}
