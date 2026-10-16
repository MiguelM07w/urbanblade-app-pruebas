package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.ui.components.UrbanSwitch
import com.urbanblade.mobile.ui.components.UrbanSwitchRow
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.BarberAdminItem
import com.urbanblade.mobile.data.model.BarberPerformance
import com.urbanblade.mobile.data.model.BarberUpsertRequest
import com.urbanblade.mobile.ui.components.UrbanAvatar
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanEmptyState
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
import com.urbanblade.mobile.ui.components.UrbanKeyValue
import com.urbanblade.mobile.ui.components.UrbanOutlineButton
import com.urbanblade.mobile.ui.components.UrbanPrimaryButton
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.components.UrbanSkeletonList
import com.urbanblade.mobile.ui.components.UrbanTextField
import com.urbanblade.mobile.ui.components.UrbanTopBar
import com.urbanblade.mobile.ui.components.UrbanMascotState
import com.urbanblade.mobile.ui.components.UrbanPageHeader
import com.urbanblade.mobile.ui.components.UrbanStateKind
import com.urbanblade.mobile.ui.components.urbanFilterChipColors
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.BarbersAdminViewModel
import com.urbanblade.mobile.ui.viewmodel.ServiceStatusFilter

/** Equipo de barberos del administrador: buscar, activar, editar perfil y comisión, ver rendimiento. */
@Composable
fun BarbersAdminScreen(onBack: () -> Unit, vm: BarbersAdminViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var editing by remember { mutableStateOf<BarberAdminItem?>(null) }

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            UrbanTopBar("", onBack) {
                IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, "Actualizar") }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { UrbanPageHeader(title = "Barberos", subtitle = "Perfil, especialidad, horario y rendimiento de tu equipo.", eyebrow = "Equipo") }
            item {
                UrbanTextField(
                    value = state.query,
                    onValueChange = vm::setQuery,
                    label = "Buscar por nombre o correo",
                    leadingIcon = Icons.Default.Search,
                    imeAction = ImeAction.Search,
                    onImeAction = { vm.load() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ServiceStatusFilter.entries.forEach { filter ->
                        FilterChip(selected = state.status == filter, onClick = { vm.setStatus(filter) }, label = { Text(filter.label) }, colors = urbanFilterChipColors())
                    }
                }
            }
            if (state.loading || state.saving) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            state.notice?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }
            if (editing == null) state.error?.let { msg ->
                item {
                    if (state.items.isEmpty()) UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar el equipo", msg, "Reintentar") { vm.load() }
                    else UrbanErrorBanner(msg)
                }
            }

            if (state.loading && state.items.isEmpty()) item { UrbanSkeletonList(4) }

            if (!state.loading && state.items.isEmpty() && state.error == null) {
                item {
                    val firstTime = state.query.isBlank() && state.status == ServiceStatusFilter.Todos
                    UrbanMascotState(
                        UrbanStateKind.EMPTY,
                        if (firstTime) "Aún no hay barberos" else "Sin resultados",
                        if (firstTime) "Los barberos se dan de alta desde Usuarios, asignándoles ese rol." else "Prueba con otra búsqueda o filtro."
                    )
                }
            }

            if (state.items.isNotEmpty()) {
                item { UrbanSectionTitle("Equipo", UrbanFormat.count(state.total, "barbero", "barberos")) }
            }
            items(state.items, key = { it.id }) { barber ->
                BarberAdminCard(
                    barber = barber,
                    onOpen = { vm.clearMessages(); vm.loadPerformance(barber); editing = barber },
                    onToggle = { vm.toggleActive(barber) }
                )
            }
            if (state.hasMore) {
                item {
                    UrbanOutlineButton(
                        text = if (state.loadingMore) "Cargando…" else "Cargar más barberos",
                        onClick = { vm.loadMore() },
                        icon = Icons.Default.ExpandMore,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    editing?.let { barber ->
        BarberFormSheet(
            barber = barber,
            performance = state.performance,
            performanceLoading = state.performanceLoading,
            saving = state.saving,
            error = state.error,
            onDismiss = { editing = null },
            onSave = { body -> vm.save(barber, body) { editing = null } }
        )
    }
}

@Composable
private fun BarberAdminCard(barber: BarberAdminItem, onOpen: () -> Unit, onToggle: () -> Unit) {
    UrbanCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UrbanAvatar(barber.user.name, imageUrl = barber.fotoUrl)
            Column(Modifier.weight(1f)) {
                Text(
                    barber.user.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (barber.activo) UrbanColors.Ink else UrbanColors.Muted
                )
                Text(
                    barber.especialidades?.takeIf { it.isNotBlank() } ?: barber.user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = UrbanColors.Muted,
                    maxLines = 1
                )
                Text(
                    "Comisión ${formatPct(barber.comisionPct)}" + if (barber.activo) "" else " · Inactivo",
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted
                )
            }
            UrbanSwitch(
                checked = barber.activo,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

private fun formatPct(value: Double): String =
    if (value % 1.0 == 0.0) "${value.toInt()} %" else "%.1f %%".format(value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarberFormSheet(
    barber: BarberAdminItem,
    performance: BarberPerformance?,
    performanceLoading: Boolean,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (BarberUpsertRequest) -> Unit
) {
    var nombre by remember { mutableStateOf(barber.user.name) }
    var email by remember { mutableStateOf(barber.user.email) }
    var especialidades by remember { mutableStateOf(barber.especialidades.orEmpty()) }
    var descripcion by remember { mutableStateOf(barber.descripcion.orEmpty()) }
    var comision by remember { mutableStateOf(if (barber.comisionPct == 0.0) "" else formatPctInput(barber.comisionPct)) }
    var activo by remember { mutableStateOf(barber.activo) }
    var submitted by remember { mutableStateOf(false) }

    val comisionValue = if (comision.isBlank()) 0.0 else comision.replace(',', '.').toDoubleOrNull()
    val nombreError = if (submitted && nombre.isBlank()) "Escribe el nombre." else null
    val emailError = if (submitted && !email.contains("@")) "Escribe un correo válido." else null
    val comisionError = if (submitted && (comisionValue == null || comisionValue !in 0.0..100.0)) "Entre 0 y 100 %." else null

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = UrbanColors.Surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Editar barbero", style = MaterialTheme.typography.titleLarge, color = UrbanColors.Ink)
            PerformanceCard(performance, performanceLoading)

            UrbanTextField(
                value = nombre, onValueChange = { nombre = it }, label = "Nombre",
                error = nombreError, capitalization = KeyboardCapitalization.Words, modifier = Modifier.fillMaxWidth()
            )
            UrbanTextField(
                value = email, onValueChange = { email = it }, label = "Correo",
                error = emailError, keyboardType = KeyboardType.Email, modifier = Modifier.fillMaxWidth()
            )
            UrbanTextField(
                value = especialidades, onValueChange = { especialidades = it }, label = "Especialidades (opcional)",
                capitalization = KeyboardCapitalization.Sentences, modifier = Modifier.fillMaxWidth()
            )
            UrbanTextField(
                value = descripcion, onValueChange = { descripcion = it }, label = "Descripción (opcional)",
                capitalization = KeyboardCapitalization.Sentences, modifier = Modifier.fillMaxWidth()
            )
            UrbanTextField(
                value = comision, onValueChange = { comision = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                label = "Comisión (%)", error = comisionError, keyboardType = KeyboardType.Decimal,
                helper = "Porcentaje del precio de lista de cada servicio que completa.",
                imeAction = ImeAction.Done, modifier = Modifier.fillMaxWidth()
            )
            UrbanSwitchRow("Recibe citas", "Si lo apagas, no aparece al reservar.", checked = activo, onCheckedChange = { activo = it })
            error?.let { UrbanErrorBanner(it) }
            UrbanPrimaryButton(
                text = "Guardar cambios",
                onClick = {
                    submitted = true
                    if (nombre.isNotBlank() && email.contains("@") && comisionValue != null && comisionValue in 0.0..100.0) {
                        onSave(
                            BarberUpsertRequest(
                                nombre.trim(), email.trim().lowercase(),
                                especialidades.trim().ifEmpty { null }, descripcion.trim().ifEmpty { null },
                                barber.foto, activo, comisionValue
                            )
                        )
                    }
                },
                loading = saving,
                icon = Icons.Default.Save,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun formatPctInput(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

@Composable
private fun PerformanceCard(performance: BarberPerformance?, loading: Boolean) {
    UrbanCard(Modifier.fillMaxWidth()) {
        Text("Rendimiento", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
        Spacer(Modifier.height(8.dp))
        when {
            loading -> LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.GoldText)
            performance == null -> Text("No se pudo cargar el rendimiento.", style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UrbanKeyValue("Citas este mes", performance.appointmentsThisMonth.toString())
                UrbanKeyValue("Citas el mes pasado", performance.appointmentsLastMonth.toString())
                if (performance.appointmentsLastMonth > 0) {
                    UrbanKeyValue("Variación", (if (performance.growth >= 0) "+" else "−") + "${kotlin.math.abs(performance.growth)} %")
                }
                UrbanKeyValue("Calificación", if (performance.averageRating > 0) "%.1f / 5".format(performance.averageRating) else "Sin reseñas")
                UrbanKeyValue("Clientes atendidos", performance.totalClients.toString())
            }
        }
    }
}
