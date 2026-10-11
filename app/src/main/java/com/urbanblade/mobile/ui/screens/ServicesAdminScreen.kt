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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.urbanblade.mobile.data.model.ServiceAdminItem
import com.urbanblade.mobile.data.model.ServiceUpsertRequest
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanEmptyState
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
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
import com.urbanblade.mobile.ui.viewmodel.ServiceStatusFilter
import com.urbanblade.mobile.ui.viewmodel.ServicesAdminViewModel

/** Catálogo de servicios del administrador: buscar, filtrar, crear, editar, activar y eliminar. */
@Composable
fun ServicesAdminScreen(onBack: () -> Unit, vm: ServicesAdminViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var editing by remember { mutableStateOf<ServiceAdminItem?>(null) }
    var creating by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<ServiceAdminItem?>(null) }

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            UrbanTopBar("", onBack) {
                IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, "Actualizar") }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.clearMessages(); creating = true },
                containerColor = UrbanColors.Gold,
                contentColor = UrbanColors.OnGold,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Nuevo servicio", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { UrbanPageHeader(title = "Servicios", subtitle = "Precio, duración y lo que ven tus clientes al reservar.", eyebrow = "Catálogo") }
            item {
                UrbanTextField(
                    value = state.query,
                    onValueChange = vm::setQuery,
                    label = "Buscar servicio",
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
            // Con el formulario abierto el error se muestra dentro de la hoja, no aquí.
            if (editing == null && !creating) state.error?.let { msg ->
                item {
                    if (state.items.isEmpty()) UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar los servicios", msg, "Reintentar") { vm.load() }
                    else UrbanErrorBanner(msg)
                }
            }

            if (state.loading && state.items.isEmpty()) item { UrbanSkeletonList(4) }

            if (!state.loading && state.items.isEmpty() && state.error == null) {
                item {
                    val firstTime = state.query.isBlank() && state.status == ServiceStatusFilter.Todos
                    UrbanMascotState(
                        UrbanStateKind.EMPTY,
                        if (firstTime) "Aún no hay servicios" else "Sin resultados",
                        if (firstTime) "Crea el primero para que tus clientes puedan reservar." else "Prueba con otra búsqueda o filtro.",
                        actionLabel = if (firstTime) "Nuevo servicio" else null,
                        actionIcon = Icons.Default.Add,
                        onAction = if (firstTime) { { creating = true } } else null
                    )
                }
            }

            if (state.items.isNotEmpty()) {
                item { UrbanSectionTitle("Catálogo", UrbanFormat.count(state.total, "servicio", "servicios")) }
            }
            items(state.items, key = { it.id }) { service ->
                ServiceAdminCard(
                    service = service,
                    onOpen = { vm.clearMessages(); editing = service },
                    onToggle = { vm.toggleActive(service) }
                )
            }
            if (state.hasMore) {
                item {
                    UrbanOutlineButton(
                        text = if (state.loadingMore) "Cargando…" else "Cargar más servicios",
                        onClick = { vm.loadMore() },
                        icon = Icons.Default.ExpandMore,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (creating || editing != null) {
        ServiceFormSheet(
            service = editing,
            categories = state.categories,
            saving = state.saving,
            error = state.error,
            onDismiss = { creating = false; editing = null },
            onSave = { body -> vm.save(editing?.slug, body) { creating = false; editing = null } },
            onDelete = editing?.let { current -> { confirmDelete = current } }
        )
    }

    confirmDelete?.let { service ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            containerColor = UrbanColors.Card,
            title = { Text("Eliminar servicio") },
            text = {
                Text(
                    "¿Eliminar \"${service.nombre}\" del catálogo? Si solo quieres dejar de ofrecerlo, desactívalo: así conserva su historial.",
                    color = UrbanColors.Muted
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.delete(service); confirmDelete = null; editing = null }) {
                    Text("Sí, eliminar", color = UrbanColors.Danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Volver") } }
        )
    }
}

@Composable
private fun ServiceAdminCard(service: ServiceAdminItem, onOpen: () -> Unit, onToggle: () -> Unit) {
    UrbanCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    service.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (service.activo) UrbanColors.Ink else UrbanColors.Muted
                )
                Text(
                    "${service.categoria} · ${service.duracionMin} min" + if (service.activo) "" else " · Inactivo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = UrbanColors.Muted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    UrbanFormat.moneyShort(service.precio),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (service.activo) UrbanColors.Gold else UrbanColors.Muted
                )
                UrbanSwitch(
                    checked = service.activo,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceFormSheet(
    service: ServiceAdminItem?,
    categories: List<String>,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (ServiceUpsertRequest) -> Unit,
    onDelete: (() -> Unit)?
) {
    var nombre by remember { mutableStateOf(service?.nombre.orEmpty()) }
    var categoria by remember { mutableStateOf(service?.categoria.orEmpty()) }
    var precio by remember { mutableStateOf(service?.let { "%.0f".format(it.precio) }.orEmpty()) }
    var duracion by remember { mutableStateOf(service?.duracionMin?.toString().orEmpty()) }
    var descripcion by remember { mutableStateOf(service?.descripcion.orEmpty()) }
    var activo by remember { mutableStateOf(service?.activo ?: true) }
    var submitted by remember { mutableStateOf(false) }

    val precioValue = precio.replace(',', '.').toDoubleOrNull()
    val duracionValue = duracion.toIntOrNull()
    val nombreError = if (submitted && nombre.isBlank()) "Escribe el nombre del servicio." else null
    val categoriaError = if (submitted && categoria.isBlank()) "Elige o escribe una categoría." else null
    val precioError = if (submitted && (precioValue == null || precioValue < 0)) "Escribe un precio válido." else null
    val duracionError = if (submitted && (duracionValue == null || duracionValue !in 5..600)) "Entre 5 y 600 minutos." else null

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = UrbanColors.Surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (service == null) "Nuevo servicio" else "Editar servicio",
                style = MaterialTheme.typography.titleLarge,
                color = UrbanColors.Ink
            )
            UrbanTextField(
                value = nombre, onValueChange = { nombre = it }, label = "Nombre",
                error = nombreError, capitalization = KeyboardCapitalization.Sentences,
                modifier = Modifier.fillMaxWidth()
            )
            UrbanTextField(
                value = categoria, onValueChange = { categoria = it }, label = "Categoría",
                error = categoriaError, capitalization = KeyboardCapitalization.Sentences,
                modifier = Modifier.fillMaxWidth()
            )
            if (categories.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { name ->
                        FilterChip(selected = categoria == name, onClick = { categoria = name }, label = { Text(name) })
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                UrbanTextField(
                    value = precio, onValueChange = { precio = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = "Precio (MXN)", error = precioError, keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                UrbanTextField(
                    value = duracion, onValueChange = { duracion = it.filter(Char::isDigit) },
                    label = "Duración (min)", error = duracionError, keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
            UrbanTextField(
                value = descripcion, onValueChange = { descripcion = it }, label = "Descripción (opcional)",
                capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done,
                modifier = Modifier.fillMaxWidth()
            )
            UrbanSwitchRow("Disponible para reservar", "Si lo apagas, los clientes ya no lo ven.", checked = activo, onCheckedChange = { activo = it })
            error?.let { UrbanErrorBanner(it) }
            UrbanPrimaryButton(
                text = if (service == null) "Crear servicio" else "Guardar cambios",
                onClick = {
                    submitted = true
                    if (nombre.isNotBlank() && categoria.isNotBlank() && precioValue != null && precioValue >= 0 &&
                        duracionValue != null && duracionValue in 5..600
                    ) {
                        onSave(
                            ServiceUpsertRequest(
                                nombre.trim(), categoria.trim(), precioValue, duracionValue,
                                descripcion.trim().ifEmpty { null }, activo
                            )
                        )
                    }
                },
                loading = saving,
                icon = Icons.Default.Save,
                modifier = Modifier.fillMaxWidth()
            )
            onDelete?.let {
                TextButton(onClick = it, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DeleteOutline, null, Modifier.size(18.dp), tint = UrbanColors.Danger)
                    Text("  Eliminar servicio", color = UrbanColors.Danger)
                }
            }
        }
    }
}
