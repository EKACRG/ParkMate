package com.example.parkmate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.parkmate.background.EXTRA_OPEN_MAP
import com.example.parkmate.ui.navigation.ParkMateNavHost
import com.example.parkmate.ui.theme.ParkMateTheme

class MainActivity : ComponentActivity() {

    // true quando questa istanza è stata aperta (o riusata) dalla notifica di riepilogo
    private var openMapRequested by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openMapRequested = intent.getBooleanExtra(EXTRA_OPEN_MAP, false)

        val parkMateApplication = application as ParkMateApplication
        val viewModelFactory = ParkMateViewModelFactory(
            parkMateApplication.vehicleRepository,
            parkMateApplication.savedLocationRepository,
            parkMateApplication.parkingSessionRepository
        )

        setContent {
            ParkMateTheme {
                ParkMateNavHost(
                    viewModelFactory = viewModelFactory,
                    deviceLocationProvider = parkMateApplication.deviceLocationProvider,
                    ticketAlarmScheduler = parkMateApplication.ticketAlarmScheduler,
                    openMapRequested = openMapRequested,
                    onOpenMapHandled = { openMapRequested = false }
                )
            }
        }
    }

    // con launchMode singleTop l'Activity viene riusata invece di ricreata: senza questo,
    // intent resterebbe quello del lancio precedente e l'extra andrebbe perso
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openMapRequested = intent.getBooleanExtra(EXTRA_OPEN_MAP, false)
    }
}
