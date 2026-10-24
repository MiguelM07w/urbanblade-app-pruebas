import java.util.Properties
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    // Cobertura (T088). El plugin de Android también lo aplica, pero declararlo aquí permite
    // configurar el agente de JaCoCo en las pruebas (ver tasks.withType<Test> más abajo).
    jacoco
}

// Notificaciones push (T142): el plugin de Google Services solo se aplica si existe
// app/google-services.json (se descarga de la consola de Firebase y no se versiona,
// igual que local.properties). Sin ese archivo la app compila y el push queda apagado.
// Crashlytics y Performance (26-sep) siguen la misma regla: sin el archivo no se aplican.
// La instalación sintética de auditoría no usa el proyecto Firebase de la app habitual.
if (file("google-services.json").exists() && providers.gradleProperty("UI_AUDIT").orNull != "true") {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
    apply(plugin = "com.google.firebase.firebase-perf")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) {
        file.inputStream().use(::load)
    }
}

val googleClientId = providers.gradleProperty("GOOGLE_CLIENT_ID").orNull
    ?: localProperties.getProperty("GOOGLE_CLIENT_ID")
    ?: "PENDIENTE_CONFIGURAR.apps.googleusercontent.com"

// Mismo mecanismo que GOOGLE_CLIENT_ID: la URL de producción no se inventa ni se
// versiona -- solo release la necesita, vía local.properties o -P para CI.
val releaseApiBaseUrl = providers.gradleProperty("RELEASE_API_BASE_URL").orNull
    ?: localProperties.getProperty("RELEASE_API_BASE_URL")
    ?: "https://PENDIENTE_CONFIGURAR.example/api/v1/"

// Prototipo académico (Camino A de docs/PUBLICAR_PLAY_STORE.md): permite que el AAB de Play lleve una clave de
// Stripe en modo de PRUEBA (pk_test_). Solo se activa de forma explícita con ALLOW_TEST_PAYMENTS=true
// (local.properties o -P); sin esa bandera la guarda de bundleRelease sigue exigiendo pk_live_.
val allowTestPayments = (providers.gradleProperty("ALLOW_TEST_PAYMENTS").orNull
    ?: localProperties.getProperty("ALLOW_TEST_PAYMENTS")) == "true"

// Firma de release (Play App Signing: esta es la *upload key*). El keystore vive FUERA del repo;
// se declara en local.properties (o -P) con UPLOAD_STORE_FILE, UPLOAD_STORE_PASSWORD,
// UPLOAD_KEY_ALIAS y UPLOAD_KEY_PASSWORD. Sin ellos el release sale sin firmar y falla la guarda.
fun signingProp(name: String): String? =
    providers.gradleProperty(name).orNull ?: localProperties.getProperty(name)
val uploadStoreFile = signingProp("UPLOAD_STORE_FILE")

// Staging en AWS (CloudFront, HTTPS) detrás del dominio propio. La URL no es un secreto; se puede
// sobreescribir con STAGING_API_BASE_URL (local.properties o -P) si cambia.
val stagingApiBaseUrl = providers.gradleProperty("STAGING_API_BASE_URL").orNull
    ?: localProperties.getProperty("STAGING_API_BASE_URL")
    ?: "https://api.urbanblade.com.mx/api/v1/"
// URL de la API para debug. Por defecto el emulador (10.0.2.2). En un celular fisico
// conectado por USB: DEBUG_API_BASE_URL=http://127.0.0.1:8000/api/v1/ en local.properties
// y ejecutar "adb reverse tcp:8000 tcp:8000" cada vez que conectes el celular.
val debugApiBaseUrl = providers.gradleProperty("DEBUG_API_BASE_URL").orNull
    ?: localProperties.getProperty("DEBUG_API_BASE_URL")
    ?: "http://10.0.2.2:8000/api/v1/"

// Publishable key de Stripe -- es pública por diseño (Stripe la espera embebida
// en apps cliente), pero igual se inyecta sin hardcodear: debe coincidir con la
// misma clave que ya usan barber/frontend-urban, nunca se inventa aquí.
val stripePublishableKey = providers.gradleProperty("STRIPE_PUBLISHABLE_KEY").orNull
    ?: localProperties.getProperty("STRIPE_PUBLISHABLE_KEY")
    ?: "pk_test_PENDIENTE_CONFIGURAR"

android {
    namespace = "com.urbanblade.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.urbanblade.mobile"
        minSdk = 26
        targetSdk = 36   // Play exige API 36 en apps nuevas desde 31-ago-2026
        versionCode = 3
        versionName = "3.0.0"

        // Credential Manager solicita un ID token para el mismo Web Client ID
        // que Laravel verifica. Se inyecta desde local.properties (ignorado por
        // Git) o -PGOOGLE_CLIENT_ID para CI; nunca se deja en código versionado.
        buildConfigField(
            "String",
            "GOOGLE_CLIENT_ID",
            "\"$googleClientId\""
        )

        // Checkout con Stripe (PaymentSheet) -- ver core/payment/UrbanPaymentSheet.kt.
        // Mismo mecanismo de inyección segura que GOOGLE_CLIENT_ID.
        buildConfigField(
            "String",
            "STRIPE_PUBLISHABLE_KEY",
            "\"$stripePublishableKey\""
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        if (uploadStoreFile != null) {
            create("upload") {
                storeFile = file(uploadStoreFile)
                storePassword = signingProp("UPLOAD_STORE_PASSWORD")
                keyAlias = signingProp("UPLOAD_KEY_ALIAS")
                keyPassword = signingProp("UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        // API_BASE_URL se define por variante, no en defaultConfig: debug solo habla
        // con el emulador (10.0.2.2, HTTP -- loopback, no es un secreto); release
        // exige HTTPS real, inyectada igual que GOOGLE_CLIENT_ID (nunca hardcodeada).
        debug {
            // Cobertura de las pruebas unitarias con JaCoCo (T088): crea la tarea
            // createDebugUnitTestCoverageReport, que corre testDebugUnitTest y deja el reporte
            // en app/build/reports/coverage/test/debug/ (index.html y report.xml). Solo mide las
            // pruebas de la JVM; no cambia el APK. JaCoCo ya viene con el plugin de Android.
            enableUnitTestCoverage = true
            // QA aislado: permite instalar junto a la app habitual sin tocar su sesión.
            if (providers.gradleProperty("UI_AUDIT").orNull == "true") applicationIdSuffix = ".audit"
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
            // R8: sin esto el APK de producción sale sin ofuscar ni reducir. Los DTO que Gson
            // llena por reflexión se conservan en proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfigs.findByName("upload")?.let { signingConfig = it }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // Build de prueba contra el staging de AWS: se instala igual que debug (mismo
        // applicationId, así el login con Google sigue funcionando), pero habla HTTPS con
        // CloudFront. No usa el manifest de debug, así que NO permite HTTP en claro.
        create("staging") {
            initWith(getByName("debug"))
            buildConfigField("String", "API_BASE_URL", "\"$stagingApiBaseUrl\"")
            matchingFallbacks += listOf("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    // Las pruebas de componente (Compose UI Test sobre Robolectric, en la JVM) necesitan los recursos y el
    // manifiesto de la app para dibujar las pantallas como en el teléfono.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Guarda de publicación: el AAB que se sube a Google Play (bundleRelease) con valores PENDIENTE o clave
// de prueba de Stripe sale roto en producción, así que el build falla antes de generarlo. Solo aplica a
// bundleRelease: el CI corre assembleRelease (R8) sin claves ni keystore y debe seguir compilando.
gradle.taskGraph.whenReady {
    if (allTasks.any { it.project == project && it.name.startsWith("bundle") && it.name.endsWith("Release") }) {
        val problemas = buildList {
            if (releaseApiBaseUrl.contains("PENDIENTE") || !releaseApiBaseUrl.startsWith("https://")) add("RELEASE_API_BASE_URL (debe ser https real)")
            if (googleClientId.contains("PENDIENTE")) add("GOOGLE_CLIENT_ID")
            val stripeEnPrueba = allowTestPayments && stripePublishableKey.startsWith("pk_test_") &&
                !stripePublishableKey.contains("PENDIENTE")
            if (!stripePublishableKey.startsWith("pk_live_") && !stripeEnPrueba) {
                add("STRIPE_PUBLISHABLE_KEY (debe ser pk_live_; para el prototipo académico, una pk_test_ real y ALLOW_TEST_PAYMENTS=true)")
            }
            if (stripeEnPrueba) {
                logger.warn("AVISO: este AAB lleva Stripe en MODO DE PRUEBA (ALLOW_TEST_PAYMENTS=true): no cobra dinero real. " +
                    "Solo para prueba cerrada o prototipo académico; la ficha de Play debe decirlo.")
            }
            if (uploadStoreFile == null) add("UPLOAD_STORE_FILE / contraseñas del keystore de subida")
            if (!file("google-services.json").exists()) add("app/google-services.json")
        }
        if (problemas.isNotEmpty()) {
            throw GradleException("Release no publicable, falta configurar: " + problemas.joinToString(", "))
        }
    }
}

// `kotlinOptions { jvmTarget = "17" }` dejó de compilar con Kotlin 2.2+ ("Using 'jvmTarget:
// String' is an error"); compilerOptions es el DSL vigente y funciona también con 2.1.10.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Cobertura de las pruebas de pantallas (T088). Robolectric carga las clases de la app con su
// propio class loader y sin ubicación de archivo; JaCoCo ignora esas clases por defecto, así que
// las pantallas probadas con Compose UI Test (p. ej. BookingScreen, T091) salían con 0 %.
// jdk.internal.* se excluye porque JaCoCo no puede instrumentar esas clases del JDK.
tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.datastore:datastore-preferences:1.1.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")                              // miniatura (primer cuadro) de los videos del muro

    // Reproductor de los videos del muro de los barberos. 1.11.x compila con compileSdk 36 (verificado).
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")

    // Pulido visual (2026-09-18). Versiones fijadas a las compatibles con Kotlin 2.1.10,
    // compileSdk 35 y AGP 8.8.2; subirlas exige subir esos tres a la vez.
    implementation("dev.chrisbanes.haze:haze:1.6.10")                       // desenfoque tipo cristal
    implementation("com.valentinilk.shimmer:compose-shimmer:1.5.0")         // esqueletos de carga
    implementation("com.airbnb.android:lottie-compose:6.7.1")              // animaciones

    // Login con Google nativo (Credential Manager) -- ver core/auth/GoogleAuthHelper.kt
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Checkout con Stripe (PaymentSheet) -- ver core/payment/UrbanPaymentSheet.kt.
    // 23.x jala transitivos de AndroidX que exigen compileSdk 36 + AGP 9.x; el proyecto ya
    // está en compileSdk 36 / AGP 9.4.1. En 23.x PaymentSheet.Colors dejó de ser data class
    // (sin copy()), por eso UrbanPaymentSheet arma la paleta con el constructor.
    implementation("com.stripe:stripe-android:23.23.0")

    // Notificaciones push con Firebase Cloud Messaging (T142) -- ver core/push/.
    // Proyecto Firebase barber-c6b3a (26-sep): además del push, Analytics (embudo de reserva,
    // sin datos personales), Crashlytics (fallos en dispositivo), Performance (arranque y
    // latencia de la API) y Remote Config (interruptores sin publicar otro APK).
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-perf")
    implementation("com.google.firebase:firebase-config")

    // Pruebas JVM de ViewModels/contrato -- ver app/src/test
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    // Certificados falsos para probar el certificate pinning (CertificatePinsTest).
    testImplementation("com.squareup.okhttp3:okhttp-tls:4.12.0")
    // Mockito 5.x mockea AuthRepository (clase final) sin necesitar un
    // SessionManager real (que exige un Context de Android/DataStore no
    // disponible en pruebas JVM puras).
    testImplementation("org.mockito:mockito-core:5.24.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    // Pruebas de componente de las pantallas (Compose UI Test) en la JVM, sin emulador: Robolectric.
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("org.robolectric:robolectric:4.17")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
