package com.urbanblade.mobile.ui.screens

import com.google.gson.JsonElement
import java.time.LocalDate
import java.util.Locale
import com.urbanblade.mobile.ui.components.UrbanFormat

/** Rangos rápidos del reporte; "Todo" no manda fechas y deja que el servidor use su rango por defecto. */
enum class ReportRange(val label: String) {
    Last7("7 días"),
    Last30("30 días"),
    ThisMonth("Este mes"),
    LastMonth("Mes pasado"),
    All("Todo")
}

/** Fechas AAAA-MM-DD de inicio y fin para el rango elegido (ambas nulas en "Todo"). */
fun ReportRange.dates(today: LocalDate = LocalDate.now()): Pair<String?, String?> = when (this) {
    ReportRange.Last7 -> today.minusDays(6).toString() to today.toString()
    ReportRange.Last30 -> today.minusDays(29).toString() to today.toString()
    ReportRange.ThisMonth -> today.withDayOfMonth(1).toString() to today.toString()
    ReportRange.LastMonth -> {
        val first = today.withDayOfMonth(1).minusMonths(1)
        first.toString() to first.withDayOfMonth(first.lengthOfMonth()).toString()
    }
    ReportRange.All -> null to null
}

/** Las etiquetas de la gráfica son fechas (serie de tiempo) y no categorías como estados o métodos. */
fun labelsAreDates(labels: List<String>): Boolean =
    labels.isNotEmpty() && labels.all { it.length >= 10 && runCatching { LocalDate.parse(it.take(10)) }.isSuccess }

/** "2026-09-18" -> "18/09", para que el eje no se llene con años. */
fun shortDateLabel(label: String): String = runCatching {
    val date = LocalDate.parse(label.take(10))
    "%02d/%02d".format(date.dayOfMonth, date.monthValue)
}.getOrDefault(label)

/** Formatea un valor de la gráfica según la unidad que declara el servidor (MXN, citas…). */
fun formatChartValue(value: Double, unit: String?): String {
    val locale = Locale("es", "MX")
    return if (unit.equals("MXN", ignoreCase = true)) {
        UrbanFormat.moneyShort(value)
    } else {
        String.format(locale, if (value == value.toLong().toDouble()) "%,.0f" else "%,.1f", value) +
            (unit?.takeIf { it.isNotBlank() }?.let { " $it" } ?: "")
    }
}

private val moneyKeys = listOf("monto", "propina", "total", "precio", "gasto", "costo")

/** Texto de una celda del detalle: dinero con formato, booleanos en español y vacíos como guion. */
fun formatCell(key: String, value: JsonElement?): String {
    if (value == null || value.isJsonNull) return "—"
    if (!value.isJsonPrimitive) return value.toString()
    val primitive = value.asJsonPrimitive
    return when {
        primitive.isBoolean -> if (primitive.asBoolean) "Sí" else "No"
        primitive.isNumber && moneyKeys.any { key.contains(it) } ->
            UrbanFormat.money(primitive.asDouble)
        primitive.isNumber -> primitive.asString
        else -> primitive.asString.ifBlank { "—" }
    }
}
