package com.example.parkmate.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ParkType {
    FREE,
    HOURLY_PAID,
    FIXED_TICKET
}

@Entity(
    tableName = "parking_session",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = SavedLocation::class,
            parentColumns = ["id"],
            childColumns = ["savedLocationId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("vehicleId"),
        Index("savedLocationId")
    ]
)
data class ParkingSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val type: ParkType,
    val startTime: Long,
    val endTime: Long? = null,
    val latitude: Double,
    val longitude: Double,
    val savedLocationId: Long? = null,
    val hourlyRate: Double? = null,
    val fixedCost: Double? = null,
    val expiryTime: Long? = null,
    val finalCost: Double? = null,
    val photoPath: String? = null,
    val note: String? = null
)

// costo di una sosta HOURLY_PAID fino a instant: usata sia per la stima dal vivo sia per il
// costo finale alla chiusura, così non divergono
fun ParkingSession.hourlyCostAt(instant: Long): Double {
    val rate = checkNotNull(hourlyRate) { "Sessione HOURLY_PAID (id=$id) senza hourlyRate" }
    val hours = (instant - startTime) / 3_600_000.0
    return hours * rate
}
