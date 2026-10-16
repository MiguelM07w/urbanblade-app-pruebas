package com.urbanblade.mobile.ui.account

import com.urbanblade.mobile.ui.components.UrbanSwitchRow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.components.UrbanAvatar
import com.urbanblade.mobile.ui.components.UrbanHeroCard
import com.urbanblade.mobile.ui.components.UrbanHeroLabel
import com.urbanblade.mobile.ui.components.UrbanHeroStat
import com.urbanblade.mobile.ui.components.UrbanRolePill
import com.urbanblade.mobile.ui.theme.UrbanColors

/*
 * Piezas de "Mi cuenta", compartidas por los cinco roles. Siguen el patrón de UrbanPattern.kt:
 * la tarjeta de miembro es la tarjeta héroe (foto de la barbería, borde dorado), las pestañas
 * usan la píldora dorada de la barra inferior y cada sección es un acordeón con su icono en caja.
 */

/** Etiqueta del rol principal (si alguien tiene varios, gana el de más responsabilidad). */
fun accountRoleLabel(roles: List<String>): String {
    return com.urbanblade.mobile.core.access.UserExperience.label(roles)
}

/**
 * Tarjeta de miembro: foto (se toca para cambiarla), nombre, correo, roles y dos o tres datos
 * de la cuenta. [footnote] es una línea opcional al pie, como el "Esta semana" del administrador.
 */
@Composable
fun AccountMemberCard(
    name: String,
    email: String,
    avatarUrl: String?,
    roles: List<String>,
    uploading: Boolean,
    onChangePhoto: () -> Unit,
    stats: List<Triple<String, String, ImageVector>>,
    footnote: String? = null
) {
    UrbanHeroCard {
        UrbanHeroLabel("Miembro UrbanBlade")
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // La insignia de cámara va fuera del recorte circular de la foto para verse completa.
            Box(Modifier.size(76.dp)) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .clickable(enabled = !uploading, onClickLabel = "Cambiar foto de perfil", onClick = onChangePhoto)
                ) {
                    UrbanAvatar(name, Modifier.fillMaxSize(), imageUrl = avatarUrl)
                    if (uploading) {
                        Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = UrbanColors.Gold)
                        }
                    }
                }
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(UrbanColors.Gold)
                        .border(2.dp, UrbanColors.Background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, null, tint = UrbanColors.OnGold, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.headlineSmall, color = UrbanColors.Ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(email, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    roles.take(2).forEach { UrbanRolePill(it) }
                }
            }
        }
        if (stats.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(UrbanColors.Gold.copy(alpha = 0.22f)))
            Spacer(Modifier.height(14.dp))
            // En pantallas angostas (menos de ~340 dp de tarjeta) los datos van uno debajo del otro:
            // lado a lado, "Caballero" se cortaba en "Caball…" (checklist CN-095).
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < 300.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        stats.forEach { (label, value, icon) -> UrbanHeroStat(label, value, icon) }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        stats.forEach { (label, value, icon) -> UrbanHeroStat(label, value, icon, Modifier.weight(1f)) }
                    }
                }
            }
        }
        footnote?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
        }
    }
}

/**
 * Sección plegable: icono en caja dorada, título, un resumen de lo que hay dentro (visible aun
 * cerrada, para no tener que abrirla para saber el estado) y el contenido al desplegarla.
 */
@Composable
fun AccountAccordion(
    title: String,
    summary: String?,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    tone: Color = UrbanColors.Gold,
    content: @Composable ColumnScope.() -> Unit
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = UrbanColors.Card,
        border = BorderStroke(1.dp, if (expanded) UrbanColors.Gold.copy(alpha = 0.45f) else UrbanColors.Line),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = if (expanded) "Cerrar" else "Abrir", onClick = onToggle)
                    .semantics { stateDescription = if (expanded) "Abierta" else "Cerrada" }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tone.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = tone, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = UrbanColors.Ink)
                    summary?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                }
                Icon(Icons.Default.ExpandMore, null, tint = UrbanColors.Muted, modifier = Modifier.rotate(rotation))
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(UrbanColors.Line))
                    Spacer(Modifier.height(14.dp))
                    content()
                }
            }
        }
    }
}

/** Fila con interruptor: toda la fila se puede tocar, no solo el switch. Usa el kit ([UrbanSwitchRow]). */
@Composable
fun AccountSwitchRow(title: String, subtitle: String?, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) =
    UrbanSwitchRow(title = title, subtitle = subtitle, checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
