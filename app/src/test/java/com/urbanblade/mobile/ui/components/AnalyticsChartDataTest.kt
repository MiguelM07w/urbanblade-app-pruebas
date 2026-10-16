package com.urbanblade.mobile.ui.components

import com.google.gson.JsonParser
import com.urbanblade.mobile.data.model.AnalyticsGraph
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsChartDataTest {
    @Test fun `dato invalido conserva etiquetas de los siguientes valores`() {
        val values = JsonParser.parseString("""[12,null,30]""").asJsonArray.toList()
        val series = analyticsChartSeries(AnalyticsGraph(labels = listOf("Corte", "Barba", "Combo"), valores = values))
        assertEquals(listOf("Corte", "Combo"), series.labels)
        assertEquals(listOf(12.0, 30.0), series.values)
    }
    @Test fun `lee cifras formateadas y rechaza valores no finitos`() {
        val values = JsonParser.parseString("""["${'$'}1,250.50","25%","NaN","Infinity"]""").asJsonArray.toList()
        val series = analyticsChartSeries(AnalyticsGraph(valores = values))
        assertEquals(listOf(1250.5, 25.0), series.values)
        assertEquals(listOf("Dato 1", "Dato 2"), series.labels)
    }
}
