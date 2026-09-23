package com.example.parkmate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.parkmate.data.local.dao.ParkingSessionDao
import com.example.parkmate.data.local.dao.SavedLocationDao
import com.example.parkmate.data.local.dao.VehicleDao
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.data.local.entity.SavedLocation
import com.example.parkmate.data.local.entity.Vehicle

@Database(
    entities = [Vehicle::class, SavedLocation::class, ParkingSession::class],
    version = 1,
    exportSchema = true
)
abstract class ParkMateDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun savedLocationDao(): SavedLocationDao
    abstract fun parkingSessionDao(): ParkingSessionDao
}
