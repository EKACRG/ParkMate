package com.example.parkmate.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.parkmate.ParkMateViewModelFactory
import com.example.parkmate.ui.history.HistoryScreen
import com.example.parkmate.ui.map.MapScreen
import com.example.parkmate.ui.stats.StatsScreen
import com.example.parkmate.ui.vehicles.VehiclesScreen

sealed class ParkMateDestination(val route: String, val label: String, val icon: ImageVector) {
    object Map : ParkMateDestination("map", "Mappa", Icons.Default.Map)
    object Vehicles : ParkMateDestination("vehicles", "Veicoli", Icons.Default.DirectionsCar)
    object History : ParkMateDestination("history", "Storico", Icons.Default.History)
    object Stats : ParkMateDestination("stats", "Statistiche", Icons.Default.BarChart)
}

private val bottomBarDestinations = listOf(
    ParkMateDestination.Map,
    ParkMateDestination.Vehicles,
    ParkMateDestination.History,
    ParkMateDestination.Stats
)

@Composable
fun ParkMateNavHost(viewModelFactory: ParkMateViewModelFactory) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar { // barra di navigazione
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route

                bottomBarDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
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
            startDestination = ParkMateDestination.Vehicles.route,
            modifier = Modifier.padding(innerPadding)
        ) { // quale ui mostrare quando si naviga una certa route
            composable(ParkMateDestination.Map.route) { MapScreen() }
            composable(ParkMateDestination.Vehicles.route) {
                VehiclesScreen(viewModelFactory = viewModelFactory)
            }
            composable(ParkMateDestination.History.route) { HistoryScreen() }
            composable(ParkMateDestination.Stats.route) { StatsScreen() }
        }
    }
}
