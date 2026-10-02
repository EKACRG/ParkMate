package com.example.parkmate.data.repository

import androidx.room.withTransaction
import com.example.parkmate.data.local.ParkMateDatabase
import com.example.parkmate.data.local.dao.ParkingSessionDao
import com.example.parkmate.data.local.entity.ParkType
import com.example.parkmate.data.local.entity.ParkingSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ParkingSessionRepository(
    private val parkingSessionDao: ParkingSessionDao,
    private val database: ParkMateDatabase
) {
    suspend fun startSession(
        vehicleId: Long,
        type: ParkType,
        latitude: Double,
        longitude: Double,
        savedLocationId: Long?,
        hourlyRate: Double?,
        fixedCost: Double?,
        expiryTime: Long?
    ): Long = withContext(Dispatchers.IO) {
        val finalCostAtStart: Double? = when (type) {
            ParkType.FREE -> null
            ParkType.HOURLY_PAID -> {
                requireNotNull(hourlyRate) { "Sessione HOURLY_PAID senza hourlyRate" }
                null
            }
            ParkType.FIXED_TICKET -> {
                requireNotNull(fixedCost) { "Sessione FIXED_TICKET senza fixedCost" }
                requireNotNull(expiryTime) { "Sessione FIXED_TICKET senza expiryTime" }
                fixedCost
            }
        }

        val startTime = System.currentTimeMillis()
        val newSession = ParkingSession(
            vehicleId = vehicleId,
            type = type,
            startTime = startTime,
            latitude = latitude,
            longitude = longitude,
            savedLocationId = savedLocationId,
            hourlyRate = hourlyRate,
            fixedCost = fixedCost,
            expiryTime = expiryTime,
            finalCost = finalCostAtStart
        )

        // garantisce atomicità, se qualcosa non funziona si torna allo stato precedente
        database.withTransaction {
            val activeSession = parkingSessionDao.getActiveSessionForVehicle(vehicleId)
            if (activeSession != null) {
                parkingSessionDao.update(closeSession(activeSession, endTime = startTime))
            }
            parkingSessionDao.insert(newSession)
        }
    }

    suspend fun endSession(sessionId: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val session = parkingSessionDao.getById(sessionId)
            if (session != null && session.endTime == null) {
                parkingSessionDao.update(closeSession(session, endTime = System.currentTimeMillis()))
            }
        }
    }

    // chiamato da VehicleRepository quando un veicolo viene archiviato: se ha una sessione
    // attiva va chiusa, non lasciata orfana di un veicolo sparito dalla gestione
    suspend fun endActiveSessionForVehicle(vehicleId: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val session = parkingSessionDao.getActiveSessionForVehicle(vehicleId)
            if (session != null) {
                parkingSessionDao.update(closeSession(session, endTime = System.currentTimeMillis()))
            }
        }
    }

    // chiamato da SavedLocationRepository quando cambiano le coordinate di una saved location:
    // le sessioni collegate perdono il riferimento, non la posizione già copiata
    suspend fun detachFromSavedLocation(savedLocationId: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            parkingSessionDao.clearSavedLocationReference(savedLocationId)
        }
    }

    private fun closeSession(session: ParkingSession, endTime: Long): ParkingSession {
        val finalCost = when (session.type) {
            ParkType.FREE -> null
            ParkType.HOURLY_PAID -> {
                val rate = checkNotNull(session.hourlyRate) {
                    "Sessione HOURLY_PAID (id=${session.id}) senza hourlyRate"
                }
                val hours = (endTime - session.startTime) / 3_600_000.0
                hours * rate
            }
            ParkType.FIXED_TICKET -> {
                checkNotNull(session.fixedCost) {
                    "Sessione FIXED_TICKET (id=${session.id}) senza fixedCost"
                }
            }
        }
        return session.copy(endTime = endTime, finalCost = finalCost)
    }
}
