package com.urbanblade.mobile.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urbanblade.mobile.ui.theme.UrbanColors
import kotlinx.coroutines.delay

@Composable
fun UrbanReceiptAnimation(
    serviceName: String,
    total: Double,
    date: String,
    onAnimationEnd: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    // Riel del recibo saliendo de la impresora
    val transition = updateTransition(targetState = startAnimation, label = "receipt")
    
    val slideY by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 1200, easing = FastOutSlowInEasing) },
        label = "slideY"
    ) { state -> if (state) 0f else -300f }

    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 800, delayMillis = 400) },
        label = "alpha"
    ) { state -> if (state) 1f else 0f }

    LaunchedEffect(Unit) {
        delay(200) // Pausa breve antes de imprimir
        startAnimation = true
        delay(2500) // Espera a que termine de imprimir y que el usuario lo lea
        onAnimationEnd()
    }

    Box(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Papel del recibo animado (se desliza hacia abajo)
        Box(
            modifier = Modifier
                .offset(y = slideY.dp)
                .padding(top = 20.dp)
                .width(260.dp)
                .receiptShapeBackground(UrbanColors.Card)
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().alpha(alpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "URBANBLADE",
                    style = MaterialTheme.typography.titleLarge,
                    color = UrbanColors.Ink,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(16.dp))
                
                // Línea punteada
                Box(
                    modifier = Modifier.fillMaxWidth().height(1.dp).drawBehind {
                        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                        drawLine(
                            color = UrbanColors.Line,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                )
                
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "PAGO EXITOSO",
                    style = MaterialTheme.typography.labelMedium,
                    color = UrbanColors.Success
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = serviceName,
                    style = MaterialTheme.typography.titleMedium,
                    color = UrbanColors.Ink,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "${UrbanFormat.moneyShort(total)}",
                    style = MaterialTheme.typography.displaySmall,
                    color = UrbanColors.Gold,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Ranura de la impresora (siempre encima del recibo)
        Box(
            modifier = Modifier
                .width(280.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .background(UrbanColors.Ink) // Negro de la "impresora"
        )
    }
}

// Extensión para dibujar el borde zigzag del recibo en la parte inferior
private fun Modifier.receiptShapeBackground(color: Color): Modifier = this.drawBehind {
    val path = Path()
    path.moveTo(0f, 0f)
    path.lineTo(size.width, 0f)
    
    val toothCount = 15
    val toothWidth = size.width / toothCount
    val toothHeight = 10.dp.toPx()
    
    var currentX = size.width
    val bottomY = size.height
    
    path.lineTo(currentX, bottomY - toothHeight)
    
    for (i in 0 until toothCount) {
        currentX -= toothWidth / 2
        path.lineTo(currentX, bottomY)
        currentX -= toothWidth / 2
        path.lineTo(currentX, bottomY - toothHeight)
    }
    
    path.lineTo(0f, bottomY - toothHeight)
    path.close()
    
    drawPath(path = path, color = color, style = Fill)
}
