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
        } else throw Exception(extractError(response, "Error al obtener reservas"))
    }

    override suspend fun checkIn(reservationId: Long): Result<ReservationDto> = safeApiCall {
        val response = operatorApi.checkInReservation(reservationId)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data!!
        } else throw Exception(extractError(response, "Error al registrar ingreso"))
    }

    override suspend fun checkOut(reservationId: Long): Result<ReservationDto> = safeApiCall {
        val response = operatorApi.checkOutReservation(reservationId)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data!!
        } else throw Exception(extractError(response, "Error al registrar salida"))
    }

    private fun extractError(response: retrofit2.Response<*>, defaultMsg: String): String {
        return try {
            val raw = response.errorBody()?.string()
            if (!raw.isNullOrBlank()) {
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val obj = json.parseToJsonElement(raw)
                val msg = obj.let {
                    (it as? kotlinx.serialization.json.JsonObject)?.get("message")?.let { elem ->
                        (elem as? kotlinx.serialization.json.JsonPrimitive)?.content
                    }
                }
                msg ?: defaultMsg
            } else defaultMsg
        } catch (e: Exception) {
            response.message().ifBlank { defaultMsg }
        }
    }

    private suspend fun <T> safeApiCall(call: suspend () -> T): Result<T> {
        return try {
            Result.Success(call())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }
}
