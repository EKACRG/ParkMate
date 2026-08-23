package com.example.parkmate.ui.vehicles

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkmate.data.local.entity.Vehicle
import com.example.parkmate.data.repository.VehicleRepository
import kotlinx.coroutines.launch

class VehiclesViewModel(private val vehicleRepository: VehicleRepository) : ViewModel() {

    val activeVehicles: LiveData<List<Vehicle>> = vehicleRepository.activeVehicles

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
