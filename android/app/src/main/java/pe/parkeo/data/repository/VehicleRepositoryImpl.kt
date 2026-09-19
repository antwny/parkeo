package pe.parkeo.data.repository

import pe.parkeo.data.remote.api.VehicleApi
import pe.parkeo.data.remote.dto.*
import pe.parkeo.domain.repository.VehicleRepository
import pe.parkeo.util.Result

class VehicleRepositoryImpl(
    private val vehicleApi: VehicleApi
) : VehicleRepository {

    override suspend fun getMyVehicles(): Result<List<VehicleDto>> {
        return try {
            val response = vehicleApi.getMyVehicles()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()!!.data ?: emptyList())
            } else Result.Error(response.body()?.message ?: "Error")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    override suspend fun createVehicle(
        licensePlate: String,
        brand: String?,
        model: String?,
        color: String?,
        vehicleTypeId: Long
    ): Result<VehicleDto> {
        return try {
            val response = vehicleApi.createVehicle(
                CreateVehicleRequestDto(licensePlate, brand, model, color, vehicleTypeId)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()!!.data!!)
            } else Result.Error(response.body()?.message ?: "Error al crear vehículo")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    override suspend fun updateVehicle(
        id: Long,
        licensePlate: String,
        brand: String?,
        model: String?,
        color: String?,
        vehicleTypeId: Long
    ): Result<VehicleDto> {
        return try {
            val response = vehicleApi.updateVehicle(
                id, CreateVehicleRequestDto(licensePlate, brand, model, color, vehicleTypeId)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()!!.data!!)
            } else Result.Error(response.body()?.message ?: "Error al actualizar vehículo")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    override suspend fun deleteVehicle(id: Long): Result<Unit> {
        return try {
            val response = vehicleApi.deleteVehicle(id)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(Unit)
            } else Result.Error(response.body()?.message ?: "Error al eliminar vehículo")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    override suspend fun getVehicleTypes(): Result<List<VehicleTypeDto>> {
        return try {
            val response = vehicleApi.getVehicleTypes()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()!!.data ?: emptyList())
            } else Result.Error(response.body()?.message ?: "Error")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }
}
