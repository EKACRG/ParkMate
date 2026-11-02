package com.example.parkmate.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberUpdatedMarkerState

val DEFAULT_MAP_POSITION = LatLng(44.4949, 11.3426)

// valgono solo per questa mini-mappa, non per mappe in generale
val MINI_MAP_HEIGHT = 180.dp
const val MINI_MAP_ZOOM = 16f

// mini-mappa con zoom e pan, condivisa tra il modale di avvio sosta e il form delle saved
// location. onRecenter null nasconde il pulsante: usato da chi non ha più modo di richiedere
// il permesso da qui (il permesso si chiede altrove) e quindi non avrebbe nulla da far fare al tap
@Composable
fun PositionMap(
    pin: LatLng,
    cameraPositionState: CameraPositionState,
    markerTitle: String,
    onMove: (LatLng) -> Unit,
    onRecenter: (() -> Unit)?
) {
    val mapUiSettings = remember { MapUiSettings(zoomControlsEnabled = false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MINI_MAP_HEIGHT)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = mapUiSettings,
            onMapClick = onMove
        ) {
            Marker(
                state = rememberUpdatedMarkerState(position = pin),
                title = markerTitle
            )
        }
        if (onRecenter != null) {
            FilledTonalIconButton(
                onClick = onRecenter,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Ricentra sul GPS")
            }
        }
    }
}
