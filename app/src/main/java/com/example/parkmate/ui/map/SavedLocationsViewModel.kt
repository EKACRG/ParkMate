package com.example.parkmate.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkmate.data.local.entity.SavedLocation
import com.example.parkmate.data.repository.LocationRepository
import kotlinx.coroutines.launch

class SavedLocationsViewModel(private val locationRepository: LocationRepository): ViewModel() {
    val savedLocations: LiveData<List<SavedLocation>> = locationRepository.savedLocations

    fun addSavedLocation(location: SavedLocation) {
        viewModelScope.launch {
            locationRepository.addLocation(location)
        }
    }

    fun updateSavedLocation(location: SavedLocation) {
        viewModelScope.launch {
            locationRepository.updateLocation(location)
        }
    }

    fun deleteSavedLocation(location: SavedLocation) {
        viewModelScope.launch {
            locationRepository.deleteLocation(location)
        }
    }
}
