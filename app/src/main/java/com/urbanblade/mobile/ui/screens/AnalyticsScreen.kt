package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.JsonElement
import com.urbanblade.mobile.data.model.AnalyticsGraph
import com.urbanblade.mobile.data.model.AnalyticsInsightItem
import com.urbanblade.mobile.data.model.AnalyticsSection
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.AnalyticsViewModel

/**
 * Simplificación consciente: el backend manda "grafica" con la forma exacta
 * que espera Chart.js en la web (line/bar/doughnut/heatmap/radar/matrix/
 * factor-list). Reconstruir los 7 tipos nativamente es un proyecto aparte --
 * aquí se muestra una sola representación genérica (barras horizontales
 * proporcionales) para cualquier insight con datos graficables, priorizando
 * que el contenido real (título, mensaje, valor destacado) sí esté completo.
 */
private fun colorFor(name: String?): androidx.compose.ui.graphics.Color = when (name) {
    "success" -> UrbanColors.Success
    "info" -> UrbanColors.Info
    "warning" -> UrbanColors.Warning
    "danger" -> UrbanColors.Danger
    "gold" -> UrbanColors.Gold
    else -> UrbanColors.Gold
}

private fun JsonElement.toNumberOrNull(): Double? = try {
    when {
        isJsonPrimitive && asJsonPrimitive.isNumber -> asDouble
        isJsonPrimitive -> asString.replace(",", "").replace("%", "").replace("$", "").trim().toDoubleOrNull()
        else -> null
    }
} catch (e: Exception) { null }

@Composable
private fun InsightGraph(graph: AnalyticsGraph) {
    val values = graph.valores.mapNotNull { it.toNumberOrNull() }
    if (values.isEmpty()) return
    // Series de tiempo -> línea; distribuciones -> dona; el resto conserva las barras proporcionales.
    when (graph.tipo?.lowercase()) {
        "line" -> if (values.size >= 2) {
            UrbanLineChart(graph.labels, values, Modifier.padding(top = 10.dp))
            return
        }
        "doughnut", "pie" -> if (values.any { it > 0 }) {
            UrbanDonutChart(graph.labels, values, Modifier.padding(top = 10.dp))
            return
        }
    }
    // Cualquier otro tipo de gráfica: ranking en barras horizontales del kit común.
    UrbanHBars(
        labels = values.indices.map { graph.labels.getOrNull(it).orEmpty() },
        values = values,
        modifier = Modifier.padding(top = 10.dp)
    )
}

@Composable
private fun InsightCard(insight: AnalyticsInsightItem) {
    UrbanCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(insight.titulo ?: "—", style = MaterialTheme.typography.titleMedium)
                insight.mensaje?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                }
            }
            insight.valorDestacado?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.width(10.dp))
                Text(it, style = MaterialTheme.typography.titleLarge, color = colorFor(insight.color))
            }
        }
        if (insight.hasRenderableVisual) {
            insight.grafica?.let { InsightGraph(it) }
        }
    }
}

@Composable
private fun SectionContent(section: AnalyticsSection) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        section.subtitulo?.let {
            Text(it, style = MaterialTheme.typography.titleMedium, color = UrbanColors.GoldText)
        }
        section.intro?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
        }
        if (section.insights.isEmpty()) {
            UrbanMascotState(UrbanStateKind.EMPTY, "Sin datos todavía", "Esta sección se llena conforme haya más actividad.")
        } else {
            section.insights.forEach { InsightCard(it) }
        }
    }
}

@Composable
fun AnalyticsScreen(onBack: () -> Unit, vm: AnalyticsViewModel = viewModel()) {
    val data by vm.data.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    var tab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { vm.load() }

    val tabs = listOf(
        "Resumen" to data.secciones.resumen,
        "Operación" to data.secciones.operacion,
        "Clientes" to data.secciones.clientes,
        "Predicción" to data.secciones.prediccion,
    )

    val hasData = data.kpis.isNotEmpty() || data.operational != null || data.sparkFlow.isNotEmpty()

    UrbanModuleScreen(
        eyebrow = "ANÁLISIS",
        title = "Analítica",
        subtitle = "Operación, clientes y predicción en un solo lugar.",
        onBack = onBack,
        onRefresh = { vm.load() },
        refreshing = busy
    ) {
        if (error != null && !hasData) {
            item { UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar la analítica", error, "Reintentar") { vm.load() } }
            return@UrbanModuleScreen
        }
        if (busy && !hasData) {
            item { UrbanSkeletonList(2) }
            return@UrbanModuleScreen
        }
        error?.let { item { UrbanErrorBanner(it) } }

        val heroKpis = data.kpis.take(2)
        if (heroKpis.isNotEmpty()) {
            item {
                UrbanHeroCard {
                    UrbanHeroLabel("Indicadores clave")
                    Spacer(Modifier.height(6.dp))
                    Text(heroKpis.first().value ?: "—", style = MaterialTheme.typography.displaySmall, color = UrbanColors.Ink)
                    Text(heroKpis.first().label ?: "—", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                    heroKpis.getOrNull(1)?.let { second ->
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = UrbanColors.Ink.copy(alpha = 0.22f))
                        Spacer(Modifier.height(14.dp))
                        UrbanHeroStat(second.label ?: "—", second.value ?: "—", Icons.Default.TrendingUp)
                    }
                    data.ultimaActualizacion?.let {
                        Spacer(Modifier.height(10.dp))
                        Text("Actualizado: $it", style = MaterialTheme.typography.labelMedium, color = UrbanColors.GoldText)
                    }
                }
            }
        }

        val restKpis = data.kpis.drop(2)
        if (restKpis.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    restKpis.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { kpi ->
                                UrbanMetricCard(kpi.label ?: "—", kpi.value ?: "—", Icons.Default.TrendingUp, Modifier.weight(1f))
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        data.operational?.let { operational ->
            item { UrbanSectionTitle("Operación de hoy", "Citas, pagos y pendientes") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    operational.kpis.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { kpi ->
                                UrbanMetricCard(kpi.label ?: "—", kpi.value.toInt().toString(), Icons.Default.Today, Modifier.weight(1f))
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            if (operational.actions.isNotEmpty()) {
                items(operational.actions) { action ->
                    UrbanAttentionRow(Icons.Default.PriorityHigh, action.label ?: "Pendiente", action.detail, UrbanColors.Warning)
                }
            }
        }

        if (data.sparkFlow.isNotEmpty()) {
            item { UrbanSectionTitle("Cobertura de la analítica", "Qué tan completo está cada bloque") }
            items(data.sparkFlow) { step ->
                UrbanCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(step.titulo ?: "—", style = MaterialTheme.typography.titleMedium)
                        Text("${step.count}/${step.total}", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                    }
                    step.descripcion?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted) }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (step.progress / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = colorFor(step.color),
                        trackColor = UrbanColors.Line
                    )
                }
            }
        }

        item { UrbanSectionTitle("Secciones", "Detalle por área") }
        item {
            ScrollableTabRow(
                selectedTabIndex = tab,
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = UrbanColors.Gold,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, (label, _) ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
                }
            }
        }
        item { SectionContent(tabs[tab].second) }

        if (data.diagnosticoInsights.isNotEmpty()) {
            item { UrbanSectionTitle("Calidad de los datos", "Diagnóstico del pipeline analítico") }
            items(data.diagnosticoInsights) { InsightCard(it) }
        }
    }
}
