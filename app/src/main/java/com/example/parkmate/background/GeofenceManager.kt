package com.example.parkmate.background

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.parkmate.data.local.entity.SavedLocation
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

private const val TAG = "GeofenceManager"

// sa come registrare un geofence, non quando: lo decide SavedLocationRepository
class GeofenceManager(private val context: Context) {
    private val geofencingClient = LocationServices.getGeofencingClient(context)

    // FLAG_MUTABLE: Play Services scrive i dati dell'evento in questo Intent prima di
    // consegnarlo al receiver, altrimenti arriverebbe vuoto
    private val pendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_MUTABLE
        )
    }

    fun register(location: SavedLocation) {
        val radius = location.geofenceRadiusMeters ?: return

        val geofence = Geofence.Builder()
            .setRequestId(location.id.toString())
            .setCircularRegion(location.latitude, location.longitude, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        // se il dispositivo è già dentro l'area, genera subito un ingresso
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        try {
            geofencingClient.addGeofences(request, pendingIntent)
                .addOnFailureListener { e -> Log.e(TAG, "Registrazione geofence fallita per ${location.id}", e) }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permesso di posizione mancante, geofence non registrato per ${location.id}", e)
        }
    }

    fun unregister(locationId: Long) {
        geofencingClient.removeGeofences(listOf(locationId.toString()))
            .addOnFailureListener { e -> Log.e(TAG, "Rimozione geofence fallita per $locationId", e) }
    }

    fun registerAll(locations: List<SavedLocation>) {
        locations.filter { it.geofenceRadiusMeters != null }.forEach { register(it) }
    }
}
