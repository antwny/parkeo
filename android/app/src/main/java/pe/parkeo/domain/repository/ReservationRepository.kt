package pe.parkeo.domain.repository

import pe.parkeo.data.remote.dto.*
import pe.parkeo.util.Result

interface ReservationRepository {
    suspend fun getMyReservations(status: String? = null): Result<List<ReservationDto>>
    suspend fun getReservationDetail(id: Long): Result<ReservationDto>
    suspend fun createReservation(parkingSpaceId: Long, vehicleId: Long, startTime: String, endTime: String): Result<ReservationDto>
    suspend fun cancelReservation(id: Long): Result<ReservationDto>
}
