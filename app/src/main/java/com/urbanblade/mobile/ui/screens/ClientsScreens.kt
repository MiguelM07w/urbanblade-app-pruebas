package com.urbanblade.mobile.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.ClientsViewModel
import kotlinx.coroutines.delay

private val SEGMENT_LABEL = mapOf(
    "vip" to "VIP",
    "new" to "Nuevo",
    "active" to "Activo",
    "inactive" to "Inactivo",
)

@Composable
fun ClientsListScreen(user: AuthUser, onClientClick: (String) -> Unit, onBack: () -> Unit, vm: ClientsViewModel = viewModel()) {
    val clients by vm.clients.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    val hasMore by vm.hasMore.collectAsState()
    val loadingMore by vm.loadingMore.collectAsState()
    var search by remember { mutableStateOf("") }
    var segment by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    // Busca al escribir, con una pausa corta para no consultar en cada letra.
    LaunchedEffect(search, segment) {
        if (search.isNotBlank()) delay(350)
        vm.load(search.takeIf { it.isNotBlank() }, segment)
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            UrbanTopBar("", onBack) {
                IconButton(onClick = { showCreate = true }) { Icon(Icons.Default.PersonAdd, "Agregar cliente") }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                UrbanPageHeader(title = "Clientes", subtitle = "Busca, registra y revisa el historial de cada cliente.", eyebrow = "OPERACIÓN")
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Buscar por nombre, correo o teléfono…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { vm.load(search.takeIf { it.isNotBlank() }, segment) }),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search)
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = segment == null, onClick = { segment = null }, label = { Text("Todos") })
                    SEGMENT_LABEL.forEach { (key, label) ->
                        FilterChip(selected = segment == key, onClick = { segment = if (segment == key) null else key }, label = { Text(label) })
                    }
                }
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                urbanLoadingItem(busy, clients.data.isEmpty())
                error?.let { item { UrbanErrorBanner(it) } }
                if (clients.data.isNotEmpty()) {
                    item { UrbanSectionTitle("Clientes", UrbanFormat.count(clients.total ?: clients.data.size, "cliente", "clientes")) }
                }
                if (clients.data.isEmpty() && !busy) {
                    item { UrbanMascotState(UrbanStateKind.EMPTY, "Sin clientes", "No hay clientes que coincidan con la búsqueda.", "Registrar cliente", Icons.Default.PersonAdd) { showCreate = true } }
                }
                items(clients.data) { client ->
                    UrbanCard(Modifier.fillMaxWidth(), onClick = { onClientClick(client.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UrbanAvatar(client.name ?: "?", Modifier.size(48.dp), imageUrl = client.avatarUrl)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(client.name ?: "Sin nombre", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(client.email ?: client.telefono ?: "—", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    client.lastAppointment?.let { "Última visita: ${UrbanFormat.dateShort(it)}" } ?: "Aún sin visitas",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = UrbanColors.Muted
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                client.segment?.let { SimpleStatusPill(SEGMENT_LABEL[it] ?: it, color = if (it == "vip") UrbanColors.Gold else null) }
                                Spacer(Modifier.height(4.dp))
                                Text(UrbanFormat.moneyShort(client.totalSpent), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
                                Text(UrbanFormat.count(client.totalAppointments, "cita", "citas"), style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted)
                            }
                        }
                    }
                }
                if (hasMore) {
                    item {
                        UrbanOutlineButton(
                            text = if (loadingMore) "Cargando…" else "Cargar más clientes",
                            onClick = { vm.loadMore() },
                            icon = Icons.Default.ExpandMore,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateClientDialog(
            onDismiss = { showCreate = false },
            onCreate = { name, email, telefono, password ->
                vm.create(name, email, telefono, password) {
                    showCreate = false
                    vm.load(search.takeIf { it.isNotBlank() }, segment)
                }
            },
            saving = vm.saving.collectAsState().value
        )
    }
}

@Composable
private fun CreateClientDialog(onDismiss: () -> Unit, onCreate: (String, String, String?, String) -> Unit, saving: Boolean) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo cliente") },
        text = {
            UrbanFormScroll(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UrbanTextField(name, { name = it }, "Nombre", Modifier.fillMaxWidth(), capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words)
                UrbanTextField(email, { email = it }, "Correo", Modifier.fillMaxWidth(), keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)
                UrbanTextField(telefono, { telefono = it }, "Teléfono (opcional)", Modifier.fillMaxWidth(), keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                UrbanTextField(password, { password = it }, "Contraseña", Modifier.fillMaxWidth(), isPassword = true)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && name.isNotBlank() && email.isNotBlank() && password.length >= 8,
                onClick = { onCreate(name, email, telefono.takeIf { it.isNotBlank() }, password) }
            ) { Text(if (saving) "Guardando…" else "Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun ClientDetailScreen(user: AuthUser, clientId: String, onBack: () -> Unit, vm: ClientsViewModel = viewModel()) {
    val detail by vm.detail.collectAsState()
    val busy by vm.busy.collectAsState()
    val saving by vm.saving.collectAsState()
    val message by vm.message.collectAsState()
    val error by vm.error.collectAsState()
    val context = LocalContext.current
    var editing by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }

    LaunchedEffect(clientId) { vm.loadDetail(clientId) }
    LaunchedEffect(detail) {
        detail?.let {
            name = it.name.orEmpty()
            email = it.email.orEmpty()
            telefono = it.telefono.orEmpty()
            notas = it.notas.orEmpty()
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            UrbanTopBar(detail?.name ?: "Cliente", onBack) {
                if (user.roles.contains("administrador")) {
                    IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Default.Delete, "Eliminar cliente") }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            error?.let { item { UrbanErrorBanner(it) } }
            message?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }

            detail?.let { d ->
                item {
                    UrbanPremiumCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            UrbanAvatar(d.name ?: "?", Modifier.size(56.dp), imageUrl = d.avatarUrl)
                            Column(Modifier.weight(1f)) {
                                Text(d.name ?: "Sin nombre", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = UrbanColors.Ink)
                                Text(
                                    d.joinedAt?.let { "Cliente desde ${UrbanFormat.dateShort(it)}" } ?: "Cliente",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = UrbanColors.Muted
                                )
                            }
                            d.segment?.let { SimpleStatusPill(SEGMENT_LABEL[it] ?: it, color = if (it == "vip") UrbanColors.Gold else null) }
                        }
                        val phone = d.telefono?.filter { it.isDigit() || it == '+' }?.takeIf { it.isNotEmpty() }
                        val mail = d.email?.takeIf { it.contains("@") }
                        if (phone != null || mail != null) {
                            Spacer(Modifier.height(14.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (phone != null) {
                                    UrbanOutlineButton("Llamar", { openIntent(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }, Modifier.weight(1f), Icons.Default.Call)
                                }
                                if (mail != null) {
                                    UrbanOutlineButton("Correo", { openIntent(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$mail"))) }, Modifier.weight(1f), Icons.Default.Email)
                                }
                            }
                        }
                    }
                }
                inactivityNote(d.daysSinceLastAppointment)?.let { note ->
                    item { UrbanInfoBanner(note, Icons.Default.NotificationsActive) }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        UrbanMetricCard("Nivel", d.nivel?.replaceFirstChar { it.uppercase() } ?: "—", Icons.Default.Star, Modifier.weight(1f))
                        UrbanMetricCard("Puntos", d.puntos.toString(), Icons.Default.Toll, Modifier.weight(1f))
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        UrbanMetricCard("Citas totales", d.totalAppointments.toString(), Icons.Default.CalendarMonth, Modifier.weight(1f))
                        UrbanMetricCard("Total gastado", UrbanFormat.moneyShort(d.totalSpent), Icons.Default.Payments, Modifier.weight(1f))
                    }
                }
                if (d.averageSpent > 0) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            UrbanMetricCard("Ticket promedio", UrbanFormat.moneyShort(d.averageSpent), Icons.Default.Receipt, Modifier.weight(1f))
                            UrbanMetricCard(
                                "Días sin venir",
                                d.daysSinceLastAppointment?.toString() ?: "—",
                                Icons.Default.Schedule,
                                Modifier.weight(1f)
                            )
                        }
                    }
                }
                item {
                    UrbanCard(Modifier.fillMaxWidth()) {
                        UrbanKeyValue("Barbero preferido", d.preferredBarber ?: "N/A")
                        UrbanKeyValue("Última cita", d.lastAppointment?.let { UrbanFormat.date(it) } ?: "—", Modifier.padding(top = 6.dp))
                        UrbanKeyValue("Cliente desde", d.joinedAt?.let { UrbanFormat.date(it) } ?: "—", Modifier.padding(top = 6.dp))
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        UrbanSectionTitle("Datos de contacto")
                        TextButton(onClick = { editing = !editing }) { Text(if (editing) "Cancelar" else "Editar") }
                    }
                }
                item {
                    UrbanCard(Modifier.fillMaxWidth()) {
                        if (editing) {
                            UrbanTextField(name, { name = it }, "Nombre", Modifier.fillMaxWidth(), capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words)
                            Spacer(Modifier.height(10.dp))
                            UrbanTextField(email, { email = it }, "Correo", Modifier.fillMaxWidth(), keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)
                            Spacer(Modifier.height(10.dp))
                            UrbanTextField(telefono, { telefono = it }, "Teléfono", Modifier.fillMaxWidth(), keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                            Spacer(Modifier.height(10.dp))
                            UrbanTextField(notas, { notas = it }, "Notas", Modifier.fillMaxWidth(), capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences, imeAction = androidx.compose.ui.text.input.ImeAction.Default, minLines = 2)
                            Spacer(Modifier.height(12.dp))
                            UrbanPrimaryButton(
                                text = "Guardar cambios",
                                onClick = {
                                    vm.update(clientId, name.takeIf { it.isNotBlank() }, email.takeIf { it.isNotBlank() }, telefono.takeIf { it.isNotBlank() }, notas)
                                    editing = false
                                },
                                loading = saving,
                                icon = Icons.Default.Save,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            UrbanKeyValue("Correo", d.email ?: "—")
                            UrbanKeyValue("Teléfono", d.telefono ?: "—", Modifier.padding(top = 6.dp))
                            if (!d.notas.isNullOrBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(d.notas, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                            }
                        }
                    }
                }

                if (d.appointments.isNotEmpty()) {
                    item { UrbanSectionTitle("Historial de citas", "${d.appointments.size} citas") }
                    items(d.appointments) { appt ->
                        UrbanCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(appt.service ?: "—", style = MaterialTheme.typography.titleMedium)
                                    Text("${appt.fecha?.let { UrbanFormat.dateShort(it) } ?: "—"} · ${appt.barber ?: "—"}", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                                }
                                appt.estado?.let { SimpleStatusPill(it) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar cliente?") },
            text = { Text("Esta acción no se puede deshacer. Un cliente con citas registradas no se puede eliminar.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    vm.delete(clientId, onBack)
                }) { Text("Sí, eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") } }
        )
    }
}

/** Días sin venir a partir de los cuales conviene contactar al cliente antes de perderlo. */
private const val INACTIVITY_DAYS = 45

/** Aviso para el personal cuando un cliente lleva mucho sin reservar; nulo si viene con regularidad. */
internal fun inactivityNote(days: Int?): String? =
    if (days != null && days >= INACTIVITY_DAYS) {
        "Hace $days días que no viene. Un mensaje o llamada ahora puede recuperarlo."
    } else {
        null
    }

/** Abre una app externa (teléfono, correo); si el dispositivo no tiene una que la atienda, no hace nada. */
private fun openIntent(context: android.content.Context, intent: Intent) {
    runCatching { context.startActivity(intent) }
}
