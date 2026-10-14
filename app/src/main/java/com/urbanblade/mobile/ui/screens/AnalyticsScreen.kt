package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.AnalyticsGraph
import com.urbanblade.mobile.data.model.AnalyticsInsightItem
import com.urbanblade.mobile.data.model.AnalyticsSection
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.AnalyticsViewModel

/**
 * Consume la misma serie del backend que la web: líneas para evolución,
 * dona para distribución y ranking para las otras series numéricas disponibles.
 * No reconstruye matrices ni inventa valores a partir de datos no numéricos.
 */
private fun colorFor(name: String?): androidx.compose.ui.graphics.Color = when (name) {
    "success" -> UrbanColors.Success
    "info" -> UrbanColors.Info
    "warning" -> UrbanColors.Warning
    "danger" -> UrbanColors.Danger
    "gold" -> UrbanColors.Gold
    else -> UrbanColors.Gold
}

@Composable
private fun InsightGraph(graph: AnalyticsGraph) {
    val series = analyticsChartSeries(graph)
    val values = series.values
    if (values.isEmpty()) return
    // Series de tiempo -> línea; distribuciones -> dona; el resto conserva las barras proporcionales.
    when (graph.tipo?.lowercase()) {
        "line" -> if (values.size >= 2) {
            UrbanLineChart(series.labels, values, Modifier.padding(top = 10.dp))
            return
        }
        "doughnut", "pie" -> if (values.any { it > 0 }) {
            UrbanDonutChart(series.labels, values, Modifier.padding(top = 10.dp))
            return
        }
    }
    // Cualquier otro tipo de gráfica: ranking en barras horizontales del kit común.
    UrbanHBars(
        labels = series.labels,
        values = values,
        modifier = Modifier.padding(top = 10.dp)
    )
}

@Composable
private fun InsightCard(insight: AnalyticsInsightItem) {
    UrbanAnalyticsCard(
        title = insight.titulo ?: "Indicador",
        subtitle = insight.mensaje,
        metric = insight.valorDestacado,
        accent = colorFor(insight.color),
        eyebrow = "LECTURA DEL NEGOCIO"
    ) {
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
    var tab by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { vm.load() }

    val tabs = listOf(
        "Resumen" to data.secciones.resumen,
        "Operación" to data.secciones.operacion,
        "Clientes" to data.secciones.clientes,
        "Predicción" to data.secciones.prediccion,
    )

    val hasData = data.kpis.isNotEmpty() || data.operational != null || data.sparkFlow.isNotEmpty() ||
        tabs.any { it.second.insights.isNotEmpty() } || data.diagnosticoInsights.isNotEmpty()

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
                UrbanAnalyticsCard(
                    title = heroKpis.first().label ?: "Indicadores clave",
                    metric = heroKpis.first().value,
                    subtitle = heroKpis.first().detail,
                    eyebrow = "PULSO DEL NEGOCIO"
                ) {
                    heroKpis.getOrNull(1)?.let { second ->
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = UrbanColors.Ink.copy(alpha = 0.22f))
                        Spacer(Modifier.height(14.dp))
                        UrbanKeyValue(second.label ?: "—", second.value ?: "—")
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
                    restKpis.chunked(3).forEach { row -> UrbanStatStrip(row.mapIndexed { index, kpi -> Triple(kpi.label ?: "—", kpi.value ?: "—", UrbanChartColors.at(index)) }) }
                }
            }
        }

        data.operational?.let { operational ->
            item { UrbanSectionTitle("Operación de hoy", "Citas, pagos y pendientes") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    operational.kpis.chunked(3).forEach { row -> UrbanStatStrip(row.mapIndexed { index, kpi -> Triple(kpi.label ?: "—", kpi.value.toInt().toString(), UrbanChartColors.at(index)) }) }
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
            val icons = listOf(Icons.Default.Insights, Icons.Default.Today, Icons.Default.People, Icons.Default.AutoGraph)
            UrbanPillTabs(tabs.mapIndexed { index, pair -> pair.first to icons[index] }, tab) { tab = it }
        }
        item { SectionContent(tabs[tab].second) }

        if (data.diagnosticoInsights.isNotEmpty()) {
            item { UrbanSectionTitle("Calidad de los datos", "Diagnóstico del pipeline analítico") }
            items(data.diagnosticoInsights) { InsightCard(it) }
        }
    }
}
