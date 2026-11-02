package com.example.parkmate.ui.map

import android.Manifest
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.ParkMateViewModelFactory
import com.example.parkmate.background.TicketAlarmScheduler
import com.example.parkmate.data.local.entity.ParkType
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.data.local.entity.SavedLocation
import com.example.parkmate.data.location.DeviceLocationProvider
import com.example.parkmate.ui.components.ConfirmDialog
import com.example.parkmate.ui.components.DEFAULT_MAP_POSITION
import com.example.parkmate.ui.components.PermissionRequester
import com.google.android.gms.location.Geofence
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

// permessi per la posizione: stessa coppia e stesso schema di StartSessionDialog/SavedLocationsScreen
private val LOCATION_PERMISSIONS = listOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

// livello strada: anche una sosta sola, o soste vicinissime, non fanno avvicinare la mappa oltre questo
private const val MAX_BOUNDS_ZOOM = 16f
private const val BOUNDS_PADDING_PX = 100
private const val DEFAULT_ZOOM = 13f

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

// la mappa principale serve a ritrovare i veicoli, non a mostrare dove si trova l'utente: si
// inquadrano i dati (soste attive, e le saved location se il layer è visibile), non il GPS.
// move(), non animate(): sincrono, nessun salto visivo tra l'inquadratura e l'eventuale
// correzione dello zoom massimo subito sotto
private fun recenterOnData(
    cameraPositionState: CameraPositionState,
    activeSessions: List<ParkingSession>,
    savedLocations: List<SavedLocation>,
    showSavedLocations: Boolean
) {
    val points = buildList {
        activeSessions.forEach { add(LatLng(it.latitude, it.longitude)) }
        if (showSavedLocations) savedLocations.forEach { add(LatLng(it.latitude, it.longitude)) }
    }
    if (points.isEmpty()) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(DEFAULT_MAP_POSITION, DEFAULT_ZOOM)
        return
    }
    val bounds = LatLngBounds.Builder().apply { points.forEach { include(it) } }.build()
    cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, BOUNDS_PADDING_PX))
    // newLatLngBounds può avvicinarsi troppo con un solo punto, o con punti molto vicini tra loro
    if (cameraPositionState.position.zoom > MAX_BOUNDS_ZOOM) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(bounds.center, MAX_BOUNDS_ZOOM)
    }
}

@Composable
fun MapScreen(
    viewModelFactory: ParkMateViewModelFactory,
    deviceLocationProvider: DeviceLocationProvider,
    ticketAlarmScheduler: TicketAlarmScheduler,
    showSavedLocations: Boolean,
    geofenceSavedLocationId: Long?,
    geofenceTransition: Int?,
    onGeofenceHandled: () -> Unit,
    ticketSessionId: Long?,
    onTicketSessionHandled: () -> Unit
) {
    val viewModel: MapViewModel = viewModel(factory = viewModelFactory)
    // null finché Room non ha ancora emesso il primo valore: diverso da "nessuna sosta/saved
    // location", serve a non inquadrare Bologna prima che i dati siano davvero arrivati
    val activeSessionsOrNull by viewModel.activeSessions.observeAsState()
    val savedLocationsOrNull by viewModel.savedLocations.observeAsState()
    val activeSessions = activeSessionsOrNull ?: emptyList()
    val savedLocations = savedLocationsOrNull ?: emptyList()
    val vehicles by viewModel.vehicles.observeAsState(emptyList())
    val vehicleNames = vehicles.associate { it.id to it.name }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_MAP_POSITION, DEFAULT_ZOOM)
    }

    // vero se il permesso è concesso in questo momento: aggiornato dal PermissionRequester
    // sotto quando viene concesso, e riletto a ogni ON_RESUME perché l'utente può revocarlo
    // dalle impostazioni di sistema mentre l'app è in background — stesso schema già usato per
    // lo stato delle notifiche in Impostazioni. StartSessionDialog lo riceve come parametro,
    // così se l'utente lo concede mentre il modale è già aperto il suo LaunchedEffect reagisce
    val lifecycleOwner = LocalLifecycleOwner.current
    var locationPermissionGranted by remember {
        mutableStateOf(deviceLocationProvider.hasLocationPermission())
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                locationPermissionGranted = deviceLocationProvider.hasLocationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PermissionRequester(
        permissions = LOCATION_PERMISSIONS,
        onResult = { grants ->
            if (grants.values.any { it }) {
                locationPermissionGranted = true
            }
        },
        rationaleTitle = "Serve la posizione",
        rationaleText = "Per centrare automaticamente la posizione della sosta sul GPS serve il " +
            "permesso di posizione. Puoi comunque indicare il punto a mano sulla mappa.",
        settingsTitle = "Permesso di posizione negato",
        settingsText = "Il permesso è stato negato in modo permanente. Puoi concederlo nelle " +
            "impostazioni dell'app, oppure continuare a indicare il punto a mano sulla mappa."
    ) { requestLocationAccess ->
        val context = LocalContext.current
        // rememberSaveable: ora che il modale ha un secondo step con la fotocamera, un'Activity
        // ricreata mentre la fotocamera è in primo piano non deve far sparire l'intero modale,
        // non solo la foto in corso (quella la conserva già StartSessionDialog)
        var showStartDialog by rememberSaveable { mutableStateOf(false) }
        // saved location da preselezionare quando il modale si apre da una notifica di ingresso
        var preselectedSavedLocationId by rememberSaveable { mutableStateOf<Long?>(null) }
        // id della sosta selezionata col tap sul marker, non l'oggetto: così il dialog si aggiorna
        // da solo quando activeSessions cambia (nota o foto salvate, costo che sale), invece di
        // restare fermo su uno snapshot preso al momento del tap. rememberSaveable: se l'Activity
        // viene ricreata mentre la fotocamera è in primo piano, il dialog deve riapparire aperto
        var selectedSessionId by rememberSaveable { mutableStateOf<Long?>(null) }
        val selectedSession = activeSessions.find { it.id == selectedSessionId }
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
                        selectedSessionId = matching.single().id
                    }
                }
            }
            onGeofenceHandled()
        }

        // al tap su una notifica di scadenza ticket: apre la scheda di quella sosta, se è ancora
        // attiva. Se nel frattempo è stata terminata, si apre solo la mappa
        LaunchedEffect(ticketSessionId) {
            val sessionId = ticketSessionId ?: return@LaunchedEffect
            if (activeSessions.any { it.id == sessionId }) {
                selectedSessionId = sessionId
            }
            onTicketSessionHandled()
        }

        // basta un minuto di tolleranza per il colore dei ticket scaduti: non serve il secondo come nella scheda
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(60_000.milliseconds)
                now = System.currentTimeMillis()
            }
        }

        // true dal primo GoogleMap.onMapLoaded: prima di allora newLatLngBounds non ha una
        // dimensione della mappa su cui lavorare
        var mapLoaded by remember { mutableStateOf(false) }
        // il set degli id, non la lista: una nota o una foto salvate rifanno emettere la LiveData
        // con lo stesso insieme di soste, e qui non deve succedere nulla; solo un id aggiunto o
        // rimosso (sosta avviata o chiusa) deve far ricalcolare l'inquadratura
        val activeSessionIds = activeSessionsOrNull?.map { it.id }?.toSet()
        // con il layer spento i dati delle saved location non servono: non li si aspetta
        val savedLocationsReady = !showSavedLocations || savedLocationsOrNull != null

        // si ricalcola all'apertura della mappa (quando mapLoaded diventa vero), quando cambia
        // l'insieme delle soste attive, e quando si accende o spegne il layer delle saved
        // location — non ad ogni aggiornamento dei dati, altrimenti la mappa si sposterebbe
        // mentre l'utente la sta usando
        LaunchedEffect(mapLoaded, activeSessionIds, showSavedLocations, savedLocationsReady) {
            val sessions = activeSessionsOrNull ?: return@LaunchedEffect
            if (!mapLoaded || !savedLocationsReady) return@LaunchedEffect
            recenterOnData(cameraPositionState, sessions, savedLocationsOrNull ?: emptyList(), showSavedLocations)
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
                expiryTime = input.expiryTime,
                note = input.note,
                photoPath = input.photoPath
            )
            showStartDialog = false

            // se ho appena avviato un ticket, e in questo momento l'app non ha l'accesso esatto, mostra l'invito
            if (input.type == ParkType.FIXED_TICKET && !ticketAlarmScheduler.canScheduleExact()) {
                showExactAlarmPrompt = true
            }
        }

        // i controlli +/- di default coprirebbero il FAB in basso a destra; lo zoom con le dita resta
        val mapUiSettings = remember { MapUiSettings(zoomControlsEnabled = false) }

        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = mapUiSettings,
                onMapLoaded = { mapLoaded = true }
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
                            selectedSessionId = session.id
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
                    // se il permesso è ancora negato, lo richiede di nuovo; il modale si apre
                    // comunque, qualunque sia la risposta dell'utente
                    if (!deviceLocationProvider.hasLocationPermission()) {
                        requestLocationAccess()
                    }
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
                hasLocationPermission = locationPermissionGranted,
                initialSavedLocationId = preselectedSavedLocationId,
                onDismiss = { showStartDialog = false },
                onConfirm = { input -> start(input) }
            )
        }

        selectedSession?.let { session ->
            SessionDetailsDialog(
                session = session,
                vehicleName = vehicleNames[session.vehicleId] ?: "Veicolo",
                onDismiss = { selectedSessionId = null },
                onEndSession = {
                    viewModel.endSession(session.id)
                    selectedSessionId = null
                },
                onSaveNote = { note -> viewModel.updateNote(session.id, note) },
                onSavePhoto = { path -> viewModel.updatePhotoPath(session.id, path) }
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
}
