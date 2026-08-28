package com.example.parkmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VehicleType {
    CAR,
    MOTORCYCLE,
    BICYCLE
}

@Entity(tableName = "vehicle")
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: VehicleType,
    val isArchived: Boolean = false
)
