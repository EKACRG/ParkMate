package com.example.parkmate.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.parkmate.data.local.entity.ParkType
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.data.local.entity.SavedLocation
import com.example.parkmate.ui.components.PhotoThumbnail
import com.example.parkmate.util.formatDuration
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

// a differenza di SessionDetailsDialog (sosta attiva), qui le voci possono essere di giorni
// diversi: serve anche la data, non solo l'ora
fun formatSessionDateTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMAT)

fun ParkType.historyLabel(): String = when (this) {
    ParkType.FREE -> "Libera"
    ParkType.HOURLY_PAID -> "Tariffa oraria"
    ParkType.FIXED_TICKET -> "Biglietto fisso"
}

// il nome della saved location se esiste ancora, altrimenti le coordinate: savedLocationId può
// essere null sia perché la sosta era da GPS, sia perché la saved location è stata eliminata nel
// frattempo (SET NULL sulla foreign key, vedi ParkingSessionRepository.detachFromSavedLocation)
private fun positionLabel(session: ParkingSession, savedLocations: List<SavedLocation>): String {
    val location = session.savedLocationId?.let { id -> savedLocations.find { it.id == id } }
    return location?.name ?: "%.5f, %.5f".format(session.latitude, session.longitude)
}

// dettaglio di una voce dello storico, in sola lettura: una sosta chiusa è un fatto registrato,
// non più modificabile (stessa regola già applicata a nota e foto della sosta attiva)
@Composable
fun HistorySessionDetailDialog(
    session: ParkingSession,
    vehicleName: String,
    savedLocations: List<SavedLocation>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(vehicleName) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Tipo: ${session.type.historyLabel()}")
                Text("Inizio: ${formatSessionDateTime(session.startTime)}")
                session.endTime?.let { end ->
                    Text("Fine: ${formatSessionDateTime(end)}")
                    Text("Durata: ${formatDuration(Duration.ofMillis(end - session.startTime))}")
                }
                session.finalCost?.let { cost -> Text("Costo: €%.2f".format(cost)) }
                Text("Posizione: ${positionLabel(session, savedLocations)}")
                session.note?.takeIf { it.isNotBlank() }?.let { note -> Text("Nota: $note") }
                session.photoPath?.let { path ->
                    PhotoThumbnail(path = path, modifier = Modifier.fillMaxWidth().height(200.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi") }
        }
    )
}
