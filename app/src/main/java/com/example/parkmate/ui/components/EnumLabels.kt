package com.example.parkmate.ui.components

import com.example.parkmate.data.local.entity.ParkType
import com.example.parkmate.data.local.entity.VehicleType

// testo da mostrare per gli enum di dominio: non è una regola di dominio, vive nella UI

fun VehicleType.label(): String = when (this) {
    VehicleType.CAR -> "Auto"
    VehicleType.MOTORCYCLE -> "Moto"
    VehicleType.BICYCLE -> "Bicicletta"
}

fun ParkType.label(): String = when (this) {
    ParkType.FREE -> "Libera"
    ParkType.HOURLY_PAID -> "Oraria"
    ParkType.FIXED_TICKET -> "Ticket"
}
