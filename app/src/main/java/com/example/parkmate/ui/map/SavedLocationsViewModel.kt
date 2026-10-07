package com.example.parkmate.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkmate.data.local.entity.SavedLocation
import com.example.parkmate.data.repository.SavedLocationRepository
import kotlinx.coroutines.launch

class SavedLocationsViewModel(private val savedLocationRepository: SavedLocationRepository): ViewModel() {
    val savedLocations: LiveData<List<SavedLocation>> = savedLocationRepository.savedLocations

    fun addSavedLocation(location: SavedLocation) {
        viewModelScope.launch {
            savedLocationRepository.addLocation(location)
        }
    }

    fun updateSavedLocation(location: SavedLocation) {
        viewModelScope.launch {
            savedLocationRepository.updateLocation(location)
        }
    }

    fun deleteSavedLocation(location: SavedLocation) {
        viewModelScope.launch {
            savedLocationRepository.deleteLocation(location)
        }
    }
}
