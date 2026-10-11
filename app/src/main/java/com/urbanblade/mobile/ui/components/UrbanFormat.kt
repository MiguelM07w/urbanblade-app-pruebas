package com.urbanblade.mobile.ui.components

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * Formatos legibles para lo que la API manda en ISO ("2026-09-21", "10:00"). Si algo no se puede
 * interpretar, se devuelve el texto original en lugar de fallar.
 */
object UrbanFormat {
    private val es = Locale("es", "MX")

    /** "2026-09-21" -> "Lunes 21 de septiembre". */
    fun date(iso: String): String = runCatching {
        val d = LocalDate.parse(iso.take(10))
        val day = d.dayOfWeek.getDisplayName(TextStyle.FULL, es).replaceFirstChar { it.uppercase() }
        "$day ${d.dayOfMonth} de ${d.month.getDisplayName(TextStyle.FULL, es)}"
    }.getOrDefault(iso)

    /** "2026-09-21" -> "21 sep". */
    fun dateShort(iso: String): String = runCatching {
        val d = LocalDate.parse(iso.take(10))
        "${d.dayOfMonth} ${MONTHS_SHORT[d.monthValue - 1]}"
    }.getOrDefault(iso)

    /** "1995-05-15" -> "15 de mayo de 1995" (fechas sin día de la semana, como un cumpleaños). */
    fun dateLong(iso: String): String = runCatching {
        val d = LocalDate.parse(iso.take(10))
        "${d.dayOfMonth} de ${d.month.getDisplayName(TextStyle.FULL, es)} de ${d.year}"
    }.getOrDefault(iso)

    /** "2026-09-21T10:00:00Z" -> "sep 2026" (antigüedad de una cuenta). */
    fun monthYear(iso: String): String = runCatching {
        val d = LocalDate.parse(iso.take(10))
        "${MONTHS_SHORT[d.monthValue - 1]} ${d.year}"
    }.getOrDefault(iso)

    // Tabla fija: el nombre abreviado del sistema varía entre versiones de Android ("sep" o "sept").
    private val MONTHS_SHORT = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

    /** "10:00" o "10:00:00" -> "10:00 AM" (mismo estilo que los horarios del asistente de reserva). */
    fun time(hhmm: String): String = runCatching {
        val t = LocalTime.parse(hhmm.take(5))
        val hour12 = if (t.hour % 12 == 0) 12 else t.hour % 12
        String.format(Locale.US, "%d:%02d %s", hour12, t.minute, if (t.hour < 12) "AM" else "PM")
    }.getOrDefault(hhmm)

    fun count(n: Int, one: String, many: String): String = if (n == 1) "1 $one" else "$n $many"

    /**
     * Dinero en pesos con centavos: mismo texto que `money()` de frontend-urban
     * (`app/utils/serviceFlow.ts`). Tickets, adeudos y detalles de pago.
     *
     * Antes cada pantalla traía su propia copia: once en total, unas con centavos y otras sin
     * ellos, con `Locale` distinto (`%,.2f`, `%,.0f`, `%.0f`) y sin proteger contra NaN.
     */
    fun money(value: Double?): String = "$" + String.format(es, "%,.2f", safe(value))

    /** Dinero sin centavos, para cifras grandes (KPI, ejes de gráfica): `fmtMoney()` de `chartTheme.ts`. */
    fun moneyShort(value: Double?): String = "$" + String.format(es, "%,.0f", safe(value))

    /** "12.6%": el porcentaje ya calculado, con un decimal por omisión. */
    fun percent(value: Double?, decimals: Int = 1): String = String.format(es, "%.${decimals}f%%", safe(value))

    /**
     * Cambio relativo contra el periodo anterior, en puntos porcentuales (12.6 = +12.6 %).
     * Devuelve `null` cuando no se puede calcular (faltan datos o el periodo anterior es 0):
     * ahí la pantalla no muestra variación en vez de inventar un "+100 %".
     */
    fun delta(current: Double?, previous: Double?): Double? {
        if (current == null || previous == null) return null
        if (!current.isFinite() || !previous.isFinite() || previous == 0.0) return null
        return (current - previous) / kotlin.math.abs(previous) * 100.0
    }

    private fun safe(value: Double?): Double = if (value != null && value.isFinite()) value else 0.0
}
