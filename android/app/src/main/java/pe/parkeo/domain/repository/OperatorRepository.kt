package pe.parkeo.domain.repository

import pe.parkeo.data.remote.dto.*
import pe.parkeo.util.Result

interface OperatorRepository {
    suspend fun getMyParkingLots(): Result<List<ParkingLotDto>>
    suspend fun getSpaces(lotId: Long): Result<List<ParkingSpaceDto>>
    suspend fun updateSpaceStatus(spaceId: Long, status: String, notes: String? = null): Result<ParkingSpaceDto>
    suspend fun getReservations(lotId: Long, status: String? = null): Result<List<ReservationDto>>
}
