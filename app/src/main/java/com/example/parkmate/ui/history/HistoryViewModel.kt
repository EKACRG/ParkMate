package com.example.parkmate.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.data.local.entity.SavedLocation
import com.example.parkmate.data.local.entity.Vehicle
import com.example.parkmate.data.repository.ParkingSessionRepository
import com.example.parkmate.data.repository.SavedLocationRepository
import com.example.parkmate.data.repository.VehicleRepository

// solo lettura: lo Storico non modifica nulla, quindi non servono metodi oltre alle tre
// LiveData. Il filtro è una trasformazione della UI, non logica di dominio: resta nello
// Screen, come già fa MapScreen per vehicleNames
class HistoryViewModel(
    vehicleRepository: VehicleRepository,
    savedLocationRepository: SavedLocationRepository,
    parkingSessionRepository: ParkingSessionRepository
) : ViewModel() {
    val closedSessions: LiveData<List<ParkingSession>> = parkingSessionRepository.closedSessions

    // tutti i veicoli, anche gli archiviati: devono restare risolvibili dallo storico
    val vehicles: LiveData<List<Vehicle>> = vehicleRepository.allVehicles

    // per risolvere il nome della saved location nel dettaglio di una sosta
    val savedLocations: LiveData<List<SavedLocation>> = savedLocationRepository.savedLocations
}
