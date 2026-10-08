package pe.parkeo.domain.repository

import pe.parkeo.data.remote.dto.ReservationDto
import pe.parkeo.util.Result

interface ReservationRepository {

    suspend fun getMyReservations(status: String?): Result<List<ReservationDto>>

    suspend fun getReservationDetail(id: Long): Result<ReservationDto>

    suspend fun createReservation(
        parkingLotId: Long,
        parkingSpaceId: Long? = null,
        vehicleId: Long,
        startTime: String,
        endTime: String,
        notes: String? = null
    ): Result<ReservationDto>

    suspend fun cancelReservation(id: Long, reason: String? = null): Result<ReservationDto>
}