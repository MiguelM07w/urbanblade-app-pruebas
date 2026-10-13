---
name: urbanblade-mobile-ui-patrones
description: Patrón visual y módulos compartidos de UrbanBladeMobile (Compose). Consultar antes de crear o rediseñar cualquier pantalla de la app (inicio de un rol, módulo, Mi cuenta, estados de vacío/error) para reutilizar UrbanPattern, el kit de cuenta y las mascotas en vez de inventar componentes nuevos.
---

# UrbanBladeMobile — patrón de UI y módulos compartidos

La app sigue **un solo lenguaje visual**: el de la bienvenida, el login, el registro y el
inicio del administrador. Toda pantalla nueva o rediseñada se arma con estas piezas. Si
falta una, se agrega aquí (en `ui/components` o `ui/account`) y se documenta en esta
skill, no se crea una variante local en la pantalla.

Complementa a `urbanblade-mobile-android` (capas, seguridad, build) y a
`urbanblade-historias-sprint` (cada cambio va ligado a una tarea; el trabajo de UI
actual es **TT32, HT-12**).

## 1. Piezas del patrón (`ui/components/UrbanPattern.kt`)

| Pieza | Cuándo usarla |
|---|---|
| `UrbanPageHeader(title, subtitle, eyebrow, trailing)` | Encabezado de toda pantalla raíz (pestaña de la barra inferior). Eyebrow dorado en mayúsculas = rol o área; título serif. |
| `UrbanModuleScreen(eyebrow, title, subtitle, onBack, onRefresh)` | Módulos a los que se entra desde el inicio o "Más" (tienen volver). El título vive en el encabezado, no en la barra. |
| `UrbanHeroCard` + `UrbanHeroLabel` + `UrbanHeroStat` | El dato principal de la pantalla sobre la foto de la barbería con borde dorado. Una por pantalla, arriba. |
| `UrbanAttentionRow(icon, text, subtitle, tone, onClick)` | "Requiere tu atención": algo que revisar, con el tono de su gravedad (Danger, Warning, Success, Gold). Sin `onClick` es informativa. |
| `UrbanStatStrip` | 3–4 números en una sola tarjeta (en vez de varias `UrbanMetricCard` angostas). |
| `UrbanModuleTile` / `UrbanModuleGrid` | Accesos rápidos en dos columnas. |
| `UrbanPillTabs(tabs, selected, onSelect)` | 2 o 3 pestañas en píldora dorada (Mi cuenta, Mis citas del cliente). No usar `FilterChip` sueltos para cambiar de vista. |
| `UrbanMascotState(kind, title, subtitle, actionLabel, onAction)` | Estados vacíos y de error. |

Otras piezas compartidas:

- `UrbanTextField` y `PasswordStrengthMeter` (`UrbanAuthKit.kt`): **todo** campo de
  texto, no solo el de acceso. Etiqueta arriba, error junto al campo.
- `UrbanDateField` (`UrbanDateField.kt`): toda fecha. Entrega ISO `AAAA-MM-DD` y muestra
  "15 de mayo de 1995". `onlyPast = true` para nacimientos. Nunca pedir fechas escritas
  a mano.
- `UrbanPrimaryButton` / `UrbanOutlineButton`: el botón dorado sigue dorado mientras
  carga aunque el formulario esté incompleto (commit ccfb7c8).
- `UrbanFormat`: fechas (`date`, `dateShort`, `dateLong`, `monthYear`), horas y conteos
  ("1 cita" / "3 citas"). No formatear fechas a mano en la pantalla.

## 2. Mi cuenta (`ui/account/`) — compartida por los cinco roles

Diseño elegido por el usuario el 24-sep-2026: **tarjeta de miembro con pestañas (B) +
secciones en acordeón (C)**.

| Archivo | Contenido |
|---|---|
| `AccountKit.kt` | `AccountMemberCard` (tarjeta héroe con foto que se toca para cambiarla), `AccountAccordion` (sección plegable con resumen visible cerrada), `AccountSwitchRow`, `accountRoleLabel()`. |
| `AccountSections.kt` | `PersonalDataForm`, `ChangePasswordForm`, `NotificationChannels`, `ThemePicker`, `AccountNoticeBanner`. |
| `AccountScreen.kt` | La pantalla: pestañas Datos / Seguridad / Ajustes. |
| `ui/viewmodel/AccountViewModel.kt` | Estado único `AccountState`; avisos por sección (`AccountNotice`). |
| `data/model/AccountModels.kt` | Modelos de cuenta (separados de `Models.kt`). |

Qué cambia por rol (y nada más):

- **Cliente**: la tarjeta muestra nivel y puntos (`dashboard.loyalty`) y su descuento
  activo; Datos incluye teléfono, cumpleaños y sexo; Ajustes incluye Bladebot.
- **Personal** (admin, recepción, barbero, ingeniero): la tarjeta muestra "Miembro desde"
  y si el correo está verificado; Datos es solo nombre y correo (el backend ignora los
  campos de cliente para ellos).

Endpoints: `GET/PUT profile`, `POST profile/avatar` (multipart `avatar`, JPG/PNG/WebP
≤ 4 MB), `PUT profile/password`, `GET/PATCH notifications/preferences` (el PATCH hace
merge: mandar solo el canal que cambió). Tras cambiar nombre, correo o foto se llama
`AuthViewModel.refreshUser()` para que el resto de la app lo vea sin cerrar sesión.

Avisos: canales en la app, correo y promociones; push móvil mediante FCM cuando
el proyecto Firebase está configurado. SMS/WhatsApp no se ofrecen como entregas disponibles.

Favorito: control en catálogo, `PUT profile/favorite-barber`, estado recibido de
`GET barbers` (`es_favorito`) y preselección al reservar. Eliminar cuenta ya existe
en Seguridad, con confirmación y autorización del servidor; no duplicar otro flujo.

Para agregar una sección nueva a Mi cuenta: un `AccountAccordion` en la pestaña que le
corresponda, su formulario en `AccountSections.kt` y su `AccountSection` para los avisos.

## 3. Mis citas del cliente

`ClientAppointmentsScreen.kt` (solo para quien es únicamente cliente; el personal usa
`AppointmentsScreen.kt`). Pestañas Próximas / Historial; la siguiente cita va en la
tarjeta héroe con Pagar, Reagendar y Cancelar a la vista. Reutiliza `RescheduleSheet`,
`CheckoutSheet`, `WaitlistCard` y `AppointmentDateRail` de `AppointmentsScreen.kt`
(son `internal` para eso): no duplicarlas. "Nueva" va en el encabezado, nunca como botón
flotante sobre la lista.

## 4. Navegación del cliente (propuesta A, 25-sep-2026)

Cuatro pestañas: **Inicio · Reservar · Mis citas · Cuenta**. Reglas que la sostienen:

- El título de cada pantalla es el nombre de su pestaña (Reservar, Mis citas, Cuenta).
- Un solo verbo para reservar: «Reservar» / «Reservar cita» / «Reservar de nuevo». Nada de
  «Nueva», «Agendar» ni «Explorar».
- Cada cosa en un solo lugar: Beneficios (antes Wallet), Mis facturas y Mis pedidos en Cuenta
  → «Tu actividad»; Muro y Tienda en Inicio → «Descubre»; Bladebot en Inicio.
- Pantallas que no son pestaña llevan `UrbanTopBar("", onBack)`.
- Reservar son cuatro pasos (Servicio · Horario · Extras · Pago) con el total de la visita fijo
  abajo. Los productos de Extras se pagan aparte en el salón (pedido ligado a la cita). El
  método de pago se elige con tres mosaicos (efectivo, transferencia, tarjeta) y debajo solo
  el detalle del elegido. El estado del asistente va en `rememberSaveable`.
- Comprobantes (Mis facturas): se **descargan** con `core/media/ReceiptDownloads` a
  Descargas/UrbanBlade y se abren en el visor de PDF. Nunca abrir la liga firmada de S3 en
  el navegador.

## 5. Mascotas y tono

- **Bruno**: errores y datos técnicos (`UrbanStateKind.ERROR`).
- **Nava**: vacíos y "todavía no hay nada" (`UrbanStateKind.EMPTY`).
- **Bladebot**: bienvenida, espera, éxito y ayuda (asistente).
- No agregar mascotas nuevas ni dibujarlas; usar los assets de `res/drawable`.

Textos: español de México, frases cortas, sin tecnicismos ("No pudimos abrir tu
cuenta", no "Error 500"). En un 422 mostrar el `message` del servidor
(`HttpException.serverMessage()`), que ya explica qué falló.

## 6. Eyebrow por rol

`Administrador`, `Recepción`, `Barbero`, `Ingeniero`, `Cliente` (usar
`accountRoleLabel(user.roles)`; si alguien tiene varios roles gana el de más
responsabilidad).

## 7. Lecciones aprendidas (no repetirlas)

1. **Leer el backend antes de escribir un texto o un dato.** Varias veces un mensaje
   prometía algo que la API no hacía (p. ej. "pagos pendientes" eran citas completadas
   sin cobro; la lista de espera avisa a todos, no solo al primero).
2. **Nunca inventar datos en la UI.** Si la API no lo da, no se muestra o se pide el
   endpoint (cambio aditivo en `barber`, con prueba).
3. **Probar cada botón hasta el final en el S25** (`R5GL44QP8TT`), con la cuenta del
   rol correcto. Una pantalla que compila no está terminada.
4. **No tocar trabajo ajeno sin commit.** Antes de editar, `git status`; si el archivo
   tiene cambios de otra persona, poner lo nuevo en un archivo aparte (así nació
   `AccountModels.kt`).
5. **La tarjeta héroe sigue el tema**: su velo usa `UrbanColors.Background` (crema en
   "Libreta"), así que dentro de ella se usan `UrbanColors.Ink` / `Muted` como en cualquier
   tarjeta. Nunca colores fijos claros ni un velo negro fijo: en el tema claro el texto se
   perdía.
6. **Un botón flotante no debe tapar acciones** de la última tarjeta: dejar
   `contentPadding` inferior suficiente.
7. **Un botón que cambia de función en el mismo lugar** («Continuar» → «Reservar») se
   activa un momento después de cambiar, para que un doble toque no confirme nada.
8. **Formularios de Stripe** (`CardMultilineWidget`) heredan colores del tema oscuro: fijar
   texto, pista y acento (`styleCardWidget`) o salen blancos sobre blanco.
9. **Validar** con `gradle :app:testDebugUnitTest` e instalar con `:app:installDebug`.
   Git por rama y PR según `git-commit-conventions`; el mensaje va en español con el ID de
   la tarea y sin línea `Co-Authored-By`.
   Sin `gradlew` en el repo (lo agrega T141): el build se corre desde el panel Gradle de
   Android Studio, o con el Gradle 9.6.0 que ya está en `%USERPROFILE%\.gradle\wrapper\dists`
   (y el JBR de Android Studio como `JAVA_HOME`).
10. **No todo mapeo de estado es `statusStyle`.** `statusStyle()` (`UrbanStatus.kt`) manda
    para los estados de cita, pago, pedido y gift card: si una pantalla los vuelve a traducir
    (el calendario de citas lo hacía con su propia tabla de colores, corregido el 10-oct), se
    corrige ahí. Pero **no** son `statusStyle` la severidad de Insights (`positive` /
    `warning` / `negative`), la salud de infraestructura (`up` / `down`), las acciones de
    auditoría (`created` / `updated` / `deleted`), los términos de la lista de espera
    (`En espera`, `Avisada`) ni las etiquetas de sección en plural (`Confirmadas`): tienen su
    propio vocabulario a propósito, y migrarlas cambiaría el texto o el color que el usuario ya
    conoce.
11. **Un solo formato de dinero y de porcentaje**: `UrbanFormat.money` / `moneyShort` /
    `percent` / `delta`. Ninguna pantalla define su propio `money()` — el 10-oct-2026 había
    once copias con `%,.0f`, `%,.2f` y `%.0f`. Cifras grandes (KPI, ejes de gráfica):
    `moneyShort`; tickets y adeudos: `money`.
    **Excepciones a propósito** (no "unificar"): los ejes y etiquetas de gráfica
    (`UrbanCharts`, `DashboardCharts`) usan números sin `$`; los porcentajes, calificaciones
    y horas siguen con `%.0f` / `%.1f`; y **los campos donde se escribe un precio**
    (`OffersAdminScreen`, `ServicesAdminScreen`) no llevan separador de miles, porque el
    texto se manda al servidor tal cual.
12. **Filas compactas solo cuando el elemento es una fila de verdad.** `UrbanRow` /
    `UrbanRowGroup` (`UrbanDataTable.kt`) son para listas de título + contexto ([meta]) +
    estado o acción. **No** se convierten en fila las tarjetas que traen estructura propia:
    `OrderCard` (folio, líneas con subtotales, total y botones), `InventoryProductCard`
    (imagen, barra de stock, acciones) o la lista de clientes (avatar, pill de segmento,
    monto y conteo de citas) — migrarlas pierde información. Revisado el 10-oct-2026 sobre
    las cuatro listas de operación: solo Movimientos de inventario y Lista de espera
    calificaron.
13. **Estados vacíos: uno por caso, no tres variantes sueltas.** `UrbanEmptyState` (Nava, la
    pantalla o sección principal, con acción opcional), `UrbanMascotState(ERROR, …)` (Bruno,
    con "Reintentar") y `UrbanInlineEmpty` (bloque secundario dentro de una lista, compacto).
    `UrbanEmptyState` delega en `UrbanMascotState`, así que **no hay lógica duplicada** y no
    hace falta migrar las llamadas directas que ya existen. Un vacío que admite una acción real
    no se deja como mensaje pasivo.
    Aplicado el 11-oct-2026: en Beneficios, "Movimientos recientes" sin datos ofrece
    **"Reservar cita"** con `UrbanOutlineButton` —de contorno, para no competir con el CTA de la
    pantalla— y la acción se pasa por parámetro desde la navegación.
14. **El estado deshabilitado tiene que verse.** El botón apagado sigue siendo gris con borde
    (el dorado apagado se leía como botón roto), pero con `CardAlt` y la tinta al 75 %: sobre
    `Card` el botón se perdía en el fondo y la auditoría del 24-sep lo reportó como "parece
    defectuoso". Igual en `UrbanPrimaryButton`, `AuthPrimaryButton` y el enviar de Bladebot.
15. **Tres números o más en fila: `UrbanStatStrip`, no `UrbanMetricCard`.** Con tres
    `UrbanMetricCard` angostas el texto no cabe al lado del ícono y la palabra se parte
    ("Entrad / as" a 360 dp, visto en Movimientos de inventario el 10-oct). `UrbanMetricCard`
    es para dos por fila.
17. **El dorado es énfasis, no relleno.** Regla que salió de la auditoría (P2-3) y se aplicó el
    10-oct-2026: **lo que se repite en cada fila deja de ser dorado** —categorías, precios de una
    lista, saldos, "+puntos"— y el dorado se queda en la **acción**, el **estado activo** (pestaña,
    chip, día), el **héroe o dato único de la pantalla**, los **tonos semánticos** (Stock bajo,
    VIP) y los **iconos**. Las cajas de icono llevan fondo neutro (`CardAlt`); los chevrones que
    solo dicen "hay más" van en `Muted`. Antes de pintar algo de dorado, preguntarse: ¿esto se
    repite por fila? Si sí, va en `Ink` o `Muted`.
18. **La acción principal del acceso va anclada abajo.** `AuthScaffold` tiene dos columnas: el
    contenido se desplaza y el pie no, así que con el teclado abierto el `imePadding` sube el
    botón en vez de dejarlo debajo del formulario (auditoría P1-7). Los enlaces secundarios se
    **viajan con el botón, dentro del pie** ("¿Eres nuevo? Crea tu cuenta" debajo de
    "Entrar a UrbanBlade"): sueltos en el contenido, el botón anclado quedaba flotando solo al fondo
    de la pantalla.
16. **La familia de acceso comparte `AuthScaffold` y el registro va en DOS pasos.** Encabezado
    compartido: marca compacta, eyebrow, título (**máximo dos líneas**, para que no empuje el
    formulario con el tamaño de fuente al máximo) y subtítulo; la mascota solo aparece cuando
    comunica algo (error o correo enviado). El registro pide primero los datos (consentimiento +
    Google + nombre + correo) y después la contraseña; la flecha de la barra **vuelve al paso 1**,
    no fuera de la pantalla, y el avance se indica con "Paso X de 2". No volver al formulario
    único de cuatro campos (auditoría del 24-sep: "demasiado larga y densa"). En la bienvenida la
    mascota queda en 220 dp, no en 300.

## 8. Kit de controles, dashboards y gráficas (plan del 26-sep-2026)

Decisión del usuario: **kit propio** inspirado en shadcn/ui, Untitled UI y Tremor, igual
que en la web (`frontend-urban/.claude/skills/urbanblade-ui-kit`, mismo catálogo y mismas
fases). Sin librerías de UI nuevas; todo con `UrbanColors` y Material 3.

**Regla de oro:** el kit cambia cómo se ve, nunca lo que se manda. `BookingPayMethod`
(`EFECTIVO`, `TRANSFERENCIA`, `TARJETA`), los `Boolean` de activo/inactivo y los
request de Retrofit quedan idénticos.

| Pieza (`ui/components/UrbanControls.kt`) | Fase | Reemplaza |
|---|---|---|
| `UrbanSwitch` / `UrbanSwitchRow(title, subtitle, checked, onCheckedChange)` | Hecho (26-sep) | Los 9 `Switch` sueltos: `AccountKit`, `AdminModuleScreens`, `BarbersAdminScreen` (2), `OffersAdminScreen` (2), `ServicesAdminScreen` (2), `SettingsScreen`. Fila completa tocable, pista dorada encendida, `Role.Switch` para TalkBack. |
| `UrbanChoiceTiles(options, selected, onSelect)` con `UrbanChoice(value, title, subtitle, icon)` | Hecho (26-sep) | Los mosaicos de la "Propuesta A" (25-sep) de `BookingPayment` pasaron al kit sin cambiar su aspecto, y ahora también se usan al pagar una cita en `AppointmentsScreen` (antes eran dos botones sin estado seleccionado). Los `FilterChip` de propina y filtros se quedan (son filtros, no métodos). |
| `UrbanStatCard` + `UrbanTrendPill` (`UrbanStatCard.kt`) | Hecho (10-oct) | KPI de los inicios por rol: etiqueta, valor y variación contra el periodo anterior ("+12.6 % vs mes pasado"). La variación no siempre es verde —más cancelaciones es rojo, con `positiveIsGood`— y si no hay periodo anterior no se inventa: se muestra el texto de `caption` y nada más. |
| `UrbanDashboardChart` + `chartSeries()` (`DashboardCharts.kt`) | Hecho (26-sep) | Gráfica principal en cada inicio con los mismos datos de GET /dashboard que la web: admin (ingresos por semana, servicios), recepción (citas por hora), barbero (su semana, sus servicios). `spanishChartLabel()` traduce "Mon"/"03 Aug" que barber manda en inglés. La dona de `UrbanCharts` ahora usa tonos del oro. El cliente no lleva gráfica: su inicio es para reservar. |
| `UrbanRow` + `UrbanRowGroup` + `UrbanRowDivider` (`UrbanDataTable.kt`) | Hecho (10-oct) | Tabla/lista compacta: filas con separador dentro de una sola tarjeta, con badge de estado y cifra o acción al final. Es la conclusión P0 de la auditoría ("reducir el uso de tarjetas; usar filas y separadores para listas") y reemplaza las tarjetas-por-elemento. Para listas largas, `UrbanRow` y `UrbanRowDivider` dentro del LazyColumn de la pantalla. |

- `UrbanFormat.money()` / `moneyShort()` / `percent()` / `delta()` (`UrbanFormat.kt`): **todo** dinero y porcentaje de la app. Había once copias locales de `money()` con formatos distintos (%,.0f, %,.2f, %.0f, con y sin `Locale`); el texto es el mismo de `money()` y `fmtMoney()` de la web. Al tocar una pantalla, migrar su copia a estas. `delta()` devuelve `null` cuando no hay con qué comparar, nunca un "+100 %".

Validar igual que siempre (sección 7, punto 9) y revisar cada pantalla tocada en el S25
en al menos un tema oscuro y en "Libreta".

## 9. Movimiento y datos extremos en Compose (reglas adaptadas de emilkowalski/skills)

Mismo criterio que en la web (`frontend-urban/.claude/skills/ui-ux-visual-polish`), con estas
equivalencias:

- **Primero, ¿debe animarse?** Por frecuencia y propósito: lo que el usuario hace muchas veces
  al día no se anima; lo ocasional (hojas de pago, diálogos) sí; lo raro (reserva confirmada,
  recibo, sello) puede llevar un detalle de gusto.
- **Salidas y entradas con *ease-out*:** `tween` con `CubicBezierEasing(0.23f, 1f, 0.32f, 1f)` o
  `FastOutSlowInEasing`; nunca `LinearEasing` en la interfaz salvo barras de progreso. Lo que
  se arrastra o responde al dedo usa `spring` o `Animatable`, que se reorienta desde donde
  está, en vez de reiniciar.
- **Duración de la interfaz por debajo de 300 ms.**
- **Anima la capa, no el diseño:** `graphicsLayer { translationX/Y, scaleX/Y, alpha,
  rotation }` y no `width`, `height` ni `padding`, que obligan a recomponer y medir en cada
  cuadro. Nada nace de escala 0: parte de 0.85–0.95 con `alpha` bajo (así lo hace
  `UrbanCreditCardCarousel`).
- **Respuesta al toque:** todo control tocable cambia al presionarlo (escala 0.97 o cambio de
  color) y las acciones importantes vibran con `HapticFeedbackType`, sin abusar.
- **Movimiento reducido:** cuando el sistema tiene la escala de animaciones en 0, la pantalla
  tiene que seguir siendo clara sin movimiento.
- **Datos extremos antes de dar por buena una pantalla:** nombres largos con acentos y de una
  letra (iniciales), correo largo, campo opcional vacío (sin renglón huérfano), listas con 0, 1
  y muchos elementos (`UrbanFormat.count` para "1 cita"/"3 citas"), números con formato local,
  textos con `maxLines` y `TextOverflow.Ellipsis` donde no debe crecer la tarjeta, ancho de
  320 dp, **tamaño de fuente al máximo** y los cuatro temas. En el S25 se prueba el tamaño de
  fuente con `adb shell settings put system font_scale 2.0` y **se restaura** con
  `adb shell settings put system font_scale 1.0`. Los datos se cambian en el borde (el
  repositorio o el estado del `ViewModel`), nunca editando el diseño; los fallos se reportan
  con su propuesta antes de corregir.

## Cierre de auditoría UI/UX (11-oct-2026)

- Texto de acento: `UrbanColors.GoldText`; fondos/acento de marca: `Gold`. El tema
  claro adapta estados semánticos. No usar un acento de fondo como tinta pequeña.
- `BenefitShop` y `BenefitPurchaseViewModel`: compras de paquetes/tarjetas regalo y
  descarga de tarjeta PDF. Usar el PaymentSheet compartido y confirmación del servidor.
- `MembershipCardFiles`: descarga autenticada y FileProvider limitado; nunca abrir
  la URL de la API en navegador ni compartir el token con el visor.
- Registro conserva datos no sensibles, nunca contraseñas. Atrás del sistema vuelve
  un paso. Carga/error/vacío de horarios son estados diferentes.
- Ver contrato, checklist y evidencia en `docs/UI_UX_CONTRATO_MOVIL.md`,
  `docs/VERIFICACION_MANUAL_P0.md` y `docs/UI_UX_VALIDACION_2026-10-11.md`.

## Sincronización de roles (12-oct-2026)

Usar core/access/UserExperience para Inicio, Cuenta y navegación. Prioridad: administrador, recepcionista, barbero, ingeniero, cliente. No confundir experiencia visual con permisos del servidor. Más reutiliza UrbanRowGroup/UrbanRow. Bladebot se oculta en Cuenta, Reservar y Citas para no tapar acciones. Los avisos de pago usan pago_resuelto, nunca deducen deuda por estar confirmada la cita.

### Pulido de accesos y pestañas (12-oct-2026, TT32 / HT-12)

`UrbanActionRow` reutiliza `UrbanRow` para accesos de Cuenta y Más: icono neutro,
título de hasta dos líneas y descripción de hasta tres. Agrupar con `UrbanRowGroup`;
no usar avisos de atención para navegación ordinaria. `UrbanPillTabs` tiene objetivo
táctil de 48 dp y apila icono/etiqueta en menos de 300 dp o con fuente desde 130 %,
sin cortar las etiquetas con puntos suspensivos. La misma pieza sirve a todos los roles.

### Reserva recuperable (12-oct-2026, TT32 / HT-12)

El catálogo distingue carga, error y reintento. No avanzar ni confirmar con IDs
fuera del catálogo ni con una hora sin respuesta vigente para servicio/barbero/día
(`BookingViewModel.slotsFor`). Restaurar el borrador conserva la elección, pero
requiere volver a validarla. Cambiar servicio amplía el recorrido para que Atrás
superior, inferior y sistema vuelvan por el mismo paso; durante la reserva no se
cambia de paso. El resumen es estimado: Laravel confirma el importe y beneficios.

### Mis citas recuperable (12-oct-2026, TT32 / HT-12)

Actualizar conserva las citas visibles mientras consulta. Cargar más debe estar
accesible en Próximas e Historial y deshabilitado durante una consulta; los conteos
parciales se identifican como páginas cargadas. La próxima cita usa `response.next`
de Laravel cuando está disponible. Una recarga cancela la paginación anterior y
un reintento correcto limpia su error. Acciones con FlowRow y objetivos de 48 dp,
sin cambiar los permisos ni inferir pagos del estado de la cita.

La consulta de adeudo conserva el último importe confirmado cuando la conexión
falla o la API no entrega el total. Muestra un aviso recuperable con Actualizar;
solo una respuesta válida del servidor puede confirmar un total cero. La respuesta
antigua se cancela al reconsultar. Esta mejora es de lectura, sin efectuar pagos.


### Comprobantes recuperables (12-oct-2026, TT32 / HT-12)

Una consulta fallida de productos o membresías conserva el último dato válido;
no equivale a una lista vacía ni a un total cero. El aviso identifica la consulta
incompleta y ofrece reintento con el componente compartido. Una nueva consulta
cancela la anterior. Las descargas tienen objetivo mínimo de 48 dp.
Validación visual en teléfono y apertura real de PDF pendientes de reconexión.


### Hallazgos en dispositivo por rol (12-oct-2026, TT32 / HT-12)

Cliente, recepción, barbero y administrador revisados en el teléfono con API
publicada, sin cobros ni cambios de citas. Ingeniero pendiente de cuenta real.
Citas cuenta filas cargadas para barbero y distingue el motivo de bloqueo
`motivo_no_iniciar`: no atribuir `puede_iniciar=false` exclusivamente al pago.
Bladebot ocupa una fila propia sobre la navegación, sin superponerse al contenido.
Inventario consulta todas las páginas del endpoint y deduplica por ID; el resumen
es global y la sección de atención corresponde a la vista filtrada.


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
