package com.urbanblade.mobile.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.data.model.AccountUser
import com.urbanblade.mobile.data.model.NotificationPreferences
import com.urbanblade.mobile.ui.components.PasswordStrengthMeter
import com.urbanblade.mobile.ui.components.UrbanDateField
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
import com.urbanblade.mobile.ui.components.UrbanPrimaryButton
import com.urbanblade.mobile.ui.components.UrbanTextField
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.theme.UrbanTheme
import com.urbanblade.mobile.ui.theme.previewColors
import com.urbanblade.mobile.ui.viewmodel.AccountNotice

/** Aviso de éxito o error de una sección, debajo de su botón. */
@Composable
fun AccountNoticeBanner(notice: AccountNotice?) {
    notice ?: return
    Spacer(Modifier.height(12.dp))
    if (notice.isError) UrbanErrorBanner(notice.text) else UrbanInfoBanner(notice.text, Icons.Default.CheckCircle)
}

private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

/**
 * Datos personales. Nombre y correo son de todos; teléfono, cumpleaños y sexo solo existen en el
 * perfil de cliente (el backend los ignora para el personal). "Guardar" se activa al cambiar algo.
 */
@Composable
fun PersonalDataForm(
    profile: AccountUser,
    isClient: Boolean,
    saving: Boolean,
    notice: AccountNotice?,
    onSave: (name: String, email: String, phone: String, birth: String, sex: String) -> Unit
) {
    var name by rememberSaveable(profile) { mutableStateOf(profile.name) }
    var email by rememberSaveable(profile) { mutableStateOf(profile.email) }
    var phone by rememberSaveable(profile) { mutableStateOf(profile.client?.telefono.orEmpty()) }
    var birth by rememberSaveable(profile) { mutableStateOf(profile.client?.fechaNacimiento.orEmpty()) }
    var sex by rememberSaveable(profile) { mutableStateOf(profile.client?.sexo.orEmpty()) }

    val nameError = if (name.isBlank()) "Escribe tu nombre." else null
    val emailError = if (!EMAIL.matches(email.trim())) "Revisa el correo." else null
    val dirty = name != profile.name || email != profile.email || (isClient && (
        phone != profile.client?.telefono.orEmpty() ||
            birth != profile.client?.fechaNacimiento.orEmpty() ||
            sex != profile.client?.sexo.orEmpty()
        ))

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        UrbanTextField(
            name, { name = it }, "Nombre completo",
            leadingIcon = Icons.Default.Person,
            capitalization = KeyboardCapitalization.Words,
            error = nameError.takeIf { dirty }
        )
        UrbanTextField(
            email, { email = it }, "Correo",
            leadingIcon = Icons.Default.AlternateEmail,
            keyboardType = KeyboardType.Email,
            error = emailError.takeIf { dirty },
            imeAction = if (isClient) ImeAction.Next else ImeAction.Done
        )
        if (isClient) {
            UrbanTextField(
                phone, { value -> phone = value.filter { it.isDigit() || it in "+ -" }.take(20) }, "Teléfono",
                leadingIcon = Icons.Default.Phone,
                keyboardType = KeyboardType.Phone,
                helper = "Para avisarte de tu cita si algo cambia.",
                imeAction = ImeAction.Done
            )
            UrbanDateField(birth, { birth = it }, "Fecha de nacimiento", onlyPast = true, placeholder = "Agregar cumpleaños")
            Column {
                Text("Sexo", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted, modifier = Modifier.padding(start = 4.dp))
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("masculino" to "Masculino", "femenino" to "Femenino", "prefiero_no_decir" to "Prefiero no decir").forEach { (value, label) ->
                        FilterChip(
                            selected = sex == value,
                            onClick = { sex = if (sex == value) "" else value },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = UrbanColors.Gold.copy(alpha = 0.18f),
                                selectedLabelColor = UrbanColors.Gold
                            )
                        )
                    }
                }
            }
        }
        UrbanPrimaryButton(
            text = "Guardar datos",
            onClick = { onSave(name, email, phone, birth, sex) },
            enabled = dirty && nameError == null && emailError == null,
            loading = saving,
            icon = Icons.Default.Save,
            modifier = Modifier.fillMaxWidth()
        )
    }
    AccountNoticeBanner(notice)
}

/**
 * Cambio de contraseña con el mismo medidor del registro. [resetKey] cambia cuando el servidor
 * confirma el cambio, y entonces los campos se vacían.
 */
@Composable
fun ChangePasswordForm(saving: Boolean, resetKey: Int, notice: AccountNotice?, onSubmit: (current: String, password: String, confirmation: String) -> Unit) {
    // Contraseñas con remember y no rememberSaveable: así no se copian al estado guardado de la
    // actividad (Bundle) al rotar o cuando Android mata la app. Se vacían al rotar la pantalla.
    var current by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    LaunchedEffect(resetKey) {
        if (resetKey > 0) { current = ""; password = ""; confirmation = "" }
    }

    val passwordError = if (password.isNotEmpty() && password.length < 8) "Usa al menos 8 caracteres." else null
    val confirmError = if (confirmation.isNotEmpty() && confirmation != password) "No coincide con la nueva contraseña." else null
    val ready = current.isNotEmpty() && password.length >= 8 && confirmation == password
    val submit = { if (ready) onSubmit(current, password, confirmation) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        UrbanTextField(current, { current = it }, "Contraseña actual", leadingIcon = Icons.Default.Lock, isPassword = true)
        Column {
            UrbanTextField(password, { password = it }, "Nueva contraseña", leadingIcon = Icons.Default.LockReset, isPassword = true, error = passwordError)
            Spacer(Modifier.height(8.dp))
            PasswordStrengthMeter(password)
        }
        UrbanTextField(
            confirmation, { confirmation = it }, "Confirma la nueva contraseña",
            leadingIcon = Icons.Default.LockReset, isPassword = true, error = confirmError,
            imeAction = ImeAction.Done, onImeAction = submit
        )
        Text(
            "¿Entras con Google y no tienes contraseña? Cierra sesión y usa «¿Olvidaste tu contraseña?» para crear una.",
            style = MaterialTheme.typography.bodySmall,
            color = UrbanColors.Muted
        )
        UrbanPrimaryButton(
            text = "Cambiar contraseña",
            onClick = submit,
            enabled = ready,
            loading = saving,
            icon = Icons.Default.Lock,
            modifier = Modifier.fillMaxWidth()
        )
    }
    AccountNoticeBanner(notice)
}

/**
 * Eliminar la cuenta propia. Con sesión de Google (sin contraseña conocida) se confirma escribiendo
 * ELIMINAR; con cualquier otra, con la contraseña. [viaGoogle] null = todavía se está consultando.
 */
@Composable
fun DeleteAccountForm(viaGoogle: Boolean?, deleting: Boolean, notice: AccountNotice?, onDelete: (secret: String) -> Unit) {
    var secret by remember { mutableStateOf("") } // contraseña o ELIMINAR: no se guarda en el estado de la actividad
    val ready = when (viaGoogle) {
        true -> secret.trim() == "ELIMINAR"
        false -> secret.isNotEmpty()
        null -> false
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Se borran tu acceso y todas tus sesiones; no se puede deshacer. Antes debes cancelar tus citas pendientes. " +
                "El historial de citas y pagos puede conservarse como registro del negocio; si quieres que también se borre, pídelo por correo.",
            style = MaterialTheme.typography.bodySmall,
            color = UrbanColors.Muted
        )
        when (viaGoogle) {
            null -> Text("Revisando tu sesión…", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            true -> UrbanTextField(
                secret, { secret = it }, "Escribe ELIMINAR para confirmar",
                leadingIcon = Icons.Default.DeleteForever, placeholder = "ELIMINAR",
                helper = "Entraste con Google, así que no te pedimos contraseña.",
                capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done
            )
            false -> UrbanTextField(
                secret, { secret = it }, "Tu contraseña", leadingIcon = Icons.Default.Lock,
                isPassword = true, imeAction = ImeAction.Done
            )
        }
        androidx.compose.material3.Button(
            onClick = { if (ready && !deleting) onDelete(secret) },
            enabled = ready && !deleting,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = UrbanColors.Danger, contentColor = androidx.compose.ui.graphics.Color.White),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        ) {
            Text(if (deleting) "Eliminando…" else "Eliminar mi cuenta definitivamente", fontWeight = FontWeight.SemiBold)
        }
    }
    AccountNoticeBanner(notice)
}

/** Canales de aviso que hoy sí entregan algo (ver NotificationPreferences). */
@Composable
fun NotificationChannels(preferences: NotificationPreferences, notice: AccountNotice?, onChange: (key: String, enabled: Boolean) -> Unit) {
    Column {
        // Sin este interruptor nadie podía activar el canal "push" y los avisos de citas nunca llegaban al teléfono.
        AccountSwitchRow("En este teléfono", "Confirmaciones, cambios y recordatorios de tus citas como notificación.", preferences.push) { onChange("push", it) }
        AccountSwitchRow("En la app", "La campana de UrbanBlade: citas, pagos y novedades.", preferences.inApp) { onChange("in_app", it) }
        AccountSwitchRow("Correo", "Confirmaciones y recordatorios en tu correo.", preferences.email) { onChange("email", it) }
        AccountSwitchRow("Promociones", "Ofertas, cupones y campañas de la barbería.", preferences.promociones) { onChange("promociones", it) }
    }
    AccountNoticeBanner(notice)
}

fun NotificationPreferences.summary(): String {
    val on = listOfNotNull("teléfono".takeIf { push }, "app".takeIf { inApp }, "correo".takeIf { email }, "promociones".takeIf { promociones })
    return if (on.isEmpty()) "Todos los avisos apagados" else "Activos: " + on.joinToString(", ")
}

/** Selector de los cuatro temas con vista previa de fondo y acento. */
@Composable
fun ThemePicker(current: UrbanTheme, onSelect: (UrbanTheme) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        UrbanTheme.entries.forEach { theme ->
            ThemeSwatch(theme, theme == current) { onSelect(theme) }
        }
    }
}

@Composable
private fun ThemeSwatch(theme: UrbanTheme, selected: Boolean, onClick: () -> Unit) {
    // Fondo del tema + acento: los acentos de Noir, Salón y Libreta son dorados muy parecidos por sí solos.
    val (previewBackground, previewAccent) = theme.previewColors()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(4.dp)
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(previewBackground)
                .border(BorderStroke(if (selected) 2.5.dp else 1.dp, if (selected) UrbanColors.Gold else UrbanColors.Line), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(previewAccent))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            theme.label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) UrbanColors.GoldText else UrbanColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(66.dp)
        )
    }
}
