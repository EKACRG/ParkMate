package com.example.parkmate.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.parkmate.data.local.entity.ParkType
import com.example.parkmate.data.local.entity.SavedLocation
import com.example.parkmate.data.local.entity.Vehicle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

// altezza fissa della mini-mappa: se il contenuto supera lo schermo, il modale scorre
private val MAP_HEIGHT = 180.dp
private const val MAP_ZOOM = 16f

// dati inseriti nel modale. latitude e longitude sono il punto finale: la saved location scelta
// oppure il marker spostato sulla mini-mappa (solo con sorgente GPS).
// savedLocationId è l'id della saved location scelta, null per il GPS
data class StartSessionInput(
    val vehicleId: Long,
    val type: ParkType,
    val hourlyRate: Double?,
    val fixedCost: Double?,
    val expiryTime: Long?,
    val latitude: Double,
    val longitude: Double,
    val savedLocationId: Long?
)

// ore e minuti come testo: null se uno dei due non è un numero nell'intervallo giusto
fun parseExpiryTime(hourText: String, minuteText: String): LocalTime? {
    val hour = hourText.toIntOrNull()
    val minute = minuteText.toIntOrNull()
    if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59) return null
    return LocalTime.of(hour, minute)
}

// oggi se l'orario è dopo quello attuale, altrimenti domani (uguale compreso: una scadenza "adesso" non ha senso)
fun nextExpiryMillis(time: LocalTime, now: LocalDateTime = LocalDateTime.now()): Long {
    val today = now.toLocalDate().atTime(time)
    val expiry = if (today.isAfter(now)) today else today.plusDays(1)
    return expiry.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

// accetta anche la virgola decimale, usata in Italia; zero e negativi non sono validi
private fun String.toPositiveNumberOrNull(): Double? =
    replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }

// etichette corte, perché le tre opzioni stanno affiancate su una riga di telefono
private fun ParkType.shortLabel(): String = when (this) {
    ParkType.FREE -> "Libera"
    ParkType.HOURLY_PAID -> "Oraria"
    ParkType.FIXED_TICKET -> "Ticket"
}

// voci del primo menu della posizione: il GPS, oppure "Saved Locations" che apre il secondo menu
private sealed interface PositionChoice {
    data object Gps : PositionChoice
    data object SavedLocations : PositionChoice
}

@Composable
fun StartSessionDialog(
    vehicles: List<Vehicle>,
    activeVehicleIds: Set<Long>,
    savedLocations: List<SavedLocation>,
    gpsPosition: LatLng?,
    gpsDenied: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onConfirm: (StartSessionInput) -> Unit
) {
    // stato del form: rememberSaveable lo conserva anche alla ricostruzione dell'activity
    var vehicleId by rememberSaveable { mutableStateOf<Long?>(null) }
    var type by rememberSaveable { mutableStateOf(ParkType.FREE) }
    var hourlyRateText by rememberSaveable { mutableStateOf("") }
    var fixedCostText by rememberSaveable { mutableStateOf("") }
    var expiryHourText by rememberSaveable { mutableStateOf("") }
    var expiryMinuteText by rememberSaveable { mutableStateOf("") }
    // sorgente scelta nel menu: null = GPS, altrimenti l'id della saved location
    var sourceLocationId by rememberSaveable { mutableStateOf<Long?>(null) }
    // marker sulla mini-mappa, salvato come due Double perché LatLng non è salvabile con rememberSaveable
    var pinLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var pinLng by rememberSaveable { mutableStateOf<Double?>(null) }
    // modalità scelta nel primo menu: false = GPS (con mini-mappa), true = "Saved Locations" (con il secondo menu)
    var savedMode by rememberSaveable { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState()

    // saved location scelta, oppure null se la sorgente è il GPS
    val sourceLocation = savedLocations.find { it.id == sourceLocationId }

    // con sorgente GPS il marker parte dalla posizione letta all'apertura e la mappa la centra
    LaunchedEffect(gpsPosition, savedMode) {
        val position = gpsPosition ?: return@LaunchedEffect
        if (!savedMode) {
            pinLat = position.latitude
            pinLng = position.longitude
            cameraPositionState.position = CameraPosition.fromLatLngZoom(position, MAP_ZOOM)
        }
    }

    val pinLatitude = pinLat
    val pinLongitude = pinLng
    val gpsPin: LatLng? = if (pinLatitude != null && pinLongitude != null) LatLng(pinLatitude, pinLongitude) else null
    // il punto finale: coordinate del luogo salvato, oppure il marker sulla mini-mappa
    // in modalità saved location il punto è quello del luogo scelto (null finché non si sceglie); altrimenti il marker
    val point: LatLng? = if (savedMode) sourceLocation?.let { LatLng(it.latitude, it.longitude) } else gpsPin

    // valori derivati dal testo: ricalcolati a ogni modifica
    val hourlyRate = hourlyRateText.toPositiveNumberOrNull()
    val fixedCost = fixedCostText.toPositiveNumberOrNull()
    val expiry = parseExpiryTime(expiryHourText, expiryMinuteText)

    // ogni tipo richiede i suoi campi: il pulsante resta disabilitato finché non sono validi
    val typeValid = when (type) {
        ParkType.FREE -> true
        ParkType.HOURLY_PAID -> hourlyRate != null
        ParkType.FIXED_TICKET -> fixedCost != null && expiry != null
    }
    val canConfirm = vehicleId != null && typeValid && point != null
    val hasActiveSession = vehicleId?.let { it in activeVehicleIds } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Avvia sosta") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // messaggi in alto: compaiono solo quando servono
                if (hasActiveSession) {
                    Text(
                        "Questo veicolo ha già una sosta attiva: verrà chiusa.",
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                errorMessage?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // senza veicoli il menu non si può usare: spieghiamo cosa fare invece di mostrare un menu vuoto
                if (vehicles.isEmpty()) {
                    Text("Nessun veicolo disponibile. Aggiungine uno nella sezione Veicoli.")
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    DropdownField(
                        label = "Veicolo",
                        selectedLabel = vehicles.find { it.id == vehicleId }?.name ?: "Seleziona un veicolo",
                        options = vehicles,
                        optionLabel = { it.name },
                        onSelect = { vehicleId = it.id }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    ParkType.entries.forEach { option ->
                        TypeOption(
                            label = option.shortLabel(),
                            selected = type == option,
                            onSelect = { type = option },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // i campi compaiono solo per il tipo scelto
                when (type) {
                    ParkType.FREE -> Unit
                    ParkType.HOURLY_PAID -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = hourlyRateText,
                            onValueChange = { hourlyRateText = it },
                            label = { Text("Tariffa oraria (€)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    ParkType.FIXED_TICKET -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = fixedCostText,
                            onValueChange = { fixedCostText = it },
                            label = { Text("Costo (€)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Scadenza", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // solo cifre, massimo due per campo: i due punti li aggiungiamo noi nella validazione
                            OutlinedTextField(
                                value = expiryHourText,
                                onValueChange = { expiryHourText = it.filter { c -> c.isDigit() }.take(2) },
                                label = { Text("Ore") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = expiryMinuteText,
                                onValueChange = { expiryMinuteText = it.filter { c -> c.isDigit() }.take(2) },
                                label = { Text("Minuti") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                // primo menu: GPS, oppure "Saved Locations" (la voce compare solo se ci sono luoghi salvati)
                val positionOptions = listOfNotNull(
                    PositionChoice.Gps,
                    PositionChoice.SavedLocations.takeIf { savedLocations.isNotEmpty() }
                )
                DropdownField(
                    label = "Posizione",
                    selectedLabel = if (savedMode) "Saved Locations" else "Posizione GPS attuale",
                    options = positionOptions,
                    optionLabel = { choice ->
                        when (choice) {
                            PositionChoice.Gps -> "Posizione GPS attuale"
                            PositionChoice.SavedLocations -> "Saved Locations"
                        }
                    },
                    onSelect = { choice ->
                        when (choice) {
                            PositionChoice.Gps -> {
                                sourceLocationId = null
                                savedMode = false
                            }
                            PositionChoice.SavedLocations -> {
                                savedMode = true
                            }
                        }
                    }
                )

                // secondo menu: i luoghi salvati, aperto dalla voce "Saved Locations"
                if (savedMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DropdownField(
                        label = "Luoghi salvati",
                        selectedLabel = sourceLocation?.name ?: "Seleziona un luogo",
                        options = savedLocations,
                        optionLabel = { it.name },
                        onSelect = { location -> sourceLocationId = location.id }
                    )
                }

                // la mini-mappa esiste solo con sorgente GPS: con una saved location il punto è già fissato
                if (!savedMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    when {
                        gpsDenied -> PermissionDeniedBox(onOpenSettings = onOpenSettings)
                        gpsPin == null -> LoadingBox(loading = errorMessage == null)
                        else -> PositionMap(
                            pin = gpsPin,
                            cameraPositionState = cameraPositionState,
                            onMove = { moved ->
                                pinLat = moved.latitude
                                pinLng = moved.longitude
                            },
                            onRecenter = {
                                gpsPosition?.let { position ->
                                    pinLat = position.latitude
                                    pinLng = position.longitude
                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(position, MAP_ZOOM)
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = {
                    // il pulsante è abilitato solo con veicolo, tipo valido e punto della sosta
                    val id = vehicleId ?: return@TextButton
                    val target = point ?: return@TextButton
                    onConfirm(
                        StartSessionInput(
                            vehicleId = id,
                            type = type,
                            hourlyRate = if (type == ParkType.HOURLY_PAID) hourlyRate else null,
                            fixedCost = if (type == ParkType.FIXED_TICKET) fixedCost else null,
                            expiryTime = if (type == ParkType.FIXED_TICKET) expiry?.let { nextExpiryMillis(it) } else null,
                            latitude = target.latitude,
                            longitude = target.longitude,
                            savedLocationId = sourceLocation?.id
                        )
                    )
                }
            ) { Text("Avvia") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}

// mini-mappa con zoom e pan; il tap sposta il marker; "Ricentra" è sovrapposto in basso a destra
@Composable
private fun PositionMap(
    pin: LatLng,
    cameraPositionState: CameraPositionState,
    onMove: (LatLng) -> Unit,
    onRecenter: () -> Unit
) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(MAP_HEIGHT)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = onMove
        ) {
            Marker(
                state = rememberUpdatedMarkerState(position = pin),
                title = "Posizione della sosta"
            )
        }
        FilledTonalButton(
            onClick = onRecenter,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            Text("Ricentra sul GPS")
        }
    }
}

// in attesa della posizione GPS: uno spinner; se la lettura fallisce, il messaggio è in cima al modale
@Composable
private fun LoadingBox(loading: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MAP_HEIGHT),
        contentAlignment = Alignment.Center
    ) {
        if (loading) CircularProgressIndicator()
    }
}

// permesso negato: messaggio + pulsante per andare nelle impostazioni dell'app
@Composable
private fun PermissionDeniedBox(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(MAP_HEIGHT)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Permesso negato, apri le impostazioni per fornire il permesso")
        TextButton(onClick = onOpenSettings) { Text("Apri impostazioni") }
    }
}

// menu a tendina: il campo mostra la voce scelta, il menu si apre al tocco
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownField(
    label: String,
    selectedLabel: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

// un pallino con etichetta, tutta la voce è cliccabile
@Composable
private fun TypeOption(label: String, selected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable(onClick = onSelect)
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label)
    }
}
