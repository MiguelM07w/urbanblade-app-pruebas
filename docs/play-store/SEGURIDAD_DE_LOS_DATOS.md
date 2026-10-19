# Seguridad de los datos — respuestas para Play Console

Formulario: *Contenido de la app → Seguridad de los datos*. Las respuestas salen de tres fuentes cruzadas el 2026-10-19:
el **código** de la app (SDK y llamadas a la API), el **manifiesto final** de un `bundleRelease` real y el **Aviso de
Privacidad** publicado. Google exige que lo declarado coincida con lo que la app hace; si algo cambia, se actualiza aquí
y en la consola.

> **Importante:** el formulario de Play Console cambia con el tiempo y no pude abrirlo desde aquí. Las categorías de
> abajo siguen las definiciones de Google que conozco; contrastarlas con lo que muestre la consola, sobre todo la
> distinción entre *recopilar* y *compartir*. Los puntos marcados con ⚠️ requieren que el equipo confirme un hecho.

## 1. Preguntas generales

| Pregunta | Respuesta |
| --- | --- |
| ¿La app recopila o comparte datos de usuario requeridos? | **Sí** |
| ¿Todos los datos recopilados se cifran en tránsito? | **Sí** (HTTPS en release, sin tráfico en claro; con *certificate pinning* hacia la API) |
| ¿Se ofrece a los usuarios una forma de solicitar la eliminación de sus datos? | **Sí** (dentro de la app y en la web; ver `DECLARACIONES.md`) |
| ¿Cumple la política de Familias de Play? | **No aplica** (la app no está dirigida a menores) |
| ¿Validación de seguridad independiente? | No |

## 2. Tipos de datos

*Recopilado* = la app o su servidor lo reciben. *Compartido* = se transfiere a un tercero que no actúa solo como proveedor
de servicio por cuenta de UrbanBlade; Google no considera "compartir" el envío a proveedores que procesan los datos
en nombre de la app (AWS, MongoDB Atlas, Stripe como procesador de pago, Firebase) ni los datos que el usuario inicia
hacia un procesador de pagos. Por eso la columna *Compartido* queda en **No**, salvo la nota ⚠️ de Wikipedia.

### Información personal
| Tipo | Recopilado | Compartido | Opcional | Finalidades | Fuente |
| --- | --- | --- | --- | --- | --- |
| Nombre | Sí | No | No | Funcionalidad de la app, gestión de cuenta | Registro; Google Sign-In |
| Dirección de correo | Sí | No | No | Funcionalidad de la app, gestión de cuenta, comunicaciones del desarrollador | Registro; Google Sign-In |
| ID de usuario | Sí | No | No | Funcionalidad de la app, gestión de cuenta | Cuenta de UrbanBlade |
| Número de teléfono | Sí | No | ⚠️ confirmar | Funcionalidad de la app, gestión de cuenta | Perfil de cliente |
| Otra información (fecha de nacimiento, sexo) | Sí | No | Sí (el sexo es opcional; ⚠️ confirmar la fecha) | Funcionalidad de la app (regalo de cumpleaños), personalización | Perfil de cliente |

### Información financiera
| Tipo | Recopilado | Compartido | Opcional | Finalidades | Fuente |
| --- | --- | --- | --- | --- | --- |
| Historial de compras | Sí | No | No | Funcionalidad de la app | Pagos de citas, pedidos de la tienda, membresías, tarjetas de regalo |
| Información de pago del usuario | Sí (la captura Stripe; UrbanBlade solo guarda marca, últimos 4 dígitos y vencimiento) | No (procesador de pagos) | Sí (se puede pagar en efectivo o por transferencia) | Funcionalidad de la app | Formulario de tarjeta de Stripe |

El número completo de la tarjeta **nunca llega a los servidores de UrbanBlade** (lo dice el Aviso de Privacidad y el código usa
el formulario de Stripe).

### Fotos y videos
| Tipo | Recopilado | Compartido | Opcional | Finalidades | Fuente |
| --- | --- | --- | --- | --- | --- |
| Fotos | Sí | No | Sí | Funcionalidad de la app | Foto de perfil, comprobantes de transferencia, trabajos del muro |
| Videos | Sí | No | Sí | Funcionalidad de la app | Publicaciones del muro (las suben barberos y personal) |

La app usa el **selector de fotos del sistema**: no pide permisos de galería ni de cámara.

### Mensajes
| Tipo | Recopilado | Compartido | Opcional | Finalidades | Fuente |
| --- | --- | --- | --- | --- | --- |
| Otros mensajes dentro de la app | Sí | No (ver ⚠️) | Sí | Funcionalidad de la app | Chat con Bladebot (se guarda el historial con la sesión iniciada) |

⚠️ **Wikipedia:** para algunas preguntas generales, Bladebot envía el **texto de la pregunta** (sin nombre ni datos de la
cuenta) a Wikipedia. Es un envío a un tercero que no es un proveedor de servicio. Decidir con el equipo si se declara como
*compartido* (lo prudente) y confirmar que el Aviso de Privacidad lo siga diciendo.

✅ **Proveedor de IA (comprobado el 2026-10-19):** el Aviso de Privacidad afirma que el modelo "corre en nuestros propios
servidores" y que los mensajes no se envían a empresas externas de IA. En el código el valor por defecto de
`CHATBOT_AI_PROVIDER` es `gemini`, pero el backend al que apunta la app (staging en AWS) tiene `CHATBOT_AI_PROVIDER=ollama`
y el servicio `uba-stg-ollama` corriendo en el propio clúster. El aviso es verdadero hoy; **si el valor cambiara a `gemini`
habría que declarar los mensajes como compartidos con Google y corregir el aviso.**

### Actividad en la app
| Tipo | Recopilado | Compartido | Opcional | Finalidades | Fuente |
| --- | --- | --- | --- | --- | --- |
| Interacciones con la app | Sí | No | No | Análisis | Firebase Analytics: pantallas y pasos de la reserva; sin datos personales |
| Otro contenido generado por el usuario | Sí | No | Sí | Funcionalidad de la app | Reseñas, comentarios, reacciones y notas para el barbero |

### Información y rendimiento de la app
| Tipo | Recopilado | Compartido | Opcional | Finalidades | Fuente |
| --- | --- | --- | --- | --- | --- |
| Registros de fallos | Sí | No | No | Análisis | Firebase Crashlytics |
| Diagnósticos | Sí | No | No | Análisis | Firebase Performance (arranque y latencia de la API) |
| Otros datos de rendimiento | Sí | No | No | Análisis | Firebase Performance |

### Identificadores de dispositivo o de otro tipo
| Tipo | Recopilado | Compartido | Opcional | Finalidades | Fuente |
| --- | --- | --- | --- | --- | --- |
| ID de dispositivo u otros ID | Sí | No | No | Funcionalidad de la app (notificaciones), análisis | Token de FCM e identificador de instalación de Firebase |

## 3. Lo que **no** se recopila
Ubicación (aproximada o precisa), contactos, calendario, audio, archivos y documentos, historial de navegación, apps
instaladas, salud y actividad física, y **ID de publicidad**: el permiso se elimina del manifiesto (verificado en el
manifiesto final; PR #17) y la app no muestra anuncios.

⚠️ **Salud:** el Aviso de Privacidad dice que el personal puede anotar en la ficha del cliente datos como "una alergia".
Si esas notas se capturan desde la app o llegan a ella, Google puede considerarlo *información de salud*. Confirmar con el
equipo dónde se escriben esas notas; si es desde la app, declarar **Salud → Información de salud** como recopilada
(funcionalidad de la app, opcional) y revisar el texto del aviso.

## 4. Prácticas de seguridad
- Cifrado en tránsito: **sí**.
- Eliminación de datos: **sí**. Al eliminar la cuenta se borra el acceso y se cierran las sesiones; el historial de citas y
  pagos ya realizados puede conservarse como historial del negocio y se elimina si el usuario lo pide por correo. Esto debe
  ser exactamente lo que diga el formulario de eliminación de la consola.

## 5. Pendientes del equipo para cerrar este formulario
- [ ] Confirmar si el teléfono y la fecha de nacimiento son obligatorios u opcionales.
- [ ] Decidir cómo declarar la consulta a Wikipedia.
- [x] Confirmar `CHATBOT_AI_PROVIDER=ollama` (hecho el 2026-10-19 en el backend que usa la app; repetir si cambia el entorno).
- [ ] Confirmar dónde se capturan las notas internas (alergias).
- [ ] Contrastar cada categoría con el formulario vivo de Play Console.
