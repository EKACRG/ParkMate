package com.example.parkmate.data.repository

import androidx.lifecycle.LiveData
import com.example.parkmate.data.local.dao.VehicleDao
import com.example.parkmate.data.local.entity.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VehicleRepository(private val vehicleDao: VehicleDao) {

    val activeVehicles: LiveData<List<Vehicle>> = vehicleDao.getActiveVehicles()

    suspend fun addVehicle(vehicle: Vehicle) = withContext(Dispatchers.IO) {
        vehicleDao.insert(normalizeLicensePlate(vehicle))
    }

    suspend fun updateVehicle(vehicle: Vehicle) = withContext(Dispatchers.IO) {
        vehicleDao.update(normalizeLicensePlate(vehicle))
    }

    suspend fun archiveVehicle(vehicle: Vehicle) = withContext(Dispatchers.IO) {
        vehicleDao.update(vehicle.copy(isArchived = true))
    }

    // garantisce che nel DB non finisca mai una targa non normalizzata, a prescindere dal chiamante
    private fun normalizeLicensePlate(vehicle: Vehicle): Vehicle =
        vehicle.copy(licensePlate = vehicle.licensePlate?.trim()?.uppercase()?.takeIf { it.isNotBlank() })
}
