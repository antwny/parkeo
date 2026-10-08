package pe.parkeo.data.repository

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pe.parkeo.data.remote.api.ReservationApi
import pe.parkeo.data.remote.dto.*
import pe.parkeo.domain.repository.ReservationRepository
import pe.parkeo.util.Result
import retrofit2.Response

class ReservationRepositoryImpl(
    private val reservationApi: ReservationApi
) : ReservationRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getMyReservations(status: String?): Result<List<ReservationDto>> {
        return try {
            val response = reservationApi.getMyReservations(status)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()!!.data?.content ?: emptyList())
            } else Result.Error(errorMessage(response, "Error al cargar reservas"))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    override suspend fun getReservationDetail(id: Long): Result<ReservationDto> {
        return try {
            val response = reservationApi.getReservationDetail(id)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()!!.data!!)
            } else Result.Error(errorMessage(response, "Error al cargar la reserva"))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    override suspend fun createReservation(
        parkingLotId: Long,
        parkingSpaceId: Long?,
        vehicleId: Long,
        startTime: String,
        endTime: String,
        notes: String?
    ): Result<ReservationDto> {
        return try {
            val response = reservationApi.createReservation(
                CreateReservationRequestDto(
                    parkingLotId = parkingLotId,
                    parkingSpaceId = parkingSpaceId,
                    vehicleId = vehicleId,
                    startTime = startTime,
                    endTime = endTime,
                    notes = notes
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()!!.data!!)
            } else Result.Error(errorMessage(response, "Error al crear reserva"))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    override suspend fun cancelReservation(id: Long, reason: String?): Result<ReservationDto> {
        return try {
            val response = reservationApi.cancelReservation(id, CancelReservationRequestDto(reason))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()!!.data!!)
            } else Result.Error(errorMessage(response, "Error al cancelar reserva"))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Cuando el backend responde 400/409/etc., el mensaje viene en el errorBody
     * (ej: "El espacio ya está reservado para el período solicitado"), no en body().
     */
    private fun errorMessage(response: Response<*>, fallback: String): String {
        val raw = try {
            response.errorBody()?.string()
        } catch (e: Exception) {
            null
        }

        if (!raw.isNullOrBlank()) {
            val message = try {
                json.parseToJsonElement(raw).jsonObject["message"]?.jsonPrimitive?.contentOrNull
            } catch (e: Exception) {
                null
            }
            if (!message.isNullOrBlank()) return message
        }

        return when (response.code()) {
            401 -> "Tu sesión expiró. Inicia sesión de nuevo"
            403 -> "No tienes permiso para esta acción"
            404 -> "No se encontró lo que buscas"
            409 -> "Ese espacio u horario ya está ocupado"
            else -> fallback
        }
    }
}