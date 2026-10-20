package com.example.parkmate.ui.map

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import com.example.parkmate.data.local.entity.ParkType
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.data.local.entity.hourlyCostAt
import com.example.parkmate.ui.components.PhotoThumbnail
import com.example.parkmate.util.createPhotoFile
import com.example.parkmate.util.deletePhotoFile
import com.example.parkmate.util.formatDuration
import com.example.parkmate.util.photoUriFor
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
    onSavePhoto: (String) -> Unit
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

    val context = LocalContext.current

    // valore della nota all'apertura del dialog, per sapere alla chiusura se è stata modificata
    val initialNote = remember(session.id) { session.note ?: "" }
    var noteText by rememberSaveable(session.id) { mutableStateOf(session.note ?: "") }

    // percorso del file che la fotocamera sta scrivendo: rememberSaveable, non remember, perché
    // deve sopravvivere se l'Activity viene ricreata mentre l'app fotocamera è in primo piano
    var pendingPhotoPath by rememberSaveable(session.id) { mutableStateOf<String?>(null) }

    fun saveNoteIfChanged() {
        if (noteText != initialNote) onSaveNote(noteText)
    }

    // ActivityResultContracts.TakePicture scrive la foto nell'URI passato a launch() e
    // restituisce solo un booleano di successo, non il contenuto
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val path = pendingPhotoPath
        pendingPhotoPath = null
        if (path == null) return@rememberLauncherForActivityResult

        if (success) {
            // salva subito nel database: se il dialog si chiude prima di riaprirlo la foto c'è comunque
            val previousPath = session.photoPath
            onSavePhoto(path)
            if (previousPath != null && previousPath != path) deletePhotoFile(previousPath)
        } else {
            deletePhotoFile(path)
        }
    }

    fun launchCamera() {
        saveNoteIfChanged()
        val file = createPhotoFile(context, session.id)
        pendingPhotoPath = file.absolutePath
        cameraLauncher.launch(photoUriFor(context, file))
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

                val photoPath = session.photoPath
                if (photoPath != null) {
                    PhotoThumbnail(path = photoPath)
                    TextButton(onClick = { launchCamera() }) { Text("Rifai foto") }
                } else {
                    Button(onClick = { launchCamera() }) { Text("Scatta foto") }
                }
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
