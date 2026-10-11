package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.core.media.ReceiptDownloads
import com.urbanblade.mobile.data.model.MembershipInvoiceRow
import com.urbanblade.mobile.data.model.OrderRow
import com.urbanblade.mobile.data.model.PaymentRow
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanHeroCard
import com.urbanblade.mobile.ui.components.UrbanHeroLabel
import com.urbanblade.mobile.ui.components.UrbanHeroStat
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
import com.urbanblade.mobile.ui.components.UrbanMascotState
import com.urbanblade.mobile.ui.components.UrbanModuleScreen
import com.urbanblade.mobile.ui.components.UrbanPillTabs
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.components.UrbanSkeletonList
import com.urbanblade.mobile.ui.components.UrbanStateKind
import com.urbanblade.mobile.ui.components.UrbanStatusPill
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.InvoicesViewModel

private fun money(value: Double) = UrbanFormat.money(value)

private val METODO = mapOf("efectivo" to "Efectivo", "tarjeta" to "Tarjeta", "transferencia" to "Transferencia", "qr" to "QR")

/**
 * Mis facturas del cliente: el comprobante de cada cita pagada y de cada compra de productos
 * entregada. Los productos que agregó al reservar se pagan en el salón y tienen su propio
 * comprobante (mismo criterio que la web).
 */
@Composable
fun InvoicesScreen(onBack: () -> Unit, vm: InvoicesViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    // Descargas en curso: id de DownloadManager -> id del pago o pedido (para el indicador del botón).
    var downloading by remember { mutableStateOf(mapOf<Long, String>()) }
    var notice by remember { mutableStateOf<String?>(null) }
    // El PDF se descarga al teléfono (Descargas/UrbanBlade) y se abre en el visor; nunca se muestra la liga de S3.
    fun download(itemId: String, label: String): (String) -> Unit = { url ->
        val name = ReceiptDownloads.fileName(label)
        val id = ReceiptDownloads.enqueue(context, url, name)
        if (id == null) {
            notice = "No se pudo descargar el comprobante. Intenta de nuevo."
        } else {
            downloading = downloading + (id to itemId)
            notice = "Descargando $name…"
        }
    }

    LaunchedEffect(Unit) { vm.load() }
    DisposableEffect(Unit) {
        val stop = ReceiptDownloads.listen(context) { id, ok ->
            if (id in downloading) {
                downloading = downloading - id
                notice = if (ok) "Guardado en Descargas/UrbanBlade." else "No se pudo descargar el comprobante. Intenta de nuevo."
                if (ok) ReceiptDownloads.open(context, id)
            }
        }
        onDispose { stop() }
    }

    UrbanModuleScreen(
        eyebrow = "Cuenta",
        title = "Mis facturas",
        subtitle = "Comprobantes de tus citas y de tus compras.",
        onBack = onBack,
        onRefresh = { vm.load() },
        refreshing = state.loading
    ) {
        when {
            state.loading && state.payments.isEmpty() && state.orders.isEmpty() -> item { UrbanSkeletonList(3) }
            state.error != null && state.payments.isEmpty() -> item {
                UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar tus facturas", state.error, "Reintentar") { vm.load() }
            }
            else -> {
                item {
                    UrbanHeroCard {
                        UrbanHeroLabel("Total pagado")
                        Spacer(Modifier.height(6.dp))
                        Text(money(state.totalPaid + state.productsPaid + state.membershipPaid), style = MaterialTheme.typography.displaySmall, color = UrbanColors.Ink)
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            UrbanHeroStat("Citas", "${state.collected.size}", Icons.Default.ContentCut, Modifier.weight(1f))
                            // Mismo número que la pestaña «Productos»: por recoger y ya entregadas.
                            UrbanHeroStat("Compras", "${state.toPickUp.size + state.delivered.size}", Icons.Default.ShoppingBag, Modifier.weight(1f))
                        }
                    }
                }
                state.receiptError?.let { item { UrbanErrorBanner(it) } }
                notice?.let { item { UrbanInfoBanner(it, Icons.Default.Download, Modifier.fillMaxWidth()) } }
                item {
                    UrbanPillTabs(
                        listOf(
                            "Citas (${state.payments.size})" to Icons.Default.ContentCut,
                            "Productos (${state.toPickUp.size + state.delivered.size})" to Icons.Default.ShoppingBag,
                            "Membresía (${state.memberships.size})" to Icons.Default.CardMembership
                        ),
                        tab
                    ) { tab = it }
                }

                if (tab == 0) {
                    if (state.payments.isEmpty()) item {
                        UrbanMascotState(UrbanStateKind.EMPTY, "Aún no tienes citas pagadas", "Cuando pagues una cita, aquí tendrás su comprobante.")
                    }
                    items(state.payments, key = { "pay-${it.id}" }) { payment ->
                        PaymentInvoice(payment, opening = state.openingId == payment.id || payment.id in downloading.values) {
                            vm.openPaymentReceipt(payment.id, download(payment.id, "Comprobante ${payment.appointment?.service ?: "cita"} ${payment.appointment?.fecha?.take(10).orEmpty()}"))
                        }
                    }
                } else if (tab == 2) {
                    if (state.memberships.isEmpty()) item {
                        UrbanMascotState(UrbanStateKind.EMPTY, "Sin cobros de membresía", "Cuando contrates una membresía, aquí tendrás la factura de cada mes.")
                    }
                    items(state.memberships, key = { "mem-${it.id}" }) { invoice ->
                        MembershipInvoice(invoice, opening = state.openingId == invoice.id || invoice.id in downloading.values) {
                            vm.openMembershipReceipt(invoice.id, download(invoice.id, "Factura membresía ${invoice.pagadoEn?.take(10).orEmpty()}"))
                        }
                    }
                } else {
                    if (state.toPickUp.isEmpty() && state.delivered.isEmpty()) item {
                        UrbanMascotState(UrbanStateKind.EMPTY, "Aún no tienes compras", "Los productos que agregues al reservar o compres en la tienda aparecerán aquí.")
                    }
                    if (state.toPickUp.isNotEmpty()) {
                        item { UrbanSectionTitle("Por recoger", "Se pagan en el salón; el comprobante se genera al pagarlos.") }
                        items(state.toPickUp, key = { "pick-${it.id}" }) { order -> OrderInvoice(order, opening = false, onReceipt = null) }
                    }
                    if (state.delivered.isNotEmpty()) {
                        item { UrbanSectionTitle("Pagadas", UrbanFormat.count(state.delivered.size, "compra", "compras")) }
                        items(state.delivered, key = { "ord-${it.id}" }) { order ->
                            OrderInvoice(order, opening = state.openingId == order.id || order.id in downloading.values) {
                                vm.openOrderReceipt(order.id, download(order.id, "Comprobante ${order.folio ?: "pedido"}"))
                            }
                        }
                    }
                    if (state.orders.any { it.tipo == "cita" }) item {
                        UrbanInfoBanner("Los productos que agregas al reservar se pagan aparte, en el salón, con su propio comprobante.", Icons.Default.Storefront)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentInvoice(payment: PaymentRow, opening: Boolean, onReceipt: () -> Unit) {
    UrbanCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        payment.appointment?.service ?: "Cita",
                        style = MaterialTheme.typography.titleMedium,
                        color = UrbanColors.Ink,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Solo se marca lo que no es un cobro normal: reembolsado, por verificar o rechazado.
                    if (!payment.collected) {
                        Spacer(Modifier.width(8.dp))
                        UrbanStatusPill(payment.estado.orEmpty())
                    }
                }
                Text(
                    listOfNotNull(
                        payment.appointment?.fecha?.let { UrbanFormat.dateShort(it) },
                        payment.appointment?.barber,
                        payment.metodoPago?.let { METODO[it.lowercase()] ?: it.replaceFirstChar { c -> c.uppercase() } },
                        "Pagado al reservar".takeIf { payment.esDeposito }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (payment.propina > 0) {
                    Text("Incluye propina de ${money(payment.propina)}", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                }
                when (payment.estado) {
                    "reembolsado" -> Text("Te devolvimos este pago a tu tarjeta o cuenta.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                    "pendiente_verificacion" -> Text("Recepción está revisando tu comprobante.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                    "rechazado" -> Text("Tu comprobante no se aceptó; paga en recepción o sube otro desde Mis citas.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                }
            }
            Text(
                money(payment.monto + payment.propina),
                style = MaterialTheme.typography.titleMedium,
                color = if (payment.collected) UrbanColors.Gold else UrbanColors.Muted
            )
        }
        if (payment.collected) ReceiptButton(opening, onReceipt)
    }
}

@Composable
private fun OrderInvoice(order: OrderRow, opening: Boolean, onReceipt: (() -> Unit)?) {
    UrbanCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (order.tipo == "cita") "Productos de tu cita" else "Compra en tienda",
                        style = MaterialTheme.typography.titleMedium,
                        color = UrbanColors.Ink,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    UrbanStatusPill(order.estado)
                }
                Text(
                    order.items.joinToString(", ") { l -> if (l.cantidad > 1) "${l.nombre} ×${l.cantidad}" else l.nombre.orEmpty() },
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOfNotNull(
                        order.folio,
                        (order.entregadoEn ?: order.createdAt)?.let { UrbanFormat.dateShort(it) },
                        order.metodoPago?.let { METODO[it.lowercase()] ?: it }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted
                )
            }
            Text(money(order.total), style = MaterialTheme.typography.titleMedium, color = UrbanColors.Gold)
        }
        onReceipt?.let { ReceiptButton(opening, it) }
    }
}

@Composable
private fun MembershipInvoice(invoice: MembershipInvoiceRow, opening: Boolean, onReceipt: () -> Unit) {
    UrbanCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(invoice.plan?.let { "Membresía $it" } ?: "Membresía", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Ink)
                Text(
                    listOfNotNull(invoice.pagadoEn?.let { UrbanFormat.dateShort(it) }, "Cobro mensual · Tarjeta").joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted
                )
            }
            Text(money(invoice.monto), style = MaterialTheme.typography.titleMedium, color = UrbanColors.Gold)
        }
        ReceiptButton(opening, onReceipt)
    }
}

@Composable
private fun ReceiptButton(opening: Boolean, onClick: () -> Unit) {
    Spacer(Modifier.height(4.dp))
    // Sin el margen interno del botón para que el ícono quede alineado con el texto de la tarjeta.
    TextButton(onClick = onClick, enabled = !opening, contentPadding = PaddingValues(vertical = 4.dp)) {
        if (opening) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = UrbanColors.Gold)
        } else {
            Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp), tint = UrbanColors.Gold)
        }
        Spacer(Modifier.width(6.dp))
        Text(if (opening) "Descargando…" else "Descargar comprobante", color = UrbanColors.Gold)
    }
}
