package com.urbanblade.mobile.e2e

import com.urbanblade.mobile.core.network.AppContainer
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.rules.ExternalResource
import java.net.InetAddress
import java.util.concurrent.CopyOnWriteArrayList

/**
 * API falsa de UrbanBlade para las pruebas instrumentadas (T145).
 *
 * Es un MockWebServer que corre dentro del emulador, en el mismo proceso que la app, y contesta como la
 * API de barber con datos de ejemplo. Así las pruebas no tocan la API real (una reserva crearía una cita
 * de verdad en la agenda del equipo) y siempre dan el mismo resultado. Además:
 * - recuerda lo que pasa: la cita reservada aparece en "Mis citas" y la cancelada queda "cancelada";
 * - anota cada petición (método, ruta, cuerpo y token) para que la prueba revise qué envió la app;
 * - protege lo privado: sin "Bearer <token>" responde 401, igual que la API real.
 *
 * Las respuestas imitan las reales (formato de data/model/Models.kt; mismos datos que el servidor de
 * revisión visual del equipo, scripts/ui-audit/fixture_server.py).
 *
 * También es una regla de JUnit: antes de cada prueba enciende el servidor y deja la sesión limpia
 * (o iniciada, con [loggedIn]); al terminar lo apaga.
 */
class FakeUrbanApi(private val loggedIn: Boolean = false) : ExternalResource() {

    companion object {
        const val PORT = 18766
        const val BASE_URL = "http://127.0.0.1:$PORT/api/v1/"

        // Cuenta de prueba: no existe en ningún servidor real.
        const val EMAIL = "cliente@urbanblade.test"
        const val PASSWORD = "Prueba123"
        const val TOKEN = "token-de-prueba-t145"
        const val USER_NAME = "Cliente de Prueba"

        const val SERVICE_ID = "svc-corte"
        const val SERVICE_NAME = "Corte clásico"
        const val BARBER_ID = "barber-luis"
        const val SLOT = "12:00"

        const val BAD_CREDENTIALS = "Las credenciales no son válidas."
        const val SLOT_TAKEN = "Ese horario ya no está disponible. Elige otro."
    }

    /** Una cita guardada en la API falsa. */
    data class Appointment(val id: String, val code: String, val fecha: String, val hora: String, var estado: String)

    private val server = MockWebServer()
    private val appointments = CopyOnWriteArrayList<Appointment>()

    /** Peticiones que recibió la API, en orden de llegada. */
    val requests = CopyOnWriteArrayList<RecordedRequest>()

    /** Si es true, POST /appointments responde 422: el horario se ocupó mientras el cliente reservaba. */
    @Volatile
    var slotTaken = false

    /** Agrega una cita antes de abrir la app (p. ej. para probar la cancelación). */
    fun addAppointment(code: String, fecha: String, hora: String = "11:00", estado: String = "pendiente") {
        appointments += Appointment("appt-$code", code, fecha, hora, estado)
    }

    fun appointment(code: String): Appointment? = appointments.firstOrNull { it.code == code }

    /** Peticiones con ese método y ruta (sin /api/v1/ ni query), p. ej. requestsTo("POST", "appointments"). */
    fun requestsTo(method: String, path: String): List<RecordedRequest> =
        requests.filter { it.method == method && it.path() == path }

    override fun before() {
        server.dispatcher = dispatcher
        server.start(InetAddress.getByName("127.0.0.1"), PORT)
        // La sesión vive en el teléfono (DataStore cifrado) y sobrevive entre pruebas: se limpia siempre
        // para que ninguna prueba dependa de otra. Con loggedIn se guarda el token de prueba y la app
        // arranca con la sesión iniciada (la confirma con GET /auth/me, igual que con un usuario real).
        runBlocking {
            AppContainer.sessionManager.clear()
            if (loggedIn) AppContainer.sessionManager.saveToken(TOKEN)
        }
    }

    override fun after() {
        server.shutdown()
        runBlocking { AppContainer.sessionManager.clear() }
    }

    /** Lo que se puede consultar sin sesión, como en la API real (catálogo, horarios y el propio login). */
    private val publicPaths = setOf("auth/login", "services", "barbers", "availability/slots", "barbershop")

    private val dispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            requests += request
            val path = request.path()
            // Endpoint protegido sin el token de la sesión: 401, igual que el middleware mobile.auth de barber.
            if (path !in publicPaths && request.getHeader("Authorization") != "Bearer $TOKEN") {
                return json(401, """{"message":"No autorizado."}""")
            }
            return when (request.method) {
                "GET" -> get(path)
                "POST" -> post(path, request)
                "DELETE" -> delete(path)
                else -> ok("""{"message":"Acción de prueba completada."}""")
            }
        }
    }

    private fun get(path: String): MockResponse = when (path) {
        "auth/me", "profile" -> ok("""{"user":$userJson}""")
        "services" -> ok("""{"data":[{"id":"$SERVICE_ID","nombre":"$SERVICE_NAME","precio":180,"duracion_min":30,"descripcion":"Corte a tijera y máquina."}]}""")
        // Un solo barbero: la reserva lo asigna sola, como en una barbería pequeña.
        "barbers" -> ok("""{"data":[{"id":"$BARBER_ID","slug":"luis","user":{"id":"u-luis","name":"Luis Barbero"},"es_favorito":false,"total_reviews":0}]}""")
        "availability/slots" -> ok("""{"slots":[{"time":"$SLOT","label":"$SLOT","end_time":"12:30","end_label":"12:30"}]}""")
        "appointments" -> ok(appointmentsJson())
        "dashboard" -> ok("""{"role":"cliente","data":{"loyalty":{"puntos":0,"nivel":"bronce","nivel_label":"Bronce"}}}""")
        "memberships/mine" -> ok("""{"data":null}""")
        "no-show-fees" -> ok("""{"data":[],"adeudo_total":0}""")
        "payments/transfer-info" -> ok("""{"data":{}}""")
        "notifications/preferences" -> ok("""{"data":{"in_app":true,"email":true}}""")
        "referrals/mine" -> ok("""{"data":{"code":"QA-REFERIDO","referidos":[]}}""")
        // Todo lo demás (notificaciones, productos, tarjetas, lista de espera...): lista vacía.
        else -> ok("""{"data":[]}""")
    }

    private fun post(path: String, request: RecordedRequest): MockResponse = when (path) {
        "auth/login" -> {
            val body = JSONObject(request.text())
            if (body.optString("email") == EMAIL && body.optString("password") == PASSWORD) {
                ok("""{"message":"Autenticación exitosa.","token_type":"Bearer","token":"$TOKEN","user":$userJson}""")
            } else {
                // Igual que AuthController::login: 422 sin decir si falló el correo o la contraseña.
                json(422, """{"message":"$BAD_CREDENTIALS"}""")
            }
        }
        "appointments" -> {
            if (slotTaken) {
                json(422, """{"message":"$SLOT_TAKEN","errors":{"hora_inicio":["$SLOT_TAKEN"]}}""")
            } else {
                val body = JSONObject(request.text())
                val code = "UB-%04d".format(appointments.size + 1)
                appointments += Appointment("appt-$code", code, body.getString("fecha"), body.getString("hora_inicio"), "pendiente")
                json(201, """{"message":"Cita reservada correctamente.","data":{"id":"appt-$code","code":"$code"}}""")
            }
        }
        else -> ok("""{"message":"Acción de prueba completada."}""")
    }

    private fun delete(path: String): MockResponse {
        val code = path.removePrefix("appointments/")
        val appt = appointment(code) ?: return json(404, """{"message":"No encontramos esa cita."}""")
        appt.estado = "cancelada"
        return ok("""{"message":"Tu cita fue cancelada."}""")
    }

    private fun appointmentsJson(): String {
        val rows = JSONArray()
        appointments.forEach { a ->
            rows.put(
                JSONObject()
                    .put("id", a.id).put("code", a.code).put("fecha", a.fecha)
                    .put("hora_inicio", a.hora).put("hora_fin", "12:30").put("estado", a.estado)
                    .put("precio_cobrado", 180).put("has_payment", false).put("is_chargeable", false)
                    .put("barber", JSONObject().put("id", BARBER_ID).put("slug", "luis")
                        .put("user", JSONObject().put("id", "u-luis").put("name", "Luis Barbero")))
                    .put("service", JSONObject().put("id", SERVICE_ID).put("nombre", SERVICE_NAME)
                        .put("precio", 180).put("duracion_min", 30))
            )
        }
        val activas = appointments.count { it.estado == "pendiente" || it.estado == "confirmada" }
        return JSONObject()
            .put("data", rows)
            .put("meta", JSONObject().put("page", 1).put("per_page", 30).put("total", appointments.size).put("has_more", false))
            .put("stats", JSONObject().put("total", appointments.size).put("proximas", activas).put("completadas", 0))
            .put("cancellation_policy_hours", 24)
            .toString()
    }

    private val userJson = """{"id":"u-cliente","name":"$USER_NAME","email":"$EMAIL","roles":["cliente"],"profile_complete":true,"client_id":"c-prueba","email_verified_at":"2026-01-01"}"""

    private fun ok(body: String) = json(200, body)

    private fun json(code: Int, body: String) = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}

/** Ruta de la petición sin /api/v1/ ni query: "appointments", "auth/login"... */
fun RecordedRequest.path(): String = requestUrl?.encodedPath?.removePrefix("/api/v1/") ?: ""

/** Cuerpo de la petición como texto (copia: se puede leer varias veces). */
fun RecordedRequest.text(): String = body.clone().readUtf8()
