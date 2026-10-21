# Bitácora de Play Console (2026-10-19 a 2026-10-21)

Lo que se llenó, en orden, y las trampas que conviene conocer. No incluye correos, contraseñas ni la cuenta demo.

## Resultado final
| Área | Estado |
| --- | --- |
| Identidad y teléfono del titular | Verificados |
| App creada (`com.urbanblade.mobile`, gratuita) | Hecho |
| Prueba interna con AAB 3.0.0 (versionCode 3) | Publicada |
| Ficha: textos, ícono, gráfico de funciones, 5 capturas con la cuenta demo | Completa |
| Acceso para el revisor (cuenta demo) | Guardado solo en la consola |
| Anuncios: no; ID de publicidad: no | Declarado |
| Público objetivo: adultos; sin contenido para niños | Declarado |
| Clasificación IARC | Brasil 14+, ESRB Todos; pregunta de tarjetas de regalo en "No" (las membresías son gratuitas para el cliente y las asigna el administrador o recepción) |
| Seguridad de los datos | Importada por CSV y guardada |
| Contenido generado por usuarios | Reportar, bloquear y moderación declarados |
| Prueba cerrada | Aprobada y activa, México |

## Trampas
- **El navegador desplaza la página**: al hacer clic por referencia a veces la página se corre en horizontal; recargar lo arregla.
  Los clics por coordenadas son más fiables.
- **"¿Ir a Descripción general de la publicación?"**: elegir "Ahora no" salvo que se quiera enviar ya.
- **Clasificación**: "Siguiente" queda deshabilitado hasta pulsar "Guardar"; terminar con "Enviar".
- **Seguridad de los datos**: el CSV de exportación sirve de plantilla (el ID de pregunta lleva el formato
  `PSL_DATA_USAGE_RESPONSES:PSL_NAME:…`). Al importar puede salir el aviso de error `5E22D086` aunque el import se aplique;
  comprobar que todos los tipos quedan "Completado" y guardar.
- **Datos de contacto**: el teléfono se autocompleta con el personal; borrarlo antes de publicar si no debe verse.
- **Lista de testers**: crear la lista con el botón "Crear" del diálogo y comprobar que aparece. Agregar correos no envía
  invitaciones; hay que compartir el enlace de participación.
- **Aceptar es lo que cuenta**: el panel muestra 0 aceptados hasta que cada persona abre el enlace con su cuenta.
- **Cambios en revisión**: enviar los cambios pendientes desde "Descripción general de la publicación"; fueron 13 en el primer
  envío y Google los aprobó.
- **AAB mayor de 10 MB**: la automatización no lo sube; lo sube el titular a mano.
- **Compilar el AAB**: necesita `app/google-services.json` (fuera de git); `keytool` en Windows con idioma español falla, usar
  `-J-Duser.language=en`.

## Siguiente
1. Esperar a que 12 testers acepten; después, 14 días seguidos.
2. *Solicitar acceso a producción* y contestar el cuestionario con lo aprendido en la prueba.
3. Subir `versionCode` en cada versión nueva.
