# Integración de módulo remoto: Campañas

## 1. Alcance y tarea asociada

Este documento justifica la implementación de la **T094 — Sincronizar y validar la
información compartida entre la app móvil, la plataforma web y la base de datos**.
La entidad elegida es **campañas de marketing**, disponible para el rol
Administrador. No se creó una aplicación nueva: se integró el módulo en
UrbanBlade Mobile, la aplicación Android existente.

El módulo permite:

- consultar el historial de campañas y el número de destinatarios por segmento;
- registrar una campaña para envío inmediato o programado;
- validar los datos obligatorios antes de enviar;
- mostrar carga, estado vacío, confirmación y errores del servicio.

## 2. Servicio remoto y contrato HTTP

La aplicación consume el backend Laravel del ecosistema UrbanBlade mediante Retrofit.
La URL se inyecta por variante de Gradle y no se guarda como credencial:

| Ambiente | Configuración | Transporte |
|---|---|---|
| Desarrollo en emulador | `DEBUG_API_BASE_URL` | HTTP local (`10.0.2.2`) |
| Staging | `STAGING_API_BASE_URL` | HTTPS |
| Producción | `RELEASE_API_BASE_URL` | HTTPS obligatorio |

Todas las rutas se resuelven debajo de `/api/v1/`.

| Operación | Método y endpoint | Respuesta usada |
|---|---|---|
| Consulta | `GET /api/v1/campaigns` | `levels`, `segment_counts` y `data` |
| Registro | `POST /api/v1/campaigns` | `message` de aceptación o error HTTP |

El `POST` envía un JSON con:

```json
{
  "titulo": "Regreso a clases",
  "cuerpo": "20% en cortes",
  "cta_label": "Reservar",
  "cta_url": "https://urbanblade.com.mx/reservar",
  "segmento": "vip",
  "modo": "ahora",
  "programada_para": null
}
```

Para una campaña programada, `modo` cambia a `programada` y
`programada_para` usa el formato `AAAA-MM-DD HH:mm`.

## 3. Funcionamiento técnico

La integración conserva la arquitectura existente:

1. [`UrbanBladeApi.kt`](../app/src/main/java/com/urbanblade/mobile/core/network/UrbanBladeApi.kt)
   declara las operaciones Retrofit `campaigns()` y `createCampaign(...)`.
2. [`Repositories.kt`](../app/src/main/java/com/urbanblade/mobile/data/repository/Repositories.kt)
   expone esas operaciones mediante `UrbanRepository`.
3. [`CampaignsViewModel.kt`](../app/src/main/java/com/urbanblade/mobile/ui/viewmodel/CampaignsViewModel.kt)
   administra `loading`, `sending`, `error`, `notice` y recarga el historial después de un
   registro aceptado.
4. [`CampaignsScreen.kt`](../app/src/main/java/com/urbanblade/mobile/ui/screens/CampaignsScreen.kt)
   muestra el GET, el estado vacío y el formulario POST.
5. [`CampaignForm.kt`](../app/src/main/java/com/urbanblade/mobile/ui/screens/CampaignForm.kt)
   concentra las reglas de validación para que el formulario no envíe datos incompletos.

### Validaciones del formulario

- `titulo`: obligatorio, máximo 150 caracteres.
- `cuerpo`: obligatorio, máximo 2,000 caracteres.
- `cta_label`: opcional, máximo 40 caracteres.
- `cta_url`: opcional; si se informa debe comenzar con `http://` o `https://` y no
  contener espacios.
- segmento: debe tener al menos un destinatario.
- programación: exige fecha y hora futuras.

La validación móvil mejora la experiencia, pero Laravel sigue siendo la autoridad y
puede responder `422`. Ese mensaje se conserva en pantalla; no se simula una
respuesta exitosa.

## 4. Tres medidas de seguridad

1. **HTTPS por ambiente:** staging y release usan URLs HTTPS configurables por
   `local.properties` o CI. El build `bundleRelease` rechaza una URL de producción
   pendiente o que no sea HTTPS.
2. **Credenciales fuera del código:** el token de sesión se obtiene al autenticarse,
   se guarda cifrado en DataStore y se agrega mediante el interceptor de OkHttp. No
   se imprime el header ni se incluye ningún token en el repositorio. Las claves de
   configuración se inyectan por propiedades locales/CI.
3. **Autorización y autoridad del backend:** Laravel valida el token, el rol
   Administrador, los segmentos y el contenido del POST. La app solo presenta la
   interfaz y no decide permisos ni convierte un error HTTP en éxito. En staging y
   release también se configura certificate pinning cuando el host tiene pins
   registrados.

La autenticación remota actual es Bearer sobre el token propio de UrbanBlade:
`POST /api/v1/auth/login` devuelve el token; el cliente lo envía en
`Authorization` en las siguientes solicitudes. El token nunca debe copiarse a
capturas, logs, documentación ni tickets.

## 5. Pruebas y validación

Pruebas automatizadas relevantes:

- [`CampaignFormTest.kt`](../app/src/test/java/com/urbanblade/mobile/ui/screens/CampaignFormTest.kt):
  campos obligatorios, límites, URL, segmento vacío y programación futura.
- [`CampaignsViewModelTest.kt`](../app/src/test/java/com/urbanblade/mobile/ui/viewmodel/CampaignsViewModelTest.kt):
  carga, aceptación del POST y error `422` sin cerrar el formulario.
- [`UrbanBladeApiContractTest.kt`](../app/src/test/java/com/urbanblade/mobile/core/network/UrbanBladeApiContractTest.kt):
  deserialización del GET y forma del JSON enviado por el POST mediante MockWebServer.

Ejecutar desde Android Studio (el repositorio aún no incluye `gradlew`):

1. Abrir el panel Gradle.
2. Ejecutar `app > Tasks > verification > testDebugUnitTest`.
3. Para validación manual, levantar el backend Laravel, iniciar sesión como
   Administrador y abrir **Más > Campañas**.
4. Confirmar que aparecen segmentos e historial.
5. Crear una campaña válida y confirmar el mensaje devuelto por el servidor.
6. Repetir con título vacío, mensaje vacío, URL inválida y segmento sin clientes;
   debe permanecer el formulario y mostrarse el error.
7. Simular desconexión o backend detenido; debe aparecer el mensaje de error de carga
   y no una lista de datos inventados.

## 6. Evidencias para la entrega

Las capturas se deben tomar durante la demostración con una cuenta de prueba, sin
mostrar correo completo, token, URLs privadas ni datos personales:

| Evidencia | Qué debe verse | Archivo sugerido |
|---|---|---|
| Consulta GET | Pantalla Campañas con conteos e historial cargados | `docs/evidencia/campanas-consulta.png` |
| Formulario POST | Formulario con título, mensaje, segmento y modo de envío | `docs/evidencia/campanas-formulario.png` |
| Resultado POST | Aviso de aceptación del servidor y campaña en el historial | `docs/evidencia/campanas-resultado.png` |
| Validación | Campo obligatorio o URL inválida con su mensaje | `docs/evidencia/campanas-validacion.png` |
| Error | Backend no disponible y mensaje de error visible | `docs/evidencia/campanas-error.png` |

Estas rutas son la lista de evidencias a capturar; no se declaran capturas como
realizadas hasta ejecutarlas en un emulador o dispositivo conectado al backend.

## 7. Guion breve de demostración

1. Explicar que Campañas es una pantalla existente del rol Administrador y que la
   información proviene de Laravel, no de datos de prueba locales.
2. Mostrar la consulta GET: conteos por segmento e historial.
3. Abrir **Nueva campaña**, dejar vacío un campo obligatorio y mostrar la validación.
4. Completar el formulario, elegir un segmento con destinatarios y enviar.
5. Mostrar el mensaje del servidor y la nueva consulta del historial.
6. Detener el backend o usar una URL no disponible para mostrar el manejo de error.
7. Cerrar explicando HTTPS, token en DataStore y autorización en Laravel.

## 8. Resultado

El módulo cubre la consulta JSON, el registro JSON, las validaciones, el manejo de
fallos y la integración segura con el servicio remoto sin crear una aplicación
paralela ni exponer credenciales.
