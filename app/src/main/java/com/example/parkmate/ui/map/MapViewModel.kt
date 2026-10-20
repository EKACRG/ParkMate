package com.example.parkmate.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkmate.data.local.entity.ParkType
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.data.local.entity.SavedLocation
import com.example.parkmate.data.local.entity.Vehicle
import com.example.parkmate.data.repository.ParkingSessionRepository
import com.example.parkmate.data.repository.SavedLocationRepository
import com.example.parkmate.data.repository.VehicleRepository
import kotlinx.coroutines.launch

class MapViewModel(
    private val vehicleRepository: VehicleRepository,
    private val savedLocationRepository: SavedLocationRepository,
    private val parkingSessionRepository: ParkingSessionRepository
): ViewModel() {
    // marker sulla mappa
    val activeSessions: LiveData<List<ParkingSession>> = parkingSessionRepository.activeSessions

    // layer saved location e lista del modale di avvio
    val savedLocations: LiveData<List<SavedLocation>> = savedLocationRepository.savedLocations

    // veicoli selezionabili nel modale
    val vehicles: LiveData<List<Vehicle>> = vehicleRepository.activeVehicles

    fun startSession(
        vehicleId: Long,
        vehicleName: String,
        type: ParkType,
        latitude: Double,
        longitude: Double,
        savedLocationId: Long?,
        hourlyRate: Double?,
        fixedCost: Double?,
        expiryTime: Long?,
        note: String? = null,
        photoPath: String? = null
    ) {
        viewModelScope.launch {
            val sessionId = parkingSessionRepository.startSession(
                vehicleId = vehicleId,
                vehicleName = vehicleName,
                type = type,
                latitude = latitude,
                longitude = longitude,
                savedLocationId = savedLocationId,
                hourlyRate = hourlyRate,
                fixedCost = fixedCost,
                expiryTime = expiryTime
            )
            // nota e foto sono facoltative e arrivano dallo step 2/2 del modale di avvio:
            // la sessione esiste solo dopo startSession, quindi vanno salvate di seguito
            if (note != null) parkingSessionRepository.updateNote(sessionId, note)
            if (photoPath != null) parkingSessionRepository.updatePhotoPath(sessionId, photoPath)
        }
    }

    fun endSession(sessionId: Long) {
        viewModelScope.launch {
            parkingSessionRepository.endSession(sessionId)
        }
    }

    fun updateNote(sessionId: Long, note: String?) {
        viewModelScope.launch {
            parkingSessionRepository.updateNote(sessionId, note)
        }
    }

    fun updatePhotoPath(sessionId: Long, photoPath: String?) {
        viewModelScope.launch {
            parkingSessionRepository.updatePhotoPath(sessionId, photoPath)
        }
    }
}
