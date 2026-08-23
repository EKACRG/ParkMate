package com.example.parkmate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.parkmate.data.repository.VehicleRepository
import com.example.parkmate.ui.vehicles.VehiclesViewModel

class ParkMateViewModelFactory(
    private val vehicleRepository: VehicleRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(VehiclesViewModel::class.java) ->
                VehiclesViewModel(vehicleRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
