package com.example.parkmate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.parkmate.background.ActiveSessionReminderScheduler
import com.example.parkmate.background.TicketAlarmScheduler
import com.example.parkmate.data.repository.ParkingSessionRepository
import com.example.parkmate.data.repository.SavedLocationRepository
import com.example.parkmate.data.repository.VehicleRepository
import com.example.parkmate.ui.history.HistoryViewModel
import com.example.parkmate.ui.map.MapViewModel
import com.example.parkmate.ui.map.SavedLocationsViewModel
import com.example.parkmate.ui.settings.SettingsViewModel
import com.example.parkmate.ui.stats.StatsViewModel
import com.example.parkmate.ui.vehicles.VehiclesViewModel

class ParkMateViewModelFactory(
    private val vehicleRepository: VehicleRepository,
    private val savedLocationRepository: SavedLocationRepository,
    private val parkingSessionRepository: ParkingSessionRepository,
    private val ticketAlarmScheduler: TicketAlarmScheduler,
    private val activeSessionReminderScheduler: ActiveSessionReminderScheduler
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(VehiclesViewModel::class.java) ->
                VehiclesViewModel(vehicleRepository, parkingSessionRepository) as T
            modelClass.isAssignableFrom(SavedLocationsViewModel::class.java) ->
                SavedLocationsViewModel(savedLocationRepository) as T
            modelClass.isAssignableFrom(MapViewModel::class.java) ->
                MapViewModel(vehicleRepository, savedLocationRepository, parkingSessionRepository) as T
            modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                HistoryViewModel(vehicleRepository, savedLocationRepository, parkingSessionRepository) as T
            modelClass.isAssignableFrom(StatsViewModel::class.java) ->
                StatsViewModel(parkingSessionRepository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(
                    parkingSessionRepository,
                    vehicleRepository,
                    ticketAlarmScheduler,
                    activeSessionReminderScheduler
                ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
