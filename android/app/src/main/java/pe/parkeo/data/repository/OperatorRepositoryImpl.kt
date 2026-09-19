package pe.parkeo.data.repository

import pe.parkeo.data.remote.api.OperatorApi
import pe.parkeo.data.remote.dto.*
import pe.parkeo.domain.repository.OperatorRepository
import pe.parkeo.util.Result

class OperatorRepositoryImpl(
    private val operatorApi: OperatorApi
) : OperatorRepository {

    override suspend fun getMyParkingLots(): Result<List<ParkingLotDto>> = safeApiCall {
        val response = operatorApi.getMyParkingLots()
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data ?: emptyList()
        } else throw Exception(response.body()?.message ?: "Error al obtener estacionamientos")
    }

    override suspend fun getSpaces(lotId: Long): Result<List<ParkingSpaceDto>> = safeApiCall {
        val response = operatorApi.getSpaces(lotId)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data ?: emptyList()
        } else throw Exception(response.body()?.message ?: "Error al obtener espacios")
    }

    override suspend fun updateSpaceStatus(
        spaceId: Long,
        status: String,
        notes: String?
    ): Result<ParkingSpaceDto> = safeApiCall {
        val response = operatorApi.updateSpaceStatus(spaceId, UpdateSpaceStatusRequestDto(status, notes))
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data!!
        } else throw Exception(response.body()?.message ?: "Error al actualizar estado del espacio")
    }

    override suspend fun getReservations(lotId: Long, status: String?): Result<List<ReservationDto>> = safeApiCall {
        val response = operatorApi.getReservations(lotId, status)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data?.content ?: emptyList()
        } else throw Exception(response.body()?.message ?: "Error al obtener reservas")
    }

    private suspend fun <T> safeApiCall(call: suspend () -> T): Result<T> {
        return try {
            Result.Success(call())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }
}
