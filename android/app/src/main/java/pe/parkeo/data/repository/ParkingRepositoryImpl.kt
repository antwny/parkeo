package pe.parkeo.data.repository

import pe.parkeo.data.remote.api.ParkingApi
import pe.parkeo.data.remote.dto.*
import pe.parkeo.domain.repository.ParkingRepository
import pe.parkeo.util.Result

class ParkingRepositoryImpl(
    private val parkingApi: ParkingApi
) : ParkingRepository {

    override suspend fun getParkingLots(): Result<List<ParkingLotDto>> = safeApiCall {
        val response = parkingApi.getParkingLots()
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data ?: emptyList()
        } else throw Exception(response.body()?.message ?: "Error al obtener estacionamientos")
    }

    override suspend fun getParkingLotDetail(id: Long): Result<ParkingLotDetailDto> = safeApiCall {
        val response = parkingApi.getParkingLotDetail(id)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data ?: throw Exception("Estacionamiento no encontrado")
        } else throw Exception(response.body()?.message ?: "Error")
    }

    override suspend fun getNearbyParkingLots(lat: Double, lon: Double, radius: Double): Result<List<ParkingLotDto>> = safeApiCall {
        val response = parkingApi.getNearbyParkingLots(lat, lon, radius)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data ?: emptyList()
        } else throw Exception(response.body()?.message ?: "Error")
    }

    override suspend fun searchParkingLots(query: String): Result<List<ParkingLotDto>> = safeApiCall {
        val response = parkingApi.searchParkingLots(query)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data ?: emptyList()
        } else throw Exception(response.body()?.message ?: "Error en búsqueda")
    }

    override suspend fun getAvailability(id: Long, date: String?): Result<AvailabilityDto> = safeApiCall {
        val response = parkingApi.getAvailability(id, date)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data ?: throw Exception("Disponibilidad no disponible")
        } else throw Exception(response.body()?.message ?: "Error")
    }

    override suspend fun getAvailableSpaces(
        id: Long,
        startTime: String,
        endTime: String,
        vehicleTypeId: Long?
    ): Result<List<ParkingSpaceDto>> = safeApiCall {
        val response = parkingApi.getAvailableSpaces(id, startTime, endTime, vehicleTypeId)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data ?: emptyList()
        } else throw Exception(response.body()?.message ?: "Error")
    }

    private suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> {
        return try {
            Result.Success(apiCall())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error desconocido")
        }
    }
}
