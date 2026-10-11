package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.theme.UrbanColors

/**
 * Envoltura para contenido de formulario (diálogos, columnas planas) que debe
 * seguir siendo alcanzable cuando el teclado está abierto -- mismo patrón que
 * ya usa `AuthShell` en AuthScreens.kt, extraído aquí para no repetirlo por
 * cada diálogo con campos de texto.
 */
@Composable
fun UrbanFormScroll(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content
    )
}

@Composable
fun UrbanBladeBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        UrbanColors.GradientTop,
                        UrbanColors.Background,
                        UrbanColors.GradientBottom
                    )
                )
            ),
        content = content
    )
}

@Composable
fun UrbanBrandMark(compact: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(if (compact) 34.dp else 42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF302712), Color(0xFF17130A))
                    )
                )
                .border(1.dp, UrbanColors.Gold.copy(alpha = 0.33f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Marca "UB" real (frontend-urban/public/images/urbanblade-mark.svg,
            // portada a drawable/ic_launcher_foreground.xml) en vez del ícono
            // de tijeras genérico que había antes.
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(com.urbanblade.mobile.R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(if (compact) 26.dp else 32.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                "URBANBLADE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = UrbanColors.Ink
            )
            if (!compact) {
                Text(
                    "SASTRERÍA NOCTURNA",
                    style = MaterialTheme.typography.labelMedium,
                    color = UrbanColors.Gold,
                )
            }
        }
    }
}

@Composable
fun UrbanPageHeader(
    title: String,
    subtitle: String? = null,
    eyebrow: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            eyebrow?.let {
                Text(
                    it.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = UrbanColors.Gold
                )
                Spacer(Modifier.height(5.dp))
            }
            Text(title, style = MaterialTheme.typography.headlineMedium, color = UrbanColors.Ink)
            subtitle?.let {
                Spacer(Modifier.height(5.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
            }
        }
        trailing?.let {
            Spacer(Modifier.width(12.dp))
            it()
        }
    }
}

@Composable
fun UrbanSectionTitle(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = UrbanColors.Ink)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted) }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun UrbanPremiumCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.large,
    content: @Composable ColumnScope.() -> Unit
) {
    // GradientTop/GradientBottom son reactivos al tema (ver UrbanColors) --
    // antes esto usaba dos negros casi-fijos que se veían mal en "libreta"
    // (el único tema claro).
    val base = modifier
        .clip(shape)
        .background(
            Brush.linearGradient(
                listOf(UrbanColors.GradientTop, UrbanColors.GradientBottom)
            )
        )
        .border(1.dp, UrbanColors.Line, shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Column(base.padding(18.dp), content = content)
}

@Composable
fun UrbanCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        modifier = modifier.then(clickableModifier),
        shape = MaterialTheme.shapes.medium,
        color = UrbanColors.Card,
        border = BorderStroke(1.dp, UrbanColors.Line),
        tonalElevation = 0.dp
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun UrbanMetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    UrbanPremiumCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    // Caja neutra e icono dorado: con el tinte dorado en cada tarjeta el dorado
                    // dejaba de ser énfasis (auditoría, P2-3).
                    .background(UrbanColors.CardAlt),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = UrbanColors.Gold, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge, color = UrbanColors.Ink)
                Text(label, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun UrbanQuickAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    UrbanPremiumCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(UrbanColors.Gold),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = UrbanColors.OnGold, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            }
            // Gris, como en UrbanRow y UrbanAttentionRow: el chevrón solo dice "hay más".
            Icon(Icons.Default.ChevronRight, null, tint = UrbanColors.Muted)
        }
    }
}

/**
 * Píldora de un tono semántico, con ícono opcional. La usan los estados de cita/pago
 * ([UrbanStatusPill]) y la variación de un KPI ([UrbanTrendPill]), para que las dos se vean igual.
 *
 * El color va mezclado con el texto del tema: el ámbar o el verde puros casi no se leen sobre el
 * crema de "Libreta" (lo mismo que ya hacía UrbanStatusPill).
 */
@Composable
internal fun UrbanTonePill(label: String, icon: ImageVector?, tone: Color, modifier: Modifier = Modifier) {
    // El tono ya viene resuelto a Color (StatusTone.color() lo resuelve quien llama).
    val color = tone
    val content = androidx.compose.ui.graphics.lerp(color, UrbanColors.Ink, 0.3f)
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Row(
            Modifier.padding(start = if (icon != null) 8.dp else 10.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(it, null, tint = content, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1)
        }
    }
}

@Composable
fun UrbanStatusPill(status: String) {
    // Etiqueta, tono e ícono salen de statusStyle() (UrbanStatus.kt), el mismo en toda la app.
    val style = statusStyle(status)
    UrbanTonePill(label = style.label, icon = style.icon, tone = style.tone.color())
}

@Composable
fun UrbanRolePill(role: String) {
    Surface(
        shape = CircleShape,
        color = UrbanColors.Gold.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, UrbanColors.Gold.copy(alpha = 0.33f))
    ) {
        Text(
            role.replace('_', ' ').replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = UrbanColors.Gold
        )
    }
}

@Composable
fun UrbanAvatar(name: String, modifier: Modifier = Modifier, imageUrl: String? = null) {
    val initials = name.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(
        modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(UrbanColors.Gold.copy(alpha = 0.13f))
            .border(1.dp, UrbanColors.Gold.copy(alpha = 0.33f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(initials.ifBlank { "UB" }, style = MaterialTheme.typography.labelLarge, color = UrbanColors.Gold)
        // La foto va encima de las iniciales: mientras carga (o si falla) se siguen viendo las iniciales.
        if (!imageUrl.isNullOrBlank()) {
            coil.compose.AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun UrbanPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp),
        // Igual que AuthPrimaryButton: gris con borde si falta algo, dorado mientras carga.
        colors = ButtonDefaults.buttonColors(
            containerColor = UrbanColors.Gold,
            contentColor = UrbanColors.OnGold,
            // "Gris con borde" se queda (el dorado apagado parecía un botón roto), pero con
            // CardAlt y el texto al 75 % del color de tinta: sobre Card el botón se veía como un
            // hueco y la auditoría lo reportó como "parece defectuoso".
            disabledContainerColor = if (enabled || loading) UrbanColors.Gold else UrbanColors.CardAlt,
            disabledContentColor = if (enabled || loading) UrbanColors.OnGold else UrbanColors.Ink.copy(alpha = 0.75f)
        ),
        border = if (enabled || loading) null else BorderStroke(1.dp, UrbanColors.Line)
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp, color = UrbanColors.OnGold)
        } else {
            icon?.let { Icon(it, null, modifier = Modifier.size(19.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun UrbanOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 50.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, UrbanColors.Line)
    ) {
        icon?.let { Icon(it, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text)
    }
}

@Composable
fun UrbanErrorBanner(text: String, modifier: Modifier = Modifier.fillMaxWidth()) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = UrbanColors.Danger.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, UrbanColors.Danger.copy(alpha = 0.3f))
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ErrorOutline, null, tint = UrbanColors.Danger)
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Ink)
        }
    }
}

@Composable
fun UrbanInfoBanner(text: String, icon: ImageVector = Icons.Default.Info, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = UrbanColors.Gold.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, UrbanColors.Gold.copy(alpha = 0.25f))
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = UrbanColors.Gold)
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Ink)
        }
    }
}

/**
 * Estado vacío de pantalla completa: la mascota Nava, el título y, si aplica, una acción.
 *
 * Cuál usar, para no inventar variantes locales:
 * - pantalla o sección principal sin datos → esta ([UrbanEmptyState]);
 * - error (Bruno, con "Reintentar") → [UrbanMascotState] con [UrbanStateKind.ERROR];
 * - bloque secundario dentro de una lista → [UrbanInlineEmpty], que es compacto.
 */
@Composable
fun UrbanEmptyState(
    title: String,
    subtitle: String? = null,
    icon: ImageVector = Icons.Default.ContentCut,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    // Todo vacío lleva a Nava, igual que UrbanMascotState; el ícono queda para el botón de acción.
    UrbanMascotState(UrbanStateKind.EMPTY, title, subtitle, actionLabel = actionLabel, actionIcon = icon, onAction = onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrbanTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = UrbanColors.Background,
            titleContentColor = UrbanColors.Ink,
            navigationIconContentColor = UrbanColors.Ink,
            actionIconContentColor = UrbanColors.Gold
        )
    )
}

@Composable
fun UrbanFieldLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
}

@Composable
fun UrbanKeyValue(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
