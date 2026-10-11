---
name: urbanblade-mobile-android
description: Convenciones de desarrollo y diseño para UrbanBladeMobile (Android nativo, Kotlin + Jetpack Compose). Consultar antes de tocar pantallas, ViewModels, Repositories, el cliente Retrofit, `build.gradle.kts` o cualquier flujo que involucre precios, stock, pagos o el token de sesión.
---

# UrbanBlade Mobile — Android nativo

Este repo es la app Android nativa de UrbanBlade, reemplazo del antiguo proyecto Expo
(`mobil`, ahora archivado en `_archivados/mobil` dentro de la carpeta padre). Consume el
mismo contrato `/api/v1` que `frontend-urban` (Nuxt) expone `barber` (Laravel 13 +
MongoDB), vía Bearer token (`mobile_api_tokens`, no Sanctum).

Ver `docs/ARQUITECTURA.md` para el diagrama de capas y `README.md` para el estado de
cada rol/feature. Este skill cubre lo que no es obvio solo leyendo el código.

## Trabajo en equipo — repo compartido

Un compañero desarrolla activamente en este repo en paralelo. Antes de tocar cualquier
archivo:
- `git status` primero — no asumir que el working tree está limpio.
- Si hay cambios sin commitear que no son tuyos, no los descartes ni los pises; edita
  alrededor o pregunta.
- Cambios de configuración local (`app/build.gradle.kts` → `API_BASE_URL`,
  `local.properties`) son fáciles de chocar con lo que el compañero también está
  ajustando — confirma antes de cambiarlos si no es evidente que el valor actual está
  mal (ej. una IP LAN vieja que no sirve para el emulador).

## Stack y capas (resumen — ver ARQUITECTURA.md para el detalle completo)

Kotlin, Jetpack Compose + Material 3, MVVM, Navigation Compose, Retrofit + OkHttp +
Gson, DataStore (Bearer token), Coil (imágenes).

`Screens → ViewModels (StateFlow) → Repositories → UrbanBladeApi (Retrofit) → Laravel`.
Laravel sigue siendo la autoridad de precio, stock, disponibilidad y permisos — igual
que en `barber` y `frontend-urban`. Ninguna pantalla debe calcular o confiar en un
precio/total que no venga de una respuesta del backend.

## Reglas de seguridad heredadas de barber/frontend-urban

Estas ya son requisitos del backend, pero repetirlas aquí evita que la app Android las
rompa por accidente:

- **Nunca envíes precio/monto calculado por la app.** El carrito de la tienda solo manda
  `product_id` + `cantidad` en el checkout (ver `docs/ARQUITECTURA.md`); Laravel
  recalcula precio y valida stock. Mismo criterio para cualquier pago: el monto se
  obtiene del servidor, nunca se construye en el cliente.
- **La disponibilidad de citas se consulta antes de reservar**
  (`GET availability/slots?...`) y el backend valida de nuevo al crear la cita — no
  asumas que un slot mostrado sigue libre sin la confirmación del servidor.
- **El token vive en DataStore**, nunca en `SharedPreferences` sin cifrar ni en logs. No
  agregues logging que imprima el header `Authorization` o el token completo, ni
  siquiera en builds debug.
- **No hay claves de Stripe secretas en el proyecto** — solo la clave publicable, cuando
  se integre el SDK oficial. Si algún día se agrega, va en `local.properties` o
  `BuildConfig` generado desde variables de entorno de CI, nunca hardcodeado ni
  commiteado.
- **`cleartext traffic`** (HTTP sin TLS) es aceptable solo para desarrollo local
  (`10.0.2.2` en emulador, LAN en dispositivo físico). Antes de cualquier build de
  producción, seguir la lista de `docs/ARQUITECTURA.md` (HTTPS real, desactivar
  cleartext en el manifest, certificado de dominio real).

## `API_BASE_URL` — dónde vive y qué valor usar

`app/build.gradle.kts` → `buildTypes`, un valor por variante (no se edita a mano):
- `debug`: `DEBUG_API_BASE_URL` (local.properties o `-P`); por defecto
  `http://10.0.2.2:8000/api/v1/`, que solo funciona en el emulador.
- Celular físico por USB: `DEBUG_API_BASE_URL=http://127.0.0.1:8000/api/v1/` +
  `adb reverse tcp:8000 tcp:8000` (se pierde al desconectar el cable). Por Wi-Fi: la IP
  LAN real de la máquina que corre `barber`, que cambia entre redes y personas.
- `staging`: CloudFront HTTPS (`STAGING_API_BASE_URL`); única opción para probar pagos
  con tarjeta, porque el webhook de Stripe solo llega a staging.
- `release`: `RELEASE_API_BASE_URL`, siempre HTTPS.
- El backend (`barber`) debe estar arriba en el puerto 8000
  (`docker compose up -d` desde ese repo) antes de que la app pueda conectar,
  independientemente de que la URL esté bien.
- Síntoma de URL equivocada: `SocketTimeoutException: failed to connect to /10.0.2.2`
  en `adb logcat --pid=<pid de com.urbanblade.mobile>`. Si el login con Google falla
  antes de llegar a la API, revisar el cliente OAuth Android y la SHA-1 de quien compila
  (`barber/docs/GOOGLE_CLOUD_OAUTH.md`).
- `adb` no está en el PATH en Windows: usar
  `"$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"`.

## Antes de correr el build

- Si Android Studio reporta "insufficient memory" / el daemon de Gradle truena con
  `OutOfMemoryError` durante el build, no es un bug del proyecto — es RAM del sistema
  agotada (revisar `Memoria física disponible` con `systeminfo` en PowerShell). Cerrar
  apps pesadas y reintentar antes de tocar `gradle.properties`.
- Si el emulador ya estaba corriendo de un intento previo y Run falla con
  "already running as process N" o el dispositivo aparece `unauthorized` en
  `adb devices`, reiniciar el servidor adb (`adb kill-server && adb start-server`)
  suele resolverlo sin tener que cerrar el emulador.

## Herramientas recomendadas (Android Studio + librerías de referencia)

Ver `docs/HERRAMIENTAS.md` para la lista investigada de plugins de Android Studio y
repos de referencia de componentes Compose/Material 3. Son sugerencias para instalar
manualmente desde la IDE o revisar antes de traer código — no vienen preinstaladas ni
integradas al proyecto.

## Cuando se implemente una pantalla/feature nueva

1. Confirmar el endpoint real contra `barber` (`routes/api.php` /
   `app/Http/Controllers/Api/**`) antes de asumir forma de payload/respuesta — el
   contrato es el mismo que consume `frontend-urban`, y ese repo es buena referencia de
   UX ya validada para el mismo flujo.
2. Seguir el patrón de capas existente (Screen → ViewModel → Repository → Api), no
   llamar Retrofit directo desde un Composable.
3. Loading/error/vacío explícitos por pantalla — no dejar un estado sin manejar.
4. Si el flujo toca dinero, stock o permisos por rol, releer la sección de seguridad de
   arriba antes de escribir el ViewModel.
5. Para el diseño (encabezado, tarjeta héroe, estados con mascotas, formularios, Mi
   cuenta) seguir `.Codex/skills/urbanblade-mobile-ui-patrones/SKILL.md`: reutilizar
   sus piezas antes de crear componentes nuevos.
