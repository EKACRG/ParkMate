package com.example.parkmate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.parkmate.background.EXTRA_GEOFENCE_SAVED_LOCATION_ID
import com.example.parkmate.background.EXTRA_GEOFENCE_TRANSITION
import com.example.parkmate.background.EXTRA_OPEN_MAP
import com.example.parkmate.ui.navigation.ParkMateNavHost
import com.example.parkmate.ui.theme.ParkMateTheme

class MainActivity : ComponentActivity() {

    // true quando questa istanza è stata aperta (o riusata) dalla notifica di riepilogo
    private var openMapRequested by mutableStateOf(false)
    // non null quando l'istanza è stata aperta da una notifica di geofence
    private var geofenceSavedLocationId by mutableStateOf<Long?>(null)
    private var geofenceTransition by mutableStateOf<Int?>(null)

    // letti sia in onCreate sia in onNewIntent: stessa lettura, due punti di ingresso diversi
    private fun readIntentExtras(intent: Intent) {
        openMapRequested = intent.getBooleanExtra(EXTRA_OPEN_MAP, false)
        geofenceSavedLocationId = intent.takeIf { it.hasExtra(EXTRA_GEOFENCE_SAVED_LOCATION_ID) }
            ?.getLongExtra(EXTRA_GEOFENCE_SAVED_LOCATION_ID, -1L)
        geofenceTransition = intent.takeIf { it.hasExtra(EXTRA_GEOFENCE_TRANSITION) }
            ?.getIntExtra(EXTRA_GEOFENCE_TRANSITION, -1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readIntentExtras(intent)

        val parkMateApplication = application as ParkMateApplication
        val viewModelFactory = ParkMateViewModelFactory(
            parkMateApplication.vehicleRepository,
            parkMateApplication.savedLocationRepository,
            parkMateApplication.parkingSessionRepository,
            parkMateApplication.ticketAlarmScheduler,
            parkMateApplication.activeSessionReminderScheduler
        )

        setContent {
            ParkMateTheme {
                ParkMateNavHost(
                    viewModelFactory = viewModelFactory,
                    deviceLocationProvider = parkMateApplication.deviceLocationProvider,
                    ticketAlarmScheduler = parkMateApplication.ticketAlarmScheduler,
                    openMapRequested = openMapRequested,
                    onOpenMapHandled = { openMapRequested = false },
                    geofenceSavedLocationId = geofenceSavedLocationId,
                    geofenceTransition = geofenceTransition,
                    onGeofenceHandled = {
                        geofenceSavedLocationId = null
                        geofenceTransition = null
                    }
                )
            }
        }
    }

    // con launchMode singleTop l'Activity viene riusata invece di ricreata: senza questo,
    // intent resterebbe quello del lancio precedente e l'extra andrebbe perso
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntentExtras(intent)
    }
}
