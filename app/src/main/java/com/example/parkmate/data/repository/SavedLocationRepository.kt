package com.example.parkmate.data.repository

import androidx.lifecycle.LiveData
import androidx.room.withTransaction
import com.example.parkmate.background.GeofenceManager
import com.example.parkmate.data.local.ParkMateDatabase
import com.example.parkmate.data.local.dao.SavedLocationDao
import com.example.parkmate.data.local.entity.SavedLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SavedLocationRepository(
    private val savedLocationDao: SavedLocationDao,
    private val parkingSessionRepository: ParkingSessionRepository,
    private val database: ParkMateDatabase,
    private val geofenceManager: GeofenceManager
) {

    val savedLocations: LiveData<List<SavedLocation>> = savedLocationDao.getAll()

    // per il receiver di geofence, che legge il nome del luogo per il testo della notifica
    suspend fun getById(id: Long): SavedLocation? = withContext(Dispatchers.IO) {
        savedLocationDao.getById(id)
    }

    // per la ri-registrazione dei geofence all'avvio dell'app
    suspend fun getGeofencedLocationsSnapshot(): List<SavedLocation> = withContext(Dispatchers.IO) {
        savedLocationDao.getAllOnce().filter { it.geofenceRadiusMeters != null }
    }

    suspend fun addLocation(location: SavedLocation) = withContext(Dispatchers.IO) {
        val newId = savedLocationDao.insert(location)
        if (location.geofenceRadiusMeters != null) {
            geofenceManager.register(location.copy(id = newId))
        }
    }

    suspend fun updateLocation(location: SavedLocation) = withContext(Dispatchers.IO) {
        val existing = database.withTransaction {
            val existing = savedLocationDao.getById(location.id)
            val coordinatesChanged = existing != null &&
                (existing.latitude != location.latitude || existing.longitude != location.longitude)
            if (coordinatesChanged) {
                parkingSessionRepository.detachFromSavedLocation(location.id)
            }
            savedLocationDao.update(location)
            existing
        }

        // il geofence si tocca solo dopo che la transazione è confermata, stesso principio
        // già seguito per gli alarm dei ticket
        if (location.geofenceRadiusMeters != null) {
            geofenceManager.register(location)
        } else if (existing?.geofenceRadiusMeters != null) {
            geofenceManager.unregister(location.id)
        }
    }

    suspend fun deleteLocation(location: SavedLocation) = withContext(Dispatchers.IO) {
        savedLocationDao.delete(location)
        if (location.geofenceRadiusMeters != null) {
            geofenceManager.unregister(location.id)
        }
    }
}
