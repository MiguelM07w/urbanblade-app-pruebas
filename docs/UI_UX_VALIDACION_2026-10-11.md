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
