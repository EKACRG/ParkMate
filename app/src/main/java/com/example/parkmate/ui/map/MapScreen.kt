package com.example.parkmate.ui.map

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.ParkMateViewModelFactory
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.data.location.DeviceLocationProvider
import com.example.parkmate.ui.components.PermissionRequester
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// centro iniziale della mappa, finché non c'è la posizione GPS
private val BOLOGNA = LatLng(44.4949, 11.3426)

// permessi per la posizione: il sistema li chiede insieme, e l'utente può scegliere quello preciso
private val LOCATION_PERMISSIONS = listOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

// ora di inizio come "HH:mm", nel fuso orario del telefono
private val START_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

// titolo del marker: nome del veicolo e ora di inizio, per distinguere le soste al tocco
private fun markerTitle(session: ParkingSession, vehicleNames: Map<Long, String>): String {
    val name = vehicleNames[session.vehicleId] ?: "Veicolo"
    val start = Instant.ofEpochMilli(session.startTime).atZone(ZoneId.systemDefault()).format(START_TIME_FORMAT)
    return "$name, dalle $start"
}

@Composable
fun MapScreen(
    viewModelFactory: ParkMateViewModelFactory,
    deviceLocationProvider: DeviceLocationProvider,
    showSavedLocations: Boolean
) {
    val viewModel: MapViewModel = viewModel(factory = viewModelFactory)
    val activeSessions by viewModel.activeSessions.observeAsState(emptyList())
    val savedLocations by viewModel.savedLocations.observeAsState(emptyList())
    val vehicles by viewModel.vehicles.observeAsState(emptyList())
    val vehicleNames = vehicles.associate { it.id to it.name }
    val scope = rememberCoroutineScope()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(BOLOGNA, 13f)
    }

    var showStartDialog by remember { mutableStateOf(false) }
    // input in attesa della posizione GPS: il permesso arriva in modo asincrono, quindi lo teniamo qui
    var pendingStart by remember { mutableStateOf<StartSessionInput?>(null) }
    var startError by remember { mutableStateOf<String?>(null) }

    // avvia la sosta con le coordinate scelte, poi chiude il modale
    fun start(input: StartSessionInput, latitude: Double, longitude: Double) {
        viewModel.startSession(
            vehicleId = input.vehicleId,
            type = input.type,
            latitude = latitude,
            longitude = longitude,
            savedLocationId = input.savedLocation?.id,
            hourlyRate = input.hourlyRate,
            fixedCost = input.fixedCost,
            expiryTime = input.expiryTime
        )
        showStartDialog = false
        startError = null
    }

    PermissionRequester(
        permissions = LOCATION_PERMISSIONS,
        onResult = { grants ->
            val input = pendingStart ?: return@PermissionRequester
            pendingStart = null
            if (grants.values.any { it }) {
                // il permesso c'è: la posizione è una chiamata sospesa, quindi la lanciamo in una coroutine
                scope.launch {
                    val location = deviceLocationProvider.getCurrentLocation()
                    if (location != null) {
                        start(input, location.latitude, location.longitude)
                    } else {
                        startError = "Posizione GPS non disponibile. Riprova o scegli una saved location."
                    }
                }
            } else {
                startError = "Permesso di posizione negato. Scegli una saved location."
            }
        },
        rationaleTitle = "Serve la posizione",
        rationaleText = "Per registrare dove hai parcheggiato serve la posizione attuale. In alternativa puoi scegliere una saved location.",
        settingsTitle = "Permesso di posizione negato",
        settingsText = "Il permesso è stato negato in modo permanente. Puoi concederlo nelle impostazioni dell'app, oppure scegliere una saved location."
    ) { request ->
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // un marker per ogni sosta attiva
                activeSessions.forEach { session ->
                    Marker(
                        state = rememberUpdatedMarkerState(
                            position = LatLng(session.latitude, session.longitude)
                        ),
                        title = markerTitle(session, vehicleNames)
                    )
                }

                // layer separato delle saved location: azzurro, per distinguerle dalle soste attive (rosse)
                if (showSavedLocations) {
                    savedLocations.forEach { location ->
                        Marker(
                            state = rememberUpdatedMarkerState(
                                position = LatLng(location.latitude, location.longitude)
                            ),
                            title = location.name,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    startError = null
                    showStartDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.LocalParking, contentDescription = "Avvia sosta")
            }
        }

        if (showStartDialog) {
            StartSessionDialog(
                vehicles = vehicles,
                activeVehicleIds = activeSessions.map { it.vehicleId }.toSet(),
                savedLocations = savedLocations,
                errorMessage = startError,
                onDismiss = {
                    showStartDialog = false
                    startError = null
                },
                onConfirm = { input ->
                    val location = input.savedLocation
                    if (location != null) {
                        // saved location: coordinate note, nessun permesso necessario
                        start(input, location.latitude, location.longitude)
                    } else {
                        // GPS: salviamo l'input e chiediamo il permesso; il risultato arriva in onResult
                        pendingStart = input
                        request()
                    }
                }
            )
        }
    }
}
