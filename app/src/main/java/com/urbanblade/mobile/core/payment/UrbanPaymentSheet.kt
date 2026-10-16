package com.urbanblade.mobile.core.payment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import com.urbanblade.mobile.BuildConfig
import com.urbanblade.mobile.ui.theme.UrbanColors

/**
 * true solo si el build trae una publishable key real de Stripe. Sin ella
 * (valor por defecto "pk_test_PENDIENTE_CONFIGURAR" en app/build.gradle.kts)
 * el PaymentSheet fallaría al abrirse, así que la pantalla de pago oculta la
 * opción de tarjeta en lugar de mostrar un error de Stripe al cliente.
 */
fun isStripeConfigured(key: String = BuildConfig.STRIPE_PUBLISHABLE_KEY): Boolean =
    key.startsWith("pk_") && !key.contains("PENDIENTE")

/**
 * Inicializa Stripe con la publishable key una sola vez por pantalla y devuelve si la tarjeta está
 * disponible en este build. Reemplaza el `remember { PaymentConfiguration.init(...) }` que devolvía
 * Unit (lint RememberReturnType lo marcaba como error y frenaba el CI de Android, T141).
 */
@Composable
fun rememberStripeReady(): Boolean {
    val context = LocalContext.current
    return remember {
        isStripeConfigured().also { ready ->
            if (ready) PaymentConfiguration.init(context, BuildConfig.STRIPE_PUBLISHABLE_KEY)
        }
    }
}

/**
 * Colores de la hoja de Stripe con el tema elegido en la app (no el modo claro/oscuro del
 * sistema): Stripe escoge colorsLight o colorsDark según el sistema, así que ambos llevan la
 * misma paleta de UrbanBlade y la hoja se ve igual que el resto de la app en los cuatro temas.
 */
private fun urbanStripeColors() = PaymentSheet.Colors(
    primary = UrbanColors.Gold.toArgb(),
    surface = UrbanColors.Background.toArgb(),
    component = UrbanColors.Card.toArgb(),
    componentBorder = UrbanColors.Line.toArgb(),
    componentDivider = UrbanColors.Line.toArgb(),
    onComponent = UrbanColors.Ink.toArgb(),
    onSurface = UrbanColors.Ink.toArgb(),
    subtitle = UrbanColors.Muted.toArgb(),
    placeholderText = UrbanColors.Muted.toArgb(),
    appBarIcon = UrbanColors.Ink.toArgb(),
    error = UrbanColors.Danger.toArgb()
)

/**
 * Envoltura delgada sobre el PaymentSheet de Stripe: inicializa la
 * publishable key una sola vez y expone una función simple
 * (clientSecret) -> Unit para presentar la hoja de pago. Reusable por el
 * checkout de citas y, en la siguiente ronda, por paquetes/membresías/gift
 * cards -- la confirmación real del pago siempre ocurre por webhook en
 * barber, nunca aquí (ver StripeWebhookController).
 */
@Composable
fun rememberUrbanPaymentSheet(
    merchantDisplayName: String = "UrbanBlade",
    onResult: (PaymentSheetResult) -> Unit
): (String) -> Unit {
    rememberStripeReady()
    val paymentSheet = rememberPaymentSheet(onResult)

    return remember(paymentSheet) {
        { clientSecret: String ->
            // Los colores se leen al abrir la hoja: si el cliente cambió de tema, ya salen con el nuevo.
            val appearance = PaymentSheet.Appearance(
                colorsLight = urbanStripeColors(),
                colorsDark = urbanStripeColors(),
                shapes = PaymentSheet.Shapes(cornerRadiusDp = 16f, borderStrokeWidthDp = 1f)
            )
            paymentSheet.presentWithPaymentIntent(
                clientSecret,
                PaymentSheet.Configuration(merchantDisplayName = merchantDisplayName, appearance = appearance)
            )
        }
    }
}
