package com.urbanblade.mobile.core.network

import android.content.Context
import com.urbanblade.mobile.BuildConfig
import com.urbanblade.mobile.core.session.SessionEvents
import com.urbanblade.mobile.core.session.SessionManager
import com.urbanblade.mobile.data.repository.AuthRepository
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AppContainer {
    lateinit var sessionManager: SessionManager
        private set
    lateinit var api: UrbanBladeApi
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var urbanRepository: UrbanRepository
        private set

    /**
     * Dirección de la API para las pruebas instrumentadas (T145). Solo la fija UrbanTestRunner
     * (app/src/androidTest), antes de que arranque la app, para apuntarla a la API falsa del emulador;
     * la app nunca la escribe. En release BuildConfig.DEBUG es false y se ignora siempre.
     */
    @Volatile
    var testApiBaseUrl: String? = null

    fun init(context: Context) {
        val baseUrl = testApiBaseUrl?.takeIf { BuildConfig.DEBUG } ?: BuildConfig.API_BASE_URL
        sessionManager = SessionManager(context.applicationContext)

        val authInterceptor = okhttp3.Interceptor { chain ->
            val token = runBlocking { sessionManager.currentToken() }
            val request = chain.request().newBuilder()
                .header("Accept", "application/json")
                .apply {
                    if (!token.isNullOrBlank()) {
                        header("Authorization", "Bearer $token")
                    }
                }
                .build()
            val response = chain.proceed(request)
            // Un 401 con token en una llamada que no es de acceso significa sesión vencida o cuenta
            // inexistente: se avisa para que la app cierre la sesión y vuelva al acceso. Las llamadas de
            // login/registro/recuperación devuelven 401 por credenciales malas y no cuentan.
            val path = request.url.encodedPath
            val isAccessCall = listOf("/auth/login", "/auth/register", "/auth/google", "/auth/forgot-password", "/auth/reset")
                .any { path.contains(it) }
            if (response.code == 401 && !token.isNullOrBlank() && !isAccessCall) {
                SessionEvents.notifyExpired()
            }
            response
        }

        val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
            } else {
                okhttp3.logging.HttpLoggingInterceptor.Level.NONE
            }
        }

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
        // Certificate pinning en staging/release; el debug queda libre para depurar con un proxy.
        if (!BuildConfig.DEBUG) {
            BuildConfig.API_BASE_URL.toHttpUrlOrNull()?.host
                ?.let { CertificatePins.forApiHost(it) }
                ?.let { clientBuilder.certificatePinner(it) }
        }
        val client = clientBuilder.build()

        api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UrbanBladeApi::class.java)

        authRepository = AuthRepository(api, sessionManager)
        urbanRepository = UrbanRepository(api)
    }
}
