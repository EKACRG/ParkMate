package com.example.parkmate.data.repository

import androidx.lifecycle.LiveData
import androidx.room.withTransaction
import com.example.parkmate.data.local.ParkMateDatabase
import com.example.parkmate.data.local.dao.VehicleDao
import com.example.parkmate.data.local.entity.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VehicleRepository(
    private val vehicleDao: VehicleDao,
    private val parkingSessionRepository: ParkingSessionRepository,
    private val database: ParkMateDatabase
) {

    val activeVehicles: LiveData<List<Vehicle>> = vehicleDao.getActiveVehicles()

    // per lo storico: anche i veicoli archiviati devono restare risolvibili
    val allVehicles: LiveData<List<Vehicle>> = vehicleDao.getAllVehicles()

    // per il Worker del riepilogo periodico: nome del veicolo di una sessione attiva
    suspend fun getById(vehicleId: Long): Vehicle? = withContext(Dispatchers.IO) {
        vehicleDao.getById(vehicleId)
    }

    // il controllo in UI (VehiclesScreen) dovrebbe già impedire di arrivare qui con un
    // duplicato: check() fa crashare rumorosamente se succede comunque, non lo ignora in
    // silenzio, stessa logica già usata per i campi tipo-specifici di ParkingSession
    suspend fun addVehicle(vehicle: Vehicle) = withContext(Dispatchers.IO) {
        val normalized = normalizeLicensePlate(vehicle)
        database.withTransaction {
            checkNoDuplicates(normalized)
            vehicleDao.insert(normalized)
        }
    }

    suspend fun updateVehicle(vehicle: Vehicle) = withContext(Dispatchers.IO) {
        val normalized = normalizeLicensePlate(vehicle)
        database.withTransaction {
            checkNoDuplicates(normalized)
            vehicleDao.update(normalized)
        }
    }

    private suspend fun checkNoDuplicates(vehicle: Vehicle) {
        check(!vehicleDao.hasActiveDuplicateName(vehicle.name, excludeId = vehicle.id)) {
            "Nome veicolo '${vehicle.name}' già in uso da un altro veicolo attivo"
        }
        vehicle.licensePlate?.let { plate ->
            check(!vehicleDao.hasActiveDuplicateLicensePlate(plate, excludeId = vehicle.id)) {
                "Targa '$plate' già in uso da un altro veicolo attivo"
            }
        }
    }

    // un veicolo archiviato non deve poter restare "parcheggiato"
    // chiude la sessione attiva, se c'è, nella stessa transazione dell'archiviazione
    suspend fun archiveVehicle(vehicle: Vehicle) = withContext(Dispatchers.IO) {
        database.withTransaction {
            parkingSessionRepository.endActiveSessionForVehicle(vehicle.id)
            vehicleDao.update(vehicle.copy(isArchived = true))
        }
    }

    // garantisce che nel DB non finisca mai una targa non normalizzata, a prescindere dal chiamante
    private fun normalizeLicensePlate(vehicle: Vehicle): Vehicle =
        vehicle.copy(licensePlate = vehicle.licensePlate?.trim()?.uppercase()?.takeIf { it.isNotBlank() })
}
