package com.example.parkmate.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.parkmate.ParkMateApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// statico, non registrato a runtime: deve arrivare anche con l'app non in esecuzione, per far
// ripartire il processo dopo il riavvio e rifare il lavoro che gli alarm e i geofence perdono
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // tiene vivo il processo mentre la coroutine registra i geofence e gli alarm
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val application = context.applicationContext as ParkMateApplication
                application.geofenceManager.registerAll(
                    application.savedLocationRepository.getGeofencedLocationsSnapshot()
                )
                application.parkingSessionRepository.rescheduleOpenTicketAlarms { vehicleId ->
                    checkNotNull(application.vehicleRepository.getById(vehicleId)) {
                        "Il veicolo $vehicleId deve esistere per una ParkingSession attiva (FK RESTRICT)"
                    }.name
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
