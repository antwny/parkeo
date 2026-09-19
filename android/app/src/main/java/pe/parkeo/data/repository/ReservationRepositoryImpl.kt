package pe.parkeo.data.repository

import pe.parkeo.data.remote.api.ReservationApi
import pe.parkeo.data.remote.dto.*
import pe.parkeo.domain.repository.ReservationRepository
import pe.parkeo.util.Result

class ReservationRepositoryImpl(
    private val reservationApi: ReservationApi
) : ReservationRepository {

    override suspend fun getMyReservations(status: String?): Result<List<ReservationDto>> {
        return try {
            val response = reservationApi.getMyReservations(status)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()!!.data ?: emptyList())
            } else Result.Error(response.body()?.message ?: "Error")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    override suspend fun getReservationDetail(id: Long): Result<ReservationDto> {
        return try {
            val response = reservationApi.getReservationDetail(id)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()!!.data!!)
            } else Result.Error(response.body()?.message ?: "Error")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    override suspend fun createReservation(
        parkingSpaceId: Long,
        vehicleId: Long,
        startTime: String,
        endTime: String
    ): Result<ReservationDto> {
        return try {
            val response = reservationApi.createReservation(
                CreateReservationRequestDto(parkingSpaceId, vehicleId, startTime, endTime)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()!!.data!!)
            } else Result.Error(response.body()?.message ?: "Error al crear reserva")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    override suspend fun cancelReservation(id: Long): Result<ReservationDto> {
        return try {
            val response = reservationApi.cancelReservation(id)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()!!.data!!)
            } else Result.Error(response.body()?.message ?: "Error al cancelar reserva")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }
}
