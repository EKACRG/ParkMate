package com.example.parkmate.data.repository

import androidx.lifecycle.LiveData
import androidx.room.withTransaction
import com.example.parkmate.data.local.ParkMateDatabase
import com.example.parkmate.data.local.dao.SavedLocationDao
import com.example.parkmate.data.local.entity.SavedLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SavedLocationRepository(
    private val savedLocationDao: SavedLocationDao,
    private val parkingSessionRepository: ParkingSessionRepository,
    private val database: ParkMateDatabase
) {

    val savedLocations: LiveData<List<SavedLocation>> = savedLocationDao.getAll()

    suspend fun addLocation(location: SavedLocation) = withContext(Dispatchers.IO) {
        savedLocationDao.insert(location)
    }

    suspend fun updateLocation(location: SavedLocation) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val existing = savedLocationDao.getById(location.id)
            val coordinatesChanged = existing != null &&
                (existing.latitude != location.latitude || existing.longitude != location.longitude)
            if (coordinatesChanged) {
                parkingSessionRepository.detachFromSavedLocation(location.id)
            }
            savedLocationDao.update(location)
        }
    }

    suspend fun deleteLocation(location: SavedLocation) = withContext(Dispatchers.IO) {
        savedLocationDao.delete(location)
    }
}
