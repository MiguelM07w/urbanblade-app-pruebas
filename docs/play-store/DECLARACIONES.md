# Declaraciones de Play Console (contenido de la app)

Respuestas propuestas para *Contenido de la app*, cruzadas con el código, el manifiesto final de release y el Aviso de
Privacidad el 2026-10-19. Los formularios cambian: contrastar con la consola. Los puntos ⚠️ necesitan una confirmación del equipo.

## 1. Respuestas directas

| Declaración | Respuesta | Por qué |
| --- | --- | --- |
| Anuncios | **No contiene anuncios** | No hay SDK de publicidad; el permiso de ID de publicidad se elimina del manifiesto (verificado en el manifiesto final) |
| Público objetivo | **18 años o más** | El Aviso de Privacidad exige ser mayor de edad para crear cuenta; la app no se dirige a niños |
| Aplicación de gobierno | No | |
| Aplicación de noticias | No | |
| Funciones financieras | **Ninguna** (no es banca, préstamos ni inversión) | Solo cobra servicios y productos de la barbería |
| Aplicación de salud | No | |
| COVID-19 | No | |
| Permisos de alto riesgo (SMS, llamadas, ubicación en segundo plano, todos los archivos) | **Ninguno** | Manifiesto final sin esos permisos |
| Seguridad de los datos | Ver `SEGURIDAD_DE_LOS_DATOS.md` | |
| Nivel de API objetivo | Cumple: `targetSdk` 36 | |
| Formato | AAB, con 64 bits | Verificado |

## 2. Clasificación de contenido (cuestionario IARC)
Categoría: *Utilidad / productividad / comunicación* (la app es de reservas y compras). Respuestas esperadas:

| Pregunta | Respuesta |
| --- | --- |
| Violencia, sangre, miedo | No |
| Contenido sexual o desnudos | No |
| Lenguaje vulgar | No (ver contenido de usuarios abajo) |
| Sustancias controladas | No |
| Juegos de azar con dinero real | No (ver ⚠️ sorteos) |
| ¿Los usuarios pueden interactuar o compartir contenido? | **Sí** (comentarios, reseñas) |
| ¿Se comparte la ubicación? | No |
| ¿Compras de bienes físicos o servicios? | Sí (servicios de barbería y productos; sin Play Billing) |

⚠️ **Sorteos:** la app tiene sorteos como beneficio de lealtad ("Niveles, puntos, sorteos y cumpleaños"). Solo se declara "sin
juegos de azar" si **participar no exige pagar** (los puntos o el nivel no se compran con dinero para entrar). Si hay un costo de
entrada o un premio en dinero, la política de juegos de azar de Google aplica y habría que reformular la función.

## 3. Pagos y facturación
Los cobros son por **servicios y productos físicos** (cortes, tienda con recogida en sucursal), así que Google permite un
procesador externo (Stripe) y **no hace falta Play Billing**.

⚠️ **Membresías, paquetes y tarjetas de regalo:** para seguir fuera de Play Billing deben canjearse **solo por servicios o
productos físicos** de la barbería. Si alguna membresía desbloqueara funciones digitales dentro de la app, esa compra tendría
que pasar por Play Billing. Confirmar con el equipo qué da exactamente cada una.

## 4. Acceso a la app para el revisor
Google prueba la app. Hay que darle una cuenta que **funcione sin pasos manuales**:

- Crear una **cuenta demo de cliente** en el entorno que use el AAB, con correo y contraseña (no con Google), sin autenticación
  en dos pasos y sin datos de personas reales. **Las credenciales se escriben solo en Play Console** (*Acceso a la app*); nunca en
  el repositorio.
- Instrucciones para el revisor (plantilla, rellenar solo en la consola):

```
La app permite reservar citas de barbería. Para probarla:
1. Inicia sesión con el correo y la contraseña indicados (no uses "Continuar con Google").
2. Pestaña "Reservar" → elige un servicio → barbero, día y hora → "Continuar sin extras".
3. En el paso de pago elige "Efectivo" para completar la reserva sin cobro, o "Tarjeta"; marca la casilla de la política de inasistencia y pulsa "Reservar cita".
4. "Mis citas" muestra la reserva; se puede reagendar o cancelar.
5. En "Cuenta" → "Seguridad" está la opción "Eliminar mi cuenta".
[Solo Camino A:] Los pagos con tarjeta usan el modo de prueba de Stripe (tarjeta 4242 4242 4242 4242, cualquier fecha futura y CVC): no se cobra dinero real.
```

- ⚠️ Antes de enviar, comprobar que la cuenta demo tenga **horarios libres** en los próximos días y que la API del entorno esté arriba.

## 5. Eliminación de cuenta (obligatoria)
Google pide dos cosas: que se pueda eliminar **dentro de la app** y una **URL web** que explique cómo pedirlo sin instalar la app.

| | Dónde | Estado |
| --- | --- | --- |
| En la app | Cuenta → Seguridad → **Eliminar mi cuenta** (con contraseña, o escribiendo ELIMINAR si entró con Google) | ✅ existe |
| En la web | Mi perfil → Zona de peligro | ✅ existe, pero exige iniciar sesión |
| Explicación pública | `https://urbanblade.com.mx/privacidad`, sección 6 (también por correo, desde el correo de la cuenta) | ✅ existe |

URL para la consola: `https://urbanblade.com.mx/privacidad`. La página no tiene anclas por sección, así que Google llegará
al inicio del documento. **Mejora pequeña recomendada** (en `frontend-urban`): ponerle `id="derechos"` al título de la sección 6
y usar `https://urbanblade.com.mx/privacidad#derechos`, o crear una página corta `/eliminar-cuenta` con los pasos.

## 6. Contenido generado por usuarios — implementado (pendiente de merge)
La app permite **comentar publicaciones** del muro y la política de Google para contenido de usuarios pide: términos que
prohíban contenido ofensivo, un mecanismo **dentro de la app** para reportar/bloquear, y moderación. Cubierto así:

| Requisito | Dónde | PR |
|---|---|---|
| Reportar comentario (spam / ofensivo / sexual o violento / otro) y bloquear autor | API `POST social/comment/{id}/report` y `.../block-author` en `barber`; menú "⋮" en la app | barber#31, UrbanBladeMobile#40 |
| Moderación del administrador (eliminar, eliminar y vetar, descartar) | API `admin/comment-reports` y página web `/admin/moderacion` | barber#31, frontend_Urbanblade#27 |
| Términos que prohíben contenido ofensivo y explican reportar/bloquear | `terminos.vue`, sección 9 | frontend_Urbanblade#27 |

Para la declaración en Play Console: marcar que la app **tiene contenido generado por usuarios** y que ofrece reporte,
bloqueo y moderación. Esto solo es cierto una vez mergeados los tres PR **y desplegado el backend**; la app de la tienda
apunta a producción. Probar en el teléfono antes de subir: abrir el muro, ⋮ en un comentario ajeno, reportar y bloquear.

## 6.1 Contenido de terceros en el muro — propiedad intelectual
Las publicaciones del muro que hay hoy (comprobado en el teléfono el 2026-10-19) son videos y fotos que **no son del equipo**:
llevan marca de agua de TikTok de otros creadores (`@juanchostylez`, `@crownagency_`). La política de propiedad intelectual de
Google prohíbe distribuir contenido protegido sin permiso, y los revisores pueden verlo al abrir la app.

- Antes de producción: sustituir esas publicaciones por **trabajos propios** del equipo (o con permiso por escrito del autor).
- No usarlas en las capturas de la ficha (ya se descartó la captura del muro).
- Es contenido de demostración cargado por el propio equipo, no por clientes; el riesgo es de la **app**, no del usuario final.

## 7. Otras comprobaciones
- Enlaces profundos: `urbanblade://open` (esquema propio): sin verificación de App Links.
- Inicio de sesión con Google: tras subir el primer AAB, registrar la SHA-1 de "Firma de la app" en Firebase y Google Cloud
  (`../PUBLICAR_PLAY_STORE.md`, sección 3). Sin eso, el revisor no podría usar Google (por eso la cuenta demo es de correo).
- Contacto del desarrollador: Play muestra el correo públicamente; considerar un alias del equipo en lugar del correo personal.
