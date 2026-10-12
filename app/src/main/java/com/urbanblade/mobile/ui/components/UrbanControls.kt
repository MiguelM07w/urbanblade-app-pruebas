package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.theme.UrbanColors

/*
 * Kit de controles (skill urbanblade-mobile-ui-patrones, sección 8). Cambian cómo se ve un
 * control, nunca el dato: los Boolean y BookingPayMethod que llegan al backend son los mismos.
 */

/** Colores únicos del interruptor. `tone` = Danger para apagados peligrosos (mantenimiento). */
@Composable
fun urbanSwitchColors(tone: Color = UrbanColors.Gold): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = UrbanColors.OnGold,
    checkedTrackColor = tone,
    checkedBorderColor = tone,
    uncheckedThumbColor = UrbanColors.Muted,
    uncheckedTrackColor = UrbanColors.CardAlt,
    uncheckedBorderColor = UrbanColors.Line
)

/** Interruptor suelto (p. ej. al final de una fila de lista). */
@Composable
fun UrbanSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: Color = UrbanColors.Gold
) {
    Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled, colors = urbanSwitchColors(tone), modifier = modifier)
}

/** Fila con interruptor: toda la fila se puede tocar y TalkBack la anuncia como interruptor. */
@Composable
fun UrbanSwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: Color = UrbanColors.Gold
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics { contentDescription = title }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = UrbanColors.Ink)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted) }
        }
        Spacer(Modifier.width(12.dp))
        UrbanSwitch(checked = checked, onCheckedChange = null, enabled = enabled, tone = tone)
    }
}

/** Opción de [UrbanChoiceTiles]. `value` es el mismo valor que ya usaba la pantalla. */
data class UrbanChoice<T>(
    val value: T,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

/**
 * Elegir una opción entre 2 y 3 como mosaicos lado a lado (método de pago). Es el diseño
 * "Propuesta A" que eligió el usuario el 25-sep para la reserva, ahora compartido: seleccionado
 * con borde y círculo dorados; TalkBack lo anuncia como botón de opción.
 */
@Composable
fun <T> UrbanChoiceTiles(
    options: List<UrbanChoice<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Max).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { option ->
            val isSelected = option.value == selected
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isSelected) UrbanColors.Gold.copy(alpha = 0.14f) else UrbanColors.Card,
                border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) UrbanColors.Gold else UrbanColors.Muted.copy(alpha = 0.3f)),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option.value) })
            ) {
                Column(
                    Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) UrbanColors.Gold else UrbanColors.Gold.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(option.icon, null, tint = if (isSelected) UrbanColors.OnGold else UrbanColors.Gold, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        option.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) UrbanColors.GoldText else UrbanColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        option.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = UrbanColors.Muted,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
