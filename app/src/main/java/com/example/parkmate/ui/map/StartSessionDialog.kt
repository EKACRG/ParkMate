package com.example.parkmate.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

// dati inseriti nel modale. savedLocation == null significa "posizione GPS"
data class StartSessionInput(
    val vehicleId: Long,
    val type: ParkType,
    val hourlyRate: Double?,
    val fixedCost: Double?,
    val expiryTime: Long?,
    val savedLocation: SavedLocation?
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

@Composable
fun StartSessionDialog(
    vehicles: List<Vehicle>,
    activeVehicleIds: Set<Long>,
    savedLocations: List<SavedLocation>,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (StartSessionInput) -> Unit
) {
    // stato del form: rememberSaveable lo conserva anche alla ricostruzione dell'activity
    var vehicleId by rememberSaveable { mutableStateOf<Long?>(null) }
    var type by rememberSaveable { mutableStateOf(ParkType.FREE) }
    var hourlyRateText by rememberSaveable { mutableStateOf("") }
    var fixedCostText by rememberSaveable { mutableStateOf("") }
    var expiryHourText by rememberSaveable { mutableStateOf("") }
    var expiryMinuteText by rememberSaveable { mutableStateOf("") }
    // null = posizione GPS, altrimenti l'id della saved location scelta
    var savedLocationId by rememberSaveable { mutableStateOf<Long?>(null) }

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
    val canConfirm = vehicleId != null && typeValid
    val hasActiveSession = vehicleId?.let { it in activeVehicleIds } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Avvia sosta") },
        text = {
            Column {
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
                // null è la voce "GPS": è la prima della lista e viene preselezionata
                val locationOptions: List<SavedLocation?> = listOf(null) + savedLocations
                DropdownField(
                    label = "Posizione",
                    selectedLabel = savedLocations.find { it.id == savedLocationId }?.name ?: "Posizione GPS attuale",
                    options = locationOptions,
                    optionLabel = { it?.name ?: "Posizione GPS attuale" },
                    onSelect = { savedLocationId = it?.id }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = {
                    // il pulsante è abilitato solo con un veicolo, quindi qui non serve un controllo vero
                    val id = vehicleId ?: return@TextButton
                    onConfirm(
                        StartSessionInput(
                            vehicleId = id,
                            type = type,
                            hourlyRate = if (type == ParkType.HOURLY_PAID) hourlyRate else null,
                            fixedCost = if (type == ParkType.FIXED_TICKET) fixedCost else null,
                            expiryTime = if (type == ParkType.FIXED_TICKET) expiry?.let { nextExpiryMillis(it) } else null,
                            savedLocation = savedLocations.find { it.id == savedLocationId }
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
