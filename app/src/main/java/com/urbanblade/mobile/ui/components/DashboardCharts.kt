package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonObject

/*
 * Gráfica principal de cada inicio (kit de la app, skill urbanblade-mobile-ui-patrones §8), con los
 * mismos datos de GET /dashboard que la web: incomeChart, servicesChart, flowChart,
 * performanceChart, visitChart. Nada se inventa: si la serie no viene o viene en ceros, no se dibuja.
 */

/** Serie `{labels: [...], values: [...]}` del dashboard. */
data class ChartSeries(val labels: List<String>, val values: List<Double>) {
    val hasData: Boolean get() = values.any { it > 0.0 }
}

/** Lee la serie [key] del objeto `data` del dashboard; null si no viene. */
fun JsonObject?.chartSeries(key: String): ChartSeries? {
    val obj = this?.getAsJsonObject(key) ?: return null
    val labels = obj.getAsJsonArray("labels")?.map { it.asString }.orEmpty()
    val values = obj.getAsJsonArray("values")?.map { runCatching { it.asDouble }.getOrDefault(0.0) }.orEmpty()
    if (labels.isEmpty() || values.isEmpty()) return null

    return ChartSeries(labels.take(values.size), values.take(labels.size))
}

private val DAYS = mapOf("Mon" to "Lun", "Tue" to "Mar", "Wed" to "Mié", "Thu" to "Jue", "Fri" to "Vie", "Sat" to "Sáb", "Sun" to "Dom")
private val MONTHS = mapOf(
    "Jan" to "ene", "Feb" to "feb", "Mar" to "mar", "Apr" to "abr", "May" to "may", "Jun" to "jun",
    "Jul" to "jul", "Aug" to "ago", "Sep" to "sep", "Oct" to "oct", "Nov" to "nov", "Dec" to "dic"
)

/**
 * barber formatea con Carbon en inglés: días "Mon" y fechas "03 Aug". Se muestran en español
 * ("Lun", "3 ago"); cualquier otra etiqueta pasa igual.
 */
fun spanishChartLabel(label: String): String {
    DAYS[label]?.let { return it }
    val parts = label.split(" ")
    if (parts.size == 2) {
        val month = MONTHS[parts[1]]
        val day = parts[0].toIntOrNull()
        if (month != null && day != null) return "$day $month"
    }

    return label
}

/**
 * Tarjeta con título y la gráfica: barras verticales para series en el tiempo; barras
 * horizontales ordenadas para categorías ([categories] = true).
 */
@Composable
fun UrbanDashboardChart(
    title: String,
    subtitle: String?,
    series: ChartSeries,
    modifier: Modifier = Modifier,
    categories: Boolean = false,
    format: (Double) -> String = { String.format(java.util.Locale("es", "MX"), "%,.0f", it) }
) {
    if (!series.hasData) return
    UrbanAnalyticsCard(title = title, subtitle = subtitle, eyebrow = if (categories) "DISTRIBUCIÓN" else "EVOLUCIÓN", modifier = modifier) {
        if (categories) {
            val sorted = series.labels.zip(series.values).sortedByDescending { it.second }.take(6)
            UrbanHBars(sorted.map { it.first }, sorted.map { it.second }, format = format)
        } else {
            UrbanBarChart(series.labels.map(::spanishChartLabel), series.values, format = format)
        }
    }
}
