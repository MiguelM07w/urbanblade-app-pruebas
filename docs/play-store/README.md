# Material para Google Play

Todo lo que se pega o se sube en Play Console, listo para copiar. El estado general y el orden de los pasos están en
[`../PUBLICAR_PLAY_STORE.md`](../PUBLICAR_PLAY_STORE.md). **Léelo primero**: hay una decisión pendiente (Stripe en modo de
prueba o real) que cambia varios textos.

| Archivo | Para qué sirve | Dónde se usa en Play Console |
| --- | --- | --- |
| [`CREAR_CUENTA.md`](CREAR_CUENTA.md) | Paso a paso para crear la cuenta de Play Console y pagar los $25 USD (lo hace el titular) | Antes de todo |
| [`FICHA.md`](FICHA.md) | Nombre, descripciones corta y larga, categoría, contacto, URLs y plan de capturas | Presencia en Play Store → Ficha principal |
| [`SEGURIDAD_DE_LOS_DATOS.md`](SEGURIDAD_DE_LOS_DATOS.md) | Formulario de seguridad de los datos, respondido según el código y el manifiesto final | Contenido de la app → Seguridad de los datos |
| [`DECLARACIONES.md`](DECLARACIONES.md) | Anuncios, público objetivo, clasificación, acceso para el revisor, contenido de usuarios, eliminación de cuenta | Contenido de la app (resto de declaraciones) |
| [`BITACORA_CONSOLA.md`](BITACORA_CONSOLA.md) | Qué pidió cada formulario de la consola y las trampas encontradas | Referencia al repetir o actualizar |
| [`PRUEBA_CERRADA.md`](PRUEBA_CERRADA.md) | Plan de los 12 testers durante 14 días | Pruebas → Prueba cerrada |
| [`assets/`](assets/) | Ícono 512×512 y gráfico de funciones 1024×500 (24 bits, sin transparencia) | Ficha principal → Gráficos |

Scripts en [`../../scripts/play/`](../../scripts/play/): `crear-keystore.ps1` y `generar-aab.ps1`.

## Qué no está aquí (a propósito)
- **Credenciales**: ninguna contraseña, llave ni cuenta demo se guarda en el repositorio. El acceso del revisor y las claves
  van solo en Play Console y en `local.properties` (que git ignora).
- **Capturas de pantalla**: se guardan fuera del repositorio (ya están subidas a la ficha); el plan está en `FICHA.md`.

## Verificado y no verificado
Los datos técnicos (permisos, SDK, tamaño, arquitecturas) salen de un `bundleRelease` real. Estas respuestas **ya se
capturaron en Play Console el 2026-10-20/21** y la consola las aceptó: ficha, declaraciones, clasificación (IARC), seguridad de
los datos (importada por CSV) y prueba cerrada. Los formularios de Google cambian con el tiempo; si algo no coincide,
manda lo que muestre la consola. Las trampas encontradas están en [`BITACORA_CONSOLA.md`](BITACORA_CONSOLA.md).
