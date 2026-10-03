package com.example.parkmate.data.local.model

import androidx.room.Embedded
import com.example.parkmate.data.local.entity.Vehicle

// risultato della query che unisce Vehicle allo stato parcheggiato
data class VehicleWithParkedStatus(
    @Embedded val vehicle: Vehicle,
    val activeSessionId: Long?
) {
    val isParked = activeSessionId != null
}
