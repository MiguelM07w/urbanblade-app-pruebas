package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.InsightsViewModel

private fun statusColor(status: String?) = when (status) {
    "positive" -> UrbanColors.Success
    "warning" -> UrbanColors.Warning
    "negative" -> UrbanColors.Danger
    else -> UrbanColors.Info
}

private fun statusLabel(status: String?) = when (status) {
    "positive" -> "Va bien"
    "warning" -> "Atención"
    "negative" -> "En riesgo"
    else -> "Informativo"
}

@Composable
fun InsightsScreen(onBack: () -> Unit, vm: InsightsViewModel = viewModel()) {
    val data by vm.data.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    val isEmpty = data.revenue == null && data.appointments == null && data.serviceConcentration == null

    val statuses = listOfNotNull(data.revenue?.status, data.appointments?.status, data.serviceConcentration?.status)
    val attention = statuses.count { it == "warning" || it == "negative" }

    UrbanModuleScreen(
        eyebrow = "ANÁLISIS",
        title = "Insights IA",
        subtitle = "Tendencias calculadas sobre el historial reciente.",
        onBack = onBack,
        onRefresh = { vm.load() },
        refreshing = busy
    ) {
        when {
            error != null && isEmpty -> item {
                UrbanMascotState(UrbanStateKind.ERROR, "No pudimos calcular los insights", error, "Reintentar") { vm.load() }
            }
            busy && isEmpty -> item {
                UrbanCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = UrbanColors.Gold)
                        Spacer(Modifier.width(12.dp))
                        Text("Calculando tendencias sobre el historial reciente…", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                    }
                }
            }
            isEmpty -> item {
                UrbanMascotState(UrbanStateKind.EMPTY, "Sin insights todavía", "Se necesitan más datos históricos para generar recomendaciones.")
            }
            else -> {
                error?.let { item { UrbanErrorBanner(it) } }
                item {
                    UrbanHeroCard {
                        UrbanHeroLabel("Resumen")
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (attention == 0) "Todo va bien" else UrbanFormat.count(attention, "señal requiere atención", "señales requieren atención"),
                            style = MaterialTheme.typography.headlineSmall,
                            color = UrbanColors.Ink
                        )
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = UrbanColors.Ink.copy(alpha = 0.22f))
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            UrbanHeroStat("Analizadas", statuses.size.toString(), Icons.Default.Insights, Modifier.weight(1f))
                            UrbanHeroStat(
                                "Por revisar",
                                attention.toString(),
                                Icons.Default.Warning,
                                Modifier.weight(1f),
                                tone = if (attention > 0) UrbanColors.Warning else UrbanColors.Success
                            )
                        }
                    }
                }

                data.revenue?.let { revenue ->
                    item {
                        InsightCard("Ingresos", Icons.Default.TrendingUp, revenue.status, revenue.message) {
                            revenue.avgDaily?.let { BigNumber("Promedio diario", UrbanFormat.moneyShort(it)) }
                        }
                    }
                }

                data.appointments?.let { appts ->
                    item {
                        InsightCard("Citas", Icons.Default.CalendarMonth, appts.status, appts.message) {
                            val avg = appts.avgDaily
                            val recent = appts.trend7d
                            if (avg != null && recent != null) {
                                // Comparar el ritmo reciente contra el promedio se lee mejor con barras que con dos números.
                                UrbanHBars(
                                    labels = listOf("Promedio diario", "Últimos 7 días"),
                                    values = listOf(avg, recent),
                                    color = statusColor(appts.status),
                                    format = { "%.1f".format(it) }
                                )
                            } else {
                                avg?.let { BigNumber("Promedio diario", "%.1f".format(it)) }
                            }
                        }
                    }
                }

                data.serviceConcentration?.let { service ->
                    item {
                        InsightCard("Concentración de servicios", Icons.Default.PieChart, service.status, service.message) {
                            val percent = service.percentage
                            if (percent != null) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    UrbanRingGauge(
                                        fraction = (percent / 100.0).toFloat(),
                                        centerText = "%.0f%%".format(percent),
                                        color = statusColor(service.status)
                                    )
                                    Column(Modifier.weight(1f)) {
                                        service.topService?.let { BigNumber("Servicio principal", it) }
                                        Text(
                                            "de los ingresos vienen de un solo servicio",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = UrbanColors.Muted
                                        )
                                    }
                                }
                            } else {
                                service.topService?.let { BigNumber("Servicio principal", it) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Tarjeta de un insight: icono y etiqueta de estado con su color, el mensaje del servidor y una visualización. */
@Composable
private fun InsightCard(
    title: String,
    icon: ImageVector,
    status: String?,
    message: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    val tone = statusColor(status)
    UrbanCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(tone.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = tone, modifier = Modifier.size(22.dp)) }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = UrbanColors.Ink, modifier = Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(50), color = tone.copy(alpha = 0.14f)) {
                Text(
                    statusLabel(status),
                    style = MaterialTheme.typography.labelMedium,
                    color = tone,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        message?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = MaterialTheme.typography.bodyLarge, color = UrbanColors.Ink)
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun BigNumber(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
    }
}
