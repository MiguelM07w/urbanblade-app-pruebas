package com.urbanblade.mobile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.AiBriefingViewModel

/**
 * "Resumen del día" para el inicio de recepción y administración. Se carga aparte y nunca frena la
 * pantalla: mientras llega (o si falla) simplemente no se muestra. barber contesta al instante con
 * el último resumen de la IA local o con uno armado por reglas (source "reglas").
 */
@Composable
fun AiBriefingCard(modifier: Modifier = Modifier, vm: AiBriefingViewModel = viewModel()) {
    val briefing by vm.briefing.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    AnimatedVisibility(visible = briefing != null, enter = fadeIn(), modifier = modifier) {
        val current = briefing ?: return@AnimatedVisibility
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = UrbanColors.Gold.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, UrbanColors.Gold.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = UrbanColors.Gold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Resumen del día", style = MaterialTheme.typography.labelLarge, color = UrbanColors.GoldText)
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (current.source == "ia") "Bladebot" else "Automático",
                        style = MaterialTheme.typography.labelSmall,
                        color = UrbanColors.Muted
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(current.text, style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Ink)
            }
        }
    }
}
