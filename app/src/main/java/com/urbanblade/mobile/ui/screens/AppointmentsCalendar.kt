package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.data.model.AppointmentRow
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.color
import com.urbanblade.mobile.ui.components.statusStyle
import com.urbanblade.mobile.ui.theme.UrbanColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val es = Locale("es", "MX")
private val weekdayLabels = listOf("L", "M", "M", "J", "V", "S", "D")

/** Color del punto de un día: el mismo tono que la píldora de estado, no otra tabla de colores. */
private fun estadoColor(estado: String): Color = statusStyle(estado).tone.color()

/**
 * Calendario mensual de citas: cada día marca con puntos de color (por estado) las citas que tiene,
 * y al tocarlo la pantalla muestra solo las de ese día. Lunes es el primer día de la semana.
 */
@Composable
internal fun AppointmentsCalendar(
    month: YearMonth,
    rows: List<AppointmentRow>,
    selected: LocalDate?,
    loading: Boolean,
    onMonthChange: (YearMonth) -> Unit,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val byDay = rows.groupBy { it.fecha.take(10) }
    val today = LocalDate.now()
    val leadingBlanks = month.atDay(1).dayOfWeek.value - 1
    val weeks = (leadingBlanks + month.lengthOfMonth() + 6) / 7
    val title = month.month.getDisplayName(TextStyle.FULL, es).replaceFirstChar { it.uppercase() } + " " + month.year

    UrbanCard(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior", tint = UrbanColors.Ink)
            }
            Text(
                title,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = UrbanColors.Ink
            )
            IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente", tint = UrbanColors.Ink)
            }
        }
        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold, trackColor = UrbanColors.Gold.copy(alpha = 0.15f))
        } else {
            Spacer(Modifier.height(4.dp))
        }
        Row(Modifier.fillMaxWidth()) {
            weekdayLabels.forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = UrbanColors.Muted
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(weeks) { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(7) { col ->
                        val dayNumber = week * 7 + col - leadingBlanks + 1
                        if (dayNumber < 1 || dayNumber > month.lengthOfMonth()) {
                            Spacer(Modifier.weight(1f).height(48.dp))
                        } else {
                            val date = month.atDay(dayNumber)
                            DayCell(
                                date = date,
                                appointments = byDay[date.toString()].orEmpty(),
                                isToday = date == today,
                                isSelected = date == selected,
                                onClick = { onSelect(date) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    appointments: List<AppointmentRow>,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    val description = buildString {
        append("${date.dayOfMonth} de ${date.month.getDisplayName(TextStyle.FULL, es)}")
        append(if (appointments.isEmpty()) ", sin citas" else ", ${appointments.size} ${if (appointments.size == 1) "cita" else "citas"}")
    }
    Column(
        modifier
            .height(48.dp)
            .clip(shape)
            .background(if (isSelected) UrbanColors.Gold else Color.Transparent)
            .then(if (isToday && !isSelected) Modifier.border(1.dp, UrbanColors.Gold, shape) else Modifier)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) UrbanColors.OnGold else UrbanColors.Ink
        )
        Spacer(Modifier.height(3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(6.dp)) {
            appointments.map { it.estado }.distinct().take(3).forEach { estado ->
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) UrbanColors.OnGold else estadoColor(estado))
                )
            }
        }
    }
}
