# Cierre de auditoría móvil — TT32 / HT-12

11-oct-2026. Rama `fix/tt32-cierre-auditoria-ui-ux`, creada desde `origin/main` (`c96ae418`). Trabajo aislado; los cambios existentes del checkout original no se incorporan.

## Correcciones

| Hallazgo | Implementación | Evidencia automatizada |
|---|---|---|
| UX-01: primer acceso sin resumen local | Login, registro y Google guardan usuario confirmado; sin caché se pide reintento; 401 limpia sesión | `AuthSessionAuditTest` |
| UX-02: registro pierde datos | Datos y consentimiento guardables; contraseñas solo en memoria | Revisión del estado Compose; ejecución visual se registra abajo |
| UX-03: error Google fuera del paso visible | Aviso independiente del paso; controles bloqueados durante petición | Revisión de `RegisterScreen` |
| UX-04: Atrás inconsistente | Barra, botón y sistema regresan al paso anterior | Revisión de registro/reserva |
| UX-05: contraste | `GoldText`, semánticos claros, fondo de acceso claro, selección de apariencia | `ThemeContrastTest`, cuatro temas ≥ 4.5:1 en superficies comprobadas |
| UX-06: paridad de autoservicio | Favorito, PDF autenticado, compras de paquetes/tarjetas regalo con PaymentSheet | `BenefitApiContractTest`, `BenefitPurchaseViewModelTest` |
| UX-07: contrato/documentación desalineados | Cuatro pestañas, catálogo invitado accesible, documentos y skills actuales | `UI_UX_CONTRATO_MOVIL.md`, checklist actual |
| UX-08: invitación Bladebot por rol | Mensaje cliente/personal y conservación del espacio inferior compartido | Revisión de navegación y componentes |

También se separan carga/error/vacío de horarios, se cancela la consulta anterior y se conservan beneficios previos en fallos parciales (`SlotsAuditTest`, `WalletViewModelTest`).

## Validación local

- `:app:testDebugUnitTest`: 317 casos, **316 aprobados, 0 fallos, 1 omitido**. El omitido es la consulta optativa de certificados del servicio real.
- `:app:lintDebug`: aprobado, **0 errores**. El informe registra 32 advertencias y 4 sugerencias (actualizaciones de SDK/dependencias y recomendaciones existentes de Compose/API); no se han ocultado ni convertido en errores de UI cerrados.
- `:app:assembleDebug`: aprobado. Variante QA `com.urbanblade.mobile.audit`, con API local por USB `http://127.0.0.1:18765/api/v1/`.
- `git diff --check`: sin errores de formato.

Comando reproducible (PowerShell, JDK de Android Studio y Gradle 9.6):

```powershell
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --max-workers=2 --console=plain '-Pkotlin.compiler.execution.strategy=in-process' '-PUI_AUDIT=true' '-PDEBUG_API_BASE_URL=http://127.0.0.1:18765/api/v1/'
adb -s <dispositivo-QA> reverse tcp:18765 tcp:18765
```

## Validación visual

API sintética: `scripts/ui-audit/fixture_server.py`. Solo escucha en localhost; no conecta con Laravel, Google ni Stripe. La variante QA tiene sesión separada. No se modificó la sesión de la app habitual del teléfono.

Dispositivo físico Samsung SM-S731B; instalación `.audit` y datos sintéticos. El emulador se usó para bienvenida, pero presentó inestabilidad de System UI; la aceptación visual se comprobó en el teléfono. Los cambios temporales de tamaño/fuente se restauraron a los valores originales (1080×2340, densidad 450, fuente 0.9).

| Recorrido ejecutado | Resultado |
|---|---|
| Invitado → seleccionar servicio → acceso QA | Retoma servicio en Horario (paso 2); cuatro pestañas del cliente |
| Registro / Google no disponible | Aviso visible en Datos, sin elegir cuenta Google real |
| Registro / Atrás superior y sistema | Regresa a Datos y conserva nombre, correo y consentimiento |
| Registro / muerte de proceso | PID cambió de 20411 a 23995; restaura paso 2 con ambas contraseñas vacías; al volver conserva datos |
| Reserva / Extras → Atrás superior | Regresa a Horario, conserva selección |
| Disponibilidad / 503 sintético → reintentar | Aviso y acción visibles; no ofrece lista de espera por un fallo de red |
| Favorito / guardar y quitar | Cambian icono, estado y descripción según la respuesta sintética |
| Beneficios / paquetes y tarjeta regalo | Formularios y errores 422 recuperables; el fixture impide crear cobros |
| PDF / descarga y apertura | Documento válido de QA abierto en lector local de Samsung; contenido «TARJETA QA SIN VALIDEZ» |
| Sesión / reinicio ante 503 | Conserva usuario y muestra aviso; reintento recupera sesión |
| Noir, Acero, Salón, Libreta | Selector y textos comprobados en Cuenta; contraste automatizado en los cuatro |
| Bienvenida / 320 dp y fuente hasta 200% | Composición desplazable sin mascota sobre texto; acciones visibles |
| Registro / 320 dp y fuente 200% | Enlace de acceso completo debajo del texto; formulario desplazable |
| Cuenta / cliente y roles mixtos | Sin burbuja sobre el formulario; cliente + recepcionista usa Inicio, Citas, Más y Cuenta, con prioridad de recepción |

La revisión visual detectó y corrigió adicionalmente el esqueleto de horarios durante error, mensajes heredados entre compras, superposición de mascota, estrechamiento del enlace de acceso y burbuja sobre Ajustes. Cuenta y navegación aplican la misma prioridad de personal en cuentas con roles mixtos.

Evidencia seleccionada en [`ui-audit-2026-10-11/`](ui-audit-2026-10-11/): capturas de catálogo invitado, registro conservado, tema claro y bienvenida a 200%; dumps de semántica de sesión, favorito y restauración. Se publican solo datos sintéticos.

## Límites de la entrega

### Continuación contra la API publicada (12-oct-2026)

Se compiló e instaló con `install -r` el paquete habitual `com.urbanblade.mobile`,
con `DEBUG_API_BASE_URL=https://api.urbanblade.com.mx/api/v1/`, conservando la sesión.
La API respondió 200 en `/up` y devolvió 20 servicios. Se revisaron Inicio, Mis
citas, Cuenta y Reservar antes de actualizar; después se comprobó Inicio y Mis
citas en el APK actualizado. La cita confirmada mostró el aviso «pago resuelto»
según Laravel, y sus acciones quedaron sin la burbuja de Bladebot. La revisión
posterior de Reservar/Cuenta queda por completar debido a conexión USB intermitente.
No se crearon/cancelaron citas, cambiaron roles ni ejecutaron cobros. Las capturas
con información de la sesión se conservan solo como archivos locales ignorados.

La continuación pasó 319 casos JVM: 318 aprobados, una omisión deliberada y cero
fallos. Lint conservó cero errores y 32 advertencias. La selección de experiencia
y los avisos de pago comparten reglas con web y backend; ver contrato móvil y
`barber/docs/PLAN_CONTRATO_API.md`.

Los contratos de favoritos/compras/PDF se contrastaron con las rutas y modelos actuales del backend. Las pruebas MockWebServer verifican petición y deserialización; no acreditan persistencia en Laravel ni autorización real.

La aceptación de integración requiere cuenta QA y entorno de prueba: OAuth Google real, cobro/cancelación/fallo y webhook Stripe, PDF servido por Laravel, todos los roles y TalkBack en dispositivo. No declarar pagos reales, publicación en Play ni aplicación global «sin pendientes» a partir de pruebas JVM. Los ocho hallazgos tienen corrección de código; su aceptación visual e integración son estados separados.

## Continuación de componentes compartidos (12-oct-2026)

- Cuenta y Más reutilizan `UrbanActionRow`/`UrbanRowGroup`, con títulos y descripciones multilínea y semántica de botón. Las rutas y permisos se mantienen.
- `UrbanPillTabs` adapta icono/etiqueta a ancho menor de 300 dp o fuente desde 130 %; objetivo táctil mínimo de 48 dp.
- Build del paquete habitual contra la API publicada: aprobado. Pruebas JVM: 318 aprobadas, 1 omitida, 0 errores/fallos. Lint: 0 errores, 32 advertencias y 4 hints de mantenimiento.
- La revisión visual en el teléfono de estos nuevos cambios se aplaza hasta la reconexión del usuario. No se ha instalado este último APK ni se han realizado cobros.

## Reserva recuperable (12-oct-2026)

- Catálogo de servicios/barberos con carga y reintento; una falla evita publicar un catálogo incompleto.
- El horario pertenece a la combinación de servicio, barbero y fecha. Al recargar se invalida de inmediato; restaurar una pantalla exige volver a comprobar disponibilidad antes de continuar.
- El regreso permite cambiar el servicio y respeta los pasos visitados; se bloquea mientras se registra la reserva. El resumen muestra «Total estimado».
- Validación final local: 321 casos JVM, 320 aprobados, 1 omitido y 0 fallos/errores; lint 0 errores, 32 advertencias y 4 hints. assembleDebug aprobado contra https://api.urbanblade.com.mx/api/v1/ en el paquete habitual.
- La instalación y revisión visual de este APK en el teléfono siguen pendientes de reconexión. No se crearon/cancelaron citas ni se realizaron cobros.

## Continuación: Mis citas (12-oct-2026)

- Actualizar conserva las citas ya visibles; la paginación está accesible en Próximas e Historial y muestra cuándo los conteos son parciales.
- La tarjeta prioriza la próxima cita informada por Laravel (`next`). Si solo existe una lista parcial, evita presentarla como agenda completa.
- Actualizar cancela consultas antiguas, evita peticiones de página duplicadas y limpia el error tras reintentar. Estas reglas y el bloqueo de Cargar más también aplican al personal.
- Acciones adaptables con FlowRow, objetivos de 48 dp y texto GoldText. No cambian permisos, pagos ni rutas.
- La revisión visual en teléfono y la instalación se aplazan hasta el regreso y reconexión del usuario; no se crearon/cancelaron citas ni se realizaron cobros reales.

### Revisión al reconectar el teléfono

1. Instalar el APK del paquete habitual con actualización (`adb install -r`), conservando datos y sesión; no desinstalar ni borrar almacenamiento.
2. Abrir Mis citas y comprobar que la tarjeta destacada coincide con la próxima cita del backend. Alternar Próximas e Historial; si hay más páginas, Cargar más debe estar disponible en ambas.
3. Tocar Actualizar: la lista visible se conserva durante la consulta. Cargar más no debe generar peticiones duplicadas mientras está ocupado.
4. Revisar fuente grande y pantalla estrecha: Reservar/Actualizar y Reagendar/Cancelar/Reservar de nuevo deben acomodarse y conservar sus etiquetas. Probar foco y lectura con TalkBack.
5. Sin confirmar operaciones reales, revisar error y recuperación de conexión. No crear/cancelar/reagendar citas ni realizar cobros en esta aceptación sin una autorización adicional específica.
6. El build local y CI no sustituyen OAuth Google real, validación visual de todos los roles, Stripe/webhook ni preparación de un AAB release para Play.

Validación local de Mis citas: 324 casos JVM, 323 aprobados, 1 omitido y 0 fallos/errores. Las 24 pruebas de gestión de citas pasaron, incluidas recuperación de página, cancelación de respuestas antiguas y recargas concurrentes. Lint: 0 errores, 32 advertencias y 4 hints de mantenimiento. Build del paquete habitual contra la API publicada aprobado. La aceptación visual y de integración en teléfono se mantiene separada.

### Adeudo confirmado ante errores de conexión (12-oct-2026)

Una falla de red o una respuesta sin adeudo_total conserva el último importe conocido y muestra un aviso para actualizar. Solo una respuesta válida del backend confirma un total cero; una consulta nueva cancela la anterior. No se ejecutan pagos ni se cambia la autorización. Web usa el mismo criterio.

Validación final local: 325 casos JVM, 324 aprobados, 1 omitido y cero fallos/errores; 25 casos de gestión de citas aprobados. Lint: cero errores, 32 advertencias y 4 hints. assembleDebug aprobado contra la API publicada en el paquete habitual. La instalación y aceptación visual en el teléfono quedan para el regreso y reconexión del usuario.


### Comprobantes recuperables (12-oct-2026, TT32 / HT-12)

Una consulta fallida de productos o membresías conserva el último dato válido;
no equivale a una lista vacía ni a un total cero. El aviso identifica la consulta
incompleta y ofrece reintento con el componente compartido. Una nueva consulta
cancela la anterior. Las descargas tienen objetivo mínimo de 48 dp.
Validación visual en teléfono y apertura real de PDF pendientes de reconexión.

Pruebas JVM de esta tanda: 326 en total, 325 aprobadas, una omitida, sin fallos.


### Hallazgos en dispositivo por rol (12-oct-2026, TT32 / HT-12)

Cliente, recepción, barbero y administrador revisados en el teléfono con API
publicada, sin cobros ni cambios de citas. Ingeniero pendiente de cuenta real.
Citas cuenta filas cargadas para barbero y distingue el motivo de bloqueo
`motivo_no_iniciar`: no atribuir `puede_iniciar=false` exclusivamente al pago.
Bladebot ocupa una fila propia sobre la navegación, sin superponerse al contenido.
Inventario consulta todas las páginas del endpoint y deduplica por ID; el resumen
es global y la sección de atención corresponde a la vista filtrada.

Validación final local: 327 pruebas JVM, 326 aprobadas, una omitida; lint sin
errores (32 advertencias y 4 hints existentes), APK compilado e instalado con
`adb install -r` conservando la sesión. En administrador: motivo de fecha correcto,
inventario con 6 en atención y 6 en resumen, Bladebot en fila propia. Contador de
barbero corregido en código; revalidación física pendiente de cambiar sesión.


### Agenda premium compartida (12-oct-2026, TT32 / HT-12)

`UrbanAppointmentHeading` separa estado y título completo para evitar la
competencia horizontal en nombres largos. Tarjetas de cliente usan el gradiente
del kit premium. Agenda general de personal identifica al cliente y conserva
barbero y fecha. Cancelar se accede por Más acciones y mantiene su confirmación;
los permisos, payloads y precios no cambian.


### Legibilidad premium sin dispositivo (13-oct-2026, TT32 / HT-12)

Nombres completos de personas ajustan líneas en la agenda. El rail de fecha
usa mes abreviado (13 OCT), no el día de semana que podía parecer marzo.
Cabecera de cita, estado y gradiente compartidos; acciones por rol sin cambios.
Frontend y backend sin cambios para esta tanda: contratos existentes y CI verde.
Nueva compilación preparada para teléfono; revalidación física pendiente.

Validación 13-oct: build debug correcto contra API publicada, 327 pruebas JVM
(326 aprobadas y una omitida), lint sin errores. El último ajuste de nombres
y fechas no se ha revisado en teléfono. Frontend y backend limpios con CI verde.


### Kit adaptable y pedidos recuperables (13-oct-2026, TT32 / HT-12)

Mosaicos comparten una columna con ancho menor de 340 dp o fontScale >= 1.3.
Títulos, subtítulos y avisos clave no se truncan; UrbanKeyValue apila etiqueta y
valor con poco espacio o texto grande. Catálogo separa precio de nombre y
descripción, y no presenta fallos/carga como ausencia de barberos. Pedidos cancela
consulta y paginación anteriores al recargar; carga más bloquea duplicados,
conserva filas al fallar y limpia errores al reintentar. Acciones de pedido se
apilan y se bloquean durante su operación. Revalidación física pendiente.

Comprobantes lee todas las páginas de compras y deduplica por ID antes del
resumen. Fallos de pagos conservan los últimos datos y permiten reintento; no
deben cancelar el ViewModel por una excepción de coroutine hija.

Catálogo respeta UserExperience.canBook: barbero/ingeniero consultan servicios
y perfiles, sin acceso ni botón Reservar. Invitado conserva exploración y reserva
con autenticación posterior; cliente y roles autorizados conservan su recorrido.

Checklist de aceptación física restante: Inicio/Catálogo/Cuenta/beneficios con
fuente del sistema >= 1.3 y temas claro/oscuro; Pedidos con filtros y reintento;
Comprobantes con consultas y PDF; formularios con teclado y vuelta atrás. La
revisión de código y pruebas no certifica que todas las pantallas estén pulidas.
Ingeniero sigue pendiente de cuenta real; cobros y cambios reales no ejecutados.

Validación final de entrega: 331 pruebas JVM (330 aprobadas, una omitida),
compilación debug correcta y lint sin errores. APK contra API publicada; no
instalada para esta tanda por ausencia de teléfono conectado.


### Tienda y perfiles compartidos (13-oct-2026, TT32 / HT-12)

Tienda usa UrbanTextField con búsqueda IME y una estimación basada en todos los
productos conocidos del carrito, no solo los resultados visibles. No supera
existencias conocidas ni permite modificar la selección mientras envía; errores
conservan el carrito y Laravel confirma precio/stock. Precio y cantidades ocupan
un pie adaptable. Perfil del barbero usa UrbanStatStrip compartido, adaptable
a fuente grande, y ofrece actualizar conservando información y mostrando fallos.
El teléfono queda disponible para la revisión del usuario, sin automatizarlo.

Tienda explicita Crear pedido y pago/recogida en sucursal según el backend.
Tiene Volver visible; barbero/ingeniero no ven el acceso de pedidos.

Validación tienda/perfiles: 334 JVM (333 aprobadas y una omitida), compilación
debug y lint final correctos. UI final incluye búsqueda restaurada y textos de
compra claros; inventario usa Valor inventario como la web, sin cambiar el dato.
La APK nueva no se instala durante la revisión del usuario de 4f2d48c.

### 13 de octubre — Notificaciones y entrega al teléfono

Se instaló la APK 31cf6ca con adb install -r (Success) y se abrió MainActivity,
conservando la sesión. El usuario revisa tienda, carrito y perfiles; el teléfono
queda libre durante los siguientes cambios.

Notificaciones conserva la lista ante un fallo de consulta, limpia errores al
reintentar y muestra un aviso recuperable cuando hay datos previos. El encabezado
no afirma Estás al día durante una consulta o un fallo. Marcar como leídas se
bloquea durante envío para evitar duplicados y después consulta el estado del
servidor. Las pruebas utilizan repositorio simulado; no se marcaron avisos reales.

Validación de Notificaciones: 337 pruebas JVM (336 aprobadas, una omitida), sin fallos. Lint: cero errores, 32 advertencias y 4 hints existentes. APK debug compilada contra la API habitual. Esta tanda requiere revisión física posterior y no sustituye 31cf6ca durante la revisión del usuario.

### 13 de octubre — Kit de analítica renovado (TT32 / HT-12)

UrbanAnalyticsCard comparte el marco de dashboard, análisis, insights y reportes:
título completo, cifra en su propia línea, contexto y visualización sobre el fondo
degradado del tema. UrbanChartColors usa seis colores categóricos diferenciados,
con tonos de mayor contraste en Libreta. Los colores de alerta siguen indicando
estado; no representan categorías ni cambian el sentido de los datos.

Donas: gráfico de 156 dp centrado y leyenda debajo con nombre completo, valor y
porcentaje. Rankings: etiqueta y cifra separadas, barra degradada y color propio.
Series temporales: azul para líneas y verde para barras, con selección de valores.
Cuatro pestañas de analítica se muestran en dos filas del mismo UrbanPillTabs.
No introducir otra tarjeta local ni una paleta de transparencias del mismo oro.
Filtrar series por pares etiqueta/valor (analyticsChartSeries), nunca mapNotNull
solo sobre valores: desplazaría etiquetas si el servidor manda un dato inválido.

La comprobación del pin HTTPS público de producción es obligatoria (sin Bearer ni datos de usuario). CI comprueba además que los reportes JVM existan y no contengan pruebas omitidas. El rediseño requiere revisión física en temas, fuentes grandes, Analítica y Reportes; no se instala durante la revisión de 31cf6ca.

Validación final del rediseño: 339 pruebas JVM, todas aprobadas, cero omitidas y cero fallos. La comprobación HTTPS real y las dos regresiones de series analíticas pasaron. Compilación y lint finales correctos; revisión física de la nueva presentación pendiente.

### 13 de octubre — Recorrido de visita del cliente

UrbanVisitSummary comparte la identidad de una visita: servicio completo, avatar,
nombre del barbero y precio en una fila aparte mediante UrbanKeyValue. Inicio
separa estado, fecha y hora de estos datos, añade Ver mi cita como acción explícita
y reutiliza el resumen al reservar de nuevo. Solo muestra importes recibidos del
servidor: distingue Importe de la cita de Precio del servicio.

UrbanStepProgress muestra números, confirmación de pasos completados y el nombre
del paso actual, con descripción accesible. Es informativo: no permite saltar
validaciones ni cambia el envío de una reserva. El resumen de selección se muestra
completo y la invitación a reservar no promete una duración sin evidencia.

Validación del recorrido de visita: compilación debug y lint correctos, cero errores, 32 advertencias y 3 hints sin avisos nuevos en los componentes tocados. Son cambios de presentación; la última suite funcional ejecutada sigue en 339 pruebas aprobadas y cero omitidas. CI ejecutará nuevamente la suite del commit. Revisión física de Inicio y del progreso de reserva pendiente; se conserva 31cf6ca en el teléfono.

### 14 de octubre — Muro de inspiración con navegación social

UrbanWorkGallery es el carrusel compartido de trabajos: deslizar fotos y videos,
contador de medios, indicadores de página, doble toque para dar me gusta, ampliación
de foto y video bajo demanda. WorkMediaThumb muestra carga y recuperación visual
cuando no hay imagen o falla la miniatura. No hay historias temporales, seguidores
ni reproducción automática: la API no ofrece esos contratos.

SocialFeedScreen muestra autor con avatar y enlace a perfil público, fecha real del
trabajo, medios protagonistas, acciones de reacción/comentario/guardado y descripción
completa. Los comentarios se abren en ModalBottomSheet con UrbanTextField, límite de
500 caracteres y borrador conservado si falla. El servidor devuelve tres recientes;
la pantalla identifica esa muestra y conserva el contador total, sin inventar el resto.

SocialFeedViewModel serializa acciones por publicación, bloquea consulta durante
envíos y consulta páginas adicionales mediante el page existente del backend.
Reservar con el barbero solo aparece para cliente y resuelve el user.id del muro al
perfil.id del catálogo de barberos, que es el identificador del contrato de reserva.
No se cambia el servidor ni se publica un comentario o reacción real en la validación.

Validación final del muro: 343 pruebas JVM, todas aprobadas, cero omitidas y cero fallos. Compilación debug y lint correctos: cero errores, 32 advertencias y 3 hints, sin avisos nuevos en el área social. Pruebas de acciones y comentarios usan mocks; no se modificaron publicaciones reales. Carrusel, visor, panel de comentarios, paginación, perfiles y fuente grande requieren revisión física posterior. Se conserva 31cf6ca en el teléfono.

### 14 de octubre — Reserva guiada y acciones identificables

UrbanStepProgress muestra los nombres de todos los pasos junto a sus números o
marcas de completado; el estado de cada paso conserva su descripción accesible.
BookingScreen explica qué falta (servicio, barbero, consulta, horario o aceptación
de la política) y el siguiente movimiento cuando la selección está lista.

Los botones dicen Elegir horario, Elegir extras, Revisar reserva o Continuar sin
extras y Reservar cita. Con fuente grande, las acciones se apilan para conservar
sus nombres completos. Se mantienen canAdvance, comprobación de disponibilidad,
consentimiento, protección de envío y los mismos datos enviados a la API.

Validación de la guía de reserva: compilación debug y lint correctos, cero errores, 32 advertencias y 3 hints, sin avisos nuevos en los archivos tocados. Son cambios de presentación; la última suite funcional local fue de 343 pruebas aprobadas sin omisiones. GitHub también aprobó pruebas, builds y CodeQL de 4fb70ba, última instalación confirmada en el teléfono. La nueva guía requiere revisión física de pasos, fuente grande y temas; se conserva 4fb70ba instalada durante la revisión del usuario.
