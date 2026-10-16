package com.urbanblade.mobile.core.network

import com.google.gson.Gson
import com.urbanblade.mobile.data.model.CreateCampaignRequest
import com.urbanblade.mobile.data.model.LoginRequest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * P0 #4 "pruebas de contrato": golpea UrbanBladeApi (interfaz Retrofit real)
 * contra un servidor HTTP falso que responde exactamente como Laravel
 * (routes/api.php -> Api/Auth/AuthController), verificando que los códigos
 * 401/403/422/429/503 lleguen como HttpException con el code() correcto y
 * que una respuesta 200 deserialice al contrato esperado (token/user). No
 * usa SessionManager -- se llama la API directo, sin pasar por AuthRepository.
 */
class UrbanBladeApiContractTest {

    private lateinit var server: MockWebServer
    private lateinit var api: UrbanBladeApi

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UrbanBladeApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `login 200 deserializa token y user`() = runTest {
        val body = """
            {"message":"ok","token_type":"Bearer","token":"abc123",
             "user":{"id":"1","name":"Ana","email":"ana@test.com","roles":["cliente"]}}
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val response = api.login(LoginRequest("ana@test.com", "secret123"))

        assertEquals("abc123", response.token)
        assertEquals("Ana", response.user.name)
        assertEquals(listOf("cliente"), response.user.roles)
    }

    @Test
    fun `login declara la plataforma movil para recibir un token de movil`() = runTest {
        // barber da a cada plataforma su propia vigencia de token; el cuerpo que sale de verdad
        // hacia el servidor debe llevar `plataforma` (y el nombre del campo es el del backend).
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"message":"ok","token_type":"Bearer","token":"abc123","expires_at":"2026-11-04T12:00:00.000000Z",
                    "user":{"id":"1","name":"Ana","email":"ana@test.com","roles":["cliente"]}}"""
            )
        )

        api.login(LoginRequest("ana@test.com", "secret123"))

        val enviado = Gson().fromJson(server.takeRequest().body.readUtf8(), Map::class.java)
        assertEquals("movil", enviado["plataforma"])
        assertEquals("Android UrbanBlade", enviado["device_name"])
    }

    @Test
    fun `login 401 propaga HttpException con code 401`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"Credenciales incorrectas."}"""))

        val error = runCatching { api.login(LoginRequest("ana@test.com", "wrong")) }.exceptionOrNull()

        assertEquals(401, (error as HttpException).code())
    }

    @Test
    fun `login 403 propaga HttpException con code 403`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"Verifica tu correo."}"""))

        val error = runCatching { api.login(LoginRequest("ana@test.com", "secret123")) }.exceptionOrNull()

        assertEquals(403, (error as HttpException).code())
    }

    @Test
    fun `login 422 propaga HttpException con code 422`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(422)
                .setBody("""{"message":"The given data was invalid.","errors":{"email":["El correo es obligatorio."]}}""")
        )

        val error = runCatching { api.login(LoginRequest("", "secret123")) }.exceptionOrNull()

        assertEquals(422, (error as HttpException).code())
    }

    @Test
    fun `login 429 propaga HttpException con code 429`() = runTest {
        server.enqueue(MockResponse().setResponseCode(429).setBody("""{"message":"Too Many Attempts."}"""))

        val error = runCatching { api.login(LoginRequest("ana@test.com", "secret123")) }.exceptionOrNull()

        assertEquals(429, (error as HttpException).code())
    }

    @Test
    fun `login 503 propaga HttpException con code 503`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setBody("""{"message":"UrbanBlade en mantenimiento."}"""))

        val error = runCatching { api.login(LoginRequest("ana@test.com", "secret123")) }.exceptionOrNull()

        assertEquals(503, (error as HttpException).code())
    }

    @Test
    fun `request body serializa email y password`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"token":"x","user":{"name":"Ana","email":"ana@test.com"}}"""
            )
        )

        api.login(LoginRequest("ana@test.com", "secret123"))

        val recorded = server.takeRequest()
        val sent = Gson().fromJson(recorded.body.readUtf8(), Map::class.java)
        assertEquals("ana@test.com", sent["email"])
        assertEquals("secret123", sent["password"])
    }

    // T142: el token FCM se registra indicando el proveedor para no mezclarlo con Expo.
    @Test
    fun `savePushToken envia el token al endpoint de perfil`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"message":"Token registrado"}"""))

        val response = api.savePushToken(com.urbanblade.mobile.data.model.PushTokenRequest("fcm-token-123"))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/profile/push-token", request.path)
        assertEquals("""{"token":"fcm-token-123","provider":"fcm"}""", request.body.readUtf8())
        assertEquals("Token registrado", response.message)
    }

    @Test
    fun `reservar con productos los envia y lee productos_error sin perder la cita`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"message":"Cita creada correctamente.","data":{"id":"c1"},"productos_agregados":null,"productos_error":"Sin stock"}"""
            )
        )

        val response = api.createAppointment(
            com.urbanblade.mobile.data.model.AppointmentRequest(
                barberId = "b1", serviceId = "s1", fecha = "2026-09-30", horaInicio = "10:00",
                productos = listOf(com.urbanblade.mobile.data.model.OrderItemRequest("p1", 2))
            )
        )

        val sent = server.takeRequest().body.readUtf8()
        assertEquals(true, sent.contains("\"productos\":[{\"product_id\":\"p1\",\"cantidad\":2}]"))
        assertEquals("c1", response.data?.id)
        assertEquals("Sin stock", response.productosError)
    }

    @Test
    fun `comprobante de pedido pide la liga firmada del pedido`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"data":{"order_id":"o1","receipt_url":"https://cdn.test/pedido-o1.pdf"}}"""))

        val response = api.orderReceiptLink("o1")

        assertEquals("/api/v1/orders/o1/receipt-link", server.takeRequest().path)
        assertEquals("https://cdn.test/pedido-o1.pdf", response.data?.receiptUrl)
    }

    @Test
    fun `campanas GET deserializa segmentos e historial`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "levels":{"vip":"VIP"},
                  "segment_counts":{"todos":12,"vip":3},
                  "data":[
                    {
                      "id":"c1",
                      "titulo":"Regreso a clases",
                      "cuerpo":"20% en cortes",
                      "segmento":"vip",
                      "destinatarios":3,
                      "estado":"enviada",
                      "open_rate":66.7,
                      "click_rate":12.5
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        val response = api.campaigns()

        assertEquals("/api/v1/campaigns", server.takeRequest().path)
        assertEquals("VIP", response.levels["vip"])
        assertEquals(12, response.segmentCounts["todos"])
        assertEquals("Regreso a clases", response.data.single().titulo)
        assertEquals(66.7, response.data.single().openRate, 0.01)
    }

    @Test
    fun `campanas POST envia los campos del formulario y lee la respuesta`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"message":"Campaña enviada a 3 cliente(s)."}"""
            )
        )

        val response = api.createCampaign(
            CreateCampaignRequest(
                titulo = "Regreso a clases",
                cuerpo = "20% en cortes",
                ctaLabel = "Reservar",
                ctaUrl = "https://urbanblade.com.mx/reservar",
                segmento = "vip",
                modo = "ahora",
                programadaPara = null
            )
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/campaigns", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"titulo\":\"Regreso a clases\""))
        assertTrue(body.contains("\"segmento\":\"vip\""))
        assertTrue(body.contains("\"modo\":\"ahora\""))
        assertEquals("Campaña enviada a 3 cliente(s).", response.message)
    }
}
