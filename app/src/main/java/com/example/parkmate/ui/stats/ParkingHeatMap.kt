package com.example.parkmate.ui.stats

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.ui.components.EmptyState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.heatmaps.HeatmapTileProvider

// distribuzione geografica delle soste chiuse nel periodo scelto. HeatmapTileProvider lancia
// un'eccezione se la lista di punti è vuota: va creato solo con sessions non vuoto
@Composable
fun ParkingHeatMap(sessions: List<ParkingSession>) {
    if (sessions.isEmpty()) {
        EmptyState("Nessuna sosta nel periodo selezionato.")
        return
    }

    val points = remember(sessions) { sessions.map { LatLng(it.latitude, it.longitude) } }
    // un nuovo provider per ogni lista di punti diversa: il composable TileOverlay rimuove da sé
    // l'overlay nativo precedente e ne crea uno nuovo quando il tileProvider passato cambia
    val heatmapProvider = remember(points) { HeatmapTileProvider.Builder().data(points).build() }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(points.first(), 13f)
    }

    val mapUiSettings = remember { MapUiSettings(zoomControlsEnabled = false) }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = mapUiSettings
    ) {
        TileOverlay(tileProvider = heatmapProvider)
    }
}
