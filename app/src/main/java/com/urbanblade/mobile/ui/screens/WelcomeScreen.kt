package com.urbanblade.mobile.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.components.AuthBackdrop
import com.urbanblade.mobile.ui.components.AuthPrimaryButton
import com.urbanblade.mobile.ui.components.StaggerIn
import com.urbanblade.mobile.ui.components.UrbanBrandMark
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
import com.urbanblade.mobile.ui.theme.MascotMood
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.theme.mascot

/**
 * Primera pantalla para quien aún no tiene sesión: identidad de marca y dos caminos claros
 * (crear cuenta o entrar). Si la sesión anterior venció, lo avisa.
 */
@Composable
fun WelcomeScreen(onRegister: () -> Unit, onLogin: () -> Unit, sessionExpired: Boolean = false) {
    // La mascota "flota" muy despacio; con "Quitar animaciones" del sistema se queda quieta.
    val transition = rememberInfiniteTransition(label = "float")
    val bob by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )

    AuthBackdrop {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.Start
        ) {
            StaggerIn(0) { UrbanBrandMark() }
            Spacer(Modifier.height(26.dp))

            StaggerIn(1) {
                Text(
                    "MÁS QUE UN CORTE",
                    style = MaterialTheme.typography.labelMedium,
                    color = UrbanColors.Gold,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))

            Box(Modifier.fillMaxWidth().weight(1f)) {
                StaggerIn(2, Modifier.align(Alignment.TopStart)) {
                    Column(Modifier.fillMaxWidth(0.76f)) {
                        Text(
                            "Tu estilo. Tu tiempo.",
                            style = MaterialTheme.typography.displaySmall,
                            color = UrbanColors.Ink,
                            modifier = Modifier.semantics { heading() }
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Reserva con tu barbero favorito, compra lo mejor y disfruta de tus beneficios.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = UrbanColors.Muted,
                        )
                    }
                }
                StaggerIn(3, Modifier.align(Alignment.BottomEnd)) {
                    Image(
                        painter = painterResource(UrbanColors.current.mascot(MascotMood.WELCOME)),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            // 220 dp y más pegada a las acciones: con 300 dp y 88 dp de aire la
                            // composición dejaba un hueco (auditoría del 24-sep, P1).
                            .padding(bottom = 36.dp)
                            .size(220.dp)
                            .graphicsLayer { translationY = bob.dp.toPx() }
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            if (sessionExpired) {
                UrbanInfoBanner("Tu sesión venció. Inicia sesión de nuevo para continuar.", Icons.Default.Lock)
                Spacer(Modifier.height(12.dp))
            }

            StaggerIn(4) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AuthPrimaryButton(
                        text = "Crear mi cuenta",
                        onClick = onRegister,
                        icon = Icons.Default.PersonAdd,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = onLogin,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    ) {
                        Text("Iniciar sesión", color = UrbanColors.Gold, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.size(8.dp))
                        androidx.compose.material3.Icon(Icons.Default.Login, contentDescription = null, tint = UrbanColors.Gold)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
