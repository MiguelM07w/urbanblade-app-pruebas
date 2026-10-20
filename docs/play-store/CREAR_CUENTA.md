# Crear la cuenta de Google Play Console (paso a paso)

**Enlace oficial para empezar:** <https://play.google.com/console/signup>
(si Google cambió la ruta: entra a <https://play.google.com/console> y pulsa **Crear cuenta**).
Ayuda oficial de Google sobre este trámite: <https://support.google.com/googleplay/android-developer/answer/6112435>

Este trámite lo hace **el titular de la cuenta, en su propio navegador**: son datos personales, un documento oficial y un pago.
Ninguna herramienta (ni la IA) debe escribir tus datos, aceptar el contrato ni pagar por ti. La ayuda posible es guiarte y
revisar capturas de lo que te salga **sin mostrar datos personales, números de tarjeta ni documentos**.

> Los pasos describen el proceso como lo conozco; Google cambia el orden y los textos. **Si la pantalla dice algo distinto, manda
> la pantalla**, y me cuentas qué dice (o me pasas una captura con los datos tapados).

## 0. Antes de empezar (reúne esto)
- [ ] **Quién será el titular.** Una persona mayor de 18 años. La cuenta queda ligada a su identidad y **no se transfiere fácil**
  entre personas; elige a quien vaya a conservarla hasta el final del proyecto.
- [ ] **Una cuenta de Google** del titular (la que usará siempre para Play Console) con la verificación en dos pasos activada.
- [ ] **Documento oficial vigente** (INE o pasaporte) para la verificación de identidad.
- [ ] **Tarjeta de crédito o débito internacional** para pagar **$25 USD** (pago único). Algunas tarjetas de débito o virtuales
  no se aceptan; ten una segunda opción.
- [ ] **Un teléfono Android con Play Store** a la mano: Google puede pedir confirmar el número y el dispositivo.
- [ ] **Correo y teléfono de contacto.** El correo del desarrollador **se muestra públicamente** en la ficha: conviene uno del
  equipo en lugar de uno personal.
- [ ] **Sitio web:** `https://urbanblade.com.mx` (ya existe y tiene la política de privacidad en `/privacidad`).

## 1. Registro
1. Abre el enlace con la cuenta de Google del titular.
2. **Tipo de cuenta: "Personal" (para ti mismo).** No elijas "Organización": exige un número D-U-N-S y una empresa constituida,
   y UrbanBlade no lo es. Esto implica el requisito de **12 testers durante 14 días** antes de producción (ver `PRUEBA_CERRADA.md`).
3. **Nombre del desarrollador.** Se muestra bajo el nombre de la app. Confirma en pantalla si debe coincidir con tu nombre legal.
4. **Datos de contacto:** correo y teléfono. Google envía **códigos de verificación**; escríbelos tú.
5. **Sitio web** del desarrollador y **dirección** del titular (la usan para verificar tu identidad).
6. Lee el **Acuerdo de distribución para desarrolladores** y, si estás de acuerdo, acéptalo tú: es un contrato a tu nombre.

## 2. Pago de $25 USD
- Lo capturas **tú** con tu tarjeta. Revisa que el cargo sea de **25 USD, pago único**. Si te piden suscripción o un monto distinto,
  detente y pregunta.
- Guarda el recibo (te llega por correo).

## 3. Verificación de identidad
- Google pide una **foto del documento** y puede pedir una **selfie o un video corto**. El nombre del documento debe coincidir
  con el de la cuenta.
- Puede pedir instalar la **app Play Console** en un Android para verificar el dispositivo.
- **Tarda de unas horas a varios días.** Mientras no termine no puedes publicar nada, ni siquiera una prueba cerrada.
- Si la rechazan, el correo dice el motivo (foto borrosa, nombre distinto…). Se corrige y se vuelve a enviar.

## 4. Cuando la cuenta esté aprobada
Avísame y seguimos con lo que ya está preparado:
1. **Crear la app** en Play Console: nombre `UrbanBlade`, idioma español (México), **aplicación**, **gratuita**, y las declaraciones
   de política (se responden con `DECLARACIONES.md`).
2. **Llenar la ficha** con `FICHA.md` y subir `assets/` y las capturas.
3. **Seguridad de los datos** con `SEGURIDAD_DE_LOS_DATOS.md`.
4. Crear la **llave de firma** (`scripts/play/crear-keystore.ps1`) y el **AAB** (`scripts/play/generar-aab.ps1`).
5. Subir el AAB a una **prueba interna** y luego a la **prueba cerrada** con los 12 testers.

## Dónde me puedes pedir ayuda mientras haces el trámite
- "Me salió esta pantalla y dice …" (pega el texto, sin datos personales).
- "¿Qué elijo aquí?" — te digo qué conviene para UrbanBlade y por qué.
- "Me rechazaron la verificación" — pega el motivo del correo.

## Qué NO hacer
- No compartas contraseñas, códigos de verificación, fotos del documento ni números de tarjeta con nadie, ni conmigo.
- No pagues más de $25 USD ni aceptes una suscripción.
- No crees la cuenta como "Organización" para "ganar tiempo": pide documentos de una empresa que no existe.
- No uses una cuenta de Google que puedas perder (de la escuela, por ejemplo): si se cierra, pierdes la cuenta de Play y sus apps.
