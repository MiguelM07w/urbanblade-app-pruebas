package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.theme.UrbanColors

/** Identidad de una visita: nombres completos y el importe separado, sin competir por ancho. */
@Composable
fun UrbanVisitSummary(
    service: String,
    barber: String,
    modifier: Modifier = Modifier,
    photo: String? = null,
    amount: String? = null,
    amountLabel: String = "Precio del servicio"
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(service, style = MaterialTheme.typography.titleLarge, color = UrbanColors.Ink)
        Row(verticalAlignment = Alignment.Top) {
            UrbanAvatar(barber, Modifier.size(40.dp), imageUrl = photo)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Tu barbero", style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted)
                Text(barber, style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Ink)
            }
        }
        amount?.let {
            HorizontalDivider(color = UrbanColors.Line)
            UrbanKeyValue(amountLabel, it)
        }
    }
}
