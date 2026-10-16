package com.urbanblade.mobile.ui.components

import com.urbanblade.mobile.data.model.AnalyticsGraph

/** Filtra pares completos: un dato inválido nunca desplaza la etiqueta del siguiente. */
fun analyticsChartSeries(graph: AnalyticsGraph): ChartSeries {
    val pairs = graph.valores.mapIndexedNotNull { index, element ->
        val value = runCatching {
            if (!element.isJsonPrimitive) null
            else element.asString.replace(",", "").replace("%", "").replace("$", "").trim().toDoubleOrNull()
        }.getOrNull()?.takeIf { it.isFinite() } ?: return@mapIndexedNotNull null
        (graph.labels.getOrNull(index)?.takeIf { it.isNotBlank() } ?: "Dato ${index + 1}") to value
    }
    return ChartSeries(pairs.map { it.first }, pairs.map { it.second })
}
