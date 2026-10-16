package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.theme.UrbanColors

/** Marco compartido de analítica: título, cifra completa, contexto y visualización. */
@Composable
fun UrbanAnalyticsCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    metric: String? = null,
    accent: Color = UrbanChartColors.at(0),
    eyebrow: String? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    UrbanPremiumCard(modifier.fillMaxWidth()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.padding(top = 5.dp).size(8.dp).clip(CircleShape).background(accent))
            Text(eyebrow ?: "ANÁLISIS", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
        }
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = UrbanColors.Ink)
        metric?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
        }
        subtitle?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
        }
        Spacer(Modifier.height(18.dp))
        content()
    }
}

/** Paleta categórica independiente de los colores de alerta; adapta contraste en Libreta. */
object UrbanChartColors {
    fun palette(): List<Color> = if (UrbanColors.current.isLight) {
        listOf(Color(0xFF087F8C), Color(0xFF355DC2), Color(0xFF8051B8), Color(0xFFC15D41), Color(0xFF56802E), Color(0xFFAE4780))
    } else {
        listOf(Color(0xFF52C9BC), Color(0xFF7DAAFF), Color(0xFFB395F2), Color(0xFFF59A7B), Color(0xFFA7C96A), Color(0xFFEF99C2))
    }
    fun at(index: Int): Color = palette()[Math.floorMod(index, palette().size)]
}
