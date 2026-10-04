package com.example.parkmate.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.ParkMateViewModelFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

// centro iniziale della mappa, finché non c'è la posizione GPS
private val BOLOGNA = LatLng(44.4949, 11.3426)
@Composable
fun MapScreen(viewModelFactory: ParkMateViewModelFactory) {
    val viewModel: MapViewModel = viewModel(factory = viewModelFactory)
    val activeSessions by viewModel.activeSessions.observeAsState(emptyList())
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(BOLOGNA, 13f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        // un marker per ogni sosta attiva; la key evita di ricreare il market a ogni ricomposizione
        activeSessions.forEach { session ->
            Marker(
                state = rememberUpdatedMarkerState(
                    position = LatLng(session.latitude, session.longitude)
                ),
                title = "Sosta in corso"
            )
        }
    }
}
