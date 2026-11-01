package com.example.parkmate.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.parkmate.data.local.entity.ParkType
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.data.local.entity.hourlyCostAt
import com.example.parkmate.ui.components.PhotoCaptureField
import com.example.parkmate.util.formatDuration
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun ParkType.displayLabel(): String = when (this) {
    ParkType.FREE -> "Libera"
    ParkType.HOURLY_PAID -> "Tariffa oraria"
    ParkType.FIXED_TICKET -> "Biglietto fisso"
}

private fun formatTime(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(TIME_FORMAT)

private fun formatCountdown(duration: Duration): String {
    val totalSeconds = duration.seconds
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "${hours}h ${minutes}m ${seconds}s"
}

// scheda di una sosta attiva, aperta con il tap sul marker: riepilogo, nota libera, foto e il
// pulsante per terminare. session arriva già aggiornata da activeSessions (LiveData), quindi
// dopo ogni salvataggio il dialog si ricompone da solo con i dati nuovi
@Composable
fun SessionDetailsDialog(
    session: ParkingSession,
    vehicleName: String,
    onDismiss: () -> Unit,
    onEndSession: () -> Unit,
    onSaveNote: (String) -> Unit,
    // suspend: PhotoCaptureField aspetta l'esito prima di cancellare la foto precedente
    onSavePhoto: suspend (String) -> Boolean
) {
    var now by remember(session.id) { mutableLongStateOf(System.currentTimeMillis()) }

    // per il fixed ticket il ciclo si ferma da sé alla scadenza, non c'è altro da aggiornare
    // per il hourly paid il costo continua a salire finché non termina la sosta
    LaunchedEffect(session.id) {
        when(session.type) {
            ParkType.FREE -> Unit
            ParkType.HOURLY_PAID -> {
                while (true) {
                    delay(1000.milliseconds)
                    now = System.currentTimeMillis()
                }
            }

            ParkType.FIXED_TICKET -> {
                val expiry = session.expiryTime ?: return@LaunchedEffect
                while (now < expiry) {
                    delay(1000.milliseconds)
                    now = System.currentTimeMillis()
                }
            }
        }
    }

    // valore della nota all'apertura del dialog, per sapere alla chiusura se è stata modificata
    val initialNote = remember(session.id) { session.note ?: "" }
    var noteText by rememberSaveable(session.id) { mutableStateOf(session.note ?: "") }

    fun saveNoteIfChanged() {
        if (noteText != initialNote) onSaveNote(noteText)
    }

    AlertDialog(
        onDismissRequest = {
            saveNoteIfChanged()
            onDismiss()
        },
        title = { Text(vehicleName) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Tipo: ${session.type.displayLabel()}")
                Text("Inizio: ${formatTime(session.startTime)}")

                when (session.type) {
                    ParkType.FREE -> Unit
                    ParkType.HOURLY_PAID -> {
                        val elapsed = Duration.ofMillis((now - session.startTime).coerceAtLeast(0))
                        Text("Tempo trascorso: ${formatDuration(elapsed)}")
                        Text("Costo stimato: €%.2f".format(session.hourlyCostAt(now)))
                    }
                    ParkType.FIXED_TICKET -> {
                        session.expiryTime?.let { expiry ->
                            val remaining = expiry - now
                            if (remaining > 0) {
                                Text("Scade alle ${formatTime(expiry)}")
                                Text("Tempo rimanente: ${formatCountdown(Duration.ofMillis(remaining))}")
                            } else {
                                Text("Scaduto alle ${formatTime(expiry)}")
                            }
                        }
                        session.fixedCost?.let { cost -> Text("Costo: €%.2f".format(cost)) }
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Nota") },
                    modifier = Modifier.fillMaxWidth()
                )

                PhotoCaptureField(
                    photoPath = session.photoPath,
                    onPhotoChanged = { path -> onSavePhoto(path) },
                    onBeforeLaunch = { saveNoteIfChanged() },
                    modifier = Modifier.height(200.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                saveNoteIfChanged()
                onEndSession()
            }) { Text("Termina sosta") }
        },
        dismissButton = {
            TextButton(onClick = {
                saveNoteIfChanged()
                onDismiss()
            }) { Text("Chiudi") }
        }
    )
}
