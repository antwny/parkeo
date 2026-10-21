package pe.parkeo.domain.repository

import pe.parkeo.data.remote.dto.*
import pe.parkeo.util.Result

interface AdminRepository {
    suspend fun getStatistics(): Result<AdminStatisticsDto>
    suspend fun getParkingLots(): Result<List<ParkingLotDto>>
    suspend fun updateParkingLotStatus(id: Long, isOpen: Boolean?, isActive: Boolean?): Result<ParkingLotDto>
    suspend fun assignOperator(id: Long, operatorId: Long): Result<ParkingLotDto>
    suspend fun deactivateParkingLot(id: Long): Result<Unit>
    suspend fun getUsers(search: String? = null): Result<List<UserDto>>
    suspend fun getReservations(status: String? = null): Result<List<ReservationDto>>
    suspend fun updateUserStatus(id: Long, isActive: Boolean): Result<UserDto>
}
