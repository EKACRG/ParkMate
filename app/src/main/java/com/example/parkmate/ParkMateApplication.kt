package com.example.parkmate

import android.app.Application
import androidx.room.Room
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.parkmate.background.ActiveSessionReminderWorker
import com.example.parkmate.background.TicketAlarmScheduler
import com.example.parkmate.background.createNotificationChannels
import com.example.parkmate.background.getReminderIntervalMinutes
import com.example.parkmate.data.local.ParkMateDatabase
import com.example.parkmate.data.location.DeviceLocationProvider
import com.example.parkmate.data.repository.ParkingSessionRepository
import com.example.parkmate.data.repository.SavedLocationRepository
import com.example.parkmate.data.repository.VehicleRepository
import java.util.concurrent.TimeUnit

private const val REMINDER_WORK_NAME = "active_session_reminder"

class ParkMateApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels(this)
        enqueueReminderWork()
    }

    private fun enqueueReminderWork() {
        val intervalMinutes = getReminderIntervalMinutes(this).toLong()
        val periodicRequest = PeriodicWorkRequestBuilder<ActiveSessionReminderWorker>(
            intervalMinutes, TimeUnit.MINUTES
        ).build()

        // KEEP: non disturba un lavoro già pianificato con lo stesso nome.
        // Da passare a UPDATE nello Step 4, quando le impostazioni potranno cambiare l'intervallo.
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            REMINDER_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, periodicRequest
        )
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

    val parkingSessionRepository: ParkingSessionRepository by lazy {
        ParkingSessionRepository(database.parkingSessionDao(), database, ticketAlarmScheduler)
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
