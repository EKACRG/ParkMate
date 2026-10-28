package com.example.parkmate.ui.vehicles

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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.ParkMateViewModelFactory
import com.example.parkmate.data.local.entity.Vehicle
import com.example.parkmate.data.local.entity.VehicleType
import com.example.parkmate.ui.components.ConfirmDialog
import com.example.parkmate.ui.components.EmptyState

private const val MIN_LICENSE_PLATE_LENGTH = 1
private const val MAX_LICENSE_PLATE_LENGTH = 14

@Composable
fun VehiclesScreen(viewModelFactory: ParkMateViewModelFactory) {
    val viewModel: VehiclesViewModel = viewModel(factory = viewModelFactory) // istanza del ViewModel
    val vehicles by viewModel.activeVehicles.observeAsState(emptyList()) // ponte tra LiveData e Compose -> innesca la ricomposizione se un dato cambia
    val activeSessions by viewModel.activeSessions.observeAsState(emptyList())
    // join in memoria, stesso pattern di MapScreen/HistoryScreen: nessuna relation Room
    val vehicleIdsWithActiveSession = remember(activeSessions) {
        activeSessions.map { it.vehicleId }.toSet()
    }

    // variabili di stato locali della schermata
    var vehicleBeingEdited by remember { mutableStateOf<Vehicle?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var vehicleToArchive by remember { mutableStateOf<Vehicle?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (vehicles.isEmpty()) {
            EmptyState("Nessun veicolo. Tocca + per aggiungerne uno")
        }
        else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // key serve a Compose per riconoscere gli elementi quando la lista si riordina
                items(vehicles, key = { it.id }) { vehicle ->
                    VehicleRow(
                        vehicle = vehicle,
                        onEdit = { vehicleBeingEdited = vehicle },
                        onArchive = { vehicleToArchive = vehicle }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Aggiungi veicolo")
        }
    }

    // dialog per effettuare le azioni di aggiunta, modifica e archivio di un veicolo
    if (showAddDialog) {
        VehicleFormDialog(
            initialVehicle = null,
            otherVehicles = vehicles,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, licensePlate ->
                viewModel.addVehicle(Vehicle(name = name, type = type, licensePlate = licensePlate))
                showAddDialog = false
            }
        )
    }

    vehicleBeingEdited?.let { vehicle ->
        VehicleFormDialog(
            initialVehicle = vehicle,
            otherVehicles = vehicles.filter { it.id != vehicle.id },
            onDismiss = { vehicleBeingEdited = null },
            onConfirm = { name, type, licensePlate ->
                viewModel.updateVehicle(vehicle.copy(name = name, type = type, licensePlate = licensePlate))
                vehicleBeingEdited = null
            }
        )
    }

    vehicleToArchive?.let { vehicle ->
        var text = "\"${vehicle.name}\" sparirà dalla gestione veicoli, ma resterà visibile nello storico."
        if (vehicle.id in vehicleIdsWithActiveSession) {
            text += " Ha una sosta attiva: verrà terminata automaticamente insieme all'archiviazione."
        }
        ConfirmDialog(
            title= "Archiviare veicolo?",
            text = text,
            confirmLabel = "Archivia",
            onConfirm = {
                viewModel.archiveVehicle(vehicle)
                vehicleToArchive = null
            },
            onDismiss = {
                vehicleToArchive = null
            }
        )
    }
}

@Composable
private fun VehicleRow(vehicle: Vehicle, onEdit: () -> Unit, onArchive: () -> Unit) { // state hoisting
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
                Text(vehicle.name, style = MaterialTheme.typography.titleMedium)
                Text(vehicle.type.displayLabel(), style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onArchive) {
                Icon(Icons.Default.Delete, contentDescription = "Archivia veicolo")
            }
        }
    }
}

@Composable
private fun VehicleFormDialog(
    initialVehicle: Vehicle?,
    otherVehicles: List<Vehicle>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: VehicleType, licensePlate: String?) -> Unit
) {
    // a differenza di remember salva il valore alla ricostruzione dell'activity
    var name by rememberSaveable(initialVehicle?.id) { mutableStateOf(initialVehicle?.name ?: "") }
    var type by rememberSaveable(initialVehicle?.id) { mutableStateOf(initialVehicle?.type ?: VehicleType.CAR) }
    var licensePlate by rememberSaveable(initialVehicle?.id) { mutableStateOf(initialVehicle?.licensePlate ?: "") }

    // biciclette non hanno targa: il campo compare solo per gli altri tipi, ed è obbligatorio quando compare
    val requiresLicensePlate = type != VehicleType.BICYCLE

    // otherVehicles esclude già se stesso (in caso di modifica): qui basta guardare se il nome
    // o la targa coincidono con un ALTRO veicolo attivo
    val duplicateName = otherVehicles.any { it.name.equals(name.trim(), ignoreCase = true) }
    val duplicatePlate = otherVehicles.any { it.licensePlate == licensePlate }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialVehicle == null) "Nuovo veicolo" else "Modifica veicolo") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true
                )
                if (duplicateName) {
                    Text("Esiste già un veicolo con questo nome", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(8.dp))
                VehicleType.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { type = option } // rende cliccabile tutta la riga
                    ) {
                        RadioButton(selected = type == option, onClick = { type = option })
                        Text(option.displayLabel())
                    }
                }
                if (requiresLicensePlate) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = licensePlate,
                        onValueChange = { licensePlate = it.uppercase().take(MAX_LICENSE_PLATE_LENGTH) },
                        label = { Text("Targa") },
                        singleLine = true
                    )
                    if (duplicatePlate) {
                        Text("Esiste già un veicolo con questa targa", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(name, type, if (requiresLicensePlate) licensePlate else null)
                },
                enabled = name.isNotBlank() && !duplicateName &&
                    (!requiresLicensePlate || (isValidLicensePlate(licensePlate) && !duplicatePlate))
            ) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}

private fun VehicleType.displayLabel(): String = when (this) {
    VehicleType.CAR -> "Auto"
    VehicleType.MOTORCYCLE -> "Moto"
    VehicleType.BICYCLE -> "Bicicletta"
}

private fun isValidLicensePlate(lPlate: String): Boolean =
    lPlate.length in MIN_LICENSE_PLATE_LENGTH..MAX_LICENSE_PLATE_LENGTH && lPlate.all { it.isDigit() || it in 'A'..'Z' }
