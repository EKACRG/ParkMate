package com.example.parkmate.ui.map

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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.ParkMateViewModelFactory
import com.example.parkmate.data.local.entity.SavedLocation
import com.example.parkmate.ui.components.ConfirmDialog
import com.example.parkmate.ui.components.EmptyState

@Composable
fun SavedLocationsScreen(viewModelFactory: ParkMateViewModelFactory) {
    val viewModel: SavedLocationsViewModel = viewModel(factory = viewModelFactory)
    val locations by viewModel.savedLocations.observeAsState(emptyList())

    var locationBeingEdited by remember { mutableStateOf<SavedLocation?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var locationToDelete by remember { mutableStateOf<SavedLocation?>(null) }

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
                        onEdit = { locationBeingEdited = location },
                        onDelete = { locationToDelete = location }
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
            Icon(Icons.Default.Add, contentDescription = "Aggiungi saved location")
        }
    }

    if (showAddDialog) {
        SavedLocationFormDialog(
            initialLocation = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, latitude, longitude ->
                viewModel.addSavedLocation(SavedLocation(name = name, latitude = latitude, longitude = longitude))
                showAddDialog = false
            }
        )
    }

    locationBeingEdited?.let { location ->
        SavedLocationFormDialog(
            initialLocation = location,
            onDismiss = { locationBeingEdited = null },
            onConfirm = { name, latitude, longitude ->
                viewModel.updateSavedLocation(location.copy(name = name, latitude = latitude, longitude = longitude))
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
    onDismiss: () -> Unit,
    onConfirm: (name: String, latitude: Double, longitude: Double) -> Unit
) {
    var name by rememberSaveable(initialLocation?.id) { mutableStateOf(initialLocation?.name ?: "") }
    var latitudeText by rememberSaveable(initialLocation?.id) { mutableStateOf(initialLocation?.latitude?.toString() ?: "") }
    var longitudeText by rememberSaveable(initialLocation?.id) { mutableStateOf(initialLocation?.longitude?.toString() ?: "") }

    val latitude = latitudeText.toDoubleOrNull()
    val longitude = longitudeText.toDoubleOrNull()
    val isValid = name.isNotBlank() &&
            latitude != null && latitude in -90.0..90.00 &&
            longitude != null && longitude in -180.0..180.0

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
                OutlinedTextField(
                    value = latitudeText,
                    onValueChange = { latitudeText = it },
                    label = { Text("Latitudine") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = longitudeText,
                    onValueChange = { longitudeText = it },
                    label = { Text("Longitudine") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (latitude != null && longitude != null) {
                        onConfirm(name, latitude, longitude)
                    }
                },
                enabled = isValid
            ) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
