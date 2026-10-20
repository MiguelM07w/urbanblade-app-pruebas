# Ficha de Google Play — textos listos para pegar

Los textos de abajo ya están medidos contra los límites de Play (nombre 30, descripción corta 80, descripción larga 4000
caracteres). Cada función mencionada existe en la app (comprobado en el código el 2026-10-19). Revisarlos con el equipo antes de
pegar: son la cara pública de la app.

## Datos básicos

| Campo | Valor |
| --- | --- |
| Nombre de la app (≤30) | `UrbanBlade` (10) |
| Descripción corta (≤80) | `Reserva tu corte, paga en la app y mira el trabajo de tu barbero.` (65) |
| Tipo | Aplicación (no juego) |
| Gratuita o de pago | **Gratuita** (no hay compras dentro de la app mediante Play Billing; ver `DECLARACIONES.md`) |
| Categoría | **Belleza** (alternativa: Estilo de vida) |
| Idioma predeterminado | Español (México) — `es-419` o `es-MX` según la lista de Play Console |
| Correo de contacto | `al222310427@gmail.com` (Play lo muestra públicamente; conviene un alias del equipo) |
| Sitio web | `https://urbanblade.com.mx` |
| Política de privacidad | `https://urbanblade.com.mx/privacidad` |
| Paquete (applicationId) | `com.urbanblade.mobile` (no se puede cambiar después de publicar) |

## Aviso de proyecto académico (Camino A, el elegido el 2026-10-19)
Como se publica con Stripe en modo de prueba (ver `../PUBLICAR_PLAY_STORE.md`, sección 0.1), esta línea es **obligatoria** y va
**al inicio** de la descripción larga para que la ficha diga la verdad:

```
UrbanBlade es un proyecto académico del Equipo UrbanBlade. Los pagos con tarjeta funcionan en el modo de prueba de Stripe: no se cobra dinero real.
```

Si el equipo elige el Camino B (cobros reales), **no** va ese aviso, pero entonces hay que reescribir antes el Aviso de
Privacidad y los Términos, que hoy dicen exactamente lo contrario.

## Descripción larga (1318 caracteres; con el aviso, 1467)

```
Reserva tu corte en menos de un minuto y lleva el control de tus citas desde el teléfono.

RESERVA EN 4 PASOS
• Elige el servicio y consulta su precio y duración reales.
• Escoge a tu barbero, el día y un horario libre de verdad.
• Agrega productos opcionales y notas para tu barbero.
• Paga en efectivo en el salón, por transferencia SPEI o con tarjeta.

TUS CITAS, BAJO CONTROL
• Consulta tus próximas citas y tu historial.
• Reagenda o cancela cuando lo necesites.
• Recibe avisos en tu teléfono cuando tu barbero confirma la cita y cuando se acerca la hora.

MURO DE INSPIRACIÓN
• Mira fotos y videos de los trabajos del equipo.
• Reacciona, guarda tus favoritos, comenta y reserva con el barbero que te gustó.

TIENDA
• Pide productos UrbanBlade desde la app y recógelos en la sucursal.
• Consulta tus pedidos y comprobantes.

BENEFICIOS
• Niveles, puntos y beneficios por cumpleaños para clientes frecuentes.

BLADEBOT
• Un asistente dentro de la app para resolver dudas rápidas.

PARA EL EQUIPO
• Los barberos, recepción y administración ven su propia agenda, cobros y reportes según su rol.

TU CUENTA, TUS DATOS
• Entra con tu correo o con Google.
• Elige entre cuatro temas visuales.
• Puedes eliminar tu cuenta cuando quieras desde la propia app.
• Aviso de privacidad: https://urbanblade.com.mx/privacidad
```

## Gráficos
Todos están en [`assets/`](assets/) y se generaron con el logo oficial (`urbanblade-mark.svg`).

| Elemento | Archivo | Requisito de Play | Estado |
| --- | --- | --- | --- |
| Ícono | `assets/icono-512.png` | 512×512, PNG, hasta 1024 KB | Listo (24 bits, sin transparencia) |
| Gráfico de funciones | `assets/grafico-funciones-1024x500.png` | 1024×500, PNG o JPEG sin transparencia | Listo |
| Capturas de teléfono | 5 PNG en `Documents\UrbanBlade\_play_store_assets\capturas\` (fuera del repo) | 2 a 8; lado menor ≥320 px; el lado largo ≤2× el corto | **Listas** (1080×2160); ver el aviso sobre la cuenta usada |

### Plan de capturas (6 recomendadas, en este orden)
1. **Inicio** — próxima visita y botón "Reservar cita".
2. **Reservar** — lista de servicios con precios.
3. **Horario** — paso 2: barbero, día y hora.
4. **Pago** — paso 4: efectivo, transferencia o tarjeta.
5. **Mis citas** — próximas, con reagendar y cancelar.
6. ~~**Muro de Inspiración**~~ — **no usar**: las publicaciones que hay hoy son videos y fotos de terceros con marca de agua de TikTok
   (`@juanchostylez`, `@crownagency_`) y textos de prueba. Publicarlas en la ficha es un riesgo de propiedad intelectual. Sustituirla
   por una publicación **propia** del equipo cuando exista.

Opcionales: Tienda y Cuenta. Las 5 capturas actuales se tomaron con la cuenta de uso real del equipo (aparece el nombre "Luis" y su
cita). **Para publicar conviene repetirlas con una cuenta demo** (nunca con datos de una persona real): basta correr de nuevo el
script con la cuenta demo iniciada. La pantalla del teléfono
mide 1080×2340 (2.17:1), más alargada de lo que Play acepta, por eso el script quita la barra de estado y la de gestos y
deja 1080×2160 (2:1):

```powershell
# Con el teléfono conectado por USB y la app abierta en la pantalla deseada:
.\scripts\play\capturar.ps1 -Nombre 01_inicio
```

Guarda en `Documents\UrbanBlade\_play_store_assets\capturas\` (fuera del repositorio).

## Antes de pegar
- [ ] El equipo aprobó nombre, descripciones y categoría.
- [ ] Se decidió el Camino A o B y la descripción refleja la decisión.
- [ ] Las capturas muestran solo datos de la cuenta demo.
