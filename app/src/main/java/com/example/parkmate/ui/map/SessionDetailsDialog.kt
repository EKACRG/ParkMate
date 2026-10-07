package com.example.parkmate.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.parkmate.data.local.entity.ParkType
import com.example.parkmate.data.local.entity.ParkingSession
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

private fun formatDuration(duration: Duration): String {
    val minutes = duration.toMinutes() % 60
    return "${duration.toHours()}h ${minutes}m"
}

private fun formatCountdown(duration: Duration): String {
    val totalSeconds = duration.seconds
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "${hours}h ${minutes}m ${seconds}s"
}

// scheda di sola lettura per una sosta attiva, aperta con il tap sul marker
@Composable
fun SessionDetailsDialog(
    session: ParkingSession,
    vehicleName: String,
    onDismiss: () -> Unit,
    onEndSession: () -> Unit
) {
    var now by remember(session.id) { mutableStateOf(System.currentTimeMillis()) }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(vehicleName) },
        text = {
            Column {
                Text("Tipo: ${session.type.displayLabel()}")
                Text("Inizio: ${formatTime(session.startTime)}")

                when (session.type) {
                    ParkType.FREE -> Unit
                    ParkType.HOURLY_PAID -> {
                        val elapsed = Duration.ofMillis((now - session.startTime).coerceAtLeast(0))
                        Text("Tempo trascorso: ${formatDuration(elapsed)}")
                        session.hourlyRate?.let { rate ->
                            val cost = elapsed.toMillis() / 3_600_000.0 * rate
                            Text("Costo stimato: €%.2f".format(cost))
                        }
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
            }
        },
        confirmButton = {
            TextButton(onClick = onEndSession) { Text("Termina sosta") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi") }
        }
    )
}
