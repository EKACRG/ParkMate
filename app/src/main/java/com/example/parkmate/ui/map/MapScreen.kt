package com.example.parkmate.ui.map

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.ParkMateViewModelFactory
import com.example.parkmate.background.TicketAlarmScheduler
import com.example.parkmate.data.local.entity.ParkType
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.data.location.DeviceLocationProvider
import com.example.parkmate.ui.components.ConfirmDialog
import com.example.parkmate.ui.components.DEFAULT_MAP_POSITION
import com.google.android.gms.location.Geofence
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

// ora di inizio come "HH:mm", nel fuso orario del telefono
private val START_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

// titolo del marker: nome del veicolo e ora di inizio, per distinguere le soste al tocco
private fun markerTitle(session: ParkingSession, vehicleNames: Map<Long, String>): String {
    val name = vehicleNames[session.vehicleId] ?: "Veicolo"
    val start = Instant.ofEpochMilli(session.startTime).atZone(ZoneId.systemDefault()).format(START_TIME_FORMAT)
    return "$name, dalle $start"
}

// ticket scaduto ma non chiuso: colore diverso del marker, vedi DECISIONI.md
private fun isExpiredTicket(session: ParkingSession, now: Long): Boolean =
    session.type == ParkType.FIXED_TICKET && session.expiryTime != null && now >= session.expiryTime

@Composable
fun MapScreen(
    viewModelFactory: ParkMateViewModelFactory,
    deviceLocationProvider: DeviceLocationProvider,
    ticketAlarmScheduler: TicketAlarmScheduler,
    showSavedLocations: Boolean,
    geofenceSavedLocationId: Long?,
    geofenceTransition: Int?,
    onGeofenceHandled: () -> Unit
) {
    val viewModel: MapViewModel = viewModel(factory = viewModelFactory)
    val activeSessions by viewModel.activeSessions.observeAsState(emptyList())
    val savedLocations by viewModel.savedLocations.observeAsState(emptyList())
    val vehicles by viewModel.vehicles.observeAsState(emptyList())
    val vehicleNames = vehicles.associate { it.id to it.name }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_MAP_POSITION, 13f)
    }

    val context = LocalContext.current
    var showStartDialog by remember { mutableStateOf(false) }
    // saved location da preselezionare quando il modale si apre da una notifica di ingresso
    var preselectedSavedLocationId by remember { mutableStateOf<Long?>(null) }
    // sosta selezionata con il tap sul marker: apre la scheda di dettaglio
    var selectedSession by remember { mutableStateOf<ParkingSession?>(null) }
    // per richiedere accesso agli alarm esatti
    var showExactAlarmPrompt by remember { mutableStateOf(false) }

    // al tap su una notifica di geofence: la decisione si prende qui, leggendo lo stato
    // attuale (activeSessions), non quello che c'era quando la notifica è partita
    LaunchedEffect(geofenceSavedLocationId, geofenceTransition) {
        val savedLocationId = geofenceSavedLocationId ?: return@LaunchedEffect
        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                preselectedSavedLocationId = savedLocationId
                showStartDialog = true
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                val matching = activeSessions.filter { it.savedLocationId == savedLocationId }
                // solo con esattamente una sosta attiva qui l'azione è univoca: con zero o più
                // di una si apre solo la mappa, senza scegliere per l'utente
                if (matching.size == 1) {
                    selectedSession = matching.single()
                }
            }
        }
        onGeofenceHandled()
    }

    // basta un minuto di tolleranza per il colore dei ticket scaduti: non serve il secondo come nella scheda
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000.milliseconds)
            now = System.currentTimeMillis()
        }
    }

    // avvia la sosta alle coordinate finali del marker, poi chiude il modale
    fun start(input: StartSessionInput) {
        viewModel.startSession(
            vehicleId = input.vehicleId,
            vehicleName = vehicleNames[input.vehicleId] ?: "Veicolo",
            type = input.type,
            latitude = input.latitude,
            longitude = input.longitude,
            savedLocationId = input.savedLocationId,
            hourlyRate = input.hourlyRate,
            fixedCost = input.fixedCost,
            expiryTime = input.expiryTime
        )
        showStartDialog = false

        // se ho appena avviato un ticket, e in questo momento l'app non ha l'accesso esatto, mostra l'invito
        if (input.type == ParkType.FIXED_TICKET && !ticketAlarmScheduler.canScheduleExact()) {
            showExactAlarmPrompt = true
        }
    }

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
                    title = markerTitle(session, vehicleNames),
                    // arancione per i ticket scaduti ma non chiusi (overstay), rosso di default per il resto
                    icon = if (isExpiredTicket(session, now)) {
                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                    } else {
                        null
                    },
                    // true consuma il tocco: niente centratura automatica né InfoWindow di default,
                    // apriamo la nostra scheda al suo posto
                    onClick = {
                        selectedSession = session
                        true
                    }
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
                // un avvio normale non deve riusare una preselezione lasciata da un geofence precedente
                preselectedSavedLocationId = null
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
            deviceLocationProvider = deviceLocationProvider,
            initialSavedLocationId = preselectedSavedLocationId,
            onDismiss = { showStartDialog = false },
            onConfirm = { input -> start(input) }
        )
    }

    selectedSession?.let { session ->
        SessionDetailsDialog(
            session = session,
            vehicleName = vehicleNames[session.vehicleId] ?: "Veicolo",
            onDismiss = { selectedSession = null },
            onEndSession = {
                viewModel.endSession(session.id)
                selectedSession = null
            }
        )
    }

    if (showExactAlarmPrompt) {
        ConfirmDialog(
            title = "Avviso di scadenza più preciso",
            text = "Concedi l'accesso agli allarmi esatti nelle impostazioni per essere " +
                "avvisato esattamente al momento della scadenza del ticket. Senza, l'avviso " +
                "potrebbe arrivare qualche minuto più tardi.",
            confirmLabel = "Apri impostazioni",
            onConfirm = {
                showExactAlarmPrompt = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                }
            },
            onDismiss = { showExactAlarmPrompt = false }
        )
    }
}
