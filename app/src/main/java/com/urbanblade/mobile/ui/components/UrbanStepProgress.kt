package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.theme.UrbanColors

/** Progreso informativo: no permite saltar las validaciones de ningún paso. */
@Composable
fun UrbanStepProgress(labels: List<String>, current: Int, modifier: Modifier = Modifier) {
    if (labels.isEmpty()) return
    val focus = current.coerceIn(labels.indices)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            labels.forEachIndexed { index, label ->
                val completed = index < focus
                val active = index == focus
                Column(Modifier.weight(1f).clearAndSetSemantics {
                    contentDescription = "${index + 1}. $label: ${if (completed) "completado" else if (active) "paso actual" else "pendiente"}"
                }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier.size(30.dp).clip(CircleShape)
                        .background(if (completed || active) UrbanColors.Gold else UrbanColors.CardAlt)
                        .semantics { contentDescription = "${index + 1}. $label: ${if (completed) "completado" else if (active) "paso actual" else "pendiente"}" },
                    contentAlignment = Alignment.Center
                ) {
                    if (completed) Icon(Icons.Default.Check, null, tint = UrbanColors.OnGold, modifier = Modifier.size(17.dp))
                    else Text("${index + 1}", style = MaterialTheme.typography.labelLarge, color = if (active) UrbanColors.OnGold else UrbanColors.Muted)
                }
                Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = if (active) UrbanColors.GoldText else UrbanColors.Muted)
                }
                if (index < labels.lastIndex) Box(Modifier.padding(top = 14.dp).width(10.dp).height(2.dp).background(if (completed) UrbanColors.Gold else UrbanColors.Line))
            }
        }
        Text("${labels[focus]} · paso ${focus + 1} de ${labels.size}", style = MaterialTheme.typography.labelLarge, color = UrbanColors.GoldText)
    }
}
