package com.example.parkmate.ui.vehicles

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.data.local.entity.Vehicle
import com.example.parkmate.data.repository.ParkingSessionRepository
import com.example.parkmate.data.repository.VehicleRepository
import kotlinx.coroutines.launch

class VehiclesViewModel(
    private val vehicleRepository: VehicleRepository,
    parkingSessionRepository: ParkingSessionRepository
) : ViewModel() {

    val activeVehicles: LiveData<List<Vehicle>> = vehicleRepository.activeVehicles

    // per avvisare nel dialog di archiviazione se il veicolo ha una sosta in corso
    val activeSessions: LiveData<List<ParkingSession>> = parkingSessionRepository.activeSessions

    fun addVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            vehicleRepository.addVehicle(vehicle)
        }
    }

    fun updateVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            vehicleRepository.updateVehicle(vehicle)
        }
    }

    fun archiveVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            vehicleRepository.archiveVehicle(vehicle)
        }
    }
}
