package com.example.parkmate.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkmate.background.ActiveSessionReminderScheduler
import com.example.parkmate.background.TicketAlarmScheduler
import com.example.parkmate.data.repository.ParkingSessionRepository
import com.example.parkmate.data.repository.VehicleRepository
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val parkingSessionRepository: ParkingSessionRepository,
    private val vehicleRepository: VehicleRepository,
    private val ticketAlarmScheduler: TicketAlarmScheduler,
    private val reminderScheduler: ActiveSessionReminderScheduler
) : ViewModel() {

    private val _expiryWarningMinutes = MutableLiveData(ticketAlarmScheduler.currentExpiryWarningMinutes())
    val expiryWarningMinutes: LiveData<Int> = _expiryWarningMinutes

    private val _reminderIntervalMinutes = MutableLiveData(reminderScheduler.currentIntervalMinutes())
    val reminderIntervalMinutes: LiveData<Int> = _reminderIntervalMinutes

    // quante sessioni FIXED_TICKET aperte non riceveranno più l'avviso anticipato con il nuovo
    // valore; null = nessun salvataggio recente da segnalare
    private val _suppressedWarningCount = MutableLiveData<Int?>(null)
    val suppressedWarningCount: LiveData<Int?> = _suppressedWarningCount

    fun saveExpiryWarningMinutes(minutes: Int) {
        ticketAlarmScheduler.updateExpiryWarningMinutes(minutes)
        _expiryWarningMinutes.value = minutes
        viewModelScope.launch {
            _suppressedWarningCount.value = parkingSessionRepository.rescheduleOpenTicketAlarms { vehicleId ->
                checkNotNull(vehicleRepository.getById(vehicleId)) {
                    "Vehicle $vehicleId must exist for an active ParkingSession (FK RESTRICT)"
                }.name
            }
        }
    }

    fun saveReminderIntervalMinutes(minutes: Int) {
        reminderScheduler.updateInterval(minutes)
        _reminderIntervalMinutes.value = minutes
    }
}
