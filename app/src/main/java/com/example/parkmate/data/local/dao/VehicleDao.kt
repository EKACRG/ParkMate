package com.example.parkmate.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.parkmate.data.local.entity.Vehicle

@Dao
interface VehicleDao {
    @Insert
    suspend fun insert(vehicle: Vehicle): Long

    @Update
    suspend fun update(vehicle: Vehicle)

    @Query("SELECT * FROM vehicle WHERE isArchived = 0 ORDER BY name")
    fun getActiveVehicles(): LiveData<List<Vehicle>>

    @Query("SELECT * FROM vehicle WHERE id = :id")
    suspend fun getById(id: Long): Vehicle?
}
