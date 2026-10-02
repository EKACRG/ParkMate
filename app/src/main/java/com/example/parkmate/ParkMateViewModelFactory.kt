package com.example.parkmate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.parkmate.data.repository.SavedLocationRepository
import com.example.parkmate.data.repository.VehicleRepository
import com.example.parkmate.ui.map.SavedLocationsViewModel
import com.example.parkmate.ui.vehicles.VehiclesViewModel

class ParkMateViewModelFactory(
    private val vehicleRepository: VehicleRepository,
    private val savedLocationRepository: SavedLocationRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(VehiclesViewModel::class.java) ->
                VehiclesViewModel(vehicleRepository) as T
            modelClass.isAssignableFrom(SavedLocationsViewModel::class.java) ->
                SavedLocationsViewModel(savedLocationRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
