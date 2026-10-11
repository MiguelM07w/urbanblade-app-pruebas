package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.data.model.AppointmentRow
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanFieldLabel
import com.urbanblade.mobile.ui.components.UrbanKeyValue
import com.urbanblade.mobile.ui.components.UrbanPrimaryButton
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.AppointmentsViewModel

/**
 * Hoja de cobro del personal: método (efectivo o transferencia; la tarjeta se cobra con Stripe desde
 * la cuenta del cliente) y propina opcional. El precio que se muestra es informativo: el servidor
 * lo vuelve a leer del servicio y aplica descuentos, puntos y membresía.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChargeSheet(appt: AppointmentRow, vm: AppointmentsViewModel, onDismiss: () -> Unit) {
    val busy by vm.actionBusy.collectAsState()
    val error by vm.error.collectAsState()
    var method by remember { mutableStateOf("efectivo") }
    var tip by remember { mutableStateOf("") }
    val price = appt.precioCobrado ?: appt.service?.precio
    val tipValue = tip.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = UrbanColors.Surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Cobrar cita", style = MaterialTheme.typography.titleLarge, color = UrbanColors.Ink)
            UrbanCard(Modifier.fillMaxWidth()) {
                UrbanKeyValue("Servicio", appt.service?.nombre ?: "—")
                Spacer(Modifier.height(8.dp))
                UrbanKeyValue("Cliente", appt.client?.user?.name ?: "—")
                Spacer(Modifier.height(8.dp))
                UrbanKeyValue("Precio", price?.let { UrbanFormat.moneyShort(it) } ?: "—")
            }
            Column {
                UrbanFieldLabel("Método de pago")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("efectivo" to "Efectivo", "transferencia" to "Transferencia").forEach { (value, label) ->
                        FilterChip(selected = method == value, onClick = { method = value }, label = { Text(label) })
                    }
                }
            }
            OutlinedTextField(
                value = tip,
                onValueChange = { tip = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                label = { Text("Propina (opcional)") },
                prefix = { Text("$") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "El total final lo confirma el servidor, con los descuentos de nivel, puntos y membresía del cliente.",
                style = MaterialTheme.typography.bodySmall,
                color = UrbanColors.Muted
            )
            error?.let { UrbanErrorBanner(it) }
            UrbanPrimaryButton(
                text = "Registrar cobro",
                onClick = { vm.charge(appt, method, tipValue) { onDismiss() } },
                loading = busy,
                icon = Icons.Default.Payments,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
