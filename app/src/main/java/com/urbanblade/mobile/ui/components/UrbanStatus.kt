package com.urbanblade.mobile.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.urbanblade.mobile.ui.theme.UrbanColors

/** Tono de un estado; el color real sale del tema en [StatusTone.color]. */
enum class StatusTone { WARNING, SUCCESS, INFO, DONE, DANGER, NEUTRAL }

fun StatusTone.color(): Color = when (this) {
    StatusTone.WARNING -> UrbanColors.Warning
    StatusTone.SUCCESS -> UrbanColors.Success
    StatusTone.INFO -> UrbanColors.Info
    StatusTone.DONE -> UrbanColors.GoldText
    StatusTone.DANGER -> UrbanColors.Danger
    StatusTone.NEUTRAL -> UrbanColors.Muted
}

data class StatusStyle(val label: String, val tone: StatusTone, val icon: ImageVector?)

/**
 * Etiqueta, tono e ícono de cada estado, iguales en toda la app (checklist CN-100). Cada estado
 * de una cita tiene su propio par color + ícono, así no se distinguen solo por color:
 * confirmada (verde, check) ya no se confunde con completada (dorado, tarea hecha).
 */
fun statusStyle(status: String): StatusStyle = when (status.lowercase().trim()) {
    "pendiente" -> StatusStyle("Pendiente", StatusTone.WARNING, Icons.Default.Schedule)
    "confirmada" -> StatusStyle("Confirmada", StatusTone.SUCCESS, Icons.Default.CheckCircle)
    "en_proceso" -> StatusStyle("En proceso", StatusTone.INFO, Icons.Default.ContentCut)
    "completada" -> StatusStyle("Completada", StatusTone.DONE, Icons.Default.TaskAlt)
    "cancelada" -> StatusStyle("Cancelada", StatusTone.DANGER, Icons.Default.Cancel)
    "no_asistio" -> StatusStyle("No asistió", StatusTone.WARNING, Icons.Default.PersonOff)
    "notificado" -> StatusStyle("Notificado", StatusTone.INFO, Icons.Default.NotificationsActive)
    "entregado" -> StatusStyle("Entregado", StatusTone.SUCCESS, Icons.Default.LocalShipping)
    "activo", "activa" -> StatusStyle("Activa".takeIf { status.endsWith("a") } ?: "Activo", StatusTone.SUCCESS, Icons.Default.CheckCircle)
    "expirado", "expirada", "vencido", "vencida" -> StatusStyle(status.replaceFirstChar { it.uppercase() }, StatusTone.NEUTRAL, Icons.Default.EventBusy)
    "rechazado", "rechazada" -> StatusStyle(status.replaceFirstChar { it.uppercase() }, StatusTone.DANGER, Icons.Default.Block)
    // Pagos: el dinero se devolvió al cliente, o su comprobante espera revisión de recepción.
    "reembolsado" -> StatusStyle("Reembolsado", StatusTone.INFO, Icons.AutoMirrored.Filled.Undo)
    "pendiente_verificacion" -> StatusStyle("Por verificar", StatusTone.WARNING, Icons.Default.HourglassTop)
    else -> {
        // Estados que no son de cita (pedidos, pagos…): mismo criterio de color que antes, sin ícono.
        val n = status.lowercase()
        val tone = when {
            n.contains("complet") || n.contains("confirm") || n.contains("entreg") || n.contains("verific") || n.contains("aprob") || n.contains("pagad") -> StatusTone.SUCCESS
            n.contains("cancel") || n.contains("rechaz") || n.contains("error") || n.contains("fall") -> StatusTone.DANGER
            n.contains("pend") || n.contains("proceso") || n.contains("revis") -> StatusTone.WARNING
            else -> StatusTone.INFO
        }
        StatusStyle(status.replace('_', ' ').replaceFirstChar { it.uppercase() }, tone, null)
    }
}
