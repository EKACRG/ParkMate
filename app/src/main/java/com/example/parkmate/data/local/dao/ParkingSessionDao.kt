package com.example.parkmate.data.local.dao

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

    @Query("SELECT * FROM parking_session WHERE vehicleId = :vehicleId AND endTime IS NULL LIMIT 1")
    suspend fun getActiveSessionForVehicle(vehicleId: Long): ParkingSession?

    @Query("SELECT * FROM parking_session WHERE id = :id")
    suspend fun getById(id: Long): ParkingSession?
}
