package com.urbanblade.mobile.core.network

import com.google.gson.JsonParser
import com.urbanblade.mobile.data.model.*
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BenefitApiContractTest {
    private val server = MockWebServer()
    private lateinit var api: UrbanBladeApi
    @Before fun before() {
        server.start()
        api = Retrofit.Builder().baseUrl(server.url("/api/v1/"))
            .addConverterFactory(GsonConverterFactory.create()).build().create(UrbanBladeApi::class.java)
    }
    @After fun after() { server.shutdown() }

    @Test fun `favorito guarda ID y puede quitarse con null`() = runTest {
        server.enqueue(MockResponse().setBody("""{"barbero_favorito_id":"barber-1"}"""))
        assertEquals("barber-1", api.favoriteBarber(FavoriteBarberRequest("barber-1")).barberId)
        val saved = server.takeRequest()
        assertEquals("PUT", saved.method)
        assertEquals("/api/v1/profile/favorite-barber", saved.path)
        assertEquals("barber-1", JsonParser.parseString(saved.body.readUtf8()).asJsonObject["barber_id"].asString)
        server.enqueue(MockResponse().setBody("""{"barbero_favorito_id":null}"""))
        assertNull(api.favoriteBarber(FavoriteBarberRequest(null)).barberId)
        val cleared = JsonParser.parseString(server.takeRequest().body.readUtf8()).asJsonObject
        assertFalse(cleared.has("barber_id")) // Laravel interpreta campo ausente como null.
    }

    @Test fun `compra paquete solo envia ID sin precio calculado`() = runTest {
        server.enqueue(MockResponse().setBody("""{"data":{"client_secret":"secret","payment_intent_id":"pi_test"}}"""))
        assertEquals("secret", api.packageStripeIntent(PackagePurchaseRequest("pack-1")).data.clientSecret)
        val request = server.takeRequest()
        assertEquals("/api/v1/packages/stripe-intent", request.path)
        assertEquals("""{"service_package_id":"pack-1"}""", request.body.readUtf8())
    }

    @Test fun `tarjeta regalo manda monto elegido y destinatario sin token de tarjeta`() = runTest {
        server.enqueue(MockResponse().setBody("""{"data":{"client_secret":"secret","payment_intent_id":"pi_test"}}"""))
        api.giftCardStripeIntent(CashGiftCardRequest(100.0, destinatarioEmail = "destino@example.test"))
        val request = server.takeRequest()
        assertEquals("/api/v1/gift-cards/stripe-intent", request.path)
        val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
        assertEquals(100.0, body["monto"].asDouble, 0.0)
        assertEquals("destino@example.test", body["destinatario_email"].asString)
        assertEquals(2, body.size())
    }

    @Test fun `tarjeta de miembro recibe PDF desde endpoint autenticado`() = runTest {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/pdf").setBody("%PDF-test"))
        api.membershipCard().use { assertEquals("%PDF-test", it.string()) }
        assertEquals("/api/v1/dashboard/membership/card", server.takeRequest().path)
    }

    @Test fun `catalogo recupera favorito del servidor`() = runTest {
        server.enqueue(MockResponse().setBody("""{"data":[{"id":"b1","es_favorito":true}]}"""))
        assertTrue(api.barbers().data.single().esFavorito)
    }
}
