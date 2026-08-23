package com.example.parkmate

import android.app.Application
import androidx.room.Room
import com.example.parkmate.data.local.ParkMateDatabase
import com.example.parkmate.data.repository.LocationRepository
import com.example.parkmate.data.repository.VehicleRepository

class ParkMateApplication : Application() {

    private val database: ParkMateDatabase by lazy {
        Room.databaseBuilder(this, ParkMateDatabase::class.java, "parkmate.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    val vehicleRepository: VehicleRepository by lazy {
        VehicleRepository(database.vehicleDao())
    }

    val locationRepository: LocationRepository by lazy {
        LocationRepository(database.savedLocationDao())
    }
}
