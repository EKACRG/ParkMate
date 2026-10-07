package com.example.parkmate

import android.app.Application
import androidx.room.Room
import com.example.parkmate.data.local.ParkMateDatabase
import com.example.parkmate.data.location.DeviceLocationProvider
import com.example.parkmate.data.repository.ParkingSessionRepository
import com.example.parkmate.data.repository.SavedLocationRepository
import com.example.parkmate.data.repository.VehicleRepository

class ParkMateApplication : Application() {

    private val database: ParkMateDatabase by lazy {
        Room.databaseBuilder(this, ParkMateDatabase::class.java, "parkmate.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    val parkingSessionRepository: ParkingSessionRepository by lazy {
        ParkingSessionRepository(database.parkingSessionDao(), database)
    }

    val vehicleRepository: VehicleRepository by lazy {
        VehicleRepository(database.vehicleDao(), parkingSessionRepository, database)
    }

    val savedLocationRepository: SavedLocationRepository by lazy {
        SavedLocationRepository(database.savedLocationDao(), parkingSessionRepository, database)
    }

    // una sola istanza per tutta l'app, come i repository
    val deviceLocationProvider: DeviceLocationProvider by lazy {
        DeviceLocationProvider(this)
    }
}
