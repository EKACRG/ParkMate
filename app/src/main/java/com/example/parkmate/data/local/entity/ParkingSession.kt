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
