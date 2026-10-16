package com.example.parkmate.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.parkmate.data.local.entity.ParkingSession

@Dao
interface ParkingSessionDao {
    @Insert
    suspend fun insert(session: ParkingSession): Long

    @Update
    suspend fun update(session: ParkingSession)

    @Query("SELECT * FROM parking_session WHERE endTime IS NULL")
    fun getActiveSessions(): LiveData<List<ParkingSession>>

    // lettura singola, non osservabile: per il Worker, che non può osservare un LiveData
    @Query("SELECT * FROM parking_session WHERE endTime IS NULL")
    suspend fun getActiveSessionsOnce(): List<ParkingSession>

    @Query("SELECT * FROM parking_session WHERE vehicleId = :vehicleId AND endTime IS NULL LIMIT 1")
    suspend fun getActiveSessionForVehicle(vehicleId: Long): ParkingSession?

    // per il receiver di geofence, che deve decidere se proporre di terminare una sosta all'uscita
    @Query("SELECT * FROM parking_session WHERE savedLocationId = :savedLocationId AND endTime IS NULL")
    suspend fun getActiveSessionsForSavedLocationOnce(savedLocationId: Long): List<ParkingSession>

    @Query("SELECT * FROM parking_session WHERE id = :id")
    suspend fun getById(id: Long): ParkingSession?

    @Query("UPDATE parking_session SET savedLocationId = NULL WHERE savedLocationId = :savedLocationId")
    suspend fun clearSavedLocationReference(savedLocationId: Long)
}
