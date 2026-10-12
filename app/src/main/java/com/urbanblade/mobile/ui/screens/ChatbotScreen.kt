package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanHeroCard
import com.urbanblade.mobile.ui.components.UrbanHeroLabel
import com.urbanblade.mobile.ui.components.UrbanPageHeader
import com.urbanblade.mobile.ui.components.UrbanSkeletonList
import com.urbanblade.mobile.ui.components.UrbanTopBar
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.theme.MascotMood
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.theme.mascot
import com.urbanblade.mobile.ui.viewmodel.ChatBubble
import com.urbanblade.mobile.ui.viewmodel.ChatbotViewModel

/**
 * Preguntas rápidas: temas que el backend reconoce por palabra clave (precio, horario,
 * ubicación, pago, recomendación, barbero; ver ChatbotController::detectIntent en barber).
 */
private val SUGGESTIONS = listOf(
    "¿Cuánto cuesta un corte?",
    "¿Qué horario tienen?",
    "¿Dónde están?",
    "¿Cómo puedo pagar?",
    "¿Qué me recomiendas?",
    "¿Quiénes son los barberos?"
)

/**
 * Bladebot con el patrón de la app: bienvenida con la mascota, preguntas rápidas, el historial
 * guardado al volver, "¿te sirvió?" en cada respuesta y "Nueva conversación".
 */
@Composable
fun ChatbotScreen(onBack: () -> Unit, onBook: (serviceId: String) -> Unit = {}, vm: ChatbotViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var text by rememberSaveable { mutableStateOf("") }
    var confirmNew by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val send = { if (text.isNotBlank()) { vm.send(text); text = "" } }

    LaunchedEffect(Unit) { vm.loadHistory() }
    // Siempre a la vista lo último: la respuesta nueva o el "escribiendo…".
    LaunchedEffect(state.messages.size, state.typing) {
        val last = listState.layoutInfo.totalItemsCount - 1
        if (last > 0) listState.animateScrollToItem(last)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            UrbanTopBar("", onBack) {
                if (state.messages.isNotEmpty()) {
                    IconButton(onClick = { confirmNew = true }) { Icon(Icons.Default.AddComment, "Nueva conversación", tint = UrbanColors.Gold) }
                }
            }
        },
        bottomBar = {
            Surface(color = UrbanColors.Surface, border = BorderStroke(1.dp, UrbanColors.Line), modifier = Modifier.imePadding()) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    if (state.messages.isNotEmpty()) {
                        SuggestionRow(enabled = !state.typing) { vm.send(it) }
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it.take(2000) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Pregúntale a Bladebot…") },
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { send() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = UrbanColors.Card,
                                unfocusedContainerColor = UrbanColors.Card,
                                focusedBorderColor = UrbanColors.Gold,
                                unfocusedBorderColor = UrbanColors.Line,
                                cursorColor = UrbanColors.Gold,
                                focusedTextColor = UrbanColors.Ink,
                                unfocusedTextColor = UrbanColors.Ink,
                                focusedPlaceholderColor = UrbanColors.Muted,
                                unfocusedPlaceholderColor = UrbanColors.Muted
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = send,
                            enabled = text.isNotBlank() && !state.typing,
                            modifier = Modifier.size(52.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = UrbanColors.Gold,
                                contentColor = UrbanColors.OnGold,
                                // Igual que los botones del kit: CardAlt + tinta al 75 % para que se
                                // vea el control cuando está apagado (antes se perdía en el fondo).
                                disabledContainerColor = UrbanColors.CardAlt,
                                disabledContentColor = UrbanColors.Ink.copy(alpha = 0.75f)
                            )
                        ) { Icon(Icons.AutoMirrored.Filled.Send, "Enviar") }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            state = listState,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                UrbanPageHeader(title = "Bladebot", subtitle = "Tu asistente de UrbanBlade, a cualquier hora.", eyebrow = "Asistente")
            }
            when {
                state.loadingHistory && state.messages.isEmpty() -> item { UrbanSkeletonList(2) }
                state.messages.isEmpty() -> item { WelcomeCard(enabled = !state.typing) { vm.send(it) } }
            }
            items(state.messages, key = { it.id }) { bubble ->
                ChatBubbleRow(bubble, onRate = { helpful -> vm.rate(bubble, helpful) }, onBook = onBook)
            }
            if (state.typing) item { TypingBubble() }
            state.error?.let { item { UrbanErrorBanner(it) } }
        }
    }

    if (confirmNew) {
        AlertDialog(
            onDismissRequest = { confirmNew = false },
            containerColor = UrbanColors.Card,
            title = { Text("¿Empezar una conversación nueva?") },
            text = { Text("Se borra esta conversación y lo que Bladebot recuerda de ella. No se puede deshacer.", color = UrbanColors.Muted) },
            confirmButton = { TextButton(onClick = { confirmNew = false; vm.newConversation() }) { Text("Sí, empezar de nuevo", color = UrbanColors.Danger) } },
            dismissButton = { TextButton(onClick = { confirmNew = false }) { Text("Volver") } }
        )
    }
}

@Composable
private fun WelcomeCard(enabled: Boolean, onAsk: (String) -> Unit) {
    UrbanHeroCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                UrbanHeroLabel("Hola, soy Bladebot")
                Spacer(Modifier.height(6.dp))
                Text("Pregúntame lo que quieras", style = MaterialTheme.typography.headlineSmall, color = UrbanColors.Ink)
                Spacer(Modifier.height(4.dp))
                Text("Precios, horarios, cómo llegar, formas de pago o qué corte te va.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            }
            Image(
                painter = painterResource(UrbanColors.current.mascot(MascotMood.WELCOME)),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(96.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text("Prueba con:", style = MaterialTheme.typography.labelMedium, color = UrbanColors.GoldText)
        Spacer(Modifier.height(6.dp))
        SuggestionRow(enabled = enabled, onAsk = onAsk)
    }
}

@Composable
private fun SuggestionRow(enabled: Boolean, onAsk: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SUGGESTIONS.forEach { suggestion ->
            SuggestionChip(
                onClick = { onAsk(suggestion) },
                enabled = enabled,
                label = { Text(suggestion) },
                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = UrbanColors.Card, labelColor = UrbanColors.Ink),
                border = SuggestionChipDefaults.suggestionChipBorder(enabled, borderColor = UrbanColors.Gold.copy(alpha = 0.4f))
            )
        }
    }
}

@Composable
private fun ChatBubbleRow(bubble: ChatBubble, onRate: (Boolean) -> Unit, onBook: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (bubble.fromUser) Alignment.End else Alignment.Start) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp, topEnd = 18.dp,
                bottomStart = if (bubble.fromUser) 18.dp else 5.dp,
                bottomEnd = if (bubble.fromUser) 5.dp else 18.dp
            ),
            color = if (bubble.fromUser) UrbanColors.Gold else UrbanColors.Card,
            contentColor = if (bubble.fromUser) UrbanColors.OnGold else UrbanColors.Ink,
            border = if (bubble.fromUser) null else BorderStroke(1.dp, UrbanColors.Line)
        ) {
            Text(bubble.text, Modifier.padding(horizontal = 14.dp, vertical = 11.dp), style = MaterialTheme.typography.bodyMedium)
        }
        bubble.suggestion?.let { service ->
            Spacer(Modifier.height(6.dp))
            Surface(
                onClick = { onBook(service.id) },
                shape = RoundedCornerShape(50),
                color = UrbanColors.Gold,
                contentColor = UrbanColors.OnGold,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContentCut, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Reservar ${service.nombre} · ${UrbanFormat.moneyShort(service.precio)}",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (!bubble.fromUser && bubble.question != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                if (bubble.helpful == null) {
                    Text("¿Te sirvió?", style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted)
                    IconButton(onClick = { onRate(true) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.ThumbUp, "Me sirvió", tint = UrbanColors.Muted, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { onRate(false) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.ThumbDown, "No me sirvió", tint = UrbanColors.Muted, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Icon(
                        if (bubble.helpful) Icons.Default.ThumbUp else Icons.Default.ThumbDown,
                        null,
                        tint = UrbanColors.Gold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Gracias, así Bladebot aprende.", style = MaterialTheme.typography.labelSmall, color = UrbanColors.Muted)
                }
            }
        }
    }
}

@Composable
private fun TypingBubble() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(UrbanColors.current.mascot(MascotMood.DEFAULT)),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = UrbanColors.Gold)
        Spacer(Modifier.width(8.dp))
        Text("Bladebot está escribiendo…", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
    }
}
