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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
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
import com.urbanblade.mobile.data.model.MembershipPlanItem
import com.urbanblade.mobile.data.model.MembershipPlanRequest
import com.urbanblade.mobile.data.model.ServiceItem
import com.urbanblade.mobile.data.model.ServicePackageItem
import com.urbanblade.mobile.data.model.ServicePackageRequest
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanEmptyState
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanFieldLabel
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
import com.urbanblade.mobile.ui.components.UrbanPrimaryButton
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.components.UrbanSkeletonList
import com.urbanblade.mobile.ui.components.UrbanTextField
import com.urbanblade.mobile.ui.components.UrbanTopBar
import com.urbanblade.mobile.ui.components.UrbanMascotState
import com.urbanblade.mobile.ui.components.UrbanPageHeader
import com.urbanblade.mobile.ui.components.UrbanStateKind
import com.urbanblade.mobile.ui.components.urbanFilterChipColors
import com.urbanblade.mobile.ui.components.UrbanPillTabs
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.OffersAdminViewModel

private enum class OffersTab(val label: String) { Membresias("Membresías"), Paquetes("Paquetes") }

private fun money(value: Double) = UrbanFormat.moneyShort(value)

/** Membresías mensuales y paquetes prepagados: crear, editar y activar o desactivar. */
@Composable
fun OffersAdminScreen(onBack: () -> Unit, vm: OffersAdminViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var tab by remember { mutableStateOf(OffersTab.Membresias) }
    var editingPlan by remember { mutableStateOf<MembershipPlanItem?>(null) }
    var creatingPlan by remember { mutableStateOf(false) }
    var editingPackage by remember { mutableStateOf<ServicePackageItem?>(null) }
    var creatingPackage by remember { mutableStateOf(false) }
    val formOpen = editingPlan != null || creatingPlan || editingPackage != null || creatingPackage

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
                onClick = {
                    vm.clearMessages()
                    if (tab == OffersTab.Membresias) creatingPlan = true else creatingPackage = true
                },
                containerColor = UrbanColors.Gold,
                contentColor = UrbanColors.OnGold,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(if (tab == OffersTab.Membresias) "Nuevo plan" else "Nuevo paquete", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { UrbanPageHeader(title = "Membresías y paquetes", subtitle = "Planes mensuales con descuento y usos pagados por adelantado.", eyebrow = "Beneficios") }
            item {
                UrbanPillTabs(
                    listOf(OffersTab.Membresias.label to Icons.Default.CardMembership, OffersTab.Paquetes.label to Icons.Default.Inventory2),
                    tab.ordinal
                ) { tab = OffersTab.entries[it] }
            }
            if (state.loading || state.saving) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            state.notice?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }
            if (!formOpen) state.error?.let { msg ->
                item {
                    if (state.plans.isEmpty() && state.packages.isEmpty()) UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar los beneficios", msg, "Reintentar") { vm.load() }
                    else UrbanErrorBanner(msg)
                }
            }
            if (state.loading && state.plans.isEmpty() && state.packages.isEmpty()) item { UrbanSkeletonList(3) }

            if (tab == OffersTab.Membresias) {
                if (!state.loading && state.plans.isEmpty() && state.error == null) {
                    item {
                        UrbanMascotState(
                            UrbanStateKind.EMPTY, "Aún no hay planes", "Crea un plan mensual con descuento para tus clientes frecuentes.",
                            actionLabel = "Nuevo plan", actionIcon = Icons.Default.Add, onAction = { creatingPlan = true }
                        )
                    }
                }
                if (state.plans.isNotEmpty()) item { UrbanSectionTitle("Planes mensuales", UrbanFormat.count(state.plans.size, "plan", "planes")) }
                items(state.plans, key = { it.id }) { plan ->
                    OfferCard(
                        title = plan.nombre,
                        subtitle = "${plan.descuentoPct} % de descuento en servicios",
                        price = money(plan.precioMensual) + " / mes",
                        active = plan.activo,
                        onOpen = { vm.clearMessages(); editingPlan = plan },
                        onToggle = { vm.togglePlan(plan) }
                    )
                }
            } else {
                if (!state.loading && state.packages.isEmpty() && state.error == null) {
                    item {
                        UrbanMascotState(
                            UrbanStateKind.EMPTY, "Aún no hay paquetes", "Vende varios usos de un servicio por adelantado.",
                            actionLabel = "Nuevo paquete", actionIcon = Icons.Default.Add, onAction = { creatingPackage = true }
                        )
                    }
                }
                if (state.packages.isNotEmpty()) item { UrbanSectionTitle("Paquetes de usos", UrbanFormat.count(state.packages.size, "paquete", "paquetes")) }
                items(state.packages, key = { it.id }) { pack ->
                    OfferCard(
                        title = pack.nombre,
                        subtitle = "${pack.cantidadUsos} × ${pack.service.nombre ?: "servicio"}" +
                            (pack.vigenciaDias?.let { " · vigencia $it días" } ?: ""),
                        price = money(pack.precio),
                        active = pack.activo,
                        onOpen = { vm.clearMessages(); editingPackage = pack },
                        onToggle = { vm.togglePackage(pack) }
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (creatingPlan || editingPlan != null) {
        PlanFormSheet(
            plan = editingPlan, saving = state.saving, error = state.error,
            onDismiss = { creatingPlan = false; editingPlan = null },
            onSave = { body -> vm.savePlan(editingPlan?.id, body) { creatingPlan = false; editingPlan = null } }
        )
    }
    if (creatingPackage || editingPackage != null) {
        PackageFormSheet(
            pack = editingPackage, services = state.services, saving = state.saving, error = state.error,
            onDismiss = { creatingPackage = false; editingPackage = null },
            onSave = { body -> vm.savePackage(editingPackage?.id, body) { creatingPackage = false; editingPackage = null } }
        )
    }
}

@Composable
private fun OfferCard(title: String, subtitle: String, price: String, active: Boolean, onOpen: () -> Unit, onToggle: () -> Unit) {
    UrbanCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = if (active) UrbanColors.Ink else UrbanColors.Muted
                )
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Muted)
                Text(
                    price + if (active) "" else " · Inactivo",
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    color = if (active) UrbanColors.Gold else UrbanColors.Muted
                )
            }
            UrbanSwitch(
                checked = active, onCheckedChange = { onToggle() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanFormSheet(plan: MembershipPlanItem?, saving: Boolean, error: String?, onDismiss: () -> Unit, onSave: (MembershipPlanRequest) -> Unit) {
    var nombre by remember { mutableStateOf(plan?.nombre.orEmpty()) }
    var descripcion by remember { mutableStateOf(plan?.descripcion.orEmpty()) }
    var precio by remember { mutableStateOf(plan?.let { "%.0f".format(it.precioMensual) }.orEmpty()) }
    var descuento by remember { mutableStateOf(plan?.descuentoPct?.toString().orEmpty()) }
    var activo by remember { mutableStateOf(plan?.activo ?: true) }
    var submitted by remember { mutableStateOf(false) }

    val precioValue = precio.replace(',', '.').toDoubleOrNull()
    val descuentoValue = descuento.toIntOrNull()
    val nombreError = if (submitted && nombre.isBlank()) "Escribe el nombre del plan." else null
    val precioError = if (submitted && (precioValue == null || precioValue < 1)) "El precio mínimo es $1." else null
    val descuentoError = if (submitted && (descuentoValue == null || descuentoValue !in 1..100)) "Entre 1 y 100 %." else null
    val priceChanges = plan == null || (precioValue != null && kotlin.math.abs(precioValue - plan.precioMensual) > 0.004)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = UrbanColors.Surface) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(if (plan == null) "Nuevo plan" else "Editar plan", style = MaterialTheme.typography.titleLarge, color = UrbanColors.Ink)
            UrbanTextField(nombre, { nombre = it }, "Nombre", error = nombreError, capitalization = KeyboardCapitalization.Sentences, modifier = Modifier.fillMaxWidth())
            UrbanTextField(descripcion, { descripcion = it }, "Descripción (opcional)", capitalization = KeyboardCapitalization.Sentences, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                UrbanTextField(
                    precio, { precio = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, "Precio mensual (MXN)",
                    error = precioError, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f)
                )
                UrbanTextField(
                    descuento, { descuento = it.filter(Char::isDigit) }, "Descuento (%)",
                    error = descuentoError, keyboardType = KeyboardType.Number, imeAction = ImeAction.Done, modifier = Modifier.weight(1f)
                )
            }
            SwitchRow("Disponible para contratar", "Las membresías ya contratadas no se ven afectadas.", activo) { activo = it }
            if (priceChanges) {
                Text(
                    "Al guardar un precio nuevo se crea un precio nuevo en Stripe; el anterior deja de usarse.",
                    style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted
                )
            }
            error?.let { UrbanErrorBanner(it) }
            UrbanPrimaryButton(
                text = if (plan == null) "Crear plan" else "Guardar cambios",
                onClick = {
                    submitted = true
                    if (nombre.isNotBlank() && precioValue != null && precioValue >= 1 && descuentoValue != null && descuentoValue in 1..100) {
                        onSave(MembershipPlanRequest(nombre.trim(), descripcion.trim().ifEmpty { null }, precioValue, descuentoValue, activo))
                    }
                },
                loading = saving, icon = Icons.Default.Save, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackageFormSheet(
    pack: ServicePackageItem?, services: List<ServiceItem>, saving: Boolean, error: String?,
    onDismiss: () -> Unit, onSave: (ServicePackageRequest) -> Unit
) {
    var nombre by remember { mutableStateOf(pack?.nombre.orEmpty()) }
    var serviceId by remember { mutableStateOf(pack?.service?.id.orEmpty()) }
    var usos by remember { mutableStateOf(pack?.cantidadUsos?.toString().orEmpty()) }
    var precio by remember { mutableStateOf(pack?.let { "%.0f".format(it.precio) }.orEmpty()) }
    var vigencia by remember { mutableStateOf(pack?.vigenciaDias?.toString().orEmpty()) }
    var activo by remember { mutableStateOf(pack?.activo ?: true) }
    var submitted by remember { mutableStateOf(false) }

    val precioValue = precio.replace(',', '.').toDoubleOrNull()
    val usosValue = usos.toIntOrNull()
    val vigenciaValue = if (vigencia.isBlank()) null else vigencia.toIntOrNull()
    val nombreError = if (submitted && nombre.isBlank()) "Escribe el nombre del paquete." else null
    val serviceError = if (submitted && serviceId.isBlank()) "Elige el servicio que incluye." else null
    val usosError = if (submitted && (usosValue == null || usosValue !in 2..100)) "Entre 2 y 100 usos." else null
    val precioError = if (submitted && (precioValue == null || precioValue <= 0)) "Escribe un precio válido." else null
    val vigenciaError = if (submitted && vigencia.isNotBlank() && (vigenciaValue == null || vigenciaValue !in 1..730)) "Entre 1 y 730 días." else null

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = UrbanColors.Surface) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(if (pack == null) "Nuevo paquete" else "Editar paquete", style = MaterialTheme.typography.titleLarge, color = UrbanColors.Ink)
            UrbanTextField(nombre, { nombre = it }, "Nombre", error = nombreError, capitalization = KeyboardCapitalization.Sentences, modifier = Modifier.fillMaxWidth())
            Column {
                UrbanFieldLabel("Servicio incluido")
                Spacer(Modifier.height(8.dp))
                if (services.isEmpty()) {
                    Text("No se pudo cargar el catálogo de servicios.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    services.forEach { s ->
                        FilterChip(selected = serviceId == s.id, onClick = { serviceId = s.id }, label = { Text(s.nombre) })
                    }
                }
                serviceError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Danger) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                UrbanTextField(usos, { usos = it.filter(Char::isDigit) }, "Usos", error = usosError, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                UrbanTextField(
                    precio, { precio = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, "Precio (MXN)",
                    error = precioError, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f)
                )
            }
            UrbanTextField(
                vigencia, { vigencia = it.filter(Char::isDigit) }, "Vigencia en días (opcional)",
                error = vigenciaError, keyboardType = KeyboardType.Number, imeAction = ImeAction.Done,
                helper = "Vacío = sin vencimiento.", modifier = Modifier.fillMaxWidth()
            )
            SwitchRow("Disponible para venta", "Los paquetes ya vendidos siguen siendo válidos.", activo) { activo = it }
            error?.let { UrbanErrorBanner(it) }
            UrbanPrimaryButton(
                text = if (pack == null) "Crear paquete" else "Guardar cambios",
                onClick = {
                    submitted = true
                    val vigenciaOk = vigencia.isBlank() || (vigenciaValue != null && vigenciaValue in 1..730)
                    if (nombre.isNotBlank() && serviceId.isNotBlank() && usosValue != null && usosValue in 2..100 &&
                        precioValue != null && precioValue > 0 && vigenciaOk
                    ) {
                        onSave(ServicePackageRequest(nombre.trim(), serviceId, usosValue, precioValue, vigenciaValue, activo))
                    }
                },
                loading = saving, icon = Icons.Default.Save, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) =
    UrbanSwitchRow(title, subtitle, checked = checked, onCheckedChange = onChange)
