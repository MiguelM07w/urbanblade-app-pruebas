<div align="center">

<img src="https://raw.githubusercontent.com/KikeGonRam/frontend_Urbanblade/main/docs/assets/landing.png" alt="UrbanBlade" width="100%"/>

<br/>

# ✂️ UrbanBlade Mobile

### La experiencia UrbanBlade, ahora en Android

**Kotlin · Jetpack Compose · MVVM · Retrofit · Laravel API**

<br/>

[![Android](https://img.shields.io/badge/Android-Native-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Laravel](https://img.shields.io/badge/API-Laravel_13-FF2D20?style=for-the-badge&logo=laravel&logoColor=white)](https://laravel.com/)
[![MVVM](https://img.shields.io/badge/Arquitectura-MVVM-D4AF37?style=for-the-badge)](#-arquitectura)
[![Status](https://img.shields.io/badge/Estado-En_desarrollo-C1703D?style=for-the-badge)](#-estado-del-proyecto)

<br/>

[**🌐 Frontend Web**](https://github.com/KikeGonRam/frontend_Urbanblade)
&nbsp;&nbsp;•&nbsp;&nbsp;
[**⚙️ Backend API**](https://github.com/KikeGonRam/barber)
&nbsp;&nbsp;•&nbsp;&nbsp;
**📱 Android**

</div>

---

## ✨ Identidad visual UrbanBlade

La aplicación móvil toma como referencia el mismo lenguaje visual del frontend web: superficies oscuras, acentos metálicos, tarjetas premium, jerarquía clara y una estética inspirada en barbería clásica contemporánea.

<table>
<tr>
<td align="center" width="25%">

### 🌙 Sastrería Nocturna
![Noir](https://img.shields.io/badge/NEGRO-0A0A0A?style=for-the-badge&labelColor=161616)
![Oro](https://img.shields.io/badge/ORO-D4AF37?style=for-the-badge&labelColor=D4AF37)

**Negro + oro**

Elegante, sobrio y premium.

</td>
<td align="center" width="25%">

### ⚙️ Taller de Acero
![Grafito](https://img.shields.io/badge/GRAFITO-111317?style=for-the-badge&labelColor=1A1D22)
![Cobre](https://img.shields.io/badge/COBRE-C1703D?style=for-the-badge&labelColor=C1703D)

**Grafito + cobre**

Industrial y moderno.

</td>
<td align="center" width="25%">

### 🎩 Salón Inglés
![Verde](https://img.shields.io/badge/VERDE-0B1210?style=for-the-badge&labelColor=141C19)
![Latón](https://img.shields.io/badge/LATÓN-C9A24A?style=for-the-badge&labelColor=C9A24A)

**Verde inglés + latón**

Clásico y distinguido.

</td>
<td align="center" width="25%">

### 📒 Libreta de Barbero
![Marfil](https://img.shields.io/badge/MARFIL-F3EDE0?style=for-the-badge&labelColor=FFF8E8)
![Oro](https://img.shields.io/badge/ORO-B8860B?style=for-the-badge&labelColor=B8860B)

**Marfil + tinta + oro**

Claro, editorial y artesanal.

</td>
</tr>
</table>

> La web de UrbanBlade maneja estos cuatro temas completos mediante tokens de color. La app Android mantiene la misma dirección visual y puede evolucionar hacia paridad temática completa.

---

## 📱 ¿Qué es UrbanBlade Mobile?

**UrbanBlade Mobile** es la aplicación Android nativa del ecosistema UrbanBlade. Consume directamente la API de Laravel y adapta su navegación, pantallas y acciones al rol autenticado.

La app busca llevar al teléfono la misma experiencia funcional que ya existe en web:

**reservas · agenda · tienda · pedidos · pagos · clientes · inventario · reportes · notificaciones · analítica**

> **Principio de seguridad:** Android presenta la interfaz; Laravel conserva la autoridad sobre permisos, precios, stock, estados y reglas de negocio.

---

## 🖼️ Referencia visual del ecosistema

<table>
<tr>
<td width="50%" align="center">
<img src="https://raw.githubusercontent.com/KikeGonRam/frontend_Urbanblade/main/docs/assets/login.png" alt="Login UrbanBlade" width="100%"/>
<br/>
<sub><b>Acceso UrbanBlade</b> — estética oscura, acentos dorados y jerarquía limpia.</sub>
</td>
<td width="50%" align="center">
<img src="https://raw.githubusercontent.com/KikeGonRam/frontend_Urbanblade/main/docs/assets/dashboard-admin.png" alt="Dashboard UrbanBlade" width="100%"/>
<br/>
<sub><b>Dashboard administrativo</b> — tarjetas, KPIs y navegación modular.</sub>
</td>
</tr>
<tr>
<td width="50%" align="center">
<img src="https://raw.githubusercontent.com/KikeGonRam/frontend_Urbanblade/main/docs/assets/dashboard-cliente.png" alt="Dashboard cliente" width="100%"/>
<br/>
<sub><b>Experiencia cliente</b> — próxima cita, membresía y acciones rápidas.</sub>
</td>
<td width="50%" align="center">
<img src="https://raw.githubusercontent.com/KikeGonRam/frontend_Urbanblade/main/docs/assets/booking-modal-cliente.png" alt="Reserva UrbanBlade" width="100%"/>
<br/>
<sub><b>Reserva</b> — disponibilidad real según barbero, servicio y fecha.</sub>
</td>
</tr>
</table>

---

## ⚡ Funcionalidades principales

| | Módulo | Funcionalidad |
|:--:|---|---|
| 🔐 | **Autenticación** | Login, registro, recuperación y sesión persistente |
| 🏠 | **Dashboard** | Contenido dinámico según rol |
| 📅 | **Citas** | Consulta, creación, disponibilidad y cancelación |
| ✂️ | **Catálogo** | Servicios y barberos |
| 🛍️ | **Tienda** | Productos, carrito y checkout |
| 📦 | **Pedidos** | Historial, cancelación y entrega |
| 💳 | **Pagos** | Facturas, historial y transferencias |
| 🔔 | **Notificaciones** | Consulta y marcado como leído |
| 👤 | **Perfil** | Datos personales y sesión |
| 👥 | **Clientes** | Gestión para recepción y administración |
| 📦 | **Inventario** | Productos, movimientos y alertas |
| 📊 | **Analítica** | KPIs, reportes e insights |
| 🤖 | **Bladebot** | Asistente del ecosistema UrbanBlade |

---

## 🧭 Experiencia por rol

<table>
<tr>
<td width="33%" valign="top">

### 👤 Cliente
- Reservar citas
- Ver disponibilidad
- Tienda y carrito
- Pedidos
- Pagos y facturas
- Perfil
- Notificaciones

</td>
<td width="33%" valign="top">

### 🧾 Recepcionista
- Agenda operativa
- Gestión de clientes
- Pedidos
- Cobros
- Transferencias
- Inventario
- Corte de caja

</td>
<td width="33%" valign="top">

### ✂️ Barbero
- Agenda personal
- Citas asignadas
- Perfil profesional
- Portafolio
- Horario de trabajo

</td>
</tr>
<tr>
<td width="50%" valign="top" colspan="2">

### 🛡️ Administrador
- Control operativo completo
- Usuarios y configuración
- Clientes e inventario
- Campañas y sorteos
- Reportes y logs
- Métricas e insights
- Estado del sistema

</td>
<td width="50%" valign="top">

### 🧑‍💻 Ingeniero
- Solo lectura
- Reportes
- Logs
- Métricas
- Insights
- Estado del sistema

</td>
</tr>
</table>

> La interfaz oculta o muestra acciones según el rol, pero **Laravel siempre realiza la autorización real**.

---

## 🧰 Stack tecnológico

<div align="center">

| Capa | Tecnología |
|---|---|
| 📱 **Aplicación** | Kotlin + Jetpack Compose |
| 🎨 **UI** | Material 3 |
| 🧭 **Navegación** | Navigation Compose |
| 🏗️ **Arquitectura** | MVVM |
| 🌐 **Networking** | Retrofit + OkHttp |
| 🔐 **Sesión** | DataStore + Bearer Token |
| 🖼️ **Imágenes** | Coil |
| ⚙️ **Backend** | Laravel 13 |
| 🗄️ **Datos** | MongoDB + Redis |
| 💳 **Pagos** | Stripe |

</div>

---

## 🏗️ Arquitectura

```text
┌─────────────────────────────────────┐
│          URBANBLADE MOBILE          │
│        Jetpack Compose UI           │
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│             ViewModels              │
│       Estado + lógica de UI         │
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│            Repositories             │
│      Acceso y coordinación data     │
└──────────────┬──────────────┬───────┘
               │              │
               ▼              ▼
      ┌──────────────┐  ┌───────────────┐
      │  DataStore   │  │ Retrofit +    │
      │ Bearer Token │  │    OkHttp     │
      └──────────────┘  └───────┬───────┘
                                │
                                ▼
                    ┌─────────────────────┐
                    │ Laravel API /api/v1 │
                    └──────────┬──────────┘
                               │
                 ┌─────────────┼─────────────┐
                 ▼             ▼             ▼
             MongoDB         Redis         Stripe
```

---

## 🚀 Puesta en marcha

### 1. Requisitos

Necesitas:

- **Android Studio**
- **Android SDK API 35**
- JDK compatible con Android Studio/Gradle
- **Docker Desktop**
- Backend `barber`
- Emulador Android o dispositivo físico

### 2. Abrir la app

```text
Android Studio
→ File
→ Open
→ UrbanBladeMobile
```

Espera la sincronización de Gradle.

```text
BUILD SUCCESSFUL
```

### 3. Levantar el backend

Desde el repositorio `barber`:

```powershell
docker compose up -d --build
```

Prueba:

```text
http://localhost:8000/api/v1/services
```

---

## 🌐 Conexión con la API

La URL se encuentra en:

```text
app/build.gradle.kts
```

`API_BASE_URL` ahora se define por variante dentro de `buildTypes` (antes vivía en
`defaultConfig`, compartido sin querer entre debug y release):

### ☁️ Staging en AWS (HTTPS)

El build type `staging` apunta al backend desplegado en AWS (CloudFront, HTTPS) para
probar la app contra datos reales de pruebas sin levantar Laravel local:

```powershell
# Android Studio: Build Variants -> app -> staging, o por línea de comandos:
gradle assembleStaging
```

- Usa el mismo `applicationId` que debug (el login con Google sigue funcionando) y **no**
  permite HTTP en claro: solo HTTPS.
- La URL viene de `STAGING_API_BASE_URL` (en `local.properties` o `-P`); por defecto
  `https://api.urbanblade.com.mx/api/v1/` (dominio propio desde el 23-sep-2026). Si vuelve a
  cambiar, solo hay que actualizar esa propiedad.
- Para pagar con tarjeta hace falta `STRIPE_PUBLISHABLE_KEY` de la cuenta Stripe que usa el
  staging, en `local.properties` (es una clave pública; nunca la secreta).
- Los servicios de staging se apagan para ahorrar costo: si la app responde 503, hay que
  encenderlos (ver `barber/docs/DESPLIEGUE_AWS_STAGING.md`, sección Runbook).
- El login con Google en staging requiere que la URI de redirección de la API esté
  registrada en Google Cloud (`barber/docs/GOOGLE_CLOUD_OAUTH.md`).

### 🖥️ Emulador

La variante `debug` apunta por defecto a `http://10.0.2.2:8000/api/v1/`, que dentro
del emulador de Android Studio es el `localhost` de tu computadora. Solo necesitas
tener `barber` arriba (`docker compose up -d` en su carpeta).

### Google Sign-In nativo

Antes de ejecutar la app, agrega el mismo **Web Client ID** que Laravel usa
para verificar tokens a tu `local.properties` (ese archivo está ignorado por
Git):

```properties
GOOGLE_CLIENT_ID=tu-web-client-id.apps.googleusercontent.com
```

También puede inyectarse en CI con `-PGOOGLE_CLIENT_ID=...`. No copies
`GOOGLE_CLIENT_SECRET` a Android: el secreto pertenece exclusivamente al
backend.

Además, en Google Cloud (mismo proyecto que el cliente web) debe existir un
**ID de cliente de OAuth de tipo Android** con el paquete `com.urbanblade.mobile`
y la **SHA-1** de tu llave de firma. Cada integrante agrega la suya:

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" '-J-Duser.language=en' -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android | Select-String "SHA1"
```

(`'-J-Duser.language=en'` entre comillas evita un fallo de `keytool` en español;
también puedes usar Gradle → app → Tasks → android → `signingReport`.)
Guía completa: `barber/docs/GOOGLE_CLOUD_OAUTH.md`.

| Mensaje en la app | Causa | Solución |
|---|---|---|
| "No pudimos abrir Google…" | Falta el cliente OAuth Android o la SHA-1 no coincide | Registrar el cliente Android con tu SHA-1 |
| "El token de Google no es válido." | `GOOGLE_CLIENT_ID` distinto al cliente web de `barber` | Usar el mismo ID web que `barber/.env` |
| Se queda cargando; el log dice `failed to connect to /10.0.2.2` | Build `debug` en un celular físico | Ver "Dispositivo físico" |

### 📱 Dispositivo físico

`10.0.2.2` solo existe en el emulador. En un celular físico la variante `debug`
usa la URL de `DEBUG_API_BASE_URL` (en `local.properties` o `-P`).

**Por cable USB (recomendado):** no depende de la Wi-Fi ni de la IP de tu equipo.

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
Add-Content -Path local.properties -Value "DEBUG_API_BASE_URL=http://127.0.0.1:8000/api/v1/"
& $adb devices                    # el celular debe aparecer como "device"
& $adb reverse tcp:8000 tcp:8000  # repetir cada vez que conectes el celular
& $adb reverse --list
```

Después: `docker compose up -d` en `barber`, **Sync Gradle**, variante `debug` y ▶ Run.

**Por Wi-Fi:** celular y computadora en la misma red; toma la IPv4 de `ipconfig` y usa
`DEBUG_API_BASE_URL=http://TU_IP:8000/api/v1/`. Verifica desde Chrome del celular
`http://TU_IP:8000/api/v1/services`.

**Contra AWS:** variante `staging` (ver arriba). Es la opción para probar pagos con
tarjeta, porque el webhook de Stripe solo llega a staging.

Para ver el error real de la app en el celular:

```powershell
& $adb logcat -c
# ...reproduce el error en el celular y luego:
$appPid = (& $adb shell pidof com.urbanblade.mobile).Trim()
& $adb logcat -d --pid=$appPid | Select-Object -Last 40
```

### 🚀 Release

Los builds release **no** usan `10.0.2.2` ni HTTP: exigen una URL HTTPS real, que se
inyecta igual que `GOOGLE_CLIENT_ID` (nunca hardcodeada en el repo). Agrega a tu
`local.properties`:

```properties
RELEASE_API_BASE_URL=https://tu-dominio-real.example/api/v1/
```

O para CI: `-PRELEASE_API_BASE_URL=...`. Sin este valor, un build release compila con
un placeholder obviamente inválido (`https://PENDIENTE_CONFIGURAR.example/api/v1/`)
que falla en tiempo de ejecución en vez de apuntar accidentalmente al emulador o a
HTTP sin cifrar. El cleartext (`usesCleartextTraffic`) también quedó scoped solo a
debug (`app/src/debug/AndroidManifest.xml`) — release usa el valor seguro por defecto
de Android (`false`).

### 💳 Checkout con Stripe

El pago de citas (tarjeta vía `PaymentSheet`) necesita la misma **publishable key**
que ya usan `barber`/`frontend-urban`. Es una clave pública por diseño de Stripe (no
un secreto), pero igual se inyecta sin hardcodear, para no tener que tocar código
versionado si cambia:

```properties
STRIPE_PUBLISHABLE_KEY=pk_test_tu-clave-publica-aqui
```

O para CI: `-PSTRIPE_PUBLISHABLE_KEY=...`. Sin este valor, el build compila con un
placeholder obviamente inválido (`pk_test_PENDIENTE_CONFIGURAR`) que Stripe rechaza en
tiempo de ejecución en vez de fallar en silencio.

---

## 🔐 Flujo de autenticación

```text
┌──────────────┐
│    Login     │
└──────┬───────┘
       │ POST /auth/login
       ▼
┌──────────────┐
│ Bearer Token │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  DataStore   │
└──────┬───────┘
       │
       ▼
Authorization: Bearer <token>
```

El interceptor de OkHttp agrega el token automáticamente en las peticiones protegidas.

---

## 🛡️ Seguridad

- 🔒 Las contraseñas **no se almacenan** en Android.
- 🔑 El token se persiste mediante **DataStore**.
- 🧱 Laravel valida nuevamente cada permiso.
- 💰 Android no decide precios finales.
- 📦 Laravel valida stock durante el checkout.
- 📅 La disponibilidad se consulta antes de reservar.
- 💳 Las claves secretas de Stripe viven solamente en servidor.
- 🌐 Producción debe utilizar **HTTPS**.
- 👁️ Ocultar un botón en Android nunca sustituye una autorización del backend.

---

## 💳 Stripe

La aplicación contempla:

```text
POST /api/v1/payments/stripe-intent
```

El backend calcula el monto real antes de crear el `PaymentIntent`.

El SDK de Stripe ya está integrado con una **Publishable Key**. Reserva, membresía y
compras de paquetes/tarjetas regalo usan la API para crear los intentos; el webhook del
servidor confirma el resultado. Ver [contrato móvil](docs/UI_UX_CONTRATO_MOVIL.md) y
[evidencia de validación](docs/UI_UX_VALIDACION_2026-10-11.md).

---

## 📂 Estructura del proyecto

```text
UrbanBladeMobile/
│
├── app/
│   └── src/main/java/com/urbanblade/mobile/
│       ├── data/
│       │   ├── api/
│       │   ├── model/
│       │   ├── repository/
│       │   └── session/
│       │
│       ├── ui/
│       │   ├── navigation/
│       │   ├── screens/
│       │   ├── theme/
│       │   └── viewmodel/
│       │
│       ├── MainActivity.kt
│       └── UrbanBladeApplication.kt
│
├── docs/
│   └── ARQUITECTURA.md
│
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🔗 Ecosistema UrbanBlade

<div align="center">

| Proyecto | Tecnología | Repositorio |
|---|---|---|
| 🌐 **UrbanBlade Web** | Nuxt 4 + Vue + Tailwind | [frontend_Urbanblade](https://github.com/KikeGonRam/frontend_Urbanblade) |
| ⚙️ **UrbanBlade API** | Laravel 13 + MongoDB | [barber](https://github.com/KikeGonRam/barber) |
| 📱 **UrbanBlade Mobile** | Kotlin + Compose | **Este proyecto** |

</div>

---

## 📌 Estado del proyecto

```text
╭──────────────────────────────────────────────────────────╮
│                  URBANBLADE MOBILE                       │
├──────────────────────────────────────────────────────────┤
│  ✅ Arquitectura Android base                            │
│  ✅ Autenticación Bearer Token                           │
│  ✅ Navegación por roles                                 │
│  ✅ Dashboard                                            │
│  ✅ Citas y disponibilidad                               │
│  ✅ Catálogo                                             │
│  ✅ Tienda y carrito                                     │
│  ✅ Pedidos                                              │
│  ✅ Pagos e historial                                    │
│  ✅ Perfil                                               │
│  ✅ Notificaciones                                       │
│  ✅ Clientes e inventario                                │
│  ✅ Analítica y reportes                                 │
│  ✅ Herramientas admin / ingeniero                       │
│  ✅ Stripe Android SDK integrado                          │
│  ✅ Push FCM integrado                                   │
│  🟡 Pruebas E2E móviles                                  │
│  🟡 Release firmado / Google Play                        │
╰──────────────────────────────────────────────────────────╯
```

---

## 🗺️ Roadmap

- [x] Cuatro temas y componentes compartidos; contraste adaptado al móvil
- [x] Stripe Android SDK (requiere configuración y validación de integración)
- [x] Firebase Cloud Messaging (requiere configuración del proyecto Firebase)
- [x] Pruebas unitarias JVM: resultado fechado en `docs/UI_UX_VALIDACION_2026-10-11.md`
- [ ] Pruebas instrumentadas (`app/src/androidTest/` está vacío)
- [ ] Soporte offline selectivo
- [ ] APK / AAB firmado
- [x] CI/CD Android (`.github/workflows/android-ci.yml`: lint + pruebas unitarias + build de debug)
- [ ] Publicación en Google Play

---

## ✅ Verificación

El sandbox del workspace corre las pruebas de los cuatro proyectos de una vez:

```powershell
cd ..\barber\scripts\verificacion
.\verificar.ps1 -Proyecto mobile
```

Ejecuta, en este orden: `gradle :app:lintDebug`, `:app:testDebugUnitTest` (suite JVM del repositorio) y `:app:assembleDebug`. Necesita el **Android SDK** y **Gradle 9.6.0** — el
repo no versiona `gradlew` a propósito, se usa la misma versión declarada en
`gradle-wrapper.properties`. Si falta alguno, el sandbox lo reporta como **omitido** con
el motivo, en vez de darlo por bueno.

> El sandbox avisa además de una trampa conocida: `local.properties` define `sdk.dir`,
> y ese archivo **no se versiona**. Sin él, Gradle no encuentra el SDK.

---

<div align="center">

<br/>

### ✂️ URBAN**BLADE**

**Corta menos pasos. Conecta toda la operación.**

`Web · API · Android`

<br/>

![Noir](https://img.shields.io/badge/Sastrería_Nocturna-0A0A0A?style=flat-square&labelColor=0A0A0A)
![Acero](https://img.shields.io/badge/Taller_de_Acero-C1703D?style=flat-square&labelColor=111317)
![Salon](https://img.shields.io/badge/Salón_Inglés-C9A24A?style=flat-square&labelColor=0B1210)
![Libreta](https://img.shields.io/badge/Libreta_de_Barbero-B8860B?style=flat-square&labelColor=F3EDE0)

<br/>

Desarrollado como parte del ecosistema **UrbanBlade**.

</div>
