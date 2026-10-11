package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.theme.UrbanColors

/**
 * Estado vacío compacto, en una sola fila. Para secciones secundarias que pueden estar vacías
 * (membresía, paquetes, gift cards): el estado vacío centrado y grande ([UrbanEmptyState]) se queda
 * para la pantalla principal, no para cada bloque de una lista de secciones.
 *
 * Si el bloque admite una acción real, va aquí (no se deja un mensaje pasivo sin salida).
 */
@Composable
fun UrbanInlineEmpty(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    UrbanCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(UrbanColors.CardAlt),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = UrbanColors.Gold, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = UrbanColors.Ink)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                }
            }
        }
    }
}
