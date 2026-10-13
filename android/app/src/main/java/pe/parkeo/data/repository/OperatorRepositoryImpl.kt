// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Acceso/abstracción de datos para el área indicada; conecta operaciones de negocio con sus fuentes.
package pe.parkeo.data.repository

import pe.parkeo.data.remote.api.OperatorApi
import pe.parkeo.data.remote.dto.*
import pe.parkeo.domain.repository.OperatorRepository
import pe.parkeo.util.Result

class OperatorRepositoryImpl(
    private val operatorApi: OperatorApi
) : OperatorRepository {

    /** Consulta y devuelve: get my parking lots. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    override suspend fun getMyParkingLots(): Result<List<ParkingLotDto>> = safeApiCall {
        val response = operatorApi.getMyParkingLots()
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data ?: emptyList()
        } else throw Exception(response.body()?.message ?: "Error al obtener estacionamientos")
    }

    /** Consulta y devuelve: get spaces. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    override suspend fun getSpaces(lotId: Long): Result<List<ParkingSpaceDto>> = safeApiCall {
        val response = operatorApi.getSpaces(lotId)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data ?: emptyList()
        } else throw Exception(response.body()?.message ?: "Error al obtener espacios")
    }

    /** Actualiza el recurso o estado indicado: update space status. Consulte el catálogo de métodos y conceptos en la guía maestra. */
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

    /** Consulta y devuelve: get reservations. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    override suspend fun getReservations(lotId: Long, status: String?): Result<List<ReservationDto>> = safeApiCall {
        val response = operatorApi.getReservations(lotId, status)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data?.content ?: emptyList()
        } else throw Exception(extractError(response, "Error al obtener reservas"))
    }

    /** Comprueba la condición indicada: check in. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    override suspend fun checkIn(reservationId: Long): Result<ReservationDto> = safeApiCall {
        val response = operatorApi.checkInReservation(reservationId)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data!!
        } else throw Exception(extractError(response, "Error al registrar ingreso"))
    }

    /** Comprueba la condición indicada: check out. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    override suspend fun checkOut(reservationId: Long): Result<ReservationDto> = safeApiCall {
        val response = operatorApi.checkOutReservation(reservationId)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data!!
        } else throw Exception(extractError(response, "Error al registrar salida"))
    }

    /** Ejecuta la operación indicada por el nombre: extract error. Consulte el catálogo de métodos y conceptos en la guía maestra. */
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

    /** Ejecuta la operación indicada por el nombre: safe api call. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    private suspend fun <T> safeApiCall(call: suspend () -> T): Result<T> {
        return try {
            Result.Success(call())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }
}
