package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.urbanblade.mobile.core.media.MembershipCardFiles
import com.urbanblade.mobile.core.payment.rememberStripeReady
import com.urbanblade.mobile.core.payment.rememberUrbanPaymentSheet
import com.urbanblade.mobile.core.config.UrbanRemoteConfig
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.BenefitPurchaseViewModel

/** Acciones reales de autoservicio: la compra la confirma Laravel mediante el webhook de Stripe. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BenefitShop(onRefresh: () -> Unit, vm: BenefitPurchaseViewModel = viewModel()) {
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    val message by vm.message.collectAsState()
    val secret by vm.secret.collectAsState()
    val document by vm.document.collectAsState()
    val packages by vm.catalog.collectAsState()
    val cardEnabled by UrbanRemoteConfig.cardPayments.collectAsState()
    val configured = rememberStripeReady()
    val context = LocalContext.current
    var shop by rememberSaveable { mutableStateOf<String?>(null) }
    var amount by rememberSaveable { mutableStateOf("") }
    var recipient by rememberSaveable { mutableStateOf("") }
    val openPayment = rememberUrbanPaymentSheet { result ->
        val completed = result is PaymentSheetResult.Completed
        vm.paymentResult(completed, result is PaymentSheetResult.Canceled)
        if (completed) { shop = null; onRefresh() }
    }
    LaunchedEffect(secret) {
        secret?.let { vm.consumeSecret(); openPayment(it) }
    }
    LaunchedEffect(document) {
        document?.let { vm.documentOpened(MembershipCardFiles.open(context, it)) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        UrbanOutlineButton("Mi tarjeta de miembro", { vm.downloadCard(context) },
            icon = Icons.Default.Download, enabled = !busy, modifier = Modifier.fillMaxWidth())
        if (configured && cardEnabled) {
            UrbanOutlineButton("Ver paquetes", { shop = "packages"; vm.loadCatalog() },
                icon = Icons.Default.Inventory2, enabled = !busy, modifier = Modifier.fillMaxWidth())
            UrbanOutlineButton("Comprar tarjeta regalo", { vm.clearError(); shop = "gift" },
                icon = Icons.Default.Redeem, enabled = !busy, modifier = Modifier.fillMaxWidth())
        } else {
            UrbanInlineEmpty("Compras con tarjeta no disponibles", Icons.Default.Info,
                subtitle = "Puedes comprar paquetes y tarjetas regalo en recepción.")
        }
        if (shop == null) error?.let { UrbanErrorBanner(it) }
        message?.let {
            UrbanInfoBanner(it)
            UrbanOutlineButton("Actualizar mis beneficios", onRefresh, icon = Icons.Default.Refresh,
                modifier = Modifier.fillMaxWidth())
        }
    }

    shop?.let { selected ->
        ModalBottomSheet(
            onDismissRequest = { if (!busy) shop = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = UrbanColors.Background
        ) {
            Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(if (selected == "packages") "Paquetes de servicios" else "Tarjeta regalo",
                    style = MaterialTheme.typography.headlineSmall, color = UrbanColors.Ink)
                error?.let { UrbanErrorBanner(it) }
                if (selected == "packages") {
                    if (busy && packages.isEmpty()) UrbanSkeletonList(2)
                    if (!busy && packages.isEmpty()) UrbanEmptyState("Sin paquetes disponibles", "Puedes consultar en recepción.", Icons.Default.Inventory2)
                    packages.forEach { pack ->
                        UrbanCard(Modifier.fillMaxWidth()) {
                            Text(pack.nombre, style = MaterialTheme.typography.titleMedium)
                            Text("${pack.cantidadUsos} usos · ${pack.vigenciaDias} días", color = UrbanColors.Muted)
                            Text(UrbanFormat.money(pack.precio), style = MaterialTheme.typography.titleLarge)
                            UrbanPrimaryButton("Comprar con tarjeta", { vm.buyPackage(pack.id) },
                                enabled = !busy && cardEnabled, loading = busy,
                                modifier = Modifier.fillMaxWidth())
                        }
                    }
                    if (error != null && !busy && packages.isEmpty()) UrbanOutlineButton("Reintentar catálogo", { vm.loadCatalog() }, modifier = Modifier.fillMaxWidth())
                } else {
                    UrbanTextField(amount, { amount = it }, "Monto en MXN", keyboardType = KeyboardType.Decimal,
                        helper = "El servidor valida el monto permitido antes de abrir el pago.")
                    UrbanTextField(recipient, { recipient = it }, "Correo del destinatario (opcional)", keyboardType = KeyboardType.Email)
                    val value = amount.replace(',', '.').toDoubleOrNull()
                    val email = recipient.trim()
                    val validEmail = email.isEmpty() || android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                    UrbanPrimaryButton("Continuar con tarjeta", {
                        value?.let { vm.buyGiftCard(it, email.ifBlank { null }) }
                    }, enabled = !busy && cardEnabled && value != null && value.isFinite() && value > 0 && validEmail,
                        loading = busy, modifier = Modifier.fillMaxWidth())
                }
                Text("El cobro se procesa con Stripe. La compra estará disponible cuando el servidor lo confirme.", color = UrbanColors.Muted)
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}
