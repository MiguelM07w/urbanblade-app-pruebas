package com.urbanblade.mobile.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.HorizontalDivider
import com.urbanblade.mobile.ui.components.UrbanAttentionRow
import com.urbanblade.mobile.ui.components.UrbanHeroCard
import com.urbanblade.mobile.ui.components.UrbanHeroLabel
import com.urbanblade.mobile.ui.components.UrbanHeroStat
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.NoShowFeeItem
import com.urbanblade.mobile.data.model.PaymentRow
import com.urbanblade.mobile.data.model.PendingPaymentRow
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanEmptyState
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
import com.urbanblade.mobile.ui.components.UrbanOutlineButton
import com.urbanblade.mobile.ui.components.UrbanPremiumCard
import com.urbanblade.mobile.ui.components.UrbanPrimaryButton
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.components.UrbanSkeletonList
import com.urbanblade.mobile.ui.components.UrbanTextField
import com.urbanblade.mobile.ui.components.UrbanTopBar
import com.urbanblade.mobile.ui.components.UrbanMascotState
import com.urbanblade.mobile.ui.components.UrbanStateKind
import com.urbanblade.mobile.ui.components.UrbanPageHeader
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.PaymentMethodFilter
import com.urbanblade.mobile.ui.viewmodel.PaymentsStaffViewModel
import com.urbanblade.mobile.ui.viewmodel.REJECT_REASON_MAX

private fun money(value: Double) = UrbanFormat.money(value)
private fun moneyShort(value: Double) = UrbanFormat.moneyShort(value)

/** Motivos frecuentes de rechazo: en mostrador se elige uno en vez de escribirlo cada vez. */
internal val REJECT_QUICK_REASONS = listOf(
    "El monto no coincide con el servicio",
    "La imagen no se lee bien",
    "No es un comprobante de transferencia",
    "No vemos el depósito en la cuenta"
)

/** Comprobante en revisión: pago completo de la cita o anticipo ("pagar ahora" / depósito). */
private data class ReviewTarget(val row: PendingPaymentRow, val deposit: Boolean)

/** Pagos del negocio: totales del servidor, comprobantes por revisar e historial filtrable. */
@Composable
fun PaymentsStaffScreen(onBack: () -> Unit, vm: PaymentsStaffViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var approving by remember { mutableStateOf<ReviewTarget?>(null) }
    var rejecting by remember { mutableStateOf<ReviewTarget?>(null) }
    var waiving by remember { mutableStateOf<NoShowFeeItem?>(null) }

    LaunchedEffect(Unit) { vm.load() }

    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { UrbanTopBar("", onBack) { IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, "Actualizar") } } }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                val toReview = state.pending.size + state.deposits.size
                val pendingLabel = UrbanFormat.count(toReview, "comprobante requiere tu revisión", "comprobantes requieren tu revisión")
                UrbanPageHeader(
                    title = "Cobros y pagos",
                    subtitle = if (toReview > 0) pendingLabel else "Todo al día. Consulta el historial cuando lo necesites.",
                    eyebrow = "OPERACIÓN"
                )
            }
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            state.notice?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }
            if (rejecting == null) state.error?.let { item { UrbanErrorBanner(it) } }

            item {
                UrbanHeroCard {
                    UrbanHeroLabel("Cobrado hoy")
                    Spacer(Modifier.height(6.dp))
                    Text(moneyShort(state.stats.totalHoy), style = MaterialTheme.typography.displaySmall, color = UrbanColors.Ink)
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = UrbanColors.Ink.copy(alpha = 0.22f))
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        UrbanHeroStat("Este mes", moneyShort(state.stats.totalMes), Icons.Default.Payments, Modifier.weight(1f))
                        UrbanHeroStat("Pagos", state.stats.count.toString(), Icons.Default.ReceiptLong, Modifier.weight(1f))
                    }
                }
            }
            if (state.pending.isNotEmpty()) {
                item {
                    val pendingAmount = state.pending.sumOf { it.monto }
                    UrbanSectionTitle(
                        "Por revisar ahora",
                        "${UrbanFormat.count(state.pending.size, "comprobante pendiente", "comprobantes pendientes")} · ${money(pendingAmount)}"
                    )
                }
                items(state.pending, key = { it.id }) { payment ->
                    PendingCard(
                        payment = payment,
                        deposit = false,
                        busy = state.busyPaymentId == payment.id,
                        onReceipt = { payment.comprobanteUrl?.let(::openUrl) },
                        onApprove = { approving = ReviewTarget(payment, false) },
                        onReject = { vm.clearMessages(); rejecting = ReviewTarget(payment, false) }
                    )
                }
            }
            if (state.deposits.isNotEmpty()) {
                item {
                    UrbanSectionTitle(
                        "Anticipos por revisar",
                        "${UrbanFormat.count(state.deposits.size, "cliente pagó", "clientes pagaron")} al reservar · ${money(state.deposits.sumOf { it.monto })}"
                    )
                }
                items(state.deposits, key = { "d-" + it.id }) { payment ->
                    PendingCard(
                        payment = payment,
                        deposit = true,
                        busy = state.busyPaymentId == payment.id,
                        onReceipt = { payment.comprobanteUrl?.let(::openUrl) },
                        onApprove = { approving = ReviewTarget(payment, true) },
                        onReject = { vm.clearMessages(); rejecting = ReviewTarget(payment, true) }
                    )
                }
            }

            if (state.fees.isNotEmpty()) {
                item {
                    UrbanSectionTitle(
                        "Adeudos por inasistencia",
                        "${UrbanFormat.count(state.fees.size, "cliente no puede", "clientes no pueden")} reservar hasta pagar · ${money(state.fees.sumOf { it.monto })}"
                    )
                }
                items(state.fees, key = { "fee-" + it.id }) { fee ->
                    FeeCard(
                        fee = fee,
                        busy = state.busyFeeId == fee.id,
                        onPay = { metodo -> vm.payFee(fee.id, metodo) },
                        onWaive = { vm.clearMessages(); waiving = fee }
                    )
                }
            }

            item { UrbanSectionTitle("Historial", "Pagos registrados en UrbanBlade") }
            item {
                UrbanTextField(
                    value = state.query,
                    onValueChange = vm::setQuery,
                    label = "Buscar por cliente, servicio o barbero",
                    leadingIcon = Icons.Default.Search,
                    imeAction = ImeAction.Search,
                    onImeAction = { vm.load() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMethodFilter.entries.forEach { m ->
                        FilterChip(selected = state.method == m, onClick = { vm.setMethod(m) }, label = { Text(m.label) })
                    }
                }
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportRange.entries.forEach { r ->
                        FilterChip(selected = state.range == r, onClick = { vm.setRange(r) }, label = { Text(if (r == ReportRange.All) "Todo el tiempo" else r.label) })
                    }
                }
            }
            if (state.loading && state.items.isEmpty()) item { UrbanSkeletonList(3) }
            if (!state.loading && state.items.isEmpty() && state.error == null) {
                item { UrbanMascotState(UrbanStateKind.EMPTY, "No hay pagos con estos filtros", "Prueba con otro método o rango de fechas.") }
            }
            items(state.items, key = { it.id }) { payment ->
                PaymentCard(payment, onReceipt = { vm.receiptUrl(payment.id, ::openUrl) })
            }
            if (state.hasMore) {
                item {
                    UrbanOutlineButton(
                        text = if (state.loadingMore) "Cargando…" else "Cargar más pagos",
                        onClick = { vm.loadMore() },
                        icon = Icons.Default.ExpandMore,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    approving?.let { target ->
        val payment = target.row
        AlertDialog(
            onDismissRequest = { approving = null },
            containerColor = UrbanColors.Card,
            title = { Text(if (target.deposit) "Aprobar anticipo" else "Aprobar comprobante") },
            text = {
                Text(
                    "¿Confirmas que recibiste ${money(payment.monto)}${payment.appointment?.client?.let { " de $it" } ?: ""}? " +
                        if (target.deposit) "Se descontará al cobrar; la cita sigue esperando que el barbero la confirme." else "La cita quedará completada.",
                    color = UrbanColors.Muted
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (target.deposit) vm.approveDeposit(payment.id) else vm.approve(payment.id)
                    approving = null
                }) { Text("Sí, aprobar") }
            },
            dismissButton = { TextButton(onClick = { approving = null }) { Text("Volver") } }
        )
    }

    waiving?.let { fee ->
        WaiveFeeDialog(
            fee = fee,
            busy = state.busyFeeId == fee.id,
            error = state.error,
            onDismiss = { waiving = null },
            onConfirm = { motivo -> vm.waiveFee(fee.id, motivo) { waiving = null } }
        )
    }

    rejecting?.let { target ->
        val payment = target.row
        RejectDialog(
            payment = payment,
            busy = state.busyPaymentId == payment.id,
            error = state.error,
            onDismiss = { rejecting = null },
            onConfirm = { reason ->
                if (target.deposit) vm.rejectDeposit(payment.id, reason) { rejecting = null }
                else vm.reject(payment.id, reason) { rejecting = null }
            }
        )
    }
}

@Composable
private fun PendingCard(payment: PendingPaymentRow, deposit: Boolean, busy: Boolean, onReceipt: () -> Unit, onApprove: () -> Unit, onReject: () -> Unit) {
    val price = payment.appointment?.servicePrice
    val check = compareTransfer(payment.monto, price)
    UrbanPremiumCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (deposit) "ANTICIPO AL RESERVAR" else "TRANSFERENCIA", style = MaterialTheme.typography.labelMedium, color = UrbanColors.GoldText)
            SimpleStatusPill("por revisar", UrbanColors.Warning)
        }
        Spacer(Modifier.height(4.dp))
        Text(money(payment.monto), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
        Text(
            listOfNotNull(payment.appointment?.client, payment.appointment?.service).joinToString(" · ").ifEmpty { "Cita sin datos" },
            style = MaterialTheme.typography.bodyMedium,
            color = UrbanColors.Ink
        )
        payment.createdAt?.let { Text("Enviado el ${formatWhen(it)}", style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted) }

        Spacer(Modifier.height(10.dp))
        when (check) {
            TransferCheck.Match -> Text("Monto confirmado: coincide con el servicio (${money(price ?: 0.0)}).", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Success)
            TransferCheck.Differs -> Text(
                "Revisa el comprobante: difiere del precio de lista (${money(price ?: 0.0)}). Puede incluir un descuento.",
                style = MaterialTheme.typography.bodySmall,
                color = UrbanColors.Warning
            )
            TransferCheck.Unknown -> Unit
        }
        payment.ocrMontoDetectado?.let { ocr ->
            Text(
                "Monto que leyó el sistema en la imagen: ${money(ocr)}" + if (ocrDisagrees(payment.monto, ocr)) " (no coincide)" else "",
                style = MaterialTheme.typography.bodySmall,
                color = if (ocrDisagrees(payment.monto, ocr)) UrbanColors.Warning else UrbanColors.Muted
            )
        }

        Spacer(Modifier.height(12.dp))
        if (!payment.comprobanteUrl.isNullOrBlank()) {
            UrbanOutlineButton("Ver comprobante", onReceipt, Modifier.fillMaxWidth(), Icons.Default.Receipt)
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UrbanPrimaryButton("Aprobar", onApprove, Modifier.weight(1f), enabled = !busy, icon = Icons.Default.Check, loading = busy)
            UrbanOutlineButton("Rechazar", onReject, Modifier.weight(1f))
        }
    }
}

/** Adeudo por inasistencia: se cobra en sucursal (efectivo o transferencia) o administración lo condona. */
@Composable
private fun FeeCard(fee: NoShowFeeItem, busy: Boolean, onPay: (String) -> Unit, onWaive: () -> Unit) {
    UrbanPremiumCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("CARGO POR INASISTENCIA", style = MaterialTheme.typography.labelMedium, color = UrbanColors.GoldText)
            SimpleStatusPill("por cobrar", UrbanColors.Warning)
        }
        Spacer(Modifier.height(4.dp))
        Text(money(fee.monto), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
        Text(
            listOfNotNull(fee.cita?.cliente, fee.cita?.servicio).joinToString(" · ").ifEmpty { "Cita sin datos" },
            style = MaterialTheme.typography.bodyMedium,
            color = UrbanColors.Ink
        )
        Text(
            "${fee.porcentaje}% del servicio" +
                (fee.cita?.fecha?.let { " · cita del $it" } ?: "") +
                if (fee.creditoAnticipo > 0) " · ${money(fee.creditoAnticipo)} ya cubiertos por pago anticipado" else "",
            style = MaterialTheme.typography.labelSmall,
            color = UrbanColors.Muted
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UrbanPrimaryButton("Efectivo", { onPay("efectivo") }, Modifier.weight(1f), enabled = !busy, icon = Icons.Default.Payments, loading = busy)
            UrbanOutlineButton("Transferencia", { onPay("transferencia") }, Modifier.weight(1f))
        }
        TextButton(onClick = onWaive, enabled = !busy) { Text("Condonar (solo administración)", color = UrbanColors.Muted) }
    }
}

@Composable
private fun WaiveFeeDialog(fee: NoShowFeeItem, busy: Boolean, error: String?, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var motivo by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = UrbanColors.Card,
        title = { Text("Condonar cargo de ${money(fee.monto)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "${fee.cita?.cliente ?: "El cliente"} volverá a poder reservar. El motivo queda registrado.",
                    color = UrbanColors.Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
                UrbanTextField(
                    value = motivo,
                    onValueChange = { motivo = it.take(300) },
                    label = "Motivo",
                    capitalization = KeyboardCapitalization.Sentences,
                    helper = "${motivo.length}/300",
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { UrbanErrorBanner(it) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && motivo.trim().length >= 3, onClick = { onConfirm(motivo) }) {
                Text(if (busy) "Enviando…" else "Condonar", color = UrbanColors.Danger)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Volver") } }
    )
}

@Composable
private fun PaymentCard(payment: PaymentRow, onReceipt: () -> Unit) {
    val metodo = payment.metodoPago?.lowercase()
    UrbanAttentionRow(
        icon = when (metodo) {
            "tarjeta" -> Icons.Default.CreditCard
            "transferencia" -> Icons.Default.AccountBalance
            "efectivo" -> Icons.Default.Payments
            else -> Icons.Default.Savings
        },
        text = payment.appointment?.let { a -> listOfNotNull(a.client, a.service).joinToString(" · ") }?.ifBlank { null }
            ?: payment.metodoPago?.replaceFirstChar { it.uppercase() } ?: "Pago",
        subtitle = listOfNotNull(
            payment.metodoPago?.replaceFirstChar { it.uppercase() },
            payment.appointment?.barber,
            formatWhen(payment.createdAt),
            payment.propina.takeIf { it > 0 }?.let { "+${moneyShort(it)} propina" }
        ).joinToString(" · "),
        tone = UrbanColors.Gold,
        trailing = {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(money(payment.monto + payment.propina), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = UrbanColors.GoldText)
                IconButton(onClick = onReceipt) { Icon(Icons.Default.Receipt, "Ver comprobante", tint = UrbanColors.Muted) }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RejectDialog(payment: PendingPaymentRow, busy: Boolean, error: String?, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UrbanColors.Card,
        title = { Text("Rechazar comprobante") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "El cliente${payment.appointment?.client?.let { " ($it)" } ?: ""} verá este motivo y podrá subir otro comprobante.",
                    color = UrbanColors.Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    REJECT_QUICK_REASONS.forEach { quick ->
                        FilterChip(selected = reason == quick, onClick = { reason = quick }, label = { Text(quick) })
                    }
                }
                UrbanTextField(
                    value = reason,
                    onValueChange = { reason = it.take(REJECT_REASON_MAX) },
                    label = "Motivo del rechazo",
                    capitalization = KeyboardCapitalization.Sentences,
                    helper = "${reason.length}/$REJECT_REASON_MAX",
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { UrbanErrorBanner(it) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && reason.isNotBlank(), onClick = { onConfirm(reason) }) {
                Text(if (busy) "Enviando…" else "Rechazar", color = UrbanColors.Danger)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Volver") } }
    )
}
