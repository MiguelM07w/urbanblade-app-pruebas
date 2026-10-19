# Material para Google Play

Todo lo que se pega o se sube en Play Console, listo para copiar. El estado general y el orden de los pasos están en
[`../PUBLICAR_PLAY_STORE.md`](../PUBLICAR_PLAY_STORE.md). **Léelo primero**: hay una decisión pendiente (Stripe en modo de
prueba o real) que cambia varios textos.

| Archivo | Para qué sirve | Dónde se usa en Play Console |
| --- | --- | --- |
| [`FICHA.md`](FICHA.md) | Nombre, descripciones corta y larga, categoría, contacto, URLs y plan de capturas | Presencia en Play Store → Ficha principal |
| [`SEGURIDAD_DE_LOS_DATOS.md`](SEGURIDAD_DE_LOS_DATOS.md) | Formulario de seguridad de los datos, respondido según el código y el manifiesto final | Contenido de la app → Seguridad de los datos |
| [`DECLARACIONES.md`](DECLARACIONES.md) | Anuncios, público objetivo, clasificación, acceso para el revisor, contenido de usuarios, eliminación de cuenta | Contenido de la app (resto de declaraciones) |
| [`PRUEBA_CERRADA.md`](PRUEBA_CERRADA.md) | Plan de los 12 testers durante 14 días | Pruebas → Prueba cerrada |
| [`assets/`](assets/) | Ícono 512×512 y gráfico de funciones 1024×500 (24 bits, sin transparencia) | Ficha principal → Gráficos |

Scripts en [`../../scripts/play/`](../../scripts/play/): `crear-keystore.ps1` y `generar-aab.ps1`.

## Qué no está aquí (a propósito)
- **Credenciales**: ninguna contraseña, llave ni cuenta demo se guarda en el repositorio. El acceso del revisor y las claves
  van solo en Play Console y en `local.properties` (que git ignora).
- **Capturas de pantalla**: se toman del teléfono y se guardan fuera del repositorio; el plan está en `FICHA.md`.

## Verificado y no verificado
Los datos técnicos (permisos, SDK, tamaño, arquitecturas) salen de un `bundleRelease` real del 2026-10-19. Los **formularios
de Play Console cambian con el tiempo** y no pude abrirlos desde aquí: las respuestas siguen las definiciones de Google que
conozco y deben contrastarse con lo que muestre la consola al llenarlas.
