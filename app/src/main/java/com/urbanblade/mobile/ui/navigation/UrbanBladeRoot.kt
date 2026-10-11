package com.urbanblade.mobile.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.urbanblade.mobile.core.booking.PendingBooking
import com.urbanblade.mobile.core.push.PushRegistrationEffect
import kotlinx.coroutines.launch
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.components.UrbanBladeBackground
import com.urbanblade.mobile.ui.components.UrbanBrandMark
import com.urbanblade.mobile.ui.screens.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.AuthState
import com.urbanblade.mobile.ui.viewmodel.AuthViewModel

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

private const val BOOKING_ROUTE_PATTERN = "booking?serviceId={serviceId}&barberId={barberId}"

private fun bookingRoute(serviceId: String?, barberId: String?): String {
    val params = buildList {
        serviceId?.let { add("serviceId=$it") }
        barberId?.let { add("barberId=$it") }
    }
    return if (params.isEmpty()) "booking" else "booking?" + params.joinToString("&")
}

/**
 * Abre la reserva reemplazando la que ya estuviera abierta. Antes cada «Reservar» (Bladebot, muro,
 * catálogo) apilaba otra reserva encima y había que salir de una en una.
 */
private fun NavController.openBooking(route: String) = navigate(route) {
    popUpTo(BOOKING_ROUTE_PATTERN) { inclusive = true }
}

@Composable
fun UrbanBladeRoot(authViewModel: AuthViewModel = viewModel()) {
    val authState by authViewModel.state.collectAsState()
    when (val state = authState) {
        AuthState.Loading -> UrbanBladeBackground {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                UrbanBrandMark()
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator(color = UrbanColors.Gold, strokeWidth = 2.dp)
                Spacer(Modifier.height(12.dp))
                Text("Preparando tu espacio…", color = UrbanColors.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        AuthState.Guest -> GuestNav(authViewModel)
        is AuthState.Authenticated -> {
            PushRegistrationEffect(state.user.id)
            // key: si cambia la cuenta, toda la navegación y sus pantallas se crean de cero.
            key(state.user.id) { AuthenticatedNav(state.user, authViewModel) }
        }
    }
}

@Composable
private fun GuestNav(authViewModel: AuthViewModel) {
    val nav = rememberNavController()
    // Sin Scaffold/fondo propio (AuthenticatedNav sí lo tiene), el catálogo de invitado salía sobre gris
    // y con texto negro sin color explícito (nombres de servicio ilegibles): se le da fondo y color base.
    UrbanBladeBackground {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides UrbanColors.Ink
        ) {
    NavHost(navController = nav, startDestination = "welcome") {
        composable("welcome") {
            val sessionExpired by authViewModel.sessionExpired.collectAsState()
            com.urbanblade.mobile.ui.screens.WelcomeScreen(
                onRegister = { nav.navigate("register") },
                onLogin = { nav.navigate("login") },
                sessionExpired = sessionExpired
            )
        }
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onRegister = { nav.navigate("register") },
                onForgot = { nav.navigate("forgot") },
                onBack = { nav.popBackStack() }
            )
        }
        composable("register") {
            RegisterScreen(
                authViewModel = authViewModel,
                onBack = { nav.popBackStack() },
                onLogin = { nav.navigate("login") { popUpTo("register") { inclusive = true } } }
            )
        }
        composable("forgot") { ForgotPasswordScreen(authViewModel = authViewModel, onBack = { nav.popBackStack() }) }
    }
        }
    }
}

@Composable
private fun AuthenticatedNav(user: AuthUser, authViewModel: AuthViewModel) {
    val nav = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val isClient = user.roles.contains("cliente")
    val clientOnly = isClient && user.roles.none { it in listOf("administrador", "recepcionista", "barbero", "ingeniero") }
    val engineerOnly = user.roles.contains("ingeniero") &&
        user.roles.none { it in listOf("administrador", "recepcionista", "barbero", "cliente") }

    // Nav por rol. Cliente (propuesta A, 25-sep): cuatro pestañas con un solo verbo para
    // reservar; Beneficios, pagos y pedidos viven en Cuenta y en el Inicio, ya no en la barra.
    // Cada pestaña se llama igual que el título de su pantalla.
    val items = if (isClient) {
        listOf(
            NavItem("home", "Inicio", Icons.Default.Home),
            NavItem("catalog", "Reservar", Icons.Default.ContentCut),
            NavItem("appointments", "Mis citas", Icons.Default.CalendarMonth),
            NavItem("profile", "Cuenta", Icons.Default.Person)
        )
    } else {
        buildList {
            add(NavItem("home", "Inicio", Icons.Default.Home))
            if (!engineerOnly) add(NavItem("appointments", "Citas", Icons.Default.CalendarMonth))
            add(NavItem("more", "Más", Icons.Default.GridView))
            add(NavItem("profile", "Cuenta", Icons.Default.Person))
        }
    }
    val rootRoutes = items.map { it.route }

    // Selección hecha como invitado en el catálogo (ver GuestNav): se
    // consume UNA sola vez, al montar la sesión autenticada, y redirige
    // directo al wizard de reserva con esa preselección.
    LaunchedEffect(Unit) {
        val (pendingService, pendingBarber) = PendingBooking.consume()
        if (pendingService != null || pendingBarber != null) {
            nav.openBooking(bookingRoute(pendingService, pendingBarber))
        }
    }

    // Notificación tocada (push): abre la pantalla indicada una sola vez. Si llegó antes de iniciar
    // sesión, espera aquí a que exista la navegación autenticada.
    val pushRoute by com.urbanblade.mobile.core.push.PushDeepLink.pending.collectAsState()
    LaunchedEffect(pushRoute) {
        val route = pushRoute ?: return@LaunchedEffect
        com.urbanblade.mobile.core.push.PushDeepLink.consume()
        // Se vacía la pila hasta Inicio: «atrás» desde el aviso regresa a Inicio, no a otro aviso abierto antes.
        if (route != "home") nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id)
            launchSingleTop = true
        }
    }

    var bubbleShown by remember { mutableStateOf(true) }
    LaunchedEffect(current) {
        bubbleShown = true
        // Vista de pantalla para Analytics: la ruta con sus parámetros como plantilla ({slug}), sin IDs.
        current?.let { com.urbanblade.mobile.core.analytics.UrbanAnalytics.screen(it) }
    }
    // Remote Config: la burbuja se puede apagar desde la consola de Firebase sin publicar otro APK.
    val bubbleEnabled by com.urbanblade.mobile.core.config.UrbanRemoteConfig.bladebotBubble.collectAsState()
    val bubbleScroll = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (available.y < -6f) bubbleShown = false else if (available.y > 6f) bubbleShown = true
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    Scaffold(
        containerColor = UrbanColors.Background,
        // Entrar sin red no cierra la sesión: se avisa arriba y el contenido baja solo. Cada pantalla
        // conserva su propio estado con "Reintentar", para que se pueda seguir usando lo guardado.
        topBar = {
            val offline by authViewModel.offline.collectAsState()
            if (offline) OfflineBanner(onRetry = { authViewModel.bootstrap() })
        },
        // Bladebot como en la web: una burbuja en las pantallas principales que abre el chat.
        // Se esconde al bajar (para no tapar lo que se está leyendo) y vuelve al subir o al cambiar de pantalla.
        floatingActionButton = {
            androidx.compose.animation.AnimatedVisibility(
                visible = current in rootRoutes && bubbleShown && bubbleEnabled,
                enter = androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut()
            ) {
                com.urbanblade.mobile.ui.components.BladebotBubble(onOpen = {
                    com.urbanblade.mobile.core.analytics.UrbanAnalytics.log("bladebot_abierto", "origen" to current)
                    nav.navigate("chatbot")
                })
            }
        },
        bottomBar = {
            if (current in rootRoutes) {
                Surface(
                    color = UrbanColors.Surface,
                    shadowElevation = 18.dp,
                    tonalElevation = 0.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
                        items.forEach { item ->
                            NavigationBarItem(
                                selected = current == item.route,
                                onClick = {
                                    nav.navigate(item.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(item.icon, item.label) },
                                label = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = UrbanColors.OnGold,
                                    selectedTextColor = UrbanColors.Gold,
                                    indicatorColor = UrbanColors.Gold,
                                    unselectedIconColor = UrbanColors.Muted,
                                    unselectedTextColor = UrbanColors.Muted
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        // consumeWindowInsets: el Scaffold externo ya reservó barra de estado y barra inferior; sin
        // marcarlos como consumidos, cada pantalla con su propio Scaffold/TopAppBar los sumaba otra vez
        // (hueco vacío sobre el título en todas las pantallas secundarias).
        UrbanBladeBackground(Modifier.padding(padding).consumeWindowInsets(padding).nestedScroll(bubbleScroll)) {
            NavHost(navController = nav, startDestination = "home", modifier = Modifier.fillMaxSize()) {
                composable("home") {
                    DashboardScreen(
                        user = user,
                        onAppointments = { nav.navigate("appointments") },
                        onBook = { nav.openBooking("booking") },
                        onWallet = { nav.navigate("wallet") },
                        onStore = { nav.navigate("store") },
                        onExplore = { nav.navigate("catalog") },
                        onNavigate = { route -> nav.navigate(route) }
                    )
                }
                composable("appointments") {
                    // El cliente tiene su propia vista (Próximas / Historial); el personal, la agenda del negocio.
                    if (clientOnly) {
                        ClientAppointmentsScreen(onBook = { nav.openBooking("booking") }, onNavigate = { nav.navigate(it) })
                    } else {
                        AppointmentsScreen(user = user, onBook = { nav.openBooking("booking") })
                    }
                }
                composable(
                    BOOKING_ROUTE_PATTERN,
                    arguments = listOf(
                        navArgument("serviceId") { type = NavType.StringType; nullable = true; defaultValue = null },
                        navArgument("barberId") { type = NavType.StringType; nullable = true; defaultValue = null }
                    )
                ) { entry ->
                    BookingScreen(
                        initialServiceId = entry.arguments?.getString("serviceId"),
                        initialBarberId = entry.arguments?.getString("barberId"),
                        onBack = { nav.popBackStack() },
                        onCreated = {
                            nav.navigate("appointments") {
                                popUpTo(BOOKING_ROUTE_PATTERN) { inclusive = true }
                            }
                        }
                    )
                }
                composable("catalog") {
                    CatalogScreen(
                        onBook = { serviceId, barberId -> nav.openBooking(bookingRoute(serviceId, barberId)) },
                        onOpenStore = { nav.navigate("store") },
                        onOpenInspiration = { nav.navigate("social") },
                        onOpenBarber = { slug -> nav.navigate("barber_profile/$slug") }
                    )
                }
                composable(
                    "barber_profile/{slug}",
                    arguments = listOf(navArgument("slug") { type = NavType.StringType })
                ) { entry ->
                    BarberProfileScreen(
                        slug = entry.arguments?.getString("slug").orEmpty(),
                        canBook = isClient,
                        onBack = { nav.popBackStack() },
                        onBook = { barberId -> nav.openBooking(bookingRoute(null, barberId)) }
                    )
                }
                composable("wallet") {
                    WalletScreen(onBack = { nav.popBackStack() }, onBook = { nav.openBooking("booking") })
                }
                composable("store") { StoreScreen(user = user, onOrders = { nav.navigate("orders") }) }
                composable("orders") { OrdersScreen(user = user, onBack = { nav.popBackStack() }) }
                composable("payments") { PaymentsScreen(user = user, onBack = { nav.popBackStack() }) }
                composable("notifications") {
                    NotificationsScreen(onBack = { nav.popBackStack() }, onOpen = { route -> nav.navigate(route) { launchSingleTop = true } })
                }
                composable("chatbot") { ChatbotScreen(onBack = { nav.popBackStack() }, onBook = { serviceId ->
                    com.urbanblade.mobile.core.analytics.UrbanAnalytics.log("bladebot_reservar")
                    nav.openBooking(bookingRoute(serviceId, null))
                }) }
                composable("more") { MoreScreen(user = user, onNavigate = { nav.navigate(it) }) }
                composable("profile") {
                    com.urbanblade.mobile.ui.account.AccountScreen(
                        user = user,
                        onLogout = {
                            // Antes de cerrar sesión el cel olvida su token de push: la siguiente cuenta
                            // que entre en este equipo no hereda los avisos de esta.
                            scope.launch {
                                com.urbanblade.mobile.core.push.PushNotifications.forgetDeviceToken(context)
                                authViewModel.logout()
                            }
                        },
                        onNavigate = { nav.navigate(it) },
                        onUserChanged = { authViewModel.refreshUser() }
                    )
                }

                composable("analytics") { AnalyticsScreen(onBack = { nav.popBackStack() }) }
                composable("social") {
                    SocialFeedScreen(
                        onBack = { nav.popBackStack() },
                        // Solo el cliente reserva desde el muro; el personal lo ve como galería.
                        onBookBarber = if (isClient) { barberId -> nav.openBooking(bookingRoute(null, barberId)) } else null
                    )
                }
                composable("clients") {
                    ClientsListScreen(
                        user = user,
                        onClientClick = { id -> nav.navigate("client_detail/$id") },
                        onBack = { nav.popBackStack() }
                    )
                }
                composable(
                    "client_detail/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { backStackEntry ->
                    ClientDetailScreen(
                        user = user,
                        clientId = backStackEntry.arguments?.getString("id").orEmpty(),
                        onBack = { nav.popBackStack() }
                    )
                }
                composable("inventory") { InventoryListScreen(user = user, onBack = { nav.popBackStack() }, onHistory = { nav.navigate("inventory_movements") }) }
                composable("cash") { CashCloseScreen(onBack = { nav.popBackStack() }) }
                composable("inventory_movements") { InventoryMovementsScreen(onBack = { nav.popBackStack() }) }
                composable("waitlist_staff") { WaitlistStaffScreen(onBack = { nav.popBackStack() }) }
                composable("gift_cards") { GiftCardsStaffScreen(onBack = { nav.popBackStack() }) }
                composable("offers_admin") { OffersAdminScreen(onBack = { nav.popBackStack() }) }
                composable("barbers_admin") { BarbersAdminScreen(onBack = { nav.popBackStack() }) }
                composable("services_admin") { ServicesAdminScreen(onBack = { nav.popBackStack() }) }
                composable("barber_agenda") { BarberAgendaScreen(onBack = { nav.popBackStack() }) }
                composable("barber_portfolio") { BarberPortfolioScreen(onBack = { nav.popBackStack() }) }
                composable("barber_schedule") { BarberScheduleScreen(onBack = { nav.popBackStack() }) }
                composable("reports") { ReportsScreen(onBack = { nav.popBackStack() }) }
                composable("logs") { LogsScreen(onBack = { nav.popBackStack() }) }
                composable("admin_metrics") { AdminMetricsScreen(onBack = { nav.popBackStack() }) }
                composable("insights") { InsightsScreen(onBack = { nav.popBackStack() }) }
                composable("campaigns") { CampaignsScreen(onBack = { nav.popBackStack() }) }
                composable("raffles") { RafflesScreen(onBack = { nav.popBackStack() }) }
                composable("reviews") { ReviewsScreen(onBack = { nav.popBackStack() }) }
                composable("users") { UsersScreen(currentUserId = user.id, onBack = { nav.popBackStack() }) }
                composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
                composable("system") { SystemStatusScreen(onBack = { nav.popBackStack() }) }
            }
        }
    }
}

/**
 * Aviso de que se entró sin poder confirmar la sesión con el servidor (red caída o error suyo).
 * La sesión sigue viva: esto solo lo dice, y el botón reintenta confirmarla.
 */
@Composable
private fun OfflineBanner(onRetry: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(UrbanColors.Warning.copy(alpha = 0.16f))
            // El Scaffold no reserva el inset para su topBar: sin esto el aviso queda debajo del
            // reloj y de los iconos del sistema (y el toque de "Reintentar" no llega).
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CloudOff, null, tint = UrbanColors.Warning, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Sin conexión: estás viendo lo guardado.",
            style = MaterialTheme.typography.bodySmall,
            color = UrbanColors.Ink,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRetry) {
            Text("Reintentar", color = UrbanColors.Gold, fontWeight = FontWeight.SemiBold)
        }
    }
}
