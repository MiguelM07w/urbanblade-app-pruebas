package com.urbanblade.mobile.ui.screens

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanOutlineButton
import com.urbanblade.mobile.ui.components.UrbanPageHeader
import com.urbanblade.mobile.ui.components.UrbanPremiumCard
import com.urbanblade.mobile.ui.components.UrbanPrimaryButton
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.components.UrbanTextField
import com.urbanblade.mobile.ui.components.UrbanTopBar
import com.urbanblade.mobile.ui.components.UrbanMascotState
import com.urbanblade.mobile.ui.components.UrbanStateKind
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.GIFT_CARD_MAX
import com.urbanblade.mobile.ui.viewmodel.GIFT_CARD_MIN
import com.urbanblade.mobile.ui.viewmodel.GiftCardsStaffViewModel

private val PRESETS = listOf(200, 500, 1000, 2000)

private fun money(value: Double) = UrbanFormat.moneyShort(value)

/** Gift cards en mostrador: vender en efectivo y consultar el saldo de una tarjeta por su código. */
@Composable
fun GiftCardsStaffScreen(onBack: () -> Unit, vm: GiftCardsStaffViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var monto by remember { mutableStateOf("") }
    var comprador by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }

    val montoValue = monto.replace(',', '.').toDoubleOrNull()
    val montoError = if (submitted && (montoValue == null || montoValue < GIFT_CARD_MIN || montoValue > GIFT_CARD_MAX)) {
        "Entre ${money(GIFT_CARD_MIN)} y ${money(GIFT_CARD_MAX)}."
    } else {
        null
    }
    val emailError = if (submitted && email.isNotBlank() && !email.contains("@")) "Escribe un correo válido." else null

    Scaffold(containerColor = Color.Transparent, topBar = { UrbanTopBar("", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                UrbanPageHeader("Tarjetas regalo", "Vende una tarjeta en mostrador o consulta su saldo.", "OPERACIÓN")
            }

            state.sold?.let { sold ->
                item { SoldCard(code = sold.code, saldo = sold.saldo, onNew = vm::clearSold) }
            }

            item {
                UrbanCard(Modifier.fillMaxWidth()) {
                    UrbanSectionTitle("Vender en efectivo", "Cobra el monto y entrega el código al cliente")
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PRESETS.forEach { preset ->
                            FilterChip(
                                selected = montoValue == preset.toDouble(),
                                onClick = { monto = preset.toString() },
                                label = { Text(money(preset.toDouble())) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    UrbanTextField(
                        value = monto,
                        onValueChange = { monto = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                        label = "Monto (MXN)",
                        error = montoError,
                        helper = "Mínimo ${money(GIFT_CARD_MIN)}, máximo ${money(GIFT_CARD_MAX)}.",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    UrbanTextField(
                        value = comprador,
                        onValueChange = { comprador = it },
                        label = "Nombre de quien compra (opcional)",
                        capitalization = KeyboardCapitalization.Words,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    UrbanTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Correo de quien la recibe (opcional)",
                        error = emailError,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                        modifier = Modifier.fillMaxWidth()
                    )
                    state.sellError?.let {
                        Spacer(Modifier.height(10.dp))
                        UrbanErrorBanner(it)
                    }
                    Spacer(Modifier.height(14.dp))
                    UrbanPrimaryButton(
                        text = "Vender en efectivo",
                        onClick = {
                            submitted = true
                            val valid = montoValue != null && montoValue in GIFT_CARD_MIN..GIFT_CARD_MAX &&
                                (email.isBlank() || email.contains("@"))
                            if (valid) confirm = true
                        },
                        loading = state.selling,
                        icon = Icons.Default.CardGiftcard,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                UrbanCard(Modifier.fillMaxWidth()) {
                    UrbanSectionTitle("Consultar saldo", "Con el código de la tarjeta")
                    Spacer(Modifier.height(12.dp))
                    UrbanTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = "Código",
                        leadingIcon = Icons.Default.Search,
                        imeAction = ImeAction.Search,
                        onImeAction = { vm.lookup(code) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    UrbanOutlineButton("Consultar", { vm.lookup(code) }, Modifier.fillMaxWidth(), Icons.Default.Search)
                    state.lookupError?.let {
                        Spacer(Modifier.height(10.dp))
                        UrbanErrorBanner(it)
                    }
                    state.found?.let { card ->
                        Spacer(Modifier.height(14.dp))
                        Text("Saldo disponible", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
                        Text(
                            money(card.saldo),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = UrbanColors.Gold
                        )
                        Text(
                            "de ${money(card.montoInicial)} iniciales" + (card.estado?.let { " · ${it.replaceFirstChar { c -> c.uppercase() }}" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = UrbanColors.Muted
                        )
                    }
                }
            }
        }
    }

    if (confirm && montoValue != null) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            containerColor = UrbanColors.Card,
            title = { Text("Confirmar venta") },
            text = {
                Text(
                    "¿Ya recibiste ${money(montoValue)} en efectivo? Se generará un código con ese saldo y no se puede deshacer desde la app.",
                    color = UrbanColors.Muted
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    vm.sell(montoValue, comprador.trim().ifEmpty { null }, email.trim().ifEmpty { null }) {
                        monto = ""; comprador = ""; email = ""; submitted = false
                    }
                }) { Text("Sí, vender") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Volver") } }
        )
    }
}

@Composable
private fun SoldCard(code: String, saldo: Double, onNew: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    UrbanPremiumCard(Modifier.fillMaxWidth()) {
        Text("Tarjeta vendida", style = MaterialTheme.typography.labelLarge, color = UrbanColors.Gold)
        Text(
            code,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = UrbanColors.Ink
        )
        Text("Saldo ${money(saldo)}", style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            UrbanOutlineButton("Copiar", { clipboard.setText(AnnotatedString(code)) }, Modifier.weight(1f), Icons.Default.ContentCopy)
            UrbanOutlineButton(
                "Compartir",
                {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Tarjeta de regalo UrbanBlade por ${money(saldo)}. Código: $code")
                    }
                    runCatching { context.startActivity(Intent.createChooser(send, "Compartir tarjeta")) }
                },
                Modifier.weight(1f),
                Icons.Default.Share
            )
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onNew, modifier = Modifier.fillMaxWidth()) { Text("Vender otra") }
    }
}
