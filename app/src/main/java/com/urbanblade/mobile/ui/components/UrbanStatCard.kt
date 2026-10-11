package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.theme.UrbanColors

/**
 * Tono de una variación. Igual que en los estados, el color sale de [StatusTone.color], así que
 * hay un solo lugar donde viven los colores semánticos.
 *
 * [positiveIsGood] existe porque no siempre subir es bueno: más ingresos es verde, pero más
 * cancelaciones o más gastos es rojo. Un KPI de la app fintech de referencia siempre pintaba
 * verde al alza; aquí la pantalla decide.
 */
fun trendTone(delta: Double, positiveIsGood: Boolean = true): StatusTone = when {
    delta > 0.0 -> if (positiveIsGood) StatusTone.SUCCESS else StatusTone.DANGER
    delta < 0.0 -> if (positiveIsGood) StatusTone.DANGER else StatusTone.SUCCESS
    else -> StatusTone.NEUTRAL
}

/**
 * Variación de un KPI contra el periodo anterior ("+12.6 % vs mes pasado").
 *
 * El signo va en el texto y la flecha en el ícono: el color no es la única señal, como pide el
 * checklist de accesibilidad. Si [suffix] es `null` solo se muestra la píldora.
 */
@Composable
fun UrbanTrendPill(
    delta: Double,
    positiveIsGood: Boolean = true,
    suffix: String? = "vs periodo anterior",
    modifier: Modifier = Modifier
) {
    val tone = trendTone(delta, positiveIsGood).color()
    val icon = when {
        delta > 0.0 -> Icons.AutoMirrored.Filled.TrendingUp
        delta < 0.0 -> Icons.AutoMirrored.Filled.TrendingDown
        else -> null
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        UrbanTonePill(
            label = (if (delta > 0.0) "+" else "") + UrbanFormat.percent(delta),
            icon = icon,
            tone = tone
        )
        suffix?.let {
            Spacer(Modifier.width(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * KPI con su variación: la etiqueta arriba, el valor grande y, si hay con qué compararlo, la
 * píldora de tendencia. Es la pieza que faltaba del kit (en la web ya existe `UiStatCard`) y la
 * que usan los inicios de administrador y recepción.
 *
 * [delta] llega tal cual de [UrbanFormat.delta]: si es `null` no se inventa una variación y en su
 * lugar se muestra [caption]. El valor va en `titleLarge` y no en serif: sobrevive al tamaño de
 * fuente del sistema sin desbordar la tarjeta.
 */
@Composable
fun UrbanStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    delta: Double? = null,
    positiveIsGood: Boolean = true,
    caption: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    UrbanCard(modifier, onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = UrbanColors.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            icon?.let {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(UrbanColors.CardAlt),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(it, null, tint = UrbanColors.Gold, modifier = Modifier.size(17.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = UrbanColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (delta != null) {
            Spacer(Modifier.height(8.dp))
            UrbanTrendPill(delta, positiveIsGood, suffix = caption)
        } else if (caption != null) {
            Spacer(Modifier.height(8.dp))
            Text(caption, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
