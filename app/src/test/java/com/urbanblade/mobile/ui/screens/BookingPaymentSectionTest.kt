package com.urbanblade.mobile.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.urbanblade.mobile.data.model.SavedCard
import com.urbanblade.mobile.data.model.TransferInfo
import com.urbanblade.mobile.ui.viewmodel.BookingPayMethod
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pruebas de componente del paso de pago de la reserva (BookingPaymentSection): propina, total y los tres
 * métodos. Se montan en la JVM con Compose UI Test sobre Robolectric (sin emulador ni red). El formulario de
 * Stripe (tarjeta) no se dibuja aquí: se comprueba que el método aparezca solo cuando la tarjeta está disponible.
 */
@RunWith(RobolectricTestRunner::class)
// Pantalla alta (3000 dp) para que todo el paso quepa sin desplazarse: lo que queda fuera de la ventana no cuenta
// como «mostrado» y los clics fuera de ella no llegan.
@Config(sdk = [34], qualifiers = "w411dp-h3000dp")
class BookingPaymentSectionTest {
    @get:Rule
    val compose = createComposeRule()

    private val bank = TransferInfo(
        configurado = true,
        banco = "Banco de Prueba",
        beneficiario = "UrbanBlade SA de CV",
        clabe = "012345678901234567",
        concepto = "Cita UrbanBlade"
    )

    private fun show(
        state: BookingPaymentState = BookingPaymentState(),
        price: Double = 300.0,
        transfer: TransferInfo? = bank,
        cards: List<SavedCard> = emptyList(),
        cardAvailable: Boolean = true
    ): BookingPaymentState {
        compose.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize()) {
                    BookingPaymentSection(
                        state = state,
                        servicePrice = price,
                        transferInfo = transfer,
                        savedCards = cards,
                        cardAvailable = cardAvailable,
                        testMode = false,
                        onPickReceipt = {}
                    )
                }
            }
        }
        return state
    }

    @Test
    fun `por defecto se paga en efectivo y el total es el precio del servicio`() {
        show()

        compose.onNodeWithText("Elige cómo quieres pagar tu cita").assertIsDisplayed()
        compose.onNodeWithText("Pagas $300 al llegar a recepción. Hoy no se hace ningún cargo.").assertIsDisplayed()
        // «Servicio» y «Total de la cita» muestran la misma cifra.
        compose.onAllNodesWithText("$300").assertCountEquals(2)
    }

    @Test
    fun `una propina del 10 por ciento se suma al total`() {
        val state = show()

        compose.onNodeWithText("10%").performClick()

        assertEquals(BookingPaymentState.TIP_10, state.tipOption)
        compose.onNodeWithText("Propina").assertIsDisplayed()
        compose.onNodeWithText("$30").assertIsDisplayed()
        compose.onAllNodesWithText("$330").assertCountEquals(1)
        compose.onNodeWithText("Pagas $330 al llegar a recepción. Hoy no se hace ningún cargo.").assertIsDisplayed()
    }

    @Test
    fun `con otro monto aparece el campo y solo acepta numeros`() {
        val state = show()

        compose.onNodeWithText("Otro monto").performClick()
        compose.onNodeWithText("Monto de la propina").assertIsDisplayed()
        compose.onNodeWithText("Monto en pesos").performTextInput("5a0")

        assertEquals("50", state.customTip)
        compose.onAllNodesWithText("$350").assertCountEquals(1)
    }

    @Test
    fun `sin propina no se muestra la fila de propina`() {
        show()

        compose.onAllNodesWithText("Propina").assertCountEquals(0)
    }

    @Test
    fun `transferencia muestra los datos bancarios y la CLABE por grupos`() {
        val state = show()

        compose.onNodeWithText("Transferencia").performClick()

        assertEquals(BookingPayMethod.TRANSFERENCIA, state.method)
        compose.onNodeWithText("Transfiere a esta cuenta").assertIsDisplayed()
        compose.onNodeWithText("Banco de Prueba").assertIsDisplayed()
        compose.onNodeWithText("UrbanBlade SA de CV").assertIsDisplayed()
        compose.onNodeWithText("0123 4567 8901 2345 67").assertIsDisplayed()
        compose.onNodeWithText("Adjuntar comprobante (opcional)").assertIsDisplayed()
    }

    @Test
    fun `transferencia sin datos bancarios configurados avisa y no muestra la cuenta`() {
        show(transfer = TransferInfo(configurado = false))

        compose.onNodeWithText("Transferencia").performClick()

        compose.onNodeWithText("La barbería todavía no registra sus datos bancarios. Elige otro método o pregunta en recepción.")
            .assertIsDisplayed()
        compose.onAllNodesWithText("Transfiere a esta cuenta").assertCountEquals(0)
    }

    @Test
    fun `transferencia mientras cargan los datos muestra el aviso de carga`() {
        show(transfer = null)

        compose.onNodeWithText("Transferencia").performClick()

        compose.onNodeWithText("Cargando datos bancarios…").assertIsDisplayed()
    }

    @Test
    fun `con el comprobante elegido el boton ofrece cambiarlo`() {
        val state = BookingPaymentState().also {
            it.method = BookingPayMethod.TRANSFERENCIA
            it.receiptUri = Uri.parse("content://comprobantes/1")
        }
        show(state = state)

        compose.onNodeWithText("Comprobante listo · cambiar").assertIsDisplayed()
        compose.onAllNodesWithText("Adjuntar comprobante (opcional)").assertCountEquals(0)
    }

    @Test
    fun `la tarjeta solo se ofrece cuando esta disponible`() {
        show(cardAvailable = false)

        compose.onNodeWithText("Efectivo").assertIsDisplayed()
        compose.onNodeWithText("Transferencia").assertIsDisplayed()
        compose.onAllNodesWithText("Tarjeta").assertCountEquals(0)
    }
}
