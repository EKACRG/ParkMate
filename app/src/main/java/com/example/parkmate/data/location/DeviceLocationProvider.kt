package com.example.parkmate.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

// tempo massimo di attesa del GPS: oltre, la lettura è considerata fallita
private const val LOCATION_TIMEOUT_MS = 10_000L

class DeviceLocationProvider(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // vero se almeno uno dei due permessi (preciso o approssimativo) è concesso
    fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    // il controllo è già in hasLocationPermission(), Lint non riesce a tracciarlo attraverso
    // una funzione separata e segnala il warning comunque
    // se il GPS non risponde entro LOCATION_TIMEOUT_MS, withTimeoutOrNull restituisce null
    // invece di lasciare la coroutine in attesa per sempre
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) return null

        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            val cancellationTokenSource = CancellationTokenSource()
            suspendCancellableCoroutine { continuation ->
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                )
                    .addOnSuccessListener { location -> continuation.resume(location) }
                    .addOnFailureListener { continuation.resume(null) }
                // se il timeout scatta, la coroutine viene annullata: annulliamo anche la richiesta al GPS
                continuation.invokeOnCancellation { cancellationTokenSource.cancel() }
            }
        }
    }
}
