# Contrato UI/UX de UrbanBlade Mobile

11-oct-2026 · TT32 / HT-12. Describe la implementación; la evidencia de ejecución se registra en `UI_UX_VALIDACION_2026-10-11.md`.

## Navegación y acceso

- Sin sesión: Bienvenida → Acceso/Registro/Recuperación. La bienvenida permite consultar servicios. Elegir servicio/barbero pide acceso y preserva la elección durante el recorrido, sin prometer guardar un borrador tras cerrar el proceso.
- Cliente exclusivo: Inicio / Reservar / Mis citas / Cuenta. Personal y cuentas con varios roles usan navegación de operación; ingeniero exclusivo conserva destinos de solo lectura.
- La experiencia se resuelve en `core/access/UserExperience`: administrador → recepción → barbero → ingeniero → cliente. Web y API siguen la misma prioridad, sin depender del orden de `roles`. Elegir experiencia no concede permisos; Laravel sigue validando cada acción.
- Beneficios, pedidos y facturas son secundarios de Cuenta; tienda y muro, de Inicio. Laravel valida permisos.
- Atrás superior, inferior y sistema de reserva regresan un paso; desde el paso de entrada salen. En registro, contraseña vuelve a datos antes de salir.
- Los datos no sensibles del formulario sobreviven a recreación; las contraseñas permanecen en memoria y se vuelven a escribir. Los mensajes de Google aparecen en el paso visible. La acción principal tiene pie con `imePadding`.
- Todos los métodos de acceso guardan token cifrado y resumen del usuario. Sin red se usa ese resumen con aviso; token antiguo sin resumen pide reintento. Un 401 limpia sesión. Reintentar no desmonta la navegación autenticada.

## Reserva y favorito

- Servicio → Horario → Extras → Pago. Disponibilidad, permisos y precios finales los valida el servidor.
- Favorito: `PUT profile/favorite-barber`; la respuesta determina el estado. `GET barbers` devuelve `es_favorito`; se preselecciona al reservar si no hay elección explícita y permite reservar con otro.
- Horarios: carga/error/reintento separados del estado vacío. Cambiar día/barbero cancela la consulta anterior.
- Productos y propina mantienen el contrato existente. Comprar paquetes envía ID, nunca un precio calculado.

## Beneficios

- Membresía recurrente: contratación/cancelación existentes.
- Paquetes: catálogo y `POST packages/stripe-intent`. Tarjeta regalo: monto elegido y destinatario opcional con `POST gift-cards/stripe-intent`; Laravel valida límites.
- Stripe recoge la tarjeta. El resultado de pago no declara el beneficio activo: el webhook lo confirma. Se explica la espera y se ofrece actualizar. Doble toque bloqueado; cancelar/fallar permite reintento.
- Sin Stripe configurado o con tarjetas apagadas por Remote Config se explica cómo comprar en recepción.
- Tarjeta de miembro: `GET dashboard/membership/card` con cliente autenticado. PDF validado compartido por `FileProvider` limitado a su carpeta; no se expone token ni URL.
- Carga parcial: conserva beneficios anteriores y avisa qué no pudo actualizarse. Una caída de red no equivale a no tener beneficios.

## Apariencia y límites

Los cuatro temas, mascotas, tipografía y kit mantienen la identidad web. Los fondos y acentos de marca permanecen; `GoldText` y los estados del tema claro tienen tonos legibles. Acceso usa papel claro en Libreta. La bienvenida compacta desplaza el texto y coloca la mascota debajo, sin superposición.

Bladebot invita a reservar al cliente y pregunta «¿Revisamos tu día?» al personal. Se oculta en Cuenta, Reservar y Citas para no cubrir formularios, precios ni acciones de citas. Sigue disponible desde Inicio y herramientas.

Mis citas usa `pago_resuelto` del servidor para indicar si el pago está resuelto, pendiente o desconocido. Una cita confirmada no implica por sí sola deuda ni pago completo. Más reutiliza `UrbanRowGroup` y `UrbanRow`, con los mismos tokens de todas las pantallas.

Se corrigen los ocho hallazgos de la auditoría y errores de carga descubiertos durante su implementación. No se añaden check-in QR, logística ni cupones. Los puntos existentes provienen de `dashboard.loyalty`, disponible actualmente; la revisión histórica de septiembre no describe todo el contrato actual.

Esta entrega no equivale a publicación en Google Play ni a certificación de pagos reales. Distinguir siempre pruebas JVM, UI con fixtures y flujos contra Laravel/Stripe.

## Pulido de componentes compartidos (12-oct-2026)

Cuenta y Más usan `UrbanActionRow` dentro de `UrbanRowGroup` para distinguir accesos
ordinarios de avisos que requieren atención. Se conservan las rutas y restricciones
de rol; las filas permiten título de dos líneas y descripción de tres.

`UrbanPillTabs`, compartido por Cuenta, Citas y módulos, ofrece objetivos táctiles de
al menos 48 dp. En menos de 300 dp disponibles o con fuente desde 130 % coloca el
icono arriba y permite envolver la etiqueta completa. La comprobación visual en el
teléfono de esta continuación queda aplazada a la reconexión solicitada por el usuario.

## Continuación del recorrido de reserva (12-oct-2026)

- Catálogo con carga, error y reintento, sin tratar un fallo como catálogo vacío.
- Horario vinculado a servicio, barbero y día de la última respuesta exitosa.
  Una elección restaurada no permite avanzar o reservar hasta volver a validarla.
- Cambiar servicio conserva el borrador y ajusta el límite de Atrás al paso visitado.
  Mientras se registra la reserva, no se cambia de paso.
- El total mostrado es estimado a partir del catálogo; Laravel confirma precios,
  beneficios y permisos. Los productos mantienen su pago aparte en el salón.
- Web aplica la misma experiencia de roles al asistente y permite reintentar
  disponibilidad sin perder servicio/barbero. No cambian rutas ni payloads.
