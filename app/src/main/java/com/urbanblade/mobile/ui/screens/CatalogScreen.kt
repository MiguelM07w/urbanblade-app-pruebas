package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.urbanblade.mobile.data.model.BarberItem
import com.urbanblade.mobile.data.model.ServiceItem
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.CatalogViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CatalogScreen(
    isGuest: Boolean = false,
    canFavorite: Boolean = true,
    onBook: (serviceId: String?, barberId: String?) -> Unit,
    onOpenStore: (() -> Unit)? = null,
    onOpenInspiration: (() -> Unit)? = null,
    onLogin: (() -> Unit)? = null,
    /** Abre la ficha del barbero (portafolio y reseñas); sin él, tocarlo lleva a reservar. */
    onOpenBarber: ((slug: String) -> Unit)? = null,
    vm: CatalogViewModel = viewModel()
) {
    val services by vm.services.collectAsState()
    val barbers by vm.barbers.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val favoriteBusy by vm.favoriteBusy.collectAsState()
    LaunchedEffect(Unit) { vm.load() }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<ServiceFilter?>(null) }
    // Solo se ofrecen los filtros que tienen algún servicio real en el catálogo.
    val filters = remember(services) { ServiceFilter.entries.filter { f -> services.any { serviceKind(it.nombre) in f.kinds } } }
    val visible = remember(services, query, filter) { filterServices(services, query, filter) }
    val filtering = query.isNotBlank() || filter != null

    LazyColumn(
        // Como invitado no hay Scaffold que reserve la barra de estado (AuthenticatedNav sí),
        // así que el contenido quedaba debajo del reloj y la barra de gestos.
        Modifier.fillMaxSize().then(if (isGuest) Modifier.systemBarsPadding() else Modifier),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = com.urbanblade.mobile.ui.components.BladebotClearance),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            UrbanPageHeader(
                title = "Reservar",
                subtitle = "Elige un servicio o un barbero y un horario real.",
                eyebrow = "Reserva"
            )
        }
        if (isGuest) {
            item {
                UrbanInfoBanner(
                    "Explora como invitado. Para confirmar una reserva, te pediremos iniciar sesión o crear tu cuenta.",
                    Icons.Default.Info
                )
            }
            if (onLogin != null) {
                item {
                    UrbanOutlineButton(
                        text = "Ya tengo cuenta, iniciar sesión",
                        onClick = onLogin,
                        icon = Icons.Default.Login,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        item {
            UrbanHeroCard {
                UrbanHeroLabel("¿Ya sabes qué quieres?")
                Spacer(Modifier.height(6.dp))
                Text("Reserva en menos de un minuto", style = MaterialTheme.typography.headlineSmall, color = UrbanColors.Ink)
                Text(
                    "${UrbanFormat.count(services.size, "servicio", "servicios")} · ${UrbanFormat.count(barbers.size, "barbero", "barberos")} · horarios reales",
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted
                )
                Spacer(Modifier.height(16.dp))
                UrbanPrimaryButton(
                    text = "Reservar ahora",
                    onClick = { onBook(null, null) },
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (loading && services.isEmpty()) item { UrbanSkeletonList(3) }
        if (error != null && services.isEmpty() && !loading) {
            item { UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar el catálogo", error, "Reintentar") { vm.load() } }
        } else {
            error?.let { item { UrbanErrorBanner(it) } }
        }

        if (services.isNotEmpty()) {
            // Buscador y filtros se quedan arriba al bajar: son 20 servicios en una sola lista.
            stickyHeader(key = "buscador") {
                Column(
                    Modifier.fillMaxWidth().background(UrbanColors.Background).padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UrbanTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = "Buscar servicio",
                        placeholder = "Fade, barba, keratina…",
                        leadingIcon = Icons.Default.Search,
                        imeAction = ImeAction.Done
                    )
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = filter == null,
                            onClick = { filter = null },
                            label = { Text("Todos") },
                            colors = catalogChipColors()
                        )
                        filters.forEach { f ->
                            FilterChip(
                                selected = filter == f,
                                onClick = { filter = if (filter == f) null else f },
                                label = { Text(f.label) },
                                colors = catalogChipColors()
                            )
                        }
                    }
                }
            }
        }
        item {
            UrbanSectionTitle(
                "Servicios",
                if (filtering) UrbanFormat.count(visible.size, "resultado", "resultados") else "Precio y duración reales; toca uno para reservar."
            )
        }
        items(visible, key = { it.id }) { service -> ServiceCard(service) { onBook(service.id, null) } }
        if (filtering && visible.isEmpty()) item {
            UrbanMascotState(
                UrbanStateKind.EMPTY,
                "No encontramos ese servicio",
                "Prueba con otra palabra o quita el filtro.",
                actionLabel = "Limpiar búsqueda",
                actionIcon = Icons.Default.Close,
                onAction = { query = ""; filter = null }
            )
        }
        if (!loading && error == null && services.isEmpty()) item { UrbanMascotState(UrbanStateKind.EMPTY, "Sin servicios disponibles", "Vuelve a intentarlo más tarde.") }

        item { UrbanSectionTitle("Nuestro equipo", "Conoce a los profesionales de UrbanBlade") }
        items(barbers, key = { it.id }) { barber ->
            val slug = barber.slug
            BarberCard(
                barber,
                showsProfile = onOpenBarber != null && slug != null,
                favoriteEnabled = !favoriteBusy,
                onFavorite = if (!isGuest && canFavorite) ({ vm.favorite(barber) }) else null,
                onOpen = { if (onOpenBarber != null && slug != null) onOpenBarber(slug) else onBook(null, barber.id) }
            )
        }
        if (!loading && barbers.isEmpty()) item { UrbanEmptyState("Sin barberos disponibles", null, Icons.Default.Groups) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun catalogChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = UrbanColors.Gold,
    selectedLabelColor = UrbanColors.OnGold
)

@Composable
private fun ServiceCard(service: ServiceItem, onBook: () -> Unit) {
    UrbanPremiumCard(Modifier.fillMaxWidth(), onClick = onBook) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Foto del servicio (la que sube el administrador); sin foto o si no carga, el ícono.
            var photoFailed by remember(service.imagen) { mutableStateOf(false) }
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = UrbanColors.Gold.copy(alpha = 0.09f),
                modifier = Modifier.size(64.dp),
                border = BorderStroke(1.dp, UrbanColors.Gold.copy(alpha = 0.21f))
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (!service.imagen.isNullOrBlank() && !photoFailed) {
                        AsyncImage(
                            model = service.imagen,
                            contentDescription = service.nombre,
                            contentScale = ContentScale.Crop,
                            onError = { photoFailed = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(serviceIcon(service.nombre), null, tint = UrbanColors.Gold)
                    }
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(service.nombre, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(3.dp))
                Text("${service.duracionMin} min", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                service.descripcion?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("${UrbanFormat.moneyShort(service.precio)}", style = MaterialTheme.typography.titleLarge, color = UrbanColors.Ink)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Reservar", style = MaterialTheme.typography.labelLarge, color = UrbanColors.GoldText)
                    Icon(Icons.Default.ChevronRight, null, tint = UrbanColors.Gold, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun BarberCard(barber: BarberItem, showsProfile: Boolean, onOpen: () -> Unit,
    favoriteEnabled: Boolean = true, onFavorite: (() -> Unit)? = null) {
    UrbanCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Iniciales debajo de la foto: si la foto falla o no hay, siguen visibles.
            UrbanAvatar(barber.user?.name ?: "Barbero", Modifier.size(58.dp), imageUrl = barber.foto)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(barber.user?.name ?: "Barbero", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                barber.especialidades?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                barber.avgRating?.let { avg ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "★ ${"%.1f".format(avg)} · ${UrbanFormat.count(barber.totalReviews, "reseña", "reseñas")}",
                        style = MaterialTheme.typography.labelMedium,
                        color = UrbanColors.Muted
                    )
                }
                barber.descripcion?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (onFavorite != null) {
                    IconToggleButton(checked = barber.esFavorito, enabled = favoriteEnabled,
                        onCheckedChange = { onFavorite() }) {
                        Icon(if (barber.esFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            if (barber.esFavorito) "Quitar a ${barber.user?.name ?: "este barbero"} de favoritos"
                            else "Elegir a ${barber.user?.name ?: "este barbero"} como favorito",
                            tint = UrbanColors.GoldText)
                    }
                } else Icon(if (showsProfile) Icons.Default.Person else Icons.Default.CalendarMonth, null, tint = UrbanColors.GoldText, modifier = Modifier.size(20.dp))
                Text(if (showsProfile) "Ver perfil" else "Reservar", style = MaterialTheme.typography.labelMedium, color = UrbanColors.GoldText)
            }
        }
    }
}
