package com.urbanblade.mobile.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.R
import com.urbanblade.mobile.ui.theme.UrbanColors
import kotlinx.coroutines.delay

/**
 * Burbuja flotante de Bladebot, como la de la web: siempre a la mano en las pantallas principales
 * y abre el chat al tocarla. Al aparecer muestra un saludo corto que se oculta solo; el halo
 * dorado late despacio y se queda quieto si el sistema pide reducir animaciones.
 */
@Composable
fun BladebotBubble(onOpen: () -> Unit, modifier: Modifier = Modifier, hint: String = "¿Te ayudo a reservar?") {
    val reduceMotion = rememberReducedMotion()
    var showHint by remember { mutableStateOf(false) }
    // El saludo sale una sola vez por sesión: la burbuja se esconde y reaparece al desplazarse.
    LaunchedEffect(Unit) {
        if (hintShownThisSession) return@LaunchedEffect
        hintShownThisSession = true
        delay(900)
        showHint = true
        delay(4500)
        showHint = false
    }

    val pulse = if (reduceMotion) 1f else {
        val transition = rememberInfiniteTransition(label = "bladebot")
        val value by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "halo"
        )
        value
    }

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.animation.AnimatedVisibility(visible = showHint) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = UrbanColors.Card,
                shadowElevation = 6.dp,
                modifier = Modifier.padding(end = 10.dp).border(1.dp, UrbanColors.Line, MaterialTheme.shapes.medium)
            ) {
                Text(
                    hint,
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = UrbanColors.Ink
                )
            }
        }
        Box(contentAlignment = Alignment.Center) {
            // Halo dorado detrás de la burbuja.
            Box(
                Modifier
                    .size(64.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(UrbanColors.Gold.copy(alpha = 0.18f))
            )
            Surface(
                onClick = onOpen,
                shape = CircleShape,
                color = UrbanColors.Surface,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .size(58.dp)
                    .border(2.dp, UrbanColors.Gold, CircleShape)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Abrir Bladebot, el asistente de UrbanBlade"
                    }
            ) {
                Image(
                    painterResource(R.drawable.mascot_bladebot_welcome),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().padding(4.dp).clip(CircleShape)
                )
            }
        }
    }
}

private var hintShownThisSession = false

/** true si el usuario desactivó las animaciones del sistema (escala de animación en 0). */
@Composable
private fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}

/** Espacio al final de las listas de las pantallas principales para que la burbuja no tape lo último. */
val BladebotClearance = 96.dp
