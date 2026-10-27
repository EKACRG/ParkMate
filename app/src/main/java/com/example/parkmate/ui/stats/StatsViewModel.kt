package com.example.parkmate.ui.stats

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.data.repository.ParkingSessionRepository

// solo lettura, come HistoryViewModel: la schermata Statistiche non modifica nulla.
// Il filtro per periodo e l'aggregazione per giorno sono trasformazioni della UI, non logica di
// dominio: restano nello Screen
class StatsViewModel(
    parkingSessionRepository: ParkingSessionRepository
) : ViewModel() {
    val closedSessions: LiveData<List<ParkingSession>> = parkingSessionRepository.closedSessions
}
