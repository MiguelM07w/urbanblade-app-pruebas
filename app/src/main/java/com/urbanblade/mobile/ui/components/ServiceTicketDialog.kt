package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.data.model.ServiceTicket
import com.urbanblade.mobile.ui.theme.UrbanColors

/**
 * Ticket del servicio terminado (flujo de citas V2): lo ve el barbero al terminar y el cliente en su historial.
 * El comprobante en PDF se abre en el navegador; el correo con comprobante y factura sale solo.
 */
@Composable
fun ServiceTicketDialog(ticket: ServiceTicket, onDismiss: () -> Unit) {
    val openUri = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Servicio terminado", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Folio ${ticket.folio}" + (ticket.fecha?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TicketLine("Cliente", ticket.cliente ?: "—")
                TicketLine("Barbero", ticket.barbero ?: "—")
                TicketLine("Servicio", ticket.servicio ?: "—")
                TicketLine(
                    "Duración",
                    "${ticket.duracionMin} min" + if (ticket.minutosExtra > 0) " + ${ticket.minutosExtra} extra" else ""
                )
                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = UrbanColors.Muted.copy(alpha = 0.3f))
                TicketLine("Precio del servicio", UrbanFormat.money(ticket.precioServicio))
                if (ticket.descuentos > 0) TicketLine("Descuentos", "−" + UrbanFormat.money(ticket.descuentos), UrbanColors.Success)
                if (ticket.depositoAplicado > 0) TicketLine("Pagado al reservar", UrbanFormat.money(ticket.depositoAplicado))
                TicketLine("Cobro (${ticket.metodoPago ?: "—"})", UrbanFormat.money(ticket.monto))
                if (ticket.propina > 0) TicketLine("Propina", UrbanFormat.money(ticket.propina))
                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = UrbanColors.Muted.copy(alpha = 0.3f))
                TicketLine("Total pagado", UrbanFormat.money(ticket.totalPagado), UrbanColors.Gold, bold = true)
                Text(
                    "También lo enviamos al correo del cliente con su factura.",
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } },
        dismissButton = ticket.comprobanteUrl?.let { url ->
            { TextButton(onClick = { runCatching { openUri.openUri(url) } }) { Text("Ver comprobante") } }
        }
    )
}

@Composable
private fun TicketLine(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = UrbanColors.Ink,
    bold: Boolean = false
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}
