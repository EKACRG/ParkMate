package com.example.parkmate.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

import com.example.parkmate.data.local.entity.SavedLocation

@Dao
interface SavedLocationDao {
    @Insert
    suspend fun insert(location: SavedLocation): Long

    @Update
    suspend fun update(location: SavedLocation)

    @Delete
    suspend fun delete(location: SavedLocation)

    @Query("SELECT * FROM saved_location ORDER BY name")
    fun getAll(): LiveData<List<SavedLocation>>

    // per la ri-registrazione dei geofence all'avvio dell'app, non osservabile
    @Query("SELECT * FROM saved_location ORDER BY name")
    suspend fun getAllOnce(): List<SavedLocation>

    @Query("SELECT * FROM saved_location WHERE id = :id")
    suspend fun getById(id: Long): SavedLocation?
}
