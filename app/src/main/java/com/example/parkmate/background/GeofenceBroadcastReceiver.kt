package com.example.parkmate.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.GeofencingEvent

private const val TAG = "GeofenceBroadcastReceiver"

// riceve le transizioni di ingresso/uscita dai geofence registrati
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.e(TAG, "Errore geofence: ${event.errorCode}")
            return
        }
        Log.d(TAG, "Transizione ${event.geofenceTransition} per ${event.triggeringGeofences?.map { it.requestId }}")
    }
}
