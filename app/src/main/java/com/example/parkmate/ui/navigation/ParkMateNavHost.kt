package com.example.parkmate.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.parkmate.ParkMateViewModelFactory
import com.example.parkmate.background.TicketAlarmScheduler
import com.example.parkmate.data.location.DeviceLocationProvider
import com.example.parkmate.ui.components.RequestNotificationPermissionOnce
import com.example.parkmate.ui.history.HistoryScreen
import com.example.parkmate.ui.map.MapScreen
import com.example.parkmate.ui.map.SavedLocationsScreen
import com.example.parkmate.ui.settings.SettingsScreen
import com.example.parkmate.ui.stats.StatsScreen
import com.example.parkmate.ui.vehicles.VehiclesScreen

sealed class ParkMateDestination(val route: String) {
    // solo le 4 destinazioni della bottom bar hanno label + icona
    sealed class TopLevel(route: String, val label: String, val icon: ImageVector): ParkMateDestination(route) {
        object Map : TopLevel("map", "Mappa", Icons.Default.Map)
        object Vehicles : TopLevel("vehicles", "Veicoli", Icons.Default.DirectionsCar)
        object History : TopLevel("history", "Storico", Icons.Default.History)
        object Stats : TopLevel("stats", "Statistiche", Icons.Default.BarChart)
    }

    // schermate di dettaglio, pushate sopra la bottom bar, non incluse in bottomBarDestination
    object SavedLocations : ParkMateDestination("saved-locations")

    object Settings : ParkMateDestination("settings")
}

private val bottomBarDestinations = listOf(
    ParkMateDestination.TopLevel.Map,
    ParkMateDestination.TopLevel.Vehicles,
    ParkMateDestination.TopLevel.History,
    ParkMateDestination.TopLevel.Stats
)

// titolo + eventuale back button + eventuali azioni della top bar, in base alla rotta corrente
private data class TopBarConfig(
    val title: String,
    val onBack: (() -> Unit)? = null,
    val showManageSavedLocationsAction: Boolean = false,
    val showSettingsAction: Boolean = false
)

private fun topBarConfig(
    currentDestination: NavDestination?,
    navController: NavController
): TopBarConfig {
    val route = currentDestination?.route
    val topLevel = bottomBarDestinations.find { it.route == route }
    return when {
        topLevel != null -> TopBarConfig(
            title = topLevel.label,
            showManageSavedLocationsAction = topLevel == ParkMateDestination.TopLevel.Map,
            // solo sulle 4 destinazioni di primo livello: sulle rotte di dettaglio c'è già la
            // freccia indietro, e da lì nessuno cerca le impostazioni
            showSettingsAction = true
        )
        route == ParkMateDestination.SavedLocations.route -> TopBarConfig(
            title = "Saved location",
            onBack = { navController.navigateUp() }
        )
        route == ParkMateDestination.Settings.route -> TopBarConfig(
            title = "Impostazioni",
            onBack = { navController.navigateUp() }
        )
        else -> TopBarConfig(title = "")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkMateNavHost(
    viewModelFactory: ParkMateViewModelFactory,
    deviceLocationProvider: DeviceLocationProvider,
    ticketAlarmScheduler: TicketAlarmScheduler,
    openMapRequested: Boolean,
    onOpenMapHandled: () -> Unit,
    geofenceSavedLocationId: Long?,
    geofenceTransition: Int?,
    onGeofenceHandled: () -> Unit,
    ticketSessionId: Long?,
    onTicketSessionHandled: () -> Unit
) {
    // crea il controller di navigazione
    val navController = rememberNavController()
    // dove ci troviamo nell'app
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    // layer delle saved location sulla mappa
    var showSavedLocations by rememberSaveable { mutableStateOf(true) }

    // permesso di notifica, chiesto una sola volta all'avvio
    RequestNotificationPermissionOnce()

    // dalla notifica di riepilogo o di geofence: entrando dall'esterno, azzera lo stack e apre
    // la Mappa da sola, invece del solito scambio tra le tab della bottom bar
    LaunchedEffect(openMapRequested) {
        if (openMapRequested) {
            // svuota lo stack una voce alla volta, invece di affidarsi a popUpTo: si ferma da
            // sola quando resta solo la destinazione di partenza, senza poterla mai togliere
            while (navController.popBackStack()) {
                // continua finché c'è qualcosa da togliere
            }
            navController.navigate(ParkMateDestination.TopLevel.Map.route)
            onOpenMapHandled()
        }
    }

    Scaffold(
        topBar = {
            // calcola cosa mostrare in base alla rotta corrente
            val config = topBarConfig(currentDestination, navController)
            TopAppBar(
                title = { Text(config.title) },
                navigationIcon = {
                    config.onBack?.let { onBack ->
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                        }
                    }
                },
                actions = {
                    if (config.showManageSavedLocationsAction) {
                        IconButton(onClick = { showSavedLocations = !showSavedLocations }) {
                            Icon(
                                if (showSavedLocations) Icons.Default.LocationOn else Icons.Default.LocationOff,
                                contentDescription = if (showSavedLocations) "Nascondi saved location" else "Mostra saved location"
                            )
                        }
                        IconButton(onClick = { navController.navigate(ParkMateDestination.SavedLocations.route) }) {
                            Icon(Icons.Default.Layers, contentDescription = "Gestisci saved location")
                        }
                    }
                    if (config.showSettingsAction) {
                        IconButton(onClick = { navController.navigate(ParkMateDestination.Settings.route) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Impostazioni")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar { // barra di navigazione
                val currentRoute = currentDestination?.route

                bottomBarDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            // scarta le schermate di dettaglio sopra la tab corrente, senza salvarne lo stato
                            while (bottomBarDestinations.none { it.route == navController.currentDestination?.route }) {
                                navController.popBackStack()
                            }
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { // svuota il back stack fino alla destinazione iniziale, così non si accumulano schermate a ogni tocco della barra
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        // contenitore delle schermate navigabili (mostra la schermata scelta)
        NavHost(
            navController = navController,
            startDestination = ParkMateDestination.TopLevel.Vehicles.route,
            modifier = Modifier.padding(innerPadding)
        ) { // quale ui mostrare quando si naviga una certa route
            composable(ParkMateDestination.TopLevel.Map.route) {
                MapScreen(
                    viewModelFactory = viewModelFactory,
                    deviceLocationProvider = deviceLocationProvider,
                    ticketAlarmScheduler = ticketAlarmScheduler,
                    showSavedLocations = showSavedLocations,
                    geofenceSavedLocationId = geofenceSavedLocationId,
                    geofenceTransition = geofenceTransition,
                    onGeofenceHandled = onGeofenceHandled,
                    ticketSessionId = ticketSessionId,
                    onTicketSessionHandled = onTicketSessionHandled
                )
            }
            composable(ParkMateDestination.TopLevel.Vehicles.route) {
                VehiclesScreen(viewModelFactory = viewModelFactory)
            }
            composable(ParkMateDestination.TopLevel.History.route) {
                HistoryScreen(viewModelFactory = viewModelFactory)
            }
            composable(ParkMateDestination.TopLevel.Stats.route) {
                StatsScreen(viewModelFactory = viewModelFactory)
            }
            composable(ParkMateDestination.SavedLocations.route) {
                SavedLocationsScreen(
                    viewModelFactory = viewModelFactory,
                    deviceLocationProvider = deviceLocationProvider
                )
            }
            composable(ParkMateDestination.Settings.route) {
                SettingsScreen(viewModelFactory = viewModelFactory)
            }
        }
    }
}
