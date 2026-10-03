package com.example.parkmate.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.parkmate.data.local.entity.Vehicle
import com.example.parkmate.data.local.model.VehicleWithParkedStatus

@Dao
interface VehicleDao {
    @Insert
    suspend fun insert(vehicle: Vehicle): Long

    @Update
    suspend fun update(vehicle: Vehicle)

    @Query(
        """
        SELECT vehicle.*,
            (SELECT id FROM parking_session
             WHERE parking_session.vehicleId = vehicle.id AND parking_session.endTime IS NULL 
             LIMIT 1) AS activeSessionId
        FROM vehicle
        WHERE vehicle.isArchived = 0
        ORDER BY vehicle.name
        """
    )
    fun getActiveVehiclesWithStatus(): LiveData<List<VehicleWithParkedStatus>>

    @Query("SELECT * FROM vehicle WHERE id = :id")
    suspend fun getById(id: Long): Vehicle?
}
