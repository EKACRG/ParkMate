package com.example.parkmate.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberUpdatedMarkerState

val DEFAULT_MAP_POSITION = LatLng(44.4949, 11.3426)

// valgono solo per questa mini-mappa, non per mappe in generale
val MINI_MAP_HEIGHT = 180.dp
const val MINI_MAP_ZOOM = 16f

// mini-mappa con zoom e pan, condivisa tra il modale di avvio sosta e il form delle saved
// location. Se la posizione GPS non è disponibile (permesso negato),
// il pulsante "Ricentra" non ha senso e non deve comparire.
@Composable
fun PositionMap(
    pin: LatLng,
    cameraPositionState: CameraPositionState,
    markerTitle: String,
    onMove: (LatLng) -> Unit,
    onRecenter: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MINI_MAP_HEIGHT)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = onMove
        ) {
            Marker(
                state = rememberUpdatedMarkerState(position = pin),
                title = markerTitle
            )
        }
        if (onRecenter != null) {
            FilledTonalButton(
                onClick = onRecenter,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Text("Ricentra sul GPS")
            }
        }
    }
}
