# Verificación UI/UX del contrato móvil actual

Esta lista reemplaza los recorridos antiguos de cinco pestañas y catálogo inicial. Ver `UI_UX_CONTRATO_MOVIL.md` y `UI_UX_VALIDACION_2026-10-11.md`.

## Entorno seguro

Usar emulador/cuenta de prueba. No provocar errores, pagos o mantenimiento en producción ni cerrar la sesión de un teléfono personal.

Para revisar la interfaz sin backend real:

```powershell
python scripts/ui-audit/fixture_server.py
gradle :app:assembleDebug -PUI_AUDIT=true -PDEBUG_API_BASE_URL=http://10.0.2.2:18765/api/v1/
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.urbanblade.mobile.audit/com.urbanblade.mobile.MainActivity
```

`.audit` tiene datos separados. La API sintética solo escucha en localhost y no crea cobros. Esta prueba verifica UI; persistencia real, OAuth y webhook requieren integración.

Para un dispositivo por USB, compilar con `-PDEBUG_API_BASE_URL=http://127.0.0.1:18765/api/v1/` y ejecutar `adb -s <dispositivo-QA> reverse tcp:18765 tcp:18765`. Instalar únicamente la variante `.audit`.

## Acceso y sesión

- Bienvenida: crear cuenta, iniciar sesión y consultar servicios. Selección de invitado retomada después del acceso.
- Registro: datos/consentimiento → contraseña; Atrás superior/sistema vuelve a datos. Recrear conserva datos/consentimiento y borra contraseñas.
- Error/cancelación de Google visible en paso 1. Teclado: campo y acción alcanzables.
- Primer acceso → reinicio sin red: resumen y aviso. Token antiguo sin caché: reintento.
- Reintentar desde una pantalla secundaria conserva navegación.
- 401/403/422/429/503/timeouts: mensajes apropiados, sin confundir permisos con sesión vencida.

Para responder con errores sin cambiar Laravel:

```powershell
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:18765/_qa/control -ContentType application/json -Body '{"status":503}'
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:18765/_qa/control -ContentType application/json -Body '{"status":200}'
```

## Cliente

- Cuatro pestañas: Inicio / Reservar / Mis citas / Cuenta; Beneficios/facturas/pedidos desde Cuenta.
- Favorito: guardar/quitar, recargar, preseleccionar sin reemplazar selección explícita.
- Reserva: cuatro pasos; carga/error/reintento de horarios, sin lista de espera durante consulta. Cambios rápidos no dejan horarios anteriores. Atrás coherente.
- Citas: próximas/historial, reagendar/cancelar con políticas reales y confirmación de acciones destructivas.
- Checkout: cancelar, fallar y reintentar; no prometer cobro/beneficio antes del servidor.
- Beneficios: carga parcial conserva datos; compra de paquetes/tarjetas regalo, espera de webhook y actualización.
- PDF: descarga autenticada y visor; errores y ausencia de visor con instrucciones.

## Personal y accesibilidad

- Roles administrador/recepción/barbero/ingeniero y cuentas mixtas: navegación y permisos correctos.
- Bladebot adecuado al rol y sin ocultar último contenido.
- Cuatro temas, 320 dp, fuente ampliada, teclado y recreación: acciones alcanzables y contraste legible.
- TalkBack: nombres, favorito con estado, orden de foco y errores; no depender solo del color.

Registrar versión, dispositivo, entorno y evidencia por caso. No marcar como validado un recorrido solo por leer el código o compilarlo.
