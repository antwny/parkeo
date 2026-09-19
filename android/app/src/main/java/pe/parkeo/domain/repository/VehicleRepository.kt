package pe.parkeo.domain.repository

import pe.parkeo.data.remote.dto.*
import pe.parkeo.util.Result

interface VehicleRepository {
    suspend fun getMyVehicles(): Result<List<VehicleDto>>
    suspend fun createVehicle(licensePlate: String, brand: String?, model: String?, color: String?, vehicleTypeId: Long): Result<VehicleDto>
    suspend fun updateVehicle(id: Long, licensePlate: String, brand: String?, model: String?, color: String?, vehicleTypeId: Long): Result<VehicleDto>
    suspend fun deleteVehicle(id: Long): Result<Unit>
    suspend fun getVehicleTypes(): Result<List<VehicleTypeDto>>
}
