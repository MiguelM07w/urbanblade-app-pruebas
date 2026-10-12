package com.urbanblade.mobile.ui.viewmodel

import android.content.Context
import android.net.Uri
import org.mockito.kotlin.eq
import org.mockito.kotlin.anyOrNull
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.delay
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.times
import org.junit.Assert.assertNotNull
import kotlinx.coroutines.runBlocking
import com.urbanblade.mobile.data.model.PageMeta
import com.urbanblade.mobile.data.model.AppointmentsResponse
import com.urbanblade.mobile.data.model.AppointmentRow
import com.urbanblade.mobile.data.model.NoShowFeesResponse
import com.urbanblade.mobile.data.model.StripeIntentResponseData
import com.urbanblade.mobile.data.model.UploadPaymentReceiptData
import com.urbanblade.mobile.data.model.UploadPaymentReceiptResponse
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response

/**
 * Reflejo del ajuste de P0 #4 a la ronda de "gestión de citas": el 422 de
 * cancelar/reagendar debe mostrar el mensaje real que devuelve Laravel (la
 * política de cancelación o "ya no se puede gestionar"), no el genérico de
 * toFriendlyMessage().
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: UrbanRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mock()
        // La recarga tras cada acción llama a appointments(); sin este stub el mock devolvería null.
        runBlocking {
            whenever(repo.appointments(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(AppointmentsResponse())
        }
    }

    @Test
    fun `cargar mas agrega la siguiente pagina y termina cuando no hay mas`() = runTest(dispatcher) {
        val first = AppointmentsResponse(
            data = listOf(AppointmentRow(id = "1", code = "a", fecha = "2026-10-02", horaInicio = "10:00:00", estado = "pendiente")),
            meta = PageMeta(page = 1, perPage = 30, total = 2, hasMore = true)
        )
        val second = AppointmentsResponse(
            data = listOf(AppointmentRow(id = "2", code = "b", fecha = "2026-10-01", horaInicio = "10:00:00", estado = "pendiente")),
            meta = PageMeta(page = 2, perPage = 30, total = 2, hasMore = false)
        )
        whenever(repo.appointments(eq(1), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(first)
        whenever(repo.appointments(eq(2), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(second)
        val vm = AppointmentsViewModel(repo)

        vm.load()
        advanceUntilIdle()
        assertTrue(vm.hasMore.value)

        vm.loadMore()
        advanceUntilIdle()
        assertEquals(listOf("1", "2"), vm.data.value.data.map { it.id })
        assertFalse(vm.hasMore.value)
    }

    @Test
    fun `un servidor sin meta se trata como lista completa`() = runTest(dispatcher) {
        val vm = AppointmentsViewModel(repo)

        vm.load()
        advanceUntilIdle()

        assertFalse(vm.hasMore.value)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `reintentar una pagina conserva citas y limpia el error al recuperar`() = runTest(dispatcher) {
        val first = AppointmentsResponse(data = listOf(appointment()), meta = PageMeta(page = 1, hasMore = true))
        val second = AppointmentsResponse(data = listOf(appointment().copy(id = "2")), meta = PageMeta(page = 2, hasMore = false))
        whenever(repo.appointments(eq(1), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(first)
        whenever(repo.appointments(eq(2), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenAnswer { throw java.io.IOException("offline") }.thenReturn(second)
        val vm = AppointmentsViewModel(repo)
        vm.load()
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()
        assertEquals(listOf("1"), vm.data.value.data.map { it.id })
        assertTrue(vm.hasMore.value)
        assertNotNull(vm.error.value)
        vm.loadMore()
        advanceUntilIdle()
        assertEquals(listOf("1", "2"), vm.data.value.data.map { it.id })
        assertNull(vm.error.value)
        assertFalse(vm.loadingMore.value)
    }

    @Test
    fun `actualizar cancela pagina anterior y evita pedir mas durante la recarga`() = runTest(dispatcher) {
        val first = AppointmentsResponse(data = listOf(appointment()), meta = PageMeta(page = 1, hasMore = true))
        val refreshed = first.copy(data = listOf(appointment().copy(id = "actualizada")))
        whenever(repo.appointments(eq(1), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(first, refreshed)
        whenever(repo.appointments(eq(2), anyOrNull(), anyOrNull(), anyOrNull())).doSuspendableAnswer {
            delay(1000)
            AppointmentsResponse(data = listOf(appointment().copy(id = "obsoleta")))
        }
        val vm = AppointmentsViewModel(repo)
        vm.load()
        advanceUntilIdle()
        vm.loadMore()
        vm.loadMore()
        runCurrent()
        assertTrue(vm.loadingMore.value)
        vm.load()
        vm.loadMore()
        advanceUntilIdle()
        assertEquals(listOf("actualizada"), vm.data.value.data.map { it.id })
        assertNull(vm.error.value)
        assertFalse(vm.loading.value)
        assertFalse(vm.loadingMore.value)
        verify(repo, times(1)).appointments(eq(2), anyOrNull(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `una recarga anterior no reemplaza la respuesta mas reciente`() = runTest(dispatcher) {
        var calls = 0
        whenever(repo.appointments(eq(1), anyOrNull(), anyOrNull(), anyOrNull())).doSuspendableAnswer {
            val call = ++calls
            if (call == 1) delay(1000)
            AppointmentsResponse(data = listOf(appointment().copy(id = call.toString())))
        }
        val vm = AppointmentsViewModel(repo)
        vm.load()
        runCurrent()
        vm.load()
        advanceUntilIdle()
        assertEquals("2", vm.data.value.data.single().id)
        assertNull(vm.error.value)
        assertFalse(vm.loading.value)
    }

    private fun httpException(code: Int, message: String? = null): HttpException {
        val body = if (message != null) "{\"message\":\"$message\"}" else ""
        return HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))
    }

    private fun appointment(code: String = "abc123", estado: String = "pendiente") =
        AppointmentRow(id = "1", code = code, fecha = "2026-10-01", horaInicio = "10:00:00", estado = estado)

    @Test
    fun `fallo de consulta no elimina el adeudo y solo el servidor confirma cero`() = runTest(dispatcher) {
        whenever(repo.noShowFees(anyOrNull()))
            .thenReturn(NoShowFeesResponse(adeudoTotal = 75.0))
            .thenAnswer { throw java.io.IOException("offline") }
            .thenReturn(NoShowFeesResponse(adeudoTotal = null))
            .thenReturn(NoShowFeesResponse(adeudoTotal = 0.0))
        val vm = AppointmentsViewModel(repo)
        vm.loadDebt()
        advanceUntilIdle()
        assertEquals(75.0, vm.debt.value, 0.0)
        assertNull(vm.debtError.value)
        vm.loadDebt()
        advanceUntilIdle()
        assertEquals(75.0, vm.debt.value, 0.0)
        assertNotNull(vm.debtError.value)
        vm.loadDebt()
        advanceUntilIdle()
        assertEquals(75.0, vm.debt.value, 0.0)
        assertNotNull(vm.debtError.value)
        vm.loadDebt()
        advanceUntilIdle()
        assertEquals(0.0, vm.debt.value, 0.0)
        assertNull(vm.debtError.value)
    }

    @Test
    fun `cancelar con 422 muestra el mensaje real del servidor`() = runTest(dispatcher) {
        whenever(repo.cancelAppointment(any())).thenThrow(
            httpException(422, "Solo puedes cancelar con al menos 24 horas de anticipación.")
        )

        val vm = AppointmentsViewModel(repo)
        vm.cancel(appointment())
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Solo puedes cancelar con al menos 24 horas de anticipación.", vm.error.value)
    }

    @Test
    fun `cancelar con 422 sin cuerpo usable cae al mensaje por defecto`() = runTest(dispatcher) {
        whenever(repo.cancelAppointment(any())).thenThrow(httpException(422, null))

        val vm = AppointmentsViewModel(repo)
        vm.cancel(appointment())
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Esta cita ya no se puede cancelar.", vm.error.value)
    }

    @Test
    fun `reagendar con campos vacios no llama al repositorio`() = runTest(dispatcher) {
        val vm = AppointmentsViewModel(repo)
        var doneCalled = false
        vm.reschedule("abc123", "", "svc1", "2026-10-01", "10:00", null) { doneCalled = true }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Selecciona barbero, servicio, fecha y horario.", vm.error.value)
        assertEquals(false, doneCalled)
        verify(repo, never()).updateAppointment(any(), any())
    }

    @Test
    fun `reagendar con 422 muestra el mensaje real del servidor`() = runTest(dispatcher) {
        whenever(repo.updateAppointment(any(), any())).thenThrow(
            httpException(422, "Esta cita ya no se puede reagendar.")
        )

        val vm = AppointmentsViewModel(repo)
        vm.reschedule("abc123", "barber1", "svc1", "2026-10-01", "10:00", null) {}
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Esta cita ya no se puede reagendar.", vm.error.value)
    }

    @Test
    fun `reagendar exitoso llama onDone`() = runTest(dispatcher) {
        val vm = AppointmentsViewModel(repo)
        var doneCalled = false
        vm.reschedule("abc123", "barber1", "svc1", "2026-10-01", "10:00", null) { doneCalled = true }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, doneCalled)
        assertNull(vm.error.value)
    }

    @Test
    fun `startStripeCheckout exitoso guarda el client secret`() = runTest(dispatcher) {
        whenever(repo.stripeIntent(any())).thenReturn(
            StripeIntentResponseData(clientSecret = "pi_test_secret", paymentIntentId = "pi_test")
        )

        val vm = AppointmentsViewModel(repo)
        vm.startStripeCheckout("1", 0, null, 20.0)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("pi_test_secret", vm.stripeClientSecret.value)
        assertNull(vm.error.value)
    }

    @Test
    fun `startStripeCheckout con 422 muestra el mensaje real del servidor`() = runTest(dispatcher) {
        whenever(repo.stripeIntent(any())).thenThrow(
            httpException(422, "La cita ya tiene un pago registrado.")
        )

        val vm = AppointmentsViewModel(repo)
        vm.startStripeCheckout("1", 0, null, 0.0)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("La cita ya tiene un pago registrado.", vm.error.value)
        assertNull(vm.stripeClientSecret.value)
    }

    @Test
    fun `clearStripeClientSecret limpia el estado`() = runTest(dispatcher) {
        whenever(repo.stripeIntent(any())).thenReturn(
            StripeIntentResponseData(clientSecret = "pi_test_secret", paymentIntentId = "pi_test")
        )

        val vm = AppointmentsViewModel(repo)
        vm.startStripeCheckout("1", 0, null, 0.0)
        dispatcher.scheduler.advanceUntilIdle()
        vm.clearStripeClientSecret()

        assertNull(vm.stripeClientSecret.value)
    }

    @Test
    fun `uploadPaymentReceipt exitoso llama onDone y recarga`() = runTest(dispatcher) {
        whenever(repo.uploadPaymentReceipt(any(), any(), any(), any())).thenReturn(
            UploadPaymentReceiptResponse(data = UploadPaymentReceiptData(id = "p1", estado = "pendiente_verificacion"))
        )

        val vm = AppointmentsViewModel(repo)
        var doneCalled = false
        vm.uploadPaymentReceipt(mock<Context>(), "abc123", 15.0, mock<Uri>()) { doneCalled = true }
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(doneCalled)
        assertNull(vm.error.value)
    }

    @Test
    fun `uploadPaymentReceipt con 422 muestra el mensaje real del servidor`() = runTest(dispatcher) {
        whenever(repo.uploadPaymentReceipt(any(), any(), any(), any())).thenThrow(
            httpException(422, "La cita ya tiene un pago registrado o en revision.")
        )

        val vm = AppointmentsViewModel(repo)
        var doneCalled = false
        vm.uploadPaymentReceipt(mock<Context>(), "abc123", 0.0, mock<Uri>()) { doneCalled = true }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("La cita ya tiene un pago registrado o en revision.", vm.error.value)
        assertFalse(doneCalled)
    }

    // ---- T144: resultado del PaymentSheet de Stripe (HU-17) ----

    @Test
    fun `onStripeCanceled limpia el client secret y avisa que no hubo cargo`() = runTest(dispatcher) {
        whenever(repo.stripeIntent(any())).thenReturn(
            StripeIntentResponseData(clientSecret = "pi_test_secret", paymentIntentId = "pi_test")
        )
        val vm = AppointmentsViewModel(repo)
        vm.startStripeCheckout("1", 0, null, 0.0)
        dispatcher.scheduler.advanceUntilIdle()

        vm.onStripeCanceled()

        assertNull(vm.stripeClientSecret.value)
        assertNull(vm.error.value)
        assertTrue(vm.paymentNotice.value!!.contains("No se hizo ningún cargo"))
    }

    @Test
    fun `onStripeFailed muestra el motivo de Stripe como error`() = runTest(dispatcher) {
        val vm = AppointmentsViewModel(repo)

        vm.onStripeFailed("Tu tarjeta fue rechazada.")

        assertNull(vm.stripeClientSecret.value)
        assertNull(vm.paymentNotice.value)
        assertEquals(
            "El pago con tarjeta no se completó: Tu tarjeta fue rechazada. Intenta con otra tarjeta o paga por transferencia.",
            vm.error.value
        )
    }

    @Test
    fun `onStripeFailed sin motivo usa un mensaje generico`() = runTest(dispatcher) {
        val vm = AppointmentsViewModel(repo)

        vm.onStripeFailed("  ")

        assertEquals(
            "El pago con tarjeta no se completó. Intenta con otra tarjeta o paga por transferencia.",
            vm.error.value
        )
    }

    @Test
    fun `confirmAppointmentPayment avisa si el webhook aun no confirma el pago`() = runTest(dispatcher) {
        val vm = AppointmentsViewModel(repo)

        vm.confirmAppointmentPayment("1")
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.confirmingPayment.value)
        assertTrue(vm.paymentNotice.value!!.contains("no es necesario pagar de nuevo"))
    }

    @Test
    fun `confirmAppointmentPayment no deja aviso cuando el pago ya quedo confirmado`() = runTest(dispatcher) {
        whenever(repo.appointments(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(
            AppointmentsResponse(
                data = listOf(AppointmentRow(id = "1", code = "a", fecha = "2026-10-01", horaInicio = "10:00:00", estado = "confirmada", hasPayment = true))
            )
        )
        val vm = AppointmentsViewModel(repo)

        vm.confirmAppointmentPayment("1")
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.confirmingPayment.value)
        assertNull(vm.paymentNotice.value)
    }

    @Test
    fun `startStripeCheckout limpia un aviso anterior`() = runTest(dispatcher) {
        whenever(repo.stripeIntent(any())).thenReturn(
            StripeIntentResponseData(clientSecret = "pi_test_secret", paymentIntentId = "pi_test")
        )
        val vm = AppointmentsViewModel(repo)
        vm.onStripeCanceled()

        vm.startStripeCheckout("1", 0, null, 0.0)
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.paymentNotice.value)
        assertEquals("pi_test_secret", vm.stripeClientSecret.value)
    }

    private val citaRecepcion = AppointmentRow(id = "r1", code = "rcp1", fecha = "2026-09-26", horaInicio = "10:00:00", estado = "confirmada")

    @Test
    fun `recepcion cancela cambiando el estado, no con la ruta del cliente`() = runTest(dispatcher) {
        val vm = AppointmentsViewModel(repo)

        vm.cancel(citaRecepcion, asStaff = true)
        advanceUntilIdle()

        verify(repo).updateAppointmentStatus("rcp1", "cancelada")
        verify(repo, never()).cancelAppointment(any())
    }

    @Test
    fun `el cliente sigue cancelando con su propia ruta`() = runTest(dispatcher) {
        val vm = AppointmentsViewModel(repo)

        vm.cancel(citaRecepcion)
        advanceUntilIdle()

        verify(repo).cancelAppointment("rcp1")
        verify(repo, never()).updateAppointmentStatus(any(), any())
    }

    @Test
    fun `un 403 dice que falta permiso, no que hay que iniciar sesion`() = runTest(dispatcher) {
        val forbidden = HttpException(Response.error<Any>(403, """{"message":""}""".toResponseBody("application/json".toMediaType())))
        whenever(repo.cancelAppointment("rcp1")).thenThrow(forbidden)
        val vm = AppointmentsViewModel(repo)

        vm.cancel(citaRecepcion)
        advanceUntilIdle()

        assertEquals("Tu cuenta no tiene permiso para hacer esto.", vm.error.value)
    }
}
