package com.urbanblade.mobile

import android.app.Application
import androidx.test.runner.AndroidJUnitRunner
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.e2e.FakeUrbanApi

/**
 * Runner de las pruebas instrumentadas (T145), declarado en app/build.gradle.kts.
 *
 * Android crea la app (UrbanBladeApplication.onCreate -> AppContainer.init) antes de correr la primera
 * prueba, así que la dirección de la API se cambia justo antes de ese momento: la app queda apuntando a
 * la API falsa del emulador (FakeUrbanApi) y nunca a la real.
 */
class UrbanTestRunner : AndroidJUnitRunner() {
    override fun callApplicationOnCreate(app: Application) {
        AppContainer.testApiBaseUrl = FakeUrbanApi.BASE_URL
        super.callApplicationOnCreate(app)
    }
}
