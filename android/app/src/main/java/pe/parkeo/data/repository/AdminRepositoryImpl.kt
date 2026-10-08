package pe.parkeo.data.repository

import pe.parkeo.data.remote.api.AdminApi
import pe.parkeo.data.remote.dto.*
import pe.parkeo.domain.repository.AdminRepository
import pe.parkeo.util.Result

class AdminRepositoryImpl(
    private val adminApi: AdminApi
) : AdminRepository {

    override suspend fun getStatistics(): Result<AdminStatisticsDto> = safeApiCall {
        val response = adminApi.getStatistics()
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data ?: AdminStatisticsDto()
        } else throw Exception(response.body()?.message ?: "Error al obtener estadísticas")
    }

    override suspend fun getParkingLots(): Result<List<ParkingLotDto>> = safeApiCall {
        val response = adminApi.getParkingLots()
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data?.content ?: emptyList()
        } else throw Exception(response.body()?.message ?: "Error al obtener estacionamientos")
    }

    override suspend fun updateParkingLotStatus(id: Long, isOpen: Boolean?, isActive: Boolean?): Result<ParkingLotDto> = safeApiCall {
        val response = adminApi.updateParkingLotStatus(id, UpdateParkingLotStatusRequestDto(isOpen, isActive))
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data!!
        } else throw Exception(response.body()?.message ?: "Error al actualizar estacionamiento")
    }

    override suspend fun assignOperator(id: Long, operatorId: Long): Result<ParkingLotDto> = safeApiCall {
        val response = adminApi.assignOperator(id, AssignOperatorRequestDto(operatorId))
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data!!
        } else throw Exception(response.body()?.message ?: "Error al asignar operador")
    }

    override suspend fun deactivateParkingLot(id: Long): Result<Unit> = safeApiCall {
        val response = adminApi.deactivateParkingLot(id)
        if (response.isSuccessful && response.body()?.success == true) {
            Unit
        } else throw Exception(response.body()?.message ?: "Error al desactivar")
    }

    override suspend fun getUsers(): Result<List<UserDto>> = safeApiCall {
        val response = adminApi.getUsers()
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data ?: emptyList()
        } else throw Exception(response.body()?.message ?: "Error al obtener usuarios")
    }

    override suspend fun updateUserStatus(id: Long, isActive: Boolean): Result<UserDto> = safeApiCall {
        val response = adminApi.updateUserStatus(id, UpdateUserStatusRequestDto(isActive))
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data!!
        } else throw Exception(response.body()?.message ?: "Error al actualizar estado del usuario")
    }

    private suspend fun <T> safeApiCall(call: suspend () -> T): Result<T> {
        return try {
            Result.Success(call())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }
}
