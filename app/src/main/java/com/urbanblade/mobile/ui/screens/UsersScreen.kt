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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.urbanblade.mobile.data.model.SystemUserRow
import com.urbanblade.mobile.ui.components.UrbanAvatar
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanEmptyState
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanFieldLabel
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
import com.urbanblade.mobile.ui.components.UrbanOutlineButton
import com.urbanblade.mobile.ui.components.UrbanPrimaryButton
import com.urbanblade.mobile.ui.components.UrbanPremiumCard
import com.urbanblade.mobile.ui.components.UrbanPageHeader
import com.urbanblade.mobile.ui.components.UrbanRolePill
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.components.UrbanSkeletonList
import com.urbanblade.mobile.ui.components.UrbanTextField
import com.urbanblade.mobile.ui.components.UrbanTopBar
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.UsersAdminViewModel
import com.urbanblade.mobile.ui.viewmodel.passwordProblem
import kotlinx.coroutines.delay

/** Cuentas de acceso al sistema (solo administrador): buscar, filtrar por rol, crear, editar y eliminar. */
@Composable
fun UsersScreen(onBack: () -> Unit, currentUserId: String? = null, vm: UsersAdminViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SystemUserRow?>(null) }
    var deleting by remember { mutableStateOf<SystemUserRow?>(null) }

    // Busca al escribir, con una pausa corta para no consultar en cada letra.
    LaunchedEffect(state.query) {
        if (state.query.isNotBlank()) delay(350)
        vm.load()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            UrbanTopBar("", onBack) {
                IconButton(onClick = { vm.clearMessages(); creating = true }) { Icon(Icons.Default.PersonAdd, "Agregar usuario") }
                IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, "Actualizar") }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                UrbanPageHeader(
                    title = "Usuarios y personal",
                    subtitle = "Administra accesos y roles del equipo.",
                    eyebrow = "ADMINISTRACIÓN"
                )
            }
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
            if (state.roles.isNotEmpty()) {
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = state.roleFilter == null, onClick = { vm.setRoleFilter(null) }, label = { Text("Todos") })
                        state.roles.forEach { role ->
                            FilterChip(
                                selected = state.roleFilter == role,
                                onClick = { vm.setRoleFilter(if (state.roleFilter == role) null else role) },
                                label = { Text(role.replace('_', ' ').replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            }
            if (state.loading || state.saving) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            state.notice?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }
            if (!creating && editing == null) state.error?.let { item { UrbanErrorBanner(it) } }
            if (state.loading && state.items.isEmpty()) item { UrbanSkeletonList(4) }

            if (state.items.isNotEmpty()) {
                item {
                    UrbanPremiumCard(Modifier.fillMaxWidth()) {
                        Text("EQUIPO CON ACCESO", style = MaterialTheme.typography.labelLarge, color = UrbanColors.GoldText)
                        Text(
                            UrbanFormat.count(state.total, "cuenta registrada", "cuentas registradas"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = UrbanColors.Ink
                        )
                        Text(
                            UrbanFormat.count(state.roles.size, "rol disponible", "roles disponibles"),
                            style = MaterialTheme.typography.bodySmall,
                            color = UrbanColors.Muted
                        )
                    }
                }
            }

            if (!state.loading && state.items.isEmpty() && state.error == null) {
                item { UrbanEmptyState("Sin usuarios", "No hay cuentas que coincidan con la búsqueda.", Icons.Default.ManageAccounts) }
            }
            if (state.items.isNotEmpty()) {
                item { UrbanSectionTitle("Cuentas", UrbanFormat.count(state.total, "usuario", "usuarios")) }
            }
            items(state.items, key = { it.id }) { user ->
                UserCard(
                    user = user,
                    isSelf = user.id == currentUserId,
                    onEdit = { vm.clearMessages(); editing = user },
                    onDelete = { vm.clearMessages(); deleting = user }
                )
            }
            if (state.hasMore) {
                item {
                    UrbanOutlineButton(
                        text = if (state.loadingMore) "Cargando…" else "Cargar más usuarios",
                        onClick = { vm.loadMore() },
                        icon = Icons.Default.ExpandMore,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (creating || editing != null) {
        UserFormSheet(
            user = editing,
            roles = state.roles,
            saving = state.saving,
            error = state.error,
            onDismiss = { creating = false; editing = null },
            onSubmit = { name, email, password, role ->
                val target = editing
                if (target == null) {
                    vm.create(name, email, password.orEmpty(), role) { creating = false }
                } else {
                    vm.update(target.id, name, email, password, role) { editing = null }
                }
            }
        )
    }

    deleting?.let { user ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = UrbanColors.Card,
            title = { Text("Eliminar usuario") },
            text = { Text("¿Eliminar la cuenta de ${user.name} (${user.email})? Perderá el acceso al sistema.", color = UrbanColors.Muted) },
            confirmButton = { TextButton(onClick = { vm.delete(user.id); deleting = null }) { Text("Sí, eliminar", color = UrbanColors.Danger) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Volver") } }
        )
    }
}

@Composable
private fun UserCard(user: SystemUserRow, isSelf: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    UrbanCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UrbanAvatar(user.name, Modifier.size(44.dp), imageUrl = user.avatarUrl)
            Column(Modifier.weight(1f)) {
                Text(
                    user.name + if (isSelf) " (tú)" else "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = UrbanColors.Ink,
                    maxLines = 1
                )
                Text(user.email, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 1)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    user.roles.forEach { UrbanRolePill(it) }
                    if (user.emailVerifiedAt == null) {
                        Text("Sin verificar", style = MaterialTheme.typography.labelSmall, color = UrbanColors.Warning)
                    }
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar") }
            // Nadie debe poder borrar su propia cuenta desde aquí; el servidor también lo impide.
            if (!isSelf) IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Eliminar", tint = UrbanColors.Danger) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserFormSheet(
    user: SystemUserRow?,
    roles: List<String>,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (name: String, email: String, password: String?, role: String) -> Unit
) {
    val isEdit = user != null
    var name by remember { mutableStateOf(user?.name.orEmpty()) }
    var email by remember { mutableStateOf(user?.email.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(user?.roles?.firstOrNull() ?: roles.firstOrNull().orEmpty()) }
    var submitted by remember { mutableStateOf(false) }

    val nameError = if (submitted && name.isBlank()) "Escribe el nombre." else null
    val emailError = if (submitted && !email.contains("@")) "Escribe un correo válido." else null
    val passwordError = if (submitted) passwordProblem(password, confirmation, required = !isEdit) else null
    val roleError = if (submitted && role.isBlank()) "Elige un rol." else null

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = UrbanColors.Surface) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(if (isEdit) "Editar usuario" else "Nuevo usuario", style = MaterialTheme.typography.titleLarge, color = UrbanColors.Ink)
            UrbanTextField(
                value = name, onValueChange = { name = it }, label = "Nombre", error = nameError,
                capitalization = KeyboardCapitalization.Words, modifier = Modifier.fillMaxWidth()
            )
            UrbanTextField(
                value = email, onValueChange = { email = it }, label = "Correo", error = emailError,
                keyboardType = KeyboardType.Email, modifier = Modifier.fillMaxWidth()
            )
            Column {
                UrbanFieldLabel("Rol")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    roles.forEach { r ->
                        FilterChip(selected = role == r, onClick = { role = r }, label = { Text(r.replace('_', ' ').replaceFirstChar { it.uppercase() }) })
                    }
                }
                roleError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Danger) }
            }
            UrbanTextField(
                value = password, onValueChange = { password = it },
                label = if (isEdit) "Nueva contraseña (opcional)" else "Contraseña",
                error = passwordError, isPassword = true, modifier = Modifier.fillMaxWidth()
            )
            if (password.isNotEmpty() || !isEdit) {
                UrbanTextField(
                    value = confirmation, onValueChange = { confirmation = it }, label = "Confirmar contraseña",
                    isPassword = true, imeAction = ImeAction.Done, modifier = Modifier.fillMaxWidth()
                )
            }
            error?.let { UrbanErrorBanner(it) }
            UrbanPrimaryButton(
                text = if (isEdit) "Guardar cambios" else "Crear usuario",
                onClick = {
                    submitted = true
                    val valid = name.isNotBlank() && email.contains("@") && role.isNotBlank() &&
                        passwordProblem(password, confirmation, required = !isEdit) == null
                    if (valid) onSubmit(name.trim(), email.trim().lowercase(), password.takeIf { it.isNotEmpty() }, role)
                },
                loading = saving,
                icon = Icons.Default.Save,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
