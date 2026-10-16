package com.example.parkmate

import android.app.Application
import androidx.room.Room
import com.example.parkmate.background.ActiveSessionReminderScheduler
import com.example.parkmate.background.GeofenceManager
import com.example.parkmate.background.TicketAlarmScheduler
import com.example.parkmate.background.createNotificationChannels
import com.example.parkmate.data.local.ParkMateDatabase
import com.example.parkmate.data.location.DeviceLocationProvider
import com.example.parkmate.data.repository.ParkingSessionRepository
import com.example.parkmate.data.repository.SavedLocationRepository
import com.example.parkmate.data.repository.VehicleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ParkMateApplication : Application() {

    // vive quanto il processo, come l'Application stessa. SupervisorJob: se una ri-registrazione
    // fallisce, non deve cancellare le altre coroutine lanciate su questo scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels(this)
        activeSessionReminderScheduler.enqueue()
        applicationScope.launch {
            geofenceManager.registerAll(savedLocationRepository.getGeofencedLocationsSnapshot())
        }
    }

    private val database: ParkMateDatabase by lazy {
        Room.databaseBuilder(this, ParkMateDatabase::class.java, "parkmate.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    // una sola istanza, come gli altri helper Android-specifici
    val ticketAlarmScheduler: TicketAlarmScheduler by lazy {
        TicketAlarmScheduler(this)
    }

    val activeSessionReminderScheduler: ActiveSessionReminderScheduler by lazy {
        ActiveSessionReminderScheduler(this)
    }

    val geofenceManager: GeofenceManager by lazy {
        GeofenceManager(this)
    }

    val parkingSessionRepository: ParkingSessionRepository by lazy {
        ParkingSessionRepository(database.parkingSessionDao(), database, ticketAlarmScheduler)
    }

    val vehicleRepository: VehicleRepository by lazy {
        VehicleRepository(database.vehicleDao(), parkingSessionRepository, database)
    }

    val savedLocationRepository: SavedLocationRepository by lazy {
        SavedLocationRepository(database.savedLocationDao(), parkingSessionRepository, database, geofenceManager)
    }

    // una sola istanza per tutta l'app, come i repository
    val deviceLocationProvider: DeviceLocationProvider by lazy {
        DeviceLocationProvider(this)
    }
}
