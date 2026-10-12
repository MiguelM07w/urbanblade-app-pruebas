package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.ReportChart
import com.urbanblade.mobile.data.model.ReportData
import com.urbanblade.mobile.ui.components.UrbanBarChart
import com.urbanblade.mobile.ui.components.UrbanHeroCard
import com.urbanblade.mobile.ui.components.UrbanHeroLabel
import com.urbanblade.mobile.ui.components.UrbanMascotState
import com.urbanblade.mobile.ui.components.UrbanModuleScreen
import com.urbanblade.mobile.ui.components.UrbanStateKind
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanEmptyState
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanHBars
import com.urbanblade.mobile.ui.components.UrbanLineChart
import com.urbanblade.mobile.ui.components.UrbanOutlineButton
import com.urbanblade.mobile.ui.components.UrbanPremiumCard
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.components.UrbanTopBar
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.ReportsViewModel

private val TYPE_LABEL = mapOf(
    "ingresos" to "Ingresos",
    "citas" to "Citas",
    "inventario" to "Inventario",
    "clientes" to "Clientes",
)

private const val PAGE = 25

/** Reportes del negocio: tipo y rango rápidos, gráfica del periodo y el detalle en tarjetas. */
@Composable
fun ReportsScreen(onBack: () -> Unit, vm: ReportsViewModel = viewModel()) {
    val manifest by vm.manifest.collectAsState()
    val report by vm.report.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()

    var type by remember { mutableStateOf<String?>(null) }
    var range by remember { mutableStateOf(ReportRange.Last30) }
    var shown by remember(report) { mutableIntStateOf(PAGE) }

    LaunchedEffect(Unit) { vm.loadManifest() }
    LaunchedEffect(manifest) { if (type == null) type = manifest.types.firstOrNull() }
    // Al cambiar de tipo o de rango se genera solo: no hay botón que olvidar.
    LaunchedEffect(type, range) {
        val selected = type ?: return@LaunchedEffect
        val (start, end) = range.dates()
        vm.generate(selected, start, end)
    }

    val regenerate = { type?.let { t -> range.dates().let { (s, e) -> vm.generate(t, s, e) } } }

    UrbanModuleScreen(
        eyebrow = "ANÁLISIS",
        title = "Reportes",
        subtitle = "Elige qué quieres ver y el periodo; se genera solo.",
        onBack = onBack,
        onRefresh = { regenerate() },
        refreshing = busy
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    manifest.types.forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(TYPE_LABEL[t] ?: t) })
                    }
                }
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportRange.entries.forEach { option ->
                        FilterChip(selected = range == option, onClick = { range = option }, label = { Text(option.label) })
                    }
                }
            }
        }

        val r = report
        when {
            error != null && r == null -> item {
                UrbanMascotState(UrbanStateKind.ERROR, "No pudimos generar el reporte", error, "Reintentar") {
                    if (type == null) vm.loadManifest() else regenerate()
                }
            }
            r == null -> if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            else -> {
                error?.let { item { UrbanErrorBanner(it) } }
                item { ReportHeader(r, range.label) }
                r.chart?.takeIf { it.values.isNotEmpty() && it.values.any { v -> v > 0.0 } }?.let { chart ->
                    item { ReportChartCard(chart) }
                }
                if (r.rows.isEmpty()) {
                    if (!busy) item {
                        UrbanMascotState(UrbanStateKind.EMPTY, "Sin datos en este periodo", "Prueba con un rango más amplio o con otro tipo de reporte.")
                    }
                } else {
                    item { UrbanSectionTitle("Detalle", UrbanFormat.count(r.rows.size, "registro", "registros")) }
                    items(r.rows.take(shown)) { row -> ReportRowCard(r, row) }
                    if (r.rows.size > shown) {
                        item {
                            UrbanOutlineButton(
                                text = "Mostrar más (${r.rows.size - shown} restantes)",
                                onClick = { shown += PAGE },
                                icon = Icons.Default.ExpandMore,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
        item {
            Text(
                "Para descargar en PDF o Excel usa la versión web.",
                style = MaterialTheme.typography.bodySmall,
                color = UrbanColors.Muted
            )
        }
    }
}

@Composable
private fun ReportHeader(report: ReportData, rangeLabel: String) {
    UrbanHeroCard {
        UrbanHeroLabel(report.title ?: "Reporte")
        Spacer(Modifier.height(6.dp))
        val chart = report.chart
        // Las gráficas "top N" (inventario, clientes) no cubren todo: sumarlas como total sería engañoso.
        val isTopN = chart?.title.orEmpty().contains("top", ignoreCase = true)
        if (chart != null && chart.values.isNotEmpty() && !isTopN) {
            Text(formatChartValue(chart.values.sum(), chart.unit), style = MaterialTheme.typography.displaySmall, color = UrbanColors.Ink)
            Text(
                "en total · " + UrbanFormat.count(report.rows.size, "registro", "registros"),
                style = MaterialTheme.typography.bodySmall,
                color = UrbanColors.Muted
            )
        } else {
            Text(
                UrbanFormat.count(report.rows.size, "registro", "registros"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = UrbanColors.Ink
            )
        }
        Spacer(Modifier.height(10.dp))
        Text("Periodo: $rangeLabel", style = MaterialTheme.typography.labelMedium, color = UrbanColors.GoldText)
    }
}

/** Serie de fechas: barras (o línea si son muchas); categorías: barras horizontales ordenadas de mayor a menor. */
@Composable
private fun ReportChartCard(chart: ReportChart) {
    val format = { value: Double -> formatChartValue(value, chart.unit) }
    UrbanCard(Modifier.fillMaxWidth()) {
        UrbanSectionTitle(chart.title ?: "Resumen")
        Spacer(Modifier.height(12.dp))
        if (labelsAreDates(chart.labels)) {
            val labels = chart.labels.map(::shortDateLabel)
            if (chart.values.size > 31) {
                UrbanLineChart(labels, chart.values)
            } else {
                UrbanBarChart(labels, chart.values, format = format)
            }
        } else {
            val sorted = chart.labels.zip(chart.values).sortedByDescending { it.second }.take(10)
            UrbanHBars(sorted.map { it.first }, sorted.map { it.second }, format = format)
        }
    }
}

@Composable
private fun ReportRowCard(report: ReportData, row: Map<String, JsonElement>) {
    val pairs = report.keys.zip(report.headings)
    val titleKey = pairs.firstOrNull()?.first ?: return
    UrbanCard(Modifier.fillMaxWidth()) {
        Text(
            formatCell(titleKey, row[titleKey]),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = UrbanColors.Ink
        )
        Spacer(Modifier.height(6.dp))
        pairs.drop(1).forEach { (key, heading) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(heading, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, modifier = Modifier.weight(1f))
                Text(formatCell(key, row[key]), style = MaterialTheme.typography.bodySmall, color = UrbanColors.Ink)
            }
        }
    }
}
