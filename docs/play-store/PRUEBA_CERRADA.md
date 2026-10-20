# Prueba cerrada: 12 testers durante 14 días

Requisito de Google para **cuentas personales**: antes de pedir producción, la app debe estar en una pista de **prueba
cerrada con al menos 12 testers que se mantengan suscritos 14 días seguidos**. Verificar las cifras vigentes en la
consola (*Pruebas → Prueba cerrada*), porque Google puede cambiarlas.

## Cómo funciona
1. En Play Console: *Pruebas → Prueba cerrada → Crear pista* y subir el AAB.
2. Crear una **lista de testers** por correo de Google (o un Grupo de Google) y copiar el **enlace de participación**.
3. Cada tester abre el enlace **con la cuenta de Google que está en la lista**, acepta ser tester y **instala la app desde
   Play** (no sirve el APK por adb).
4. El conteo de 14 días solo avanza mientras haya 12 o más testers suscritos. Si alguien se da de baja y quedan menos de
   12, el reloj se detiene.
5. Pasados los 14 días, la consola habilita *Solicitar acceso a producción* y pide un cuestionario sobre la prueba.

## Plan práctico
- Reclutar **15 personas** (margen de 3 por si alguien abandona): equipo, familia, compañeros del grupo.
- Que cada tester use una cuenta de Google real que tenga en el teléfono Android que va a usar (se necesita Android con Play Store).
- Pedir un uso real, no solo instalar: reservar, ver el muro, cancelar. Google evalúa que haya participación.
- Pedir que reporten fallos o dudas a un solo canal (grupo de WhatsApp del equipo) y anotar lo que salga.

## Lista de testers (llenar; no subir correos al repositorio)
| # | Nombre | Cuenta de Google (solo en la consola) | Aceptó | Instaló | Notas |
| --- | --- | --- | --- | --- | --- |
| 1 | | | | | |
| … | | | | | |
| 15 | | | | | |

## Calendario orientativo
| Hito | Fecha estimada |
| --- | --- |
| Cuenta de Play creada y verificada | hacia el 23 de octubre |
| AAB subido a prueba cerrada y testers invitados | día 0 |
| Reloj de 14 días completo | día 14 (≈ 6 de noviembre si el día 0 es el 23 de octubre) |
| Solicitud de producción y revisión de Google | días 14 a 21 o más |

La fecha de entrega final del curso es el **4 de diciembre**: el margen existe, pero se acaba si un paso se retrasa o si la
revisión de producción rechaza la app (por ejemplo, por la brecha de contenido de usuarios de `DECLARACIONES.md`).

## Mientras corre la prueba
- No cambiar el `applicationId` ni la firma de la app.
- Cada AAB nuevo necesita un `versionCode` mayor (hoy 3).
- Atender los fallos que reporten: una app estable durante la prueba mejora la revisión.
