package com.urbanblade.mobile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.R
import kotlinx.coroutines.delay

/**
 * Piezas compartidas por bienvenida, acceso, registro y recuperación. Todo sale de UrbanColors,
 * así que respeta los cuatro temas (incluido el claro).
 */

/**
 * Fondo editorial compartido por bienvenida y autenticación. La fotografía es un asset local
 * (nunca depende de red) y las capas oscuras mantienen el contraste de textos y formularios.
 */
@Composable
fun AuthBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(Color(0xFF090A0C))) {
        Image(
            painter = painterResource(R.drawable.auth_barbershop_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xB808090B),
                        0.42f to Color(0x8508090B),
                        0.74f to Color(0xB808090B),
                        1f to Color(0xF208090B)
                    )
                )
                .drawBehind {
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color(0xA8000000), Color.Transparent, Color(0x38000000))
                        )
                    )
                }
        )
        content()
    }
}

/** Entrada escalonada: cada bloque aparece con un pequeño retraso según su posición. */
@Composable
fun StaggerIn(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(70L * index)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(380, easing = EaseOutCubic)) +
            slideInVertically(tween(380, easing = EaseOutCubic)) { it / 6 }
    ) {
        content()
    }
}

/**
 * Campo de formulario con la etiqueta arriba (nunca solo un placeholder), mensaje de error junto
 * al campo, acciones de teclado y, si es contraseña, botón para mostrarla u ocultarla.
 */
@Composable
fun UrbanTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    placeholder: String? = null,
    error: String? = null,
    helper: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    isPassword: Boolean = false,
    onBlur: () -> Unit = {},
    /** Más de 1 = campo de varias líneas (p. ej. notas); el teclado conserva la tecla Enter. */
    minLines: Int = 1
) {
    var focused by remember { mutableStateOf(false) }
    var reveal by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val bringIntoView = remember { BringIntoViewRequester() }

    // Con el teclado abierto, el campo enfocado y su mensaje de error deben quedar visibles
    // (antes el aviso quedaba justo debajo del campo, tapado por el teclado).
    LaunchedEffect(focused) {
        if (focused) {
            delay(350)
            bringIntoView.bringIntoView()
        }
    }
    LaunchedEffect(error) {
        if (error != null && focused) {
            delay(120)
            bringIntoView.bringIntoView()
        }
    }

    val labelColor = when {
        error != null -> UrbanColors.Danger
        focused -> UrbanColors.Gold
        else -> UrbanColors.Muted
    }
    val trailing: (@Composable () -> Unit)? = if (isPassword) {
        {
            IconButton(onClick = { reveal = !reveal }) {
                Icon(
                    if (reveal) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    if (reveal) "Ocultar contraseña" else "Mostrar contraseña"
                )
            }
        }
    } else {
        null
    }

    Column(modifier.bringIntoViewRequester(bringIntoView)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (focused && !state.isFocused) onBlur()
                    focused = state.isFocused
                },
            placeholder = placeholder?.let { text -> { Text(text) } },
            leadingIcon = leadingIcon?.let { icon -> { Icon(icon, null) } },
            trailingIcon = trailing,
            singleLine = minLines == 1,
            minLines = minLines,
            isError = error != null,
            visualTransformation = if (isPassword && !reveal) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                capitalization = capitalization,
                keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = {
                    focusManager.clearFocus()
                    onImeAction?.invoke()
                },
                onGo = {
                    focusManager.clearFocus()
                    onImeAction?.invoke()
                }
            ),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = UrbanColors.Card,
                unfocusedContainerColor = UrbanColors.Card,
                errorContainerColor = UrbanColors.Card,
                focusedBorderColor = UrbanColors.Gold,
                unfocusedBorderColor = UrbanColors.Line,
                errorBorderColor = UrbanColors.Danger,
                cursorColor = UrbanColors.Gold,
                focusedTextColor = UrbanColors.Ink,
                unfocusedTextColor = UrbanColors.Ink,
                errorTextColor = UrbanColors.Ink,
                focusedLeadingIconColor = UrbanColors.Gold,
                unfocusedLeadingIconColor = UrbanColors.Muted,
                errorLeadingIconColor = UrbanColors.Danger,
                focusedTrailingIconColor = UrbanColors.Muted,
                unfocusedTrailingIconColor = UrbanColors.Muted,
                focusedPlaceholderColor = UrbanColors.Muted,
                unfocusedPlaceholderColor = UrbanColors.Muted
            )
        )
        val message = error ?: helper
        if (message != null) {
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) UrbanColors.Danger else UrbanColors.Muted,
                modifier = Modifier
                    .padding(start = 4.dp, top = 4.dp)
                    .semantics { if (error != null) liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}

private fun passwordScore(password: String): Int {
    if (password.length < 8) return 1
    var score = 1
    if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.length >= 12 || password.any { !it.isLetterOrDigit() }) score++
    return score.coerceAtMost(4)
}

/** Medidor de seguridad de contraseña: guía visual, el mínimo real (8 caracteres) lo valida el formulario. */
@Composable
fun PasswordStrengthMeter(password: String, modifier: Modifier = Modifier) {
    if (password.isEmpty()) return
    val score = passwordScore(password)
    val (label, color) = when (score) {
        1 -> "Débil" to UrbanColors.Danger
        2 -> "Aceptable" to UrbanColors.Warning
        3 -> "Buena" to UrbanColors.Info
        else -> "Fuerte" to UrbanColors.Success
    }
    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { index ->
                val segment by animateColorAsState(
                    if (index < score) color else UrbanColors.Line,
                    label = "strength"
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(segment)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Seguridad: $label",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

/** Acción principal de las pantallas de acceso: 56 dp, rebote al pulsar y vibración corta. */
@Composable
fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "press"
    )
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled && !loading,
        interactionSource = interaction,
        modifier = modifier
            .heightIn(min = 56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(18.dp),
        // Deshabilitado por formulario incompleto: gris con borde, claramente "aún no". Mientras carga
        // sigue dorado (solo está ocupado). Antes el dorado apagado parecía un botón roto.
        colors = ButtonDefaults.buttonColors(
            containerColor = UrbanColors.Gold,
            contentColor = UrbanColors.OnGold,
            // Igual que UrbanPrimaryButton: CardAlt y tinta al 75 % para que el botón se vea sin
            // confundirse con el fondo (auditoría del 24-sep: "parece defectuoso").
            disabledContainerColor = if (enabled || loading) UrbanColors.Gold else UrbanColors.CardAlt,
            disabledContentColor = if (enabled || loading) UrbanColors.OnGold else UrbanColors.Ink.copy(alpha = 0.75f)
        ),
        border = if (enabled || loading) null else androidx.compose.foundation.BorderStroke(1.dp, UrbanColors.Line)
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = UrbanColors.OnGold)
        } else {
            icon?.let {
                Icon(it, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}
