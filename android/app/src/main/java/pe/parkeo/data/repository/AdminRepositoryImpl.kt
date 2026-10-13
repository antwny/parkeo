// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Acceso/abstracción de datos para el área indicada; conecta operaciones de negocio con sus fuentes.
package pe.parkeo.data.repository

import pe.parkeo.data.remote.api.AdminApi
import pe.parkeo.data.remote.dto.*
import pe.parkeo.domain.repository.AdminRepository
import pe.parkeo.util.Result

class AdminRepositoryImpl(
    private val adminApi: AdminApi
) : AdminRepository {

    /** Consulta y devuelve: get statistics. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    override suspend fun getStatistics(): Result<AdminStatisticsDto> = safeApiCall {
        val response = adminApi.getStatistics()
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data ?: AdminStatisticsDto()
        } else throw Exception(response.body()?.message ?: "Error al obtener estadísticas")
    }

    /** Consulta y devuelve: get parking lots. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    override suspend fun getParkingLots(): Result<List<ParkingLotDto>> = safeApiCall {
        val response = adminApi.getParkingLots()
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data?.content ?: emptyList()
        } else throw Exception(response.body()?.message ?: "Error al obtener estacionamientos")
    }

    /** Actualiza el recurso o estado indicado: update parking lot status. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    override suspend fun updateParkingLotStatus(id: Long, isOpen: Boolean?, isActive: Boolean?): Result<ParkingLotDto> = safeApiCall {
        val response = adminApi.updateParkingLotStatus(id, UpdateParkingLotStatusRequestDto(isOpen, isActive))
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data!!
        } else throw Exception(response.body()?.message ?: "Error al actualizar estacionamiento")
    }

    /** Asigna el recurso al destino indicado: assign operator. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    override suspend fun assignOperator(id: Long, operatorId: Long): Result<ParkingLotDto> = safeApiCall {
        val response = adminApi.assignOperator(id, AssignOperatorRequestDto(operatorId))
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data!!
        } else throw Exception(response.body()?.message ?: "Error al asignar operador")
    }

    /** Ejecuta la operación indicada por el nombre: deactivate parking lot. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    override suspend fun deactivateParkingLot(id: Long): Result<Unit> = safeApiCall {
        val response = adminApi.deactivateParkingLot(id)
        if (response.isSuccessful && response.body()?.success == true) {
            Unit
        } else throw Exception(response.body()?.message ?: "Error al desactivar")
    }

    /** Consulta y devuelve: get users. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    override suspend fun getUsers(): Result<List<UserDto>> = safeApiCall {
        val response = adminApi.getUsers()
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data ?: emptyList()
        } else throw Exception(response.body()?.message ?: "Error al obtener usuarios")
    }

    /** Actualiza el recurso o estado indicado: update user status. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    override suspend fun updateUserStatus(id: Long, isActive: Boolean): Result<UserDto> = safeApiCall {
        val response = adminApi.updateUserStatus(id, UpdateUserStatusRequestDto(isActive))
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data!!
        } else throw Exception(response.body()?.message ?: "Error al actualizar estado del usuario")
    }

    /** Ejecuta la operación indicada por el nombre: safe api call. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    private suspend fun <T> safeApiCall(call: suspend () -> T): Result<T> {
        return try {
            Result.Success(call())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }
}
