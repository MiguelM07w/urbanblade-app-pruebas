package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.theme.UrbanColors

/*
 * Lista compacta: filas con separador dentro de una sola tarjeta, en vez de una tarjeta con borde
 * por elemento. Es la conclusión P0 de la auditoría de interfaz (24-sep-2026): "reducir el uso de
 * tarjetas; usar filas y separadores para listas".
 *
 * A propósito NO hay encabezado de columnas: cada fila lleva su propia etiqueta y su cifra, y una
 * rejilla de columnas se desalinea en 320 dp o con el tamaño de fuente al máximo.
 */

/** Separador de 1 dp entre filas. Solo hace falta cuando la fila se usa suelta en una lista perezosa. */
@Composable
fun UrbanRowDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(UrbanColors.Line))
}

/**
 * Fila compacta de una lista: título, dato secundario, una tercera línea de contexto ([meta]), estado
 * y cifra o acción a la derecha.
 * Mide al menos 52 dp de alto, así que cumple el objetivo táctil mínimo aunque no lleve texto largo.
 *
 * [status] se dibuja con [UrbanStatusPill], de modo que "por verificar", "Cancelada" o "Entregado"
 * se lean igual aquí que en el resto de la app (mismo tono, mismo ícono).
 */
@Composable
fun UrbanRow(
    title: String,
    subtitle: String? = null,
    meta: String? = null,
    modifier: Modifier = Modifier,
    status: String? = null,
    value: String? = null,
    valueTone: Color = UrbanColors.Ink,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier
            .fillMaxWidth()
            .then(clickable)
            .heightIn(min = 52.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading?.let {
            it()
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = UrbanColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            meta?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            status?.let {
                Spacer(Modifier.height(5.dp))
                UrbanStatusPill(it)
            }
        }
        value?.let {
            Spacer(Modifier.width(10.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = valueTone, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing?.let {
            Spacer(Modifier.width(6.dp))
            it()
        }
    }
}

/**
 * Varias [UrbanRow] dentro de una sola tarjeta, con su separador entre cada una.
 *
 * Es para listas que ya están en memoria (un resumen, no cientos de movimientos). Para listas
 * largas o perezosas, la pantalla usa [UrbanRow] y [UrbanRowDivider] dentro de su propio LazyColumn,
 * porque anidar un LazyColumn dentro de otro no está permitido.
 *
 * Si la lista viene vacía se dibuja [empty] y no una tarjeta hueca.
 */
@Composable
fun <T> UrbanRowGroup(
    items: List<T>,
    modifier: Modifier = Modifier,
    empty: (@Composable () -> Unit)? = null,
    row: @Composable (T) -> Unit
) {
    if (items.isEmpty()) {
        empty?.invoke()
        return
    }
    UrbanCard(modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            if (index > 0) UrbanRowDivider()
            row(item)
        }
    }
}
