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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.CampaignRow
import com.urbanblade.mobile.data.model.CreateCampaignRequest
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanEmptyState
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanFieldLabel
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
import com.urbanblade.mobile.ui.components.UrbanMetricCard
import com.urbanblade.mobile.ui.components.UrbanOutlineButton
import com.urbanblade.mobile.ui.components.UrbanPrimaryButton
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.components.UrbanSkeletonList
import com.urbanblade.mobile.ui.components.UrbanStatusPill
import com.urbanblade.mobile.ui.components.UrbanTextField
import com.urbanblade.mobile.ui.components.UrbanTopBar
import com.urbanblade.mobile.ui.components.UrbanMascotState
import com.urbanblade.mobile.ui.components.UrbanPageHeader
import com.urbanblade.mobile.ui.components.UrbanStatStrip
import com.urbanblade.mobile.ui.components.UrbanStateKind
import com.urbanblade.mobile.ui.components.urbanFilterChipColors
import com.urbanblade.mobile.ui.components.statusStyle
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.CampaignsViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

/** Campañas de marketing por segmento (solo administrador): historial y envío inmediato o programado. */
@Composable
fun CampaignsScreen(onBack: () -> Unit, vm: CampaignsViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var creating by remember { mutableStateOf(false) }
    val data = state.data
    val labels = remember(data) { (data.segmentCounts.keys + "todos" + "inactive").associateWith { segmentLabel(it, data.levels) } }

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { UrbanTopBar("", onBack) { IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, "Actualizar") } } },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.clearMessages(); creating = true },
                containerColor = UrbanColors.Gold,
                contentColor = UrbanColors.OnGold,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Nueva campaña", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { UrbanPageHeader(title = "Campañas", subtitle = "Promociones por correo y notificación para cada segmento de clientes.", eyebrow = "Marketing") }
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            state.notice?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }
            if (!creating) state.error?.let { item { UrbanErrorBanner(it) } }

            if (data.segmentCounts.isNotEmpty()) {
                item { UrbanSectionTitle("Clientes por segmento", "A quién puedes llegar") }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        data.segmentCounts.entries.chunked(2).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { (key, count) ->
                                    UrbanMetricCard(labels[key] ?: key, count.toString(), Icons.Default.Groups, Modifier.weight(1f))
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item { UrbanSectionTitle("Últimas campañas", "Las 10 más recientes") }
            if (state.loading && data.data.isEmpty()) item { UrbanSkeletonList(3) }
            if (!state.loading && data.data.isEmpty() && state.error == null) {
                item {
                    // Sin botón propio: el flotante "Nueva campaña" ya está en pantalla y lo tapaba.
                    UrbanMascotState(
                        UrbanStateKind.EMPTY, "Sin campañas todavía", "Llega a tus clientes con una promoción por segmento: usa «Nueva campaña»."
                    )
                }
            }
            items(data.data, key = { it.id }) { campaign -> CampaignCard(campaign, labels[campaign.segmento] ?: segmentLabel(campaign.segmento, data.levels)) }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (creating) {
        CampaignFormSheet(
            segmentCounts = data.segmentCounts,
            labels = labels,
            sending = state.sending,
            error = state.error,
            onDismiss = { creating = false },
            onSend = { body -> vm.create(body) { creating = false } }
        )
    }
}

@Composable
private fun CampaignCard(c: CampaignRow, segment: String) {
    UrbanCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(c.titulo ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
                Text(segment, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            }
            // statusStyle (dentro de UrbanStatusPill) ya pone la etiqueta y el tono de cada estado.
                                c.estado?.let { UrbanStatusPill(it) }
        }
        c.cuerpo?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Ink, maxLines = 3)
        }
        Spacer(Modifier.height(8.dp))
        val whenText = when (c.estado) {
            "programada" -> c.programadaPara?.let { "Programada para ${formatWhen(it)}" }
            else -> (c.enviadaEn ?: c.createdAt)?.let { "Enviada ${formatWhen(it)}" }
        }
        whenText?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted) }
        if (c.estado == "enviada") {
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(UrbanFormat.count(c.destinatarios, "destinatario", "destinatarios"), style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                Text("${"%.0f".format(c.openRate)}% aperturas", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                Text("${"%.0f".format(c.clickRate)}% clics", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampaignFormSheet(
    segmentCounts: Map<String, Int>,
    labels: Map<String, String>,
    sending: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSend: (CreateCampaignRequest) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var cuerpo by remember { mutableStateOf("") }
    var ctaLabel by remember { mutableStateOf("") }
    var ctaUrl by remember { mutableStateOf("") }
    var segmento by remember { mutableStateOf(if ("todos" in segmentCounts) "todos" else segmentCounts.keys.firstOrNull() ?: "todos") }
    var schedule by remember { mutableStateOf(false) }
    var scheduledAt by remember { mutableStateOf<LocalDateTime?>(null) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf<LocalDate?>(null) }
    var submitted by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf(false) }

    val recipients = segmentCounts[segmento] ?: 0
    val draft = CampaignDraft(titulo, cuerpo, ctaLabel, ctaUrl, schedule, scheduledAt)
    val errors = validateCampaign(draft, recipients)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = UrbanColors.Surface) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Nueva campaña", style = MaterialTheme.typography.titleLarge, color = UrbanColors.Ink)
            UrbanTextField(
                value = titulo, onValueChange = { titulo = it }, label = "Título",
                error = if (submitted) errors.titulo else null, helper = "${titulo.length}/$CAMPAIGN_TITLE_MAX",
                capitalization = KeyboardCapitalization.Sentences, modifier = Modifier.fillMaxWidth()
            )
            UrbanTextField(
                value = cuerpo, onValueChange = { cuerpo = it }, label = "Mensaje",
                error = if (submitted) errors.cuerpo else null, helper = "${cuerpo.length}/$CAMPAIGN_BODY_MAX",
                capitalization = KeyboardCapitalization.Sentences, modifier = Modifier.fillMaxWidth()
            )
            UrbanTextField(
                value = ctaLabel, onValueChange = { ctaLabel = it }, label = "Texto del botón (opcional)",
                error = if (submitted) errors.ctaLabel else null, modifier = Modifier.fillMaxWidth()
            )
            UrbanTextField(
                value = ctaUrl, onValueChange = { ctaUrl = it }, label = "Enlace del botón (opcional)",
                error = if (submitted) errors.ctaUrl else null, keyboardType = KeyboardType.Uri, modifier = Modifier.fillMaxWidth()
            )

            Column {
                UrbanFieldLabel("Segmento")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    segmentCounts.keys.forEach { key ->
                        FilterChip(
                            selected = segmento == key,
                            onClick = { segmento = key },
                            label = { Text("${labels[key] ?: key} (${segmentCounts[key] ?: 0})") }
                        )
                    }
                }
                errors.segment?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Danger) }
            }

            Column {
                UrbanFieldLabel("Envío")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !schedule, onClick = { schedule = false }, label = { Text("Ahora") })
                    FilterChip(selected = schedule, onClick = { schedule = true }, label = { Text("Programar") })
                }
            }
            if (schedule) {
                Column {
                    UrbanOutlineButton(
                        text = scheduledAt?.let { formatWhen(formatScheduleForServer(it).replace(' ', 'T')) } ?: "Elegir fecha y hora",
                        onClick = { pickingDate = true },
                        icon = Icons.Default.Today,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (submitted) errors.scheduledAt?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Danger) }
                }
            }

            error?.let { UrbanErrorBanner(it) }
            UrbanPrimaryButton(
                text = if (schedule) "Programar campaña" else "Enviar ahora",
                onClick = {
                    submitted = true
                    if (!errors.hasAny) confirming = true
                },
                loading = sending,
                icon = Icons.Default.Send,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (pickingDate) {
        val todayUtc = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val dateState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
            }
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(
                    enabled = dateState.selectedDateMillis != null,
                    onClick = {
                        dateState.selectedDateMillis?.let { millis ->
                            pickingTime = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        pickingDate = false
                    }
                ) { Text("Siguiente") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancelar") } }
        ) { DatePicker(state = dateState) }
    }

    pickingTime?.let { date ->
        val timeState = rememberTimePickerState(initialHour = 10, initialMinute = 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickingTime = null },
            containerColor = UrbanColors.Card,
            title = { Text("Hora de envío") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    scheduledAt = LocalDateTime.of(date, LocalTime.of(timeState.hour, timeState.minute))
                    pickingTime = null
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { pickingTime = null }) { Text("Cancelar") } }
        )
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            containerColor = UrbanColors.Card,
            title = { Text(if (schedule) "Confirmar programación" else "Confirmar envío") },
            text = {
                Text(
                    sendConfirmation(recipients, schedule, scheduledAt?.let { formatWhen(formatScheduleForServer(it).replace(' ', 'T')) }),
                    color = UrbanColors.Muted
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    onSend(
                        CreateCampaignRequest(
                            titulo = titulo.trim(),
                            cuerpo = cuerpo.trim(),
                            ctaLabel = ctaLabel.trim().ifEmpty { null },
                            ctaUrl = ctaUrl.trim().ifEmpty { null },
                            segmento = segmento,
                            modo = if (schedule) "programar" else "ahora",
                            programadaPara = if (schedule) scheduledAt?.let(::formatScheduleForServer) else null
                        )
                    )
                }) { Text(if (schedule) "Sí, programar" else "Sí, enviar") }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Volver") } }
        )
    }
}
