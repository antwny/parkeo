package pe.parkeo.domain.repository

import pe.parkeo.data.remote.dto.*
import pe.parkeo.util.Result

interface ParkingRepository {
    suspend fun getParkingLots(): Result<List<ParkingLotDto>>
    suspend fun getParkingLotDetail(id: Long): Result<ParkingLotDetailDto>
    suspend fun getNearbyParkingLots(lat: Double, lon: Double, radius: Double = 5.0): Result<List<ParkingLotDto>>
    suspend fun searchParkingLots(query: String): Result<List<ParkingLotDto>>
    suspend fun getAvailability(id: Long, date: String?): Result<AvailabilityDto>
    suspend fun getAvailableSpaces(id: Long, startTime: String, endTime: String, vehicleTypeId: Long?): Result<List<ParkingSpaceDto>>
}
