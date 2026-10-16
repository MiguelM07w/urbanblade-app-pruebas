package com.urbanblade.mobile.core.network

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLPeerUnverifiedException

/**
 * Actividad 09, déficit 2: sin certificate pinning, la app confiaba en cualquier CA instalada en
 * el teléfono. Aquí el servidor con certificado "falso" hace de atacante, y el cliente confía en
 * su CA igual que lo haría un teléfono con una CA maliciosa instalada.
 */
class CertificatePinsTest {

    private lateinit var server: MockWebServer
    private lateinit var host: String
    private lateinit var rogueCert: HeldCertificate
    private lateinit var clientCerts: HandshakeCertificates

    @Before
    fun setUp() {
        host = InetAddress.getByName("localhost").canonicalHostName
        rogueCert = HeldCertificate.Builder().commonName("ca-falsa").addSubjectAlternativeName(host).build()
        val serverCerts = HandshakeCertificates.Builder().heldCertificate(rogueCert).build()
        clientCerts = HandshakeCertificates.Builder().addTrustedCertificate(rogueCert.certificate).build()
        server = MockWebServer().apply {
            useHttps(serverCerts.sslSocketFactory(), false)
            enqueue(MockResponse().setBody("ok"))
            start()
        }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun clientWith(pinner: CertificatePinner?): OkHttpClient =
        OkHttpClient.Builder()
            .sslSocketFactory(clientCerts.sslSocketFactory(), clientCerts.trustManager)
            .apply { pinner?.let { certificatePinner(it) } }
            .build()

    private fun call(client: OkHttpClient) =
        client.newCall(Request.Builder().url(server.url("/")).build()).execute()

    @Test
    fun `deficit original - sin pinning se acepta un certificado de una CA falsa de confianza`() {
        call(clientWith(null)).use { assertEquals(200, it.code) }
    }

    @Test
    fun `solucion - con pinning el certificado falso se rechaza`() {
        val pinner = CertificatePins.build(host, CertificatePins.AMAZON_ROOT_PINS)
        try {
            call(clientWith(pinner)).close()
            fail("debía rechazar el certificado que no coincide con los pins de Amazon")
        } catch (_: SSLPeerUnverifiedException) {
            // esperado: la conexión se corta antes de enviar el token Bearer
        }
    }

    @Test
    fun `con pinning el certificado esperado si se acepta`() {
        val pinner = CertificatePins.build(host, listOf(CertificatePinner.pin(rogueCert.certificate)))
        call(clientWith(pinner)).use { assertEquals(200, it.code) }
    }

    @Test
    fun `solo se fija el dominio de la API`() {
        assertNotNull(CertificatePins.forApiHost("api.urbanblade.com.mx"))
        assertNotNull(CertificatePins.forApiHost("API.URBANBLADE.COM.MX"))
        assertNull(CertificatePins.forApiHost("10.0.2.2"))
        assertNull(CertificatePins.forApiHost("localhost"))
        assertNull(CertificatePins.forApiHost("otro-dominio.com"))
    }

    @Test
    fun `los pins son hashes sha256 en base64 de 32 bytes`() {
        assertEquals(4, CertificatePins.AMAZON_ROOT_PINS.size)
        CertificatePins.AMAZON_ROOT_PINS.forEach {
            assertTrue(it, Regex("^sha256/[A-Za-z0-9+/]{43}=$").matches(it))
        }
    }

    // Comprobación obligatoria del certificado público de producción, sin token ni datos de usuario.
    @Test
    fun `en vivo - la API real coincide con los pins y un pin falso se rechaza`() {
        val request = Request.Builder().url("https://${CertificatePins.API_HOST}/").build()

        val good = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS)
            .certificatePinner(CertificatePins.forApiHost(CertificatePins.API_HOST)!!).build()
        good.newCall(request).execute().use { assertTrue(it.code < 600) }

        val bad = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS)
            .certificatePinner(CertificatePins.build(CertificatePins.API_HOST, listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")))
            .build()
        try {
            bad.newCall(request).execute().close()
            fail("un pin falso debía rechazarse")
        } catch (_: SSLPeerUnverifiedException) {
        }
    }
}
