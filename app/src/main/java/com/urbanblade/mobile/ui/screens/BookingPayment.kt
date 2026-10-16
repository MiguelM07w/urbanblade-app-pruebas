package com.urbanblade.mobile.ui.screens

import android.content.res.ColorStateList
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.view.ContextThemeWrapper
import android.widget.EditText
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.view.CardMultilineWidget
import com.urbanblade.mobile.core.payment.cardDigits
import com.urbanblade.mobile.core.payment.detectCardBrand
import com.urbanblade.mobile.core.payment.expiryPreview
import com.urbanblade.mobile.core.payment.holderNameProblem
import com.urbanblade.mobile.core.payment.normalizeHolderName
import com.urbanblade.mobile.data.model.SavedCard
import com.urbanblade.mobile.data.model.TransferInfo
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.BookingPayMethod

/** Lo que el cliente eligió en el paso de pago de la reserva. */
class BookingPaymentState {
    var method by mutableStateOf(BookingPayMethod.EFECTIVO)
    var tipOption by mutableStateOf(TIP_NONE)
    var customTip by mutableStateOf("")
    var receiptUri by mutableStateOf<Uri?>(null)
    var useNewCard by mutableStateOf(false)
    var selectedCardId by mutableStateOf<String?>(null)
    var saveCard by mutableStateOf(true)

    /**
     * Cada vez que sube, el formulario de la tarjeta se desplaza a la vista. Antes, al elegir
     * Tarjeta el formulario quedaba abajo sin que se notara y «Reservar» solo mostraba un error.
     */
    var cardAttention by mutableIntStateOf(0)

    /** Formulario de Stripe en pantalla; no es estado de Compose, solo la vista viva para leer lo que escribió el cliente. */
    var cardWidget: CardMultilineWidget? = null

    /** Nombre impreso en la tarjeta nueva; se manda a Stripe como dato de facturación y se muestra en la tarjeta. */
    var holderName by mutableStateOf("")
    var holderTouched by mutableStateOf(false)

    /** Lo que se lleva escrito del número y del vencimiento, para dibujar la tarjeta en vivo. */
    var cardNumberDigits by mutableStateOf("")
    var cardExpiryText by mutableStateOf("")

    fun newCardPreview() = NewCardPreview(
        brand = detectCardBrand(cardNumberDigits),
        digits = cardNumberDigits,
        holder = holderName,
        expiry = cardExpiryText
    )

    /** Datos de la tarjeta nueva para Stripe, con el nombre del titular como dato de facturación. */
    fun paymentMethodParams(): PaymentMethodCreateParams? {
        val card = cardWidget?.paymentMethodCard ?: return null
        return PaymentMethodCreateParams.create(card, PaymentMethod.BillingDetails(name = holderName.trim().ifBlank { null }))
    }

    /** Qué falta para pagar con una tarjeta nueva, o null si se puede. */
    fun newCardProblem(): String? =
        holderNameProblem(holderName)
            ?: if (cardWidget?.paymentMethodCard == null) "Revisa los datos de tu tarjeta: número, vencimiento y CVC." else null

    /** Propina estimada sobre el precio de lista; barber la vuelve a validar y solo la suma aparte. */
    fun tipFor(base: Double): Double = when (tipOption) {
        TIP_10 -> base * 0.10
        TIP_15 -> base * 0.15
        TIP_OTHER -> customTip.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }.let { Math.round(it * 100) / 100.0 }

    /** Tarjeta guardada con la que se paga, o null si es tarjeta nueva (o el método no es tarjeta). */
    fun savedCardToUse(cards: List<SavedCard>): String? {
        if (method != BookingPayMethod.TARJETA || useNewCard || cards.isEmpty()) return null
        return selectedCardId?.takeIf { id -> cards.any { it.id == id } } ?: cards.first().id
    }

    companion object {
        const val TIP_NONE = 0
        const val TIP_10 = 1
        const val TIP_15 = 2
        const val TIP_OTHER = 3
    }
}

@Composable
fun rememberBookingPaymentState() = remember { BookingPaymentState() }

/** "visa" -> "Visa", como lo muestran las apps de pago. */
fun cardBrandLabel(brand: String): String = when (brand.lowercase()) {
    "visa" -> "Visa"
    "mastercard" -> "Mastercard"
    "amex" -> "American Express"
    "discover" -> "Discover"
    else -> brand.replaceFirstChar { it.uppercase() }.ifBlank { "Tarjeta" }
}

fun cardExpiryLabel(card: SavedCard): String = "Vence %02d/%02d".format(card.expMonth, card.expYear % 100)

private fun money(value: Double) = UrbanFormat.moneyShort(value)

/**
 * Paso de pago de la reserva, igual que la web: propina, total estimado y
 * los tres métodos (efectivo en el salón, transferencia con los datos
 * bancarios del negocio y tarjeta con tarjetas guardadas o una nueva).
 */
@Composable
fun BookingPaymentSection(
    state: BookingPaymentState,
    servicePrice: Double,
    transferInfo: TransferInfo?,
    savedCards: List<SavedCard>,
    cardAvailable: Boolean,
    testMode: Boolean,
    onPickReceipt: () -> Unit
) {
    val tip = state.tipFor(servicePrice)

    UrbanSectionTitle("Pago", "Elige cómo quieres pagar tu cita")
    Spacer(Modifier.height(16.dp))

    UrbanFieldLabel("Propina para tu barbero (opcional)")
    Spacer(Modifier.height(8.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "Sin propina" to BookingPaymentState.TIP_NONE,
            "10%" to BookingPaymentState.TIP_10,
            "15%" to BookingPaymentState.TIP_15,
            "Otro monto" to BookingPaymentState.TIP_OTHER
        ).forEach { (label, value) ->
            FilterChip(
                selected = state.tipOption == value,
                onClick = { state.tipOption = value },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = UrbanColors.Gold, selectedLabelColor = UrbanColors.OnGold)
            )
        }
    }
    if (state.tipOption == BookingPaymentState.TIP_OTHER) {
        Spacer(Modifier.height(8.dp))
        UrbanTextField(
            value = state.customTip,
            onValueChange = { state.customTip = it.filter { c -> c.isDigit() || c == '.' }.take(7) },
            label = "Monto de la propina",
            placeholder = "Monto en pesos",
            leadingIcon = Icons.Default.AttachMoney,
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
            imeAction = androidx.compose.ui.text.input.ImeAction.Done
        )
    }

    Spacer(Modifier.height(16.dp))
    UrbanPremiumCard(Modifier.fillMaxWidth()) {
        UrbanKeyValue("Servicio", money(servicePrice))
        if (tip > 0) {
            Spacer(Modifier.height(6.dp))
            UrbanKeyValue("Propina", money(tip))
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = UrbanColors.Muted.copy(alpha = 0.2f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total de la cita", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(money(servicePrice + tip), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = UrbanColors.GoldText)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Si tienes descuento por nivel o membresía, se aplica al cobrar.",
            style = MaterialTheme.typography.bodySmall,
            color = UrbanColors.Muted
        )
    }

    Spacer(Modifier.height(24.dp))
    UrbanFieldLabel("Método de pago")
    Spacer(Modifier.height(10.dp))
    // Propuesta A (25-sep): tres mosaicos lado a lado; debajo solo el detalle del método elegido.
    UrbanChoiceTiles(
        options = buildList {
            add(UrbanChoice(BookingPayMethod.EFECTIVO, "Efectivo", "En el salón", Icons.Default.Payments))
            add(UrbanChoice(BookingPayMethod.TRANSFERENCIA, "Transferencia", "SPEI", Icons.Default.AccountBalance))
            if (cardAvailable) add(UrbanChoice(BookingPayMethod.TARJETA, "Tarjeta", "Paga ahora", Icons.Default.CreditCard))
        },
        selected = state.method,
        onSelect = { state.method = it }
    )

    when (state.method) {
        BookingPayMethod.TRANSFERENCIA -> {
            Spacer(Modifier.height(16.dp))
            TransferDetails(transferInfo, servicePrice + tip, state.receiptUri != null, onPickReceipt)
        }
        BookingPayMethod.TARJETA -> {
            Spacer(Modifier.height(16.dp))
            CardDetails(state, savedCards, testMode)
        }
        BookingPayMethod.EFECTIVO -> {
            Spacer(Modifier.height(12.dp))
            UrbanInfoBanner("Pagas ${money(servicePrice + tip)} al llegar a recepción. Hoy no se hace ningún cargo.", Icons.Default.Storefront, Modifier.fillMaxWidth())
        }
    }
}

/** Datos bancarios del negocio con botón de copiar; el comprobante es opcional y puede subirse después. */
@Composable
private fun TransferDetails(info: TransferInfo?, total: Double, receiptPicked: Boolean, onPickReceipt: () -> Unit) {
    when {
        info == null -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = UrbanColors.Gold)
            Spacer(Modifier.width(10.dp))
            Text("Cargando datos bancarios…", style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
        }
        !info.configurado -> UrbanInfoBanner(
            "La barbería todavía no registra sus datos bancarios. Elige otro método o pregunta en recepción.",
            Icons.Default.Info
        )
        else -> {
            UrbanPremiumCard(Modifier.fillMaxWidth()) {
                Text("Transfiere a esta cuenta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                info.banco?.takeIf { it.isNotBlank() }?.let { BankRow("Banco", it, copyable = false) }
                info.beneficiario?.takeIf { it.isNotBlank() }?.let { BankRow("Beneficiario", it, copyable = false) }
                info.clabe?.let { BankRow("CLABE", it.chunked(4).joinToString(" "), copyValue = it, mono = true) }
                info.concepto?.takeIf { it.isNotBlank() }?.let { BankRow("Concepto", it) }
                BankRow("Monto", money(total), copyable = false, highlight = true)
            }
            Spacer(Modifier.height(12.dp))
            UrbanOutlineButton(
                text = if (receiptPicked) "Comprobante listo · cambiar" else "Adjuntar comprobante (opcional)",
                onClick = onPickReceipt,
                icon = if (receiptPicked) Icons.Default.CheckCircle else Icons.Default.Upload,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "¿Aún no transfieres? Reserva ahora y sube el comprobante después desde Mis citas.",
                style = MaterialTheme.typography.bodySmall,
                color = UrbanColors.Muted
            )
        }
    }
}

@Composable
private fun BankRow(
    label: String,
    value: String,
    copyValue: String = value,
    copyable: Boolean = true,
    mono: Boolean = false,
    highlight: Boolean = false
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = if (mono) FontFamily.Monospace else null,
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
                color = if (highlight) UrbanColors.Gold else UrbanColors.Ink
            )
        }
        if (copyable) {
            IconButton(onClick = {
                clipboard.setText(AnnotatedString(copyValue))
                Toast.makeText(context, "$label copiado", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.ContentCopy, "Copiar $label", tint = UrbanColors.Gold)
            }
        }
    }
}

/**
 * Tarjetas guardadas (como Spotify o Netflix) o una tarjeta nueva, con opción de guardarla.
 * También lo usa la contratación de membresía (Beneficios), donde Stripe ya guarda la tarjeta de
 * la suscripción: ahí va con [showSaveOption] en false.
 */
@Composable
internal fun CardDetails(state: BookingPaymentState, savedCards: List<SavedCard>, testMode: Boolean, showSaveOption: Boolean = true) {
    val usingSaved = state.savedCardToUse(savedCards)

    if (savedCards.isNotEmpty()) {
        UrbanFieldLabel("Tus tarjetas")
        Spacer(Modifier.height(10.dp))
        // Carrusel con las tarjetas guardadas y «Otra tarjeta» como última página (mismo componente
        // que el cobro de citas): deslizar elige la tarjeta, sin filas con botón de selección.
        com.urbanblade.mobile.ui.components.UrbanCreditCardCarousel(
            cards = savedCards,
            selectedCardId = usingSaved,
            startOnNewCard = usingSaved == null,
            newCardPreview = state.newCardPreview(),
            onSelect = { id ->
                if (id == null) {
                    state.useNewCard = true
                } else {
                    state.useNewCard = false
                    state.selectedCardId = id
                }
            }
        )
    }

    // Al aparecer (se eligió Tarjeta) y cada vez que falten datos, el formulario se pone a la vista.
    val cardInView = remember { BringIntoViewRequester() }
    LaunchedEffect(state.cardAttention, usingSaved) {
        kotlinx.coroutines.delay(150)
        cardInView.bringIntoView()
    }

    if (usingSaved == null) {
        if (savedCards.isNotEmpty()) Spacer(Modifier.height(16.dp))
        UrbanFieldLabel("Datos de la tarjeta")
        Spacer(Modifier.height(10.dp))
        // Sin tarjetas guardadas no hay carrusel: la tarjeta en vivo se muestra sola, igual que en él.
        if (savedCards.isEmpty()) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { LiveCardPreview(state.newCardPreview()) }
            Spacer(Modifier.height(14.dp))
        }
        UrbanTextField(
            value = state.holderName,
            onValueChange = { state.holderName = normalizeHolderName(it) },
            label = "Nombre del titular",
            leadingIcon = Icons.Default.Person,
            placeholder = "Como aparece en la tarjeta",
            error = if ((state.holderTouched || state.cardAttention > 0)) holderNameProblem(state.holderName) else null,
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Next,
            onBlur = { state.holderTouched = true }
        )
        Spacer(Modifier.height(12.dp))
        // El formulario de Stripe es una vista clásica: toma los colores del tema de la app (tarjeta,
        // texto, pistas y dorado al enfocar) en lugar de un recuadro blanco que no combinaba.
        val colors = CardFormColors(
            ink = UrbanColors.Ink.toArgb(),
            hint = UrbanColors.Muted.toArgb(),
            accent = UrbanColors.Gold.toArgb(),
            line = UrbanColors.Muted.copy(alpha = 0.45f).toArgb()
        )
        val darkTheme = UrbanColors.Card.luminance() < 0.5f
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = UrbanColors.Card,
            border = BorderStroke(1.dp, UrbanColors.Muted.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(cardInView)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                factory = { ctx ->
                    val themed = ContextThemeWrapper(
                        ctx,
                        if (darkTheme) com.google.android.material.R.style.Theme_MaterialComponents_NoActionBar
                        else com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar
                    )
                    CardMultilineWidget(themed).also { widget ->
                        widget.setShouldShowPostalCode(false)
                        styleCardWidget(widget, colors)
                        state.cardWidget = widget
                        // La tarjeta en pantalla se llena con lo que se escribe aquí (marca, número y vencimiento).
                        state.cardNumberDigits = ""
                        state.cardExpiryText = ""
                        fun field(name: String) = widget.findViewById<EditText>(widget.resources.getIdentifier(name, "id", widget.context.packageName))
                        field("et_card_number")?.addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                            override fun afterTextChanged(s: Editable?) { state.cardNumberDigits = cardDigits(s?.toString().orEmpty()) }
                        })
                        field("et_expiry")?.addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                            override fun afterTextChanged(s: Editable?) { state.cardExpiryText = expiryPreview(s?.toString().orEmpty()) }
                        })
                    }
                },
                // Si el usuario cambia de tema con la hoja abierta, se vuelven a pintar los campos.
                update = { widget -> styleCardWidget(widget, colors) }
            )
        }
        if (showSaveOption) {
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = state.saveCard,
                onCheckedChange = { state.saveCard = it },
                colors = CheckboxDefaults.colors(checkedColor = UrbanColors.Gold, checkmarkColor = UrbanColors.OnGold)
            )
            Text("Guardar esta tarjeta para mis próximos pagos", style = MaterialTheme.typography.bodyMedium)
        }
        }
    }

    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Lock, null, tint = UrbanColors.Muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            "Pago seguro con Stripe. UrbanBlade no ve ni guarda el número de tu tarjeta.",
            style = MaterialTheme.typography.bodySmall,
            color = UrbanColors.Muted
        )
    }
    if (testMode) {
        Spacer(Modifier.height(4.dp))
        Text(
            "Modo de prueba: usa 4242 4242 4242 4242, cualquier fecha futura y cualquier CVC.",
            style = MaterialTheme.typography.bodySmall,
            color = UrbanColors.GoldText
        )
    }
}

/** Colores del tema (ARGB) para el formulario de tarjeta de Stripe. */
private data class CardFormColors(val ink: Int, val hint: Int, val accent: Int, val line: Int)

/**
 * El formulario de Stripe no conoce el tema de la app: sin esto sus textos ("Número de tarjeta",
 * "Fecha de vencimiento", "CVC") heredan colores que no se leen. Texto e hint del tema, y la línea
 * de cada campo en gris que pasa a dorado al enfocar.
 */
private fun styleCardWidget(widget: CardMultilineWidget, colors: CardFormColors) {
    val (ink, hint, accent, line) = colors
    val underline = ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()),
        intArrayOf(accent, line)
    )
    fun id(name: String) = widget.resources.getIdentifier(name, "id", widget.context.packageName)
    // Los contenedores son TextInputLayout de Material, que la app no compila directamente (llega con
    // Stripe): se ajustan sus colores por nombre de método y, si algún día cambian, se ignora sin romper.
    listOf("tl_card_number", "tl_expiry", "tl_cvc").forEach { name ->
        val layout = widget.findViewById<android.view.View>(id(name)) ?: return@forEach
        runCatching { layout.javaClass.getMethod("setDefaultHintTextColor", ColorStateList::class.java).invoke(layout, ColorStateList.valueOf(hint)) }
        runCatching { layout.javaClass.getMethod("setHintTextColor", ColorStateList::class.java).invoke(layout, ColorStateList.valueOf(accent)) }
        runCatching { layout.javaClass.getMethod("setBoxStrokeColor", Int::class.javaPrimitiveType).invoke(layout, accent) }
    }
    listOf("et_card_number", "et_expiry", "et_cvc").forEach { name ->
        widget.findViewById<EditText>(id(name))?.apply {
            setTextColor(ink)
            setHintTextColor(hint)
            backgroundTintList = underline
            highlightColor = accent
        }
    }
}

