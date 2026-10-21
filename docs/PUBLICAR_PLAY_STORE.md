# Publicar UrbanBlade en Google Play — guía y estado

Costo: **$25 USD**, pago único (cuenta de Play Console). Sin comisión de Google: los cobros son servicios y productos
físicos vía Stripe, no Play Billing. Todo el material para la ficha, las declaraciones y los scripts está en
[`docs/play-store/`](play-store/README.md); aquí está el estado y el orden de los pasos.

> **Estado al 2026-10-21: la app está en prueba cerrada.** Cuenta personal de Play Console creada el 2026-10-19 y con identidad y
> teléfono verificados. Hecho en la consola: app creada, AAB `3.0.0` (versionCode 3) en prueba interna, ficha completa con
> capturas, todas las declaraciones, seguridad de los datos y la prueba cerrada **aprobada y activa** (México, lista de testers
> en la consola). Probado en un teléfono real instalando desde Play: login con correo y con Google funcionan.
> **Falta:** que 12 testers acepten y se mantengan 14 días seguidos, y luego solicitar producción (secciones 4 y 5).
>
> Bitácora de lo que pide la consola y sus trampas: [`play-store/BITACORA_CONSOLA.md`](play-store/BITACORA_CONSOLA.md).

## 0. Decisiones que cambian todo lo demás

### 0.1 ¿Stripe real o en modo de prueba?
Hoy no coinciden dos cosas del propio proyecto:

- El **Aviso de Privacidad** publicado (`/privacidad`) dice que UrbanBlade es un proyecto académico, que no es una
  empresa constituida, que **no cobra dinero real** y que los pagos con tarjeta usan el modo de prueba de Stripe.
- La **guarda de release** (`bundleRelease`) exige `STRIPE_PUBLISHABLE_KEY=pk_live_...`.

Google exige que la ficha y la app digan la verdad sobre lo que hacen. Hay dos caminos coherentes:

| | Camino A — prototipo académico (recomendado ahora) | Camino B — producción real |
| --- | --- | --- |
| Stripe | Modo de prueba (`pk_test_`) | Cuenta Stripe activada (`pk_live_`), con datos de negocio o de persona que Stripe verifica |
| Pista de Play | **Prueba cerrada** (y, si se decide, producción con la descripción clara) | Producción |
| Ficha | Dice explícitamente "proyecto académico, pagos simulados" | Sin esa advertencia |
| Aviso de privacidad y términos | Ya son coherentes | Hay que reescribirlos (quitar "académico / no cobra dinero real") |
| Backend | Staging en AWS | Producción (hoy solo existe staging; ver T063) |
| Cambio de código | Interruptor explícito `ALLOW_TEST_PAYMENTS=true` (**ya implementado**, ver abajo) | Ninguno |

**Decisión del 2026-10-19: Camino A** ("lo más recomendado"). La prueba cerrada exige **12 testers durante 14 días**
(cuenta personal), así que el reloj corre aunque B se decida después. Si el equipo cambia de idea, basta con poner una
`pk_live_` y quitar la bandera.

**Interruptor implementado** (`app/build.gradle.kts`): la guarda de `bundleRelease` sigue exigiendo `pk_live_` salvo que
`local.properties` (o `-P`) tenga a la vez `ALLOW_TEST_PAYMENTS=true` y una `pk_test_` real. Con la bandera, el build muestra
el aviso "este AAB lleva Stripe en MODO DE PRUEBA". Verificado el 2026-10-19: sin bandera una `pk_test_` se bloquea con un
mensaje que explica el camino; con bandera el AAB se genera.

### 0.2 Tipo de cuenta de desarrollador
No hay empresa constituida, así que corresponde una cuenta **personal**: verificación de identidad con documento oficial,
el nombre real del titular puede mostrarse en la ficha y aplica el requisito de 12 testers / 14 días antes de pedir
producción.

### 0.3 Contenido de usuarios (comentarios del muro)
La app permite comentar publicaciones del muro y la política de Google para contenido generado por usuarios pide un
mecanismo **dentro de la app** para reportar y bloquear, y moderación. **Implementado** (backend barber#31, app
UrbanBladeMobile#40, web frontend_Urbanblade#27); mergeado, desplegado en staging y probado en el teléfono (reportar y bloquear funcionan). Detalle en
[`play-store/DECLARACIONES.md`](play-store/DECLARACIONES.md) sección 6.

## 1. Ya resuelto y verificado
- [x] `compileSdk` y `targetSdk` en **36** (Play lo exige en apps nuevas desde 31-ago-2026). AGP **9.4.1**, Gradle 9.6.0.
- [x] Firma de release por variables (`UPLOAD_*`), sin keystore dentro del repo.
- [x] **Guarda de release**: `bundleRelease` falla con un mensaje claro si falta algo de la sección 2. Verificado hoy: sin
  claves dice "Release no publicable, falta configurar: RELEASE_API_BASE_URL, STRIPE_PUBLISHABLE_KEY, UPLOAD_STORE_FILE…".
- [x] `bundleRelease` genera un AAB firmado de ~21 MB, con R8 y reducción de recursos activos. R8 produce el archivo de
  mapeo `app/build/outputs/mapping/release/mapping.txt`; tras subir el AAB, confirmar en Play Console que aparece el archivo
  de desofuscación y, si no, subirlo a mano (sin él, los reportes de fallos y ANR de Play Console salen ofuscados; Crashlytics sube su propio mapeo).
- [x] Paquete con **arm64-v8a, armeabi-v7a, x86 y x86_64** (Play exige 64 bits).
- [x] Manifiesto final de release: sin `debuggable`, sin tráfico en claro, sin ID de publicidad. Permisos finales:
  `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `VIBRATE`, `WAKE_LOCK`, `NFC` (de un SDK), `RECEIVE` de FCM e
  instalación referida (de Firebase/Play). Única exigencia de hardware: OpenGL ES 2.0.
- [x] Sin permisos sensibles: la galería usa el selector de fotos del sistema (no pide `READ_MEDIA_*`) y no hay cámara,
  ubicación, contactos ni micrófono.
- [x] Token de sesión excluido de backup y device-transfer; sesión cifrada y *certificate pinning* (PR #14).
- [x] Eliminar cuenta desde la app (Cuenta → Seguridad → Eliminar mi cuenta) y desde la web (Mi perfil → Zona de peligro).
- [x] Página de privacidad en la web: `https://urbanblade.com.mx/privacidad`.

## 2. Generar el AAB (ya hecho; sirve para versiones nuevas)
Crear el keystore de subida (respaldarlo en **2 lugares**; sin él no se puede actualizar la app). Hay un script que pide las
contraseñas sin mostrarlas y escribe `local.properties`:

```powershell
.\scripts\play\crear-keystore.ps1
```

Agregar a `local.properties` (no se versiona) lo que falte:

```
RELEASE_API_BASE_URL=https://api.urbanblade.com.mx/api/v1/
GOOGLE_CLIENT_ID=<Web Client ID de producción>
STRIPE_PUBLISHABLE_KEY=pk_test_...        # Camino A (la misma del build de debug); en el Camino B va pk_live_...
ALLOW_TEST_PAYMENTS=true                  # solo Camino A; en el Camino B se quita
UPLOAD_STORE_FILE=C:\ruta\fuera\del\repo\urbanblade-upload.jks
UPLOAD_STORE_PASSWORD=...
UPLOAD_KEY_ALIAS=upload
UPLOAD_KEY_PASSWORD=...
```

Generar el AAB (verifica todo antes y muestra ruta, tamaño y huella SHA-256):

```powershell
.\scripts\play\generar-aab.ps1
```

Equivale a `gradle :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.

## 3. Login con Google en producción (hecho)
La SHA-1 de Firma de la app de Play ya está registrada en Firebase y los clientes OAuth existen en Google Cloud; verificado desde la versión instalada de Play. Lo siguiente aplica a cada llave nueva:

Con Play App Signing, Google re-firma la app. Después de subir el primer AAB, copiar la huella **SHA-1 de "Firma de la
app"** (Play Console → Integridad de la app) y registrarla en Firebase (proyecto `barber-c6b3a`) y en el cliente Android de
Google Cloud. Sin esto el login con Google falla solo en la versión instalada desde Play. Registrar también la SHA-1 de
**tu llave de subida** (la imprime `crear-keystore.ps1`) para las pruebas con el AAB que tú firmas.

## 4. Orden recomendado y tiempos aproximados

| # | Paso | Quién | Nota |
| --- | --- | --- | --- |
| 1 | Crear cuenta de Play Console y pagar $25; verificar identidad. Guía paso a paso: [`play-store/CREAR_CUENTA.md`](play-store/CREAR_CUENTA.md) | Titular de la cuenta | **Empezar hoy**: la verificación puede tardar varios días. |
| 2 | ~~Decidir Camino A o B~~ — **hecho: Camino A** (sección 0.1) | Equipo | Define `pk_test` y el aviso en la ficha. |
| 3 | `crear-keystore.ps1` y respaldar el `.jks` | Titular | Sin esto no hay AAB firmado. |
| 4 | `generar-aab.ps1` | Titular (o la IA, sin ver contraseñas) | |
| 5 | Crear la app en Play Console y llenar la ficha | Titular | Textos listos en `play-store/FICHA.md`. |
| 6 | Seguridad de los datos, clasificación y declaraciones | Titular | Respuestas en `play-store/`. |
| 7 | Subir el AAB a **prueba interna** y registrar la SHA-1 de Google | Titular | Habilita el login con Google. |
| 8 | **Prueba cerrada**: 12 testers, 14 días seguidos | Equipo | Plan en `play-store/PRUEBA_CERRADA.md`. |
| 9 | Solicitar producción y esperar la revisión de Google | Titular | La revisión puede tardar días. |

Estado: pasos 1 a 7 hechos (2026-10-21) y el 8 en curso. Si los 12 testers aceptan pronto, los 14 días terminarían
hacia el 5 de noviembre y producción, con suerte, a mediados de noviembre: antes de la entrega final del 4 de diciembre, pero sin mucho margen si algo se rechaza.

## 5. Antes de enviar
- [ ] Rotar credenciales de `_seguridad/` (`barber/docs/PLAN_ROTACION_CREDENCIALES.md`).
- [x] Probar el AAB en un teléfono real (instalado desde Play: login con correo y Google OK). Sigue pendiente revisar pago y push durante la prueba cerrada.
- [ ] Subir `versionCode` en cada versión nueva (hoy 3; Play rechaza un `versionCode` repetido).
- [ ] Si el Camino es B: reescribir el Aviso de Privacidad y los Términos.
- [x] Resolver la política de contenido de usuarios (sección 0.3).
