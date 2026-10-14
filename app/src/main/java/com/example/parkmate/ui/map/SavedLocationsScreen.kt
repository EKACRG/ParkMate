package com.example.parkmate.ui.map

import android.Manifest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.ParkMateViewModelFactory
import com.example.parkmate.data.local.entity.SavedLocation
import com.example.parkmate.data.location.DeviceLocationProvider
import com.example.parkmate.ui.components.BackgroundLocationPermissionRequester
import com.example.parkmate.ui.components.ConfirmDialog
import com.example.parkmate.ui.components.DEFAULT_MAP_POSITION
import com.example.parkmate.ui.components.EmptyState
import com.example.parkmate.ui.components.hasBackgroundLocationAccess
import com.example.parkmate.ui.components.MINI_MAP_ZOOM
import com.example.parkmate.ui.components.PermissionRequester
import com.example.parkmate.ui.components.PositionMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

// permessi per la posizione: stessa coppia e stesso schema di MapScreen
private val LOCATION_PERMISSIONS = listOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

private const val GEOFENCE_RADIUS_METERS = 150f

@Composable
fun SavedLocationsScreen(
    viewModelFactory: ParkMateViewModelFactory,
    deviceLocationProvider: DeviceLocationProvider
) {
    val viewModel: SavedLocationsViewModel = viewModel(factory = viewModelFactory)
    val locations by viewModel.savedLocations.observeAsState(emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var locationBeingEdited by remember { mutableStateOf<SavedLocation?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var locationToDelete by remember { mutableStateOf<SavedLocation?>(null) }

    // posizione GPS letta all'apertura del form, come in MapScreen
    var gpsPosition by remember { mutableStateOf<LatLng?>(null) }

    PermissionRequester(
        permissions = LOCATION_PERMISSIONS,
        onResult = { grants ->
            if (grants.values.any { it }) {
                scope.launch {
                    val location = deviceLocationProvider.getCurrentLocation()
                    if (location != null) {
                        gpsPosition = LatLng(location.latitude, location.longitude)
                    }
                }
            }
        },
        rationaleTitle = "Serve la posizione",
        rationaleText = "Per posizionare un luogo salvato sulla tua posizione attuale serve il GPS. " +
            "Puoi comunque indicare il punto a mano sulla mappa.",
        settingsTitle = "Permesso di posizione negato",
        settingsText = "Il permesso è stato negato in modo permanente. Puoi concederlo nelle " +
            "impostazioni dell'app, oppure indicare il punto a mano sulla mappa."
    ) { request ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (locations.isEmpty()) {
                EmptyState("Nessuna saved location. Tocca + per aggiungerne una")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(locations, key = { it.id }) { location ->
                        SavedLocationRow(
                            location = location,
                            onEdit = {
                                gpsPosition = null
                                locationBeingEdited = location
                                request()
                            },
                            onDelete = { locationToDelete = location }
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    gpsPosition = null
                    showAddDialog = true
                    request()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi saved location")
            }
        }

        if (showAddDialog) {
            SavedLocationFormDialog(
                initialLocation = null,
                gpsPosition = gpsPosition,
                onDismiss = { showAddDialog = false },
                onConfirm = { name, latitude, longitude, geofenceEnabled ->
                    viewModel.addSavedLocation(
                        SavedLocation(
                            name = name,
                            latitude = latitude,
                            longitude = longitude,
                            geofenceRadiusMeters = if (geofenceEnabled) GEOFENCE_RADIUS_METERS else null
                        )
                    )
                    showAddDialog = false
                }
            )
        }

        locationBeingEdited?.let { location ->
            SavedLocationFormDialog(
                initialLocation = location,
                gpsPosition = gpsPosition,
                onDismiss = { locationBeingEdited = null },
                onConfirm = { name, latitude, longitude, geofenceEnabled ->
                    viewModel.updateSavedLocation(
                        location.copy(
                            name = name,
                            latitude = latitude,
                            longitude = longitude,
                            geofenceRadiusMeters = if (geofenceEnabled) GEOFENCE_RADIUS_METERS else null
                        )
                    )
                    locationBeingEdited = null
                }
            )
        }

        locationToDelete?.let { location ->
            ConfirmDialog(
                title = "Eliminare saved location?",
                text = "\"${location.name}\" verrà eliminata definitivamente.",
                confirmLabel = "Elimina",
                onConfirm = {
                    viewModel.deleteSavedLocation(location)
                    locationToDelete = null
                },
                onDismiss = { locationToDelete = null }
            )
        }
    }
}

@Composable
private fun SavedLocationRow(location: SavedLocation, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(location.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "%.5f, %.5f".format(location.latitude, location.longitude),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Elimina saved location")
            }
        }
    }
}

@Composable
private fun SavedLocationFormDialog(
    initialLocation: SavedLocation?,
    gpsPosition: LatLng?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, latitude: Double, longitude: Double, geofenceEnabled: Boolean) -> Unit
) {
    val context = LocalContext.current
    var name by rememberSaveable(initialLocation?.id) { mutableStateOf(initialLocation?.name ?: "") }
    var pin by rememberSaveable(initialLocation?.id) {
        mutableStateOf(initialLocation?.let { LatLng(it.latitude, it.longitude) } ?: DEFAULT_MAP_POSITION)
    }
    // vero da subito in modifica (le coordinate salvate non vanno mai sovrascritte dal GPS);
    // in creazione diventa vero alla prima azione dell'utente sulla mappa (tap o "Ricentra")
    var pinSetByUser by rememberSaveable(initialLocation?.id) { mutableStateOf(initialLocation != null) }
    var geofenceEnabled by rememberSaveable(initialLocation?.id) {
        mutableStateOf(initialLocation?.geofenceRadiusMeters != null)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(pin, MINI_MAP_ZOOM)
    }

    // se il GPS si carica dopo l'apertura e l'utente non ha ancora toccato la mappa, centra lì
    LaunchedEffect(gpsPosition) {
        if (!pinSetByUser && gpsPosition != null) {
            pin = gpsPosition
            cameraPositionState.position = CameraPosition.fromLatLngZoom(gpsPosition, MINI_MAP_ZOOM)
        }
    }

    BackgroundLocationPermissionRequester { requestBackgroundAccess ->
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (initialLocation == null) "Nuova saved location" else "Modifica saved location") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PositionMap(
                        pin = pin,
                        cameraPositionState = cameraPositionState,
                        markerTitle = "Posizione del luogo",
                        onMove = { moved ->
                            pin = moved
                            pinSetByUser = true
                        },
                        onRecenter = gpsPosition?.let { gps ->
                            {
                                pin = gps
                                pinSetByUser = true
                                cameraPositionState.position = CameraPosition.fromLatLngZoom(gps, MINI_MAP_ZOOM)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = geofenceEnabled,
                            onCheckedChange = { checked ->
                                geofenceEnabled = checked
                                // il geofence vero e proprio non è ancora registrato (arriva con lo
                                // Step 2): qui chiediamo solo il permesso, in anticipo
                                if (checked && !hasBackgroundLocationAccess(context)) {
                                    requestBackgroundAccess()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Avvisami quando entro o esco da qui")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirm(name, pin.latitude, pin.longitude, geofenceEnabled) },
                    enabled = name.isNotBlank()
                ) { Text("Salva") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Annulla") }
            }
        )
    }
}
