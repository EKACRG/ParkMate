package com.example.parkmate.data.repository

import androidx.lifecycle.LiveData
import com.example.parkmate.data.local.dao.SavedLocationDao
import com.example.parkmate.data.local.entity.SavedLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationRepository(private val savedLocationDao: SavedLocationDao) {

    val savedLocations: LiveData<List<SavedLocation>> = savedLocationDao.getAll()

    suspend fun addLocation(location: SavedLocation) = withContext(Dispatchers.IO) {
        savedLocationDao.insert(location)
    }

    suspend fun updateLocation(location: SavedLocation) = withContext(Dispatchers.IO) {
        savedLocationDao.update(location)
    }

    suspend fun deleteLocation(location: SavedLocation) = withContext(Dispatchers.IO) {
        savedLocationDao.delete(location)
    }
}
