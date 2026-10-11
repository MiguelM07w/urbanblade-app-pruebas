---
name: urbanblade-historias-sprint
description: Forma de trabajo de Urban Blade desde el 23-sep-2026 — todo trabajo sale de una historia de usuario (HU/T) del Product Backlog o de una historia técnica (HT/TT) asignada, dentro de su sprint. Usar al iniciar cualquier sesión de trabajo en este repositorio, antes de proponer o escribir código, al elegir qué hacer, al preparar un commit o al reportar avance.
---

# Trabajo guiado por historias (Urban Blade)

Desde el 23-sep-2026 el equipo trabaja **solo** con lo que se encargó en el
cuatrimestre: historias de usuario (HU-xx, tareas Txxx) e historias técnicas (HT-xx,
tareas TTxx). Nada de funciones nuevas, rediseños ni refactors "de paso" si no están
ligados a una tarea.

Esta skill existe con el mismo nombre en los cuatro repositorios (`barber`,
`frontend-urban`, `UrbanBladeMobile`, `spark`). El núcleo es idéntico; cambia solo la
sección **"Tareas de este repositorio"**. Si cambias el núcleo, cámbialo en los cuatro.

## Fuente de verdad

Carpeta `C:\Users\luis1\Documents\UrbanBlade\DOCUMENTACION\` (no es repositorio Git):

| Archivo | Para qué |
|---|---|
| `Product_Backlog_Urban_Blade_UBv3_Pendiente.xlsx` | **Tablero de trabajo**: 17 HU, 42 tareas, 74 pts pendientes. Hoja «Tablero de Tareas» = estado real (col. N). |
| `Historias_Tecnicas_Urban_Blade.xlsx` | HT-01 a HT-14, TT01 a TT37 (55 pts), ligadas al checklist y casos de prueba. |
| `Product_Backlog_Urban_Blade_UBv2.xlsx` | Registro completo con evidencia (incluye lo ya completado). |
| `UrbanBlade_Checklist_250_v2.xlsx`, `Casos_de_Prueba_Urban_Blade_v2.xlsx` | Pruebas que alimentan las HT. |

Si la tabla de esta skill contradice los Excel, **ganan los Excel** (esta skill es una
foto al 23-sep-2026). Para editar los Excel usar la skill `urbanblade-product-backlog`
(en `barber`).

## Calendario

| Sprint | Fechas | Notas |
|---|---|---|
| 1 | 14–25 sep | |
| 2 | 28 sep–9 oct | Terraform empieza el **5-oct** |
| 3 | 12–23 oct | |
| 4 | 26 oct–6 nov | CI/CD y Continuous Delivery desde el **26-oct** (≥ 25-oct) |
| 5 | 9–20 nov | |
| 6 | 23 nov–4 dic | Cierre, pruebas finales y documentación |

Puntos 1, 3 o 5; 1 punto = 8 horas. Estados: Pendiente, En progreso, Completado.

## Equipo

| Integrante | Rol |
|---|---|
| Alan Ruiz Vilchis | Product Owner / Backend |
| Elías García Nolasco | Scrum Master / Frontend |
| Miguel Ángel Mena Garduño | QA Tester / Frontend |
| Luis Enrique González Ramírez | Full Stack / Mobile |
| María Isabel Cruz Flores | Backend / Administradora de BD |

## Flujo obligatorio por tarea

1. **Identificar la tarea.** Pregunta o deduce quién es el usuario y busca su tarea en
   la tabla de abajo o en el Excel. Si lo pedido no corresponde a ninguna HU/HT, dilo y
   propón a qué tarea ligarlo o que el PO (Alan) la agregue; no lo hagas "por fuera".
2. **Revisar dependencias.** Si la tarea depende de otra que no está Completada
   (columna "Depende de"), no la adelantes en `main`; avisa a quién se espera.
3. **Leer los criterios de aceptación** de la HU/HT (hoja «Product Backlog» o
   «Historias Técnicas»). La tarea está hecha cuando se cumplen, no cuando compila.
4. **Revisar lo existente** en el código antes de escribir: muchas cosas ya existen a
   medias. No reinventar ni duplicar.
5. **Implementar lo mínimo** que cumple el criterio, con sus pruebas.
6. **Validar** con los comandos de este repositorio (sección de abajo). Sin validación
   no hay commit.
7. **Entregar por rama y PR** (desde el 2026-10-04 `main` está protegida): rama
   `tipo/idtarea-descripcion` desde `origin/main`, commit en español con el ID de la
   tarea (`feat(pagos): T144 mensajes claros al cancelar el pago con tarjeta`), push de la
   rama, PR a `main` y merge con el CI en verde **sin borrar la rama**. Con la orden del
   usuario de subir el cambio, la IA hace commit, push y PR y, si todo sale bien, fusiona
   y despliega barber/frontend-urban a staging sin volver a preguntar; sin esa orden,
   entrega resumen, archivos, pruebas y los comandos exactos
   (`git switch -c`, `git add` con rutas, `git commit`, `git push -u origin <rama>`,
   `gh pr create`). Detalle en `git-commit-conventions`.
8. **Marcar el estado con evidencia**: una tarea pasa a *Completado* solo con evidencia
   verificable (commit en `main` + prueba que pasa, o registro de QA). Código subido sin
   probar = *En progreso*. "NO INVENTES AVANCE".

## Reglas de ingeniería (adaptadas de mattpocock/skills)

- **Prueba roja primero.** Para un bug o una regla de negocio, escribe la prueba que falla
  con el síntoma exacto, luego el mínimo código para ponerla en verde, de una rebanada a la
  vez. Las pruebas se escriben en la interfaz pública (la ruta de la API, la pantalla, el
  servicio), no con *mocks* de colaboradores internos ni repitiendo en la aserción el cálculo
  del código. En `barber`, siempre con `.\test.ps1`.
- **Bug difícil: primero un bucle que falle por ese bug.** Antes de proponer una causa,
  consigue un comando rápido y determinista que ya hayas corrido y que se ponga rojo por el
  síntoma real (una prueba, un `curl`, un script de Playwright, la repetición de una petición
  guardada). Sin ese comando no hay hipótesis. Después reduce el caso al mínimo, formula de 3
  a 5 hipótesis ordenadas antes de probar ninguna y deja la prueba como regresión.
- **Al mostrar salidas, tachar secretos** (`<REDACTED>`): claves, tokens, `.env`, Secrets
  Manager. Construye los bucles contra variables de entorno, no con el valor pegado.
- **Al escribir o editar skills, `AGENTS.md` o `AGENTS.md`:** una sola fuente de verdad por
  regla (apunta a ella en vez de copiarla), no repitas lo que ya dicen `package.json`, el CI
  o la estructura de carpetas, y que cada paso termine en un criterio comprobable de
  "terminado". Una descripción de skill nombra solo casos de uso distintos entre sí.

## Reglas de alcance

- Tecnología real: Laravel 13 + MongoDB (`laravel-mongodb`), token Bearer propio
  (`mobile_api_tokens`, no JWT ni Sanctum), Nuxt 4, Android nativo Kotlin + Jetpack
  Compose. Nunca Next.js, Expo, Mongoose ni JWT.
- `mobil` (Expo) está descontinuado; no es evidencia ni se toca.
- La API `/api/v1` es contrato para web y Android: cambios aditivos, con prueba.
- Terraform: aún no existe en ningún repositorio; su ubicación la define **T112**
  (Alan). No crear archivos `.tf` antes de que T112 esté en `main`.
- Secretos: nunca en código, `.tf`, docs ni en la app Android (solo claves públicas
  como `pk_test_...`). `docs/ACCESOS.md` de `barber` es la única fuente de credenciales.
- Diseños (Stitch, Figma) se revisan contra la HU que implementan antes de codificar.

## Tareas de este repositorio (UrbanBladeMobile)

| Sprint | ID | Historia | Tarea | Pts | Responsable | Estado 23-sep | Depende de |
|---|---|---|---|---|---|---|---|
| 1 | T094 | HU-27 Comunicación frontend-backend | Sincronizar y validar la información compartida entre la app móvil, la plataforma web y la base de datos | 3 | María | En progreso | — |
| 1 | T144 | HU-17 Pago de la cita | Completar y probar el pago con tarjeta (Stripe PaymentSheet) en la app Android — código en `main` (0c57e2c); falta prueba en staging | 1 | Luis | En progreso | — |
| 2 | T084 | HU-25 Aplicación móvil Android | Realizar pruebas de la aplicación en dispositivos móviles | 1 | Miguel | Pendiente | — |
| 2 | T142 | HU-16 Recordatorio de citas | Integrar notificaciones push (Firebase Cloud Messaging) en la app Android — código en `main` (3de683a); falta `google-services.json` y prueba | 3 | Luis | Pendiente | `google-services.json` de Firebase (Alan); push real requiere T143 |
| 2 | T145 | HU-25 Aplicación móvil Android | Implementar pruebas instrumentadas de la app Android (login, reserva y cancelación) | 3 | Miguel | Pendiente | — |
| 2 | TT03 | HT-01 Expiración de tokens y cierre por inactividad | Mostrar el aviso de sesión expirada en la web y en Android — parte Android (aviso de sesión expirada ante 401) | 1 | Elías | Pendiente | TT01 |
| 3 | TT32 | HT-12 Revisión de interfaz y sesión | Revisar el checklist de interfaz en la app Android y corregir hallazgos | 1 | Luis | Pendiente | — |
| 4 | T126 | HU-37 Integración de la infraestructura con backend y base de datos | Validar el funcionamiento de la API desde la app Android y la plataforma web en ese ambiente | 1 | Miguel | Pendiente | T124 |
| 4 | T141 | HU-38 Pipeline de integración continua | Configurar el pipeline de integración continua de la app Android (Gradle: lint y pruebas unitarias) | 3 | Luis | Pendiente | Script `gradlew` (no está en el repo; agregarlo en esta tarea) |
| 5 | T095 | HU-27 Comunicación frontend-backend | Realizar pruebas de integración entre la app Android, el backend y la plataforma web | 1 | Miguel | Pendiente | T094 |
| 5 | T132 | HU-39 Construcción automática de las aplicaciones | Automatizar la generación del build firmado (APK/AAB) de la app Android | 3 | Luis | Pendiente | T141 |
| 5 | TT22 | HT-08 Dependencias, imágenes y licencias (SCA) | Activar Dependabot y documentar el proceso de actualización de dependencias — Dependabot de Gradle | 1 | Luis | Pendiente | — |

### Pruebas de QA que tocan este repositorio (Miguel y María)

| Sprint | ID | Historia | Tarea | Pts | Responsable | Estado 23-sep | Depende de |
|---|---|---|---|---|---|---|---|
| 1 | T102 | HU-29 Pruebas funcionales | Registrar los resultados y los errores encontrados | 1 | Miguel | Pendiente | — |
| 1 | T103 | HU-30 Pruebas de integración | Preparar escenarios de integración | 1 | Miguel | En progreso | — |
| 4 | T104 | HU-30 Pruebas de integración | Probar la integración entre frontend, backend y MongoDB contra la API real (sin simulación) | 3 | Miguel | Pendiente | T103 |
| 5 | T105 | HU-30 Pruebas de integración | Registrar los resultados y verificar las correcciones aplicadas | 1 | Miguel | Pendiente | T104 |
| 6 | T108 | HU-31 Preparar sistema para producción | Realizar pruebas finales del sistema | 1 | Miguel | Pendiente | T107 |
| 6 | T111 | HU-32 Documentación del proyecto | Revisar y organizar la documentación final | 1 | Miguel | Pendiente | — |

## Validación antes de entregar el commit

No hay `gradlew` en el repositorio (solo `gradle/wrapper/gradle-wrapper.properties`,
Gradle 8.13). Mientras T141 no lo agregue:

- Android Studio → panel **Gradle** → `app › Tasks › verification › testDebugUnitTest`,
  o clic derecho en `app/src/test/java` → *Run 'Tests in java'*.
- Prueba en el celular físico (variante `debug`): `adb reverse tcp:8000 tcp:8000` y
  `DEBUG_API_BASE_URL=http://127.0.0.1:8000/api/v1/` en `local.properties`.
- Pagos con tarjeta: variante `staging` + `STRIPE_PUBLISHABLE_KEY=pk_test_...`;
  tarjetas `4242 4242 4242 4242` (aprobada) y `4000 0000 0000 0002` (rechazada).
- Logs: `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -d --pid=<pid>`.

Ver también la skill `urbanblade-mobile-android`.
