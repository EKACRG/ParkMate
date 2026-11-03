package com.example.parkmate.background

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.parkmate.MainActivity
import com.example.parkmate.ParkMateApplication
import com.example.parkmate.R
import com.example.parkmate.data.local.entity.SavedLocation
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "GeofenceBroadcastReceiver"

// extra letti da MainActivity al tap sulla notifica
const val EXTRA_GEOFENCE_SAVED_LOCATION_ID = "geofence_saved_location_id"
const val EXTRA_GEOFENCE_TRANSITION = "geofence_transition"

// un notification id è univoco per app, non per canale: senza questo numero sommato all'id
// della saved location, una saved location e un ticket con lo stesso id (sequenze indipendenti)
// si sovrascriverebbero a vicenda in tendina. Nessuna sosta in questo progetto arriverà a un milione
private const val GEOFENCE_NOTIFICATION_ID_BASE = 1_000_000

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.e(TAG, "Errore geofence: ${event.errorCode}")
            return
        }
        val transition = event.geofenceTransition
        val savedLocationIds = event.triggeringGeofences?.mapNotNull { it.requestId.toLongOrNull() }
        if (savedLocationIds.isNullOrEmpty()) return

        // tiene vivo il receiver mentre la coroutine legge dal database e posta la notifica
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val application = context.applicationContext as ParkMateApplication
                savedLocationIds.forEach { savedLocationId ->
                    handleTransition(application, savedLocationId, transition)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Errore durante la gestione del geofence", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    // legge il nome del luogo e smista verso la notifica giusta
    private suspend fun handleTransition(application: ParkMateApplication, savedLocationId: Long, transition: Int) {
        val location = application.savedLocationRepository.getById(savedLocationId) ?: return
        when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> notifyEnter(application, location)
            Geofence.GEOFENCE_TRANSITION_EXIT -> notifyExit(application, location)
        }
    }

    // notifica l'ingresso nel geofence, proponendo di avviare una sosta
    private fun notifyEnter(application: ParkMateApplication, location: SavedLocation) {
        notify(
            application,
            location,
            transition = Geofence.GEOFENCE_TRANSITION_ENTER,
            text = "Sei arrivato a ${location.name}. Vuoi avviare una sosta?"
        )
    }

    // notifica l'uscita dal geofence proponendo di terminare la sosta, ma solo se ce n'è una attiva qui
    private suspend fun notifyExit(application: ParkMateApplication, location: SavedLocation) {
        val activeSessions = application.parkingSessionRepository.getActiveSessionsForSavedLocationSnapshot(location.id)
        if (activeSessions.isEmpty()) return

        val text = if (activeSessions.size == 1) {
            "Hai lasciato ${location.name}. Hai una sosta attiva qui: vuoi terminarla?"
        } else {
            "Hai lasciato ${location.name}. Hai ${activeSessions.size} soste attive qui: vuoi terminarle?"
        }
        notify(application, location, transition = Geofence.GEOFENCE_TRANSITION_EXIT, text = text)
    }

    // costruisce e posta la notifica; il tap apre MainActivity sulla Mappa con gli extra del geofence
    private fun notify(application: ParkMateApplication, location: SavedLocation, transition: Int, text: String) {
        val granted = ContextCompat.checkSelfPermission(
            application, android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val notificationId = (GEOFENCE_NOTIFICATION_ID_BASE + location.id).toInt()
        val contentIntent = PendingIntent.getActivity(
            application,
            notificationId,
            Intent(application, MainActivity::class.java).apply {
                putExtra(EXTRA_OPEN_MAP, true)
                putExtra(EXTRA_GEOFENCE_SAVED_LOCATION_ID, location.id)
                putExtra(EXTRA_GEOFENCE_TRANSITION, transition)
            },
            // stesso requestCode per ingresso e uscita della stessa location: senza
            // FLAG_UPDATE_CURRENT, il secondo PendingIntent manterrebbe gli extra del primo
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(application, NOTIFICATION_CHANNEL_GEOFENCE)
            .setSmallIcon(R.drawable.ic_stat_parkmate)
            .setContentTitle("ParkMate")
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        // stesso id per ingresso e uscita della stessa location: la transizione più recente sostituisce la precedente
        NotificationManagerCompat.from(application).notify(notificationId, notification)
    }
}
