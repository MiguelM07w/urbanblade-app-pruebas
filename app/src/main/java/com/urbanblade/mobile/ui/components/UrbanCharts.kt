package com.urbanblade.mobile.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.theme.UrbanColors
import java.util.Locale

/**
 * Gráficos propios de analítica, dibujados con Canvas (sin dependencias extra) y con los colores
 * del tema activo. Se animan al aparecer; con "Quitar animaciones" del sistema salen completos.
 */
private fun chartPalette(): List<Color> = UrbanChartColors.palette()

private fun formatNumber(value: Double): String =
    if (value >= 1000 || value == value.toLong().toDouble()) {
        String.format(Locale("es", "MX"), "%,.0f", value)
    } else {
        String.format(Locale("es", "MX"), "%,.1f", value)
    }

/** Índice del punto más cercano a la posición [x] de un trazo con [count] puntos y márgenes [pad]. */
private fun nearestIndex(x: Float, width: Float, pad: Float, count: Int): Int =
    Math.round(((x - pad) / (width - pad * 2)) * (count - 1)).coerceIn(0, count - 1)

/**
 * Serie de tiempo: línea con área degradada. Al tocar o arrastrar sobre ella se muestra el valor de
 * cada punto (por defecto, el último). Requiere al menos 2 valores.
 */
@Composable
fun UrbanLineChart(
    labels: List<String>,
    values: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = UrbanChartColors.at(1),
    format: (Double) -> String = ::defaultFormat
) {
    if (values.size < 2) return
    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { progress.animateTo(1f, tween(durationMillis = 800)) }
    var selected by remember(values) { mutableStateOf<Int?>(null) }
    val focus = selected ?: values.lastIndex

    val lineColor = color
    val gridColor = UrbanColors.Line
    val guideColor = UrbanColors.Muted.copy(alpha = 0.5f)
    val summary = "Gráfica de línea de ${values.size} puntos, de ${format(values.first())} a ${format(values.last())}"

    Column(modifier.fillMaxWidth().semantics { contentDescription = summary }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                labels.getOrElse(focus) { "" },
                style = MaterialTheme.typography.labelMedium,
                color = UrbanColors.Muted,
                modifier = Modifier.weight(1f)
            )
            Text(format(values[focus]), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
        }
        Spacer(Modifier.height(8.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(132.dp)
                .pointerInput(values) {
                    detectTapGestures(onPress = { offset ->
                        selected = nearestIndex(offset.x, size.width.toFloat(), 6.dp.toPx(), values.size)
                    })
                }
                .pointerInput(values) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            selected = nearestIndex(offset.x, size.width.toFloat(), 6.dp.toPx(), values.size)
                        },
                        onHorizontalDrag = { change, _ ->
                            selected = nearestIndex(change.position.x, size.width.toFloat(), 6.dp.toPx(), values.size)
                        }
                    )
                }
        ) {
            val padTop = 8.dp.toPx()
            val padBottom = 8.dp.toPx()
            val padH = 6.dp.toPx()
            val plotW = size.width - padH * 2
            val plotH = size.height - padTop - padBottom
            val min = minOf(0.0, values.min())
            val max = values.max().takeIf { it > min } ?: (min + 1.0)

            fun x(i: Int) = padH + plotW * i / (values.size - 1)
            fun y(v: Double) = padTop + plotH * (1f - ((v - min) / (max - min)).toFloat())

            repeat(3) { i ->
                val gy = padTop + plotH * i / 2f
                drawLine(gridColor, Offset(padH, gy), Offset(size.width - padH, gy), strokeWidth = 1.dp.toPx())
            }

            val line = Path().apply {
                values.forEachIndexed { i, v -> if (i == 0) moveTo(x(i), y(v)) else lineTo(x(i), y(v)) }
            }
            val area = Path().apply {
                addPath(line)
                lineTo(x(values.lastIndex), padTop + plotH)
                lineTo(x(0), padTop + plotH)
                close()
            }

            clipRect(left = 0f, top = 0f, right = size.width * progress.value, bottom = size.height) {
                drawPath(
                    area,
                    Brush.verticalGradient(
                        listOf(lineColor.copy(alpha = 0.32f), lineColor.copy(alpha = 0f)),
                        startY = padTop,
                        endY = padTop + plotH
                    )
                )
                drawPath(
                    line,
                    lineColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                if (values.size <= 14) {
                    values.forEachIndexed { i, v -> drawCircle(lineColor.copy(alpha = 0.55f), radius = 3.dp.toPx(), center = Offset(x(i), y(v))) }
                }
            }
            // Guía vertical y punto resaltado del valor seleccionado.
            drawLine(guideColor, Offset(x(focus), padTop), Offset(x(focus), padTop + plotH), strokeWidth = 1.dp.toPx())
            drawCircle(lineColor, radius = 5.5.dp.toPx(), center = Offset(x(focus), y(values[focus])))
            drawCircle(Color.White.copy(alpha = 0.9f), radius = 2.2.dp.toPx(), center = Offset(x(focus), y(values[focus])))
        }
        if (labels.size >= 2) {
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(labels.first(), style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(8.dp))
                Text(labels.last(), style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            }
        }
    }
}

/** Distribución: dona con leyenda de porcentajes. Ignora valores no positivos. */
@Composable
fun UrbanDonutChart(
    labels: List<String>,
    values: List<Double>,
    modifier: Modifier = Modifier
) {
    val total = values.filter { it > 0 }.sum()
    if (total <= 0.0) return
    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { progress.animateTo(1f, tween(durationMillis = 800)) }
    val palette = chartPalette()

    Column(
        modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Gráfica de dona con ${values.size} categorías" },
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(156.dp)) {
            val stroke = 22.dp.toPx()
            val diameter = size.minDimension - stroke
            var start = -90f
            values.forEachIndexed { i, v ->
                if (v <= 0) return@forEachIndexed
                val fullSweep = (v / total * 360.0).toFloat()
                drawArc(
                    color = palette[i % palette.size],
                    startAngle = start,
                    sweepAngle = (fullSweep - 1.5f).coerceAtLeast(0f) * progress.value,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(diameter, diameter),
                    style = Stroke(width = stroke)
                )
                start += fullSweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("100%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
            Text("Distribución", style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted)
        }
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            values.forEachIndexed { i, v ->
                if (v <= 0) return@forEachIndexed
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier
                            .padding(top = 5.dp).size(10.dp)
                            .clip(CircleShape)
                            .background(palette[i % palette.size])
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        labels.getOrNull(i).orEmpty().ifBlank { "Categoría ${i + 1}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = UrbanColors.Ink,
                        modifier = Modifier.weight(1f)
                    )
                    Column(horizontalAlignment = Alignment.End) {
                    Text(formatNumber(v), style = MaterialTheme.typography.bodySmall, color = UrbanColors.Ink, fontWeight = FontWeight.SemiBold)
                    Text(
                        String.format(Locale("es", "MX"), "%.1f%%", v / total * 100),
                        style = MaterialTheme.typography.bodySmall,
                        color = UrbanColors.Muted
                    )
                    }
                }
            }
        }
    }
}

private fun defaultFormat(value: Double): String = formatNumber(value)

/**
 * Barras verticales: resalta la más alta (o la que se toque) y muestra su valor arriba, en lugar de
 * rotular cada barra. Con muchas barras solo se rotulan algunas para que el eje no se amontone.
 */
@Composable
fun UrbanBarChart(
    labels: List<String>,
    values: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = UrbanChartColors.at(0),
    format: (Double) -> String = ::defaultFormat
) {
    if (values.isEmpty() || values.all { it <= 0.0 }) return
    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { progress.animateTo(1f, tween(durationMillis = 700)) }
    var selected by remember(values) { mutableStateOf<Int?>(null) }
    val max = values.max()
    val focus = selected ?: values.indexOf(max)
    val step = if (values.size <= 8) 1 else (values.size + 5) / 6

    Column(
        modifier.fillMaxWidth().semantics {
            contentDescription = "Gráfica de barras con ${values.size} valores; el más alto es ${format(max)}"
        }
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                labels.getOrElse(focus) { "" },
                style = MaterialTheme.typography.labelMedium,
                color = UrbanColors.Muted,
                modifier = Modifier.weight(1f)
            )
            Text(format(values[focus]), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().height(128.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            values.forEachIndexed { i, v ->
                val fraction = if (v <= 0.0) 0f else ((v / max).toFloat() * progress.value).coerceIn(0.03f, 1f)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            selected = if (selected == i) null else i
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(Brush.verticalGradient(listOf(if (i == focus) color else color.copy(alpha = 0.75f), color.copy(alpha = 0.25f))))
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            values.indices.forEach { i ->
                Text(
                    if (i % step == 0) labels.getOrElse(i) { "" } else "",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    style = MaterialTheme.typography.labelSmall,
                    color = UrbanColors.Muted
                )
            }
        }
    }
}

/** Ranking o desglose: una fila por elemento con su barra proporcional al mayor. */
@Composable
fun UrbanHBars(
    labels: List<String>,
    values: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = UrbanChartColors.at(0),
    format: (Double) -> String = ::defaultFormat,
    multicolor: Boolean = true
) {
    if (values.isEmpty() || values.all { it <= 0.0 }) return
    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { progress.animateTo(1f, tween(durationMillis = 700)) }
    val max = values.max()

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        values.forEachIndexed { i, v ->
            val tone = if (multicolor) UrbanChartColors.at(i) else color
            Column(Modifier.semantics { contentDescription = "${labels.getOrElse(i) { "" }}: ${format(v)}" }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 5.dp).size(8.dp).clip(CircleShape).background(tone))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(labels.getOrElse(i) { "" }, style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Ink)
                        Text(format(v), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = UrbanColors.Ink)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(UrbanColors.Line.copy(alpha = 0.5f))) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(((v / max).toFloat() * progress.value).coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(50))
                            .background(Brush.horizontalGradient(listOf(tone.copy(alpha = 0.65f), tone)))
                    )
                }
            }
        }
    }
}

/** Medidor circular de un porcentaje (0 a 1) con el valor al centro. */
@Composable
fun UrbanRingGauge(
    fraction: Float,
    centerText: String,
    modifier: Modifier = Modifier,
    color: Color = UrbanColors.Gold,
    diameter: androidx.compose.ui.unit.Dp = 96.dp
) {
    val target = fraction.coerceIn(0f, 1f)
    val progress = remember(target) { Animatable(0f) }
    LaunchedEffect(target) { progress.animateTo(target, tween(durationMillis = 800)) }
    val track = UrbanColors.Line.copy(alpha = 0.5f)

    Box(modifier.size(diameter).semantics { contentDescription = centerText }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val stroke = 10.dp.toPx()
            val d = size.minDimension - stroke
            drawArc(track, -90f, 360f, useCenter = false, topLeft = Offset(stroke / 2, stroke / 2), size = Size(d, d), style = Stroke(stroke))
            drawArc(
                color, -90f, 360f * progress.value, useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2), size = Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Text(centerText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
    }
}

/**
 * Nivel de stock: barra llena hasta las existencias actuales con una marca en el mínimo. Roja si está
 * en el mínimo o por debajo, verde si no. La escala llega al doble del mínimo para que la marca se vea.
 */
@Composable
fun UrbanStockBar(current: Int, minimum: Int, modifier: Modifier = Modifier) {
    val low = current <= minimum
    val scale = maxOf(minimum * 2, current, 1).toFloat()
    val fillTarget = (current / scale).coerceIn(0f, 1f)
    val markTarget = (minimum / scale).coerceIn(0f, 1f)
    val progress = remember(current, minimum) { Animatable(0f) }
    LaunchedEffect(current, minimum) { progress.animateTo(1f, tween(durationMillis = 600)) }
    val tone = if (low) UrbanColors.Danger else UrbanColors.Success

    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(UrbanColors.Line.copy(alpha = 0.5f))
            .semantics { contentDescription = "Stock $current, mínimo $minimum" }
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fillTarget * progress.value).clip(RoundedCornerShape(50)).background(tone))
        if (minimum > 0) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(markTarget), contentAlignment = Alignment.CenterEnd) {
                Box(Modifier.fillMaxHeight().width(2.dp).background(UrbanColors.Ink.copy(alpha = 0.55f)))
            }
        }
    }
}
