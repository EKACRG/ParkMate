package com.example.parkmate.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.example.parkmate.data.local.entity.ParkType
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.data.local.entity.VehicleType
import com.example.parkmate.ui.components.DropdownField
import com.example.parkmate.ui.components.label
import com.example.parkmate.util.formatDuration
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val FILTER_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

// il giorno di INIZIO della sosta decide sotto quale data compare, anche se finisce il giorno
// dopo. selectedDateMillis arriva dal DatePicker di Material3 ed è la mezzanotte UTC del giorno
// scelto, non quella del fuso del telefono: va confrontato il giorno di calendario, ciascuno nel
// proprio fuso, non il timestamp grezzo (altrimenti una sosta iniziata poco dopo la mezzanotte
// italiana finirebbe sotto il giorno prima)
private fun startedOnSelectedDate(startTime: Long, selectedDateMillis: Long): Boolean {
    val selectedDay = Instant.ofEpochMilli(selectedDateMillis).atZone(ZoneOffset.UTC).toLocalDate()
    val sessionDay = Instant.ofEpochMilli(startTime).atZone(ZoneId.systemDefault()).toLocalDate()
    return selectedDay == sessionDay
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModelFactory: ParkMateViewModelFactory) {
    val viewModel: HistoryViewModel = viewModel(factory = viewModelFactory)
    val closedSessions by viewModel.closedSessions.observeAsState(emptyList())
    val vehicles by viewModel.vehicles.observeAsState(emptyList())
    val savedLocations by viewModel.savedLocations.observeAsState(emptyList())
    val vehiclesById = vehicles.associateBy { it.id }

    // tre filtri indipendenti e combinabili tra loro; null = nessun vincolo su quel criterio
    var selectedVehicleType by rememberSaveable { mutableStateOf<VehicleType?>(null) }
    var selectedParkType by rememberSaveable { mutableStateOf<ParkType?>(null) }
    var selectedDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var selectedSessionId by rememberSaveable { mutableStateOf<Long?>(null) }

    // calcolato in memoria sulla lista già osservata, non con query SQL dinamiche: con i
    // volumi di un'app single-user non serve, ed è più semplice da spiegare all'orale
    val filteredSessions = remember(closedSessions, vehiclesById, selectedVehicleType, selectedParkType, selectedDateMillis) {
        closedSessions.filter { session ->
            val matchesVehicleType = selectedVehicleType == null ||
                vehiclesById[session.vehicleId]?.type == selectedVehicleType
            val matchesParkType = selectedParkType == null || session.type == selectedParkType
            val matchesDate = selectedDateMillis?.let { date ->
                startedOnSelectedDate(session.startTime, date)
            } ?: true
            matchesVehicleType && matchesParkType && matchesDate
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        DropdownField(
            label = "Tipo di veicolo",
            selectedLabel = selectedVehicleType?.label() ?: "Tutti",
            options = listOf(null) + VehicleType.entries,
            optionLabel = { it?.label() ?: "Tutti" },
            onSelect = { selectedVehicleType = it }
        )
        Spacer(modifier = Modifier.height(8.dp))
        DropdownField(
            label = "Tipo di sosta",
            selectedLabel = selectedParkType?.label() ?: "Tutti",
            options = listOf(null) + ParkType.entries,
            optionLabel = { it?.label() ?: "Tutti" },
            onSelect = { selectedParkType = it }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssistChip(
                onClick = { showDatePicker = true },
                label = {
                    val label = selectedDateMillis?.let { millis ->
                        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).format(FILTER_DATE_FORMAT)
                    } ?: "Tutte le date"
                    Text(label)
                }
            )
            if (selectedDateMillis != null) {
                IconButton(onClick = { selectedDateMillis = null }) {
                    Icon(Icons.Default.Close, contentDescription = "Rimuovi filtro data")
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (filteredSessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nessuna sosta trovata.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredSessions, key = { it.id }) { session ->
                    HistoryEntryCard(
                        session = session,
                        vehicleName = vehiclesById[session.vehicleId]?.name ?: "Veicolo",
                        onClick = { selectedSessionId = session.id }
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annulla") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val selectedSession = closedSessions.find { it.id == selectedSessionId }
    selectedSession?.let { session ->
        HistorySessionDetailDialog(
            session = session,
            vehicleName = vehiclesById[session.vehicleId]?.name ?: "Veicolo",
            savedLocations = savedLocations,
            onDismiss = { selectedSessionId = null }
        )
    }
}

@Composable
private fun HistoryEntryCard(session: ParkingSession, vehicleName: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(vehicleName, style = MaterialTheme.typography.titleMedium)
            Text("${session.type.label()} · ${formatSessionDateTime(session.startTime)}")
            session.endTime?.let { end ->
                Text("Durata: ${formatDuration(Duration.ofMillis(end - session.startTime))}")
            }
            session.finalCost?.let { cost -> Text("Costo: €%.2f".format(cost)) }
        }
    }
}
