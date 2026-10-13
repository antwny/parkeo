// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Acceso/abstracción de datos para el área indicada; conecta operaciones de negocio con sus fuentes.
package pe.parkeo.data.repository

import pe.parkeo.data.remote.api.VehicleApi
import pe.parkeo.data.remote.dto.*
import pe.parkeo.domain.repository.VehicleRepository
import pe.parkeo.util.Result

class VehicleRepositoryImpl(
    private val vehicleApi: VehicleApi
) : VehicleRepository {

    /** Consulta y devuelve: get my vehicles. Consulte el catálogo de métodos y conceptos en la guía maestra. */
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

    /** Crea y devuelve: create vehicle. Consulte el catálogo de métodos y conceptos en la guía maestra. */
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
            } else {
                val errorMsg = pe.parkeo.util.NetworkErrorParser.parseErrorMessage(
                    response.errorBody(),
                    response.body()?.message ?: "Error al crear vehículo"
                )
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /** Actualiza el recurso o estado indicado: update vehicle. Consulte el catálogo de métodos y conceptos en la guía maestra. */
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
            } else {
                val errorMsg = pe.parkeo.util.NetworkErrorParser.parseErrorMessage(
                    response.errorBody(),
                    response.body()?.message ?: "Error al actualizar vehículo"
                )
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /** Elimina o desactiva el recurso indicado: delete vehicle. Consulte el catálogo de métodos y conceptos en la guía maestra. */
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

    /** Consulta y devuelve: get vehicle types. Consulte el catálogo de métodos y conceptos en la guía maestra. */
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
