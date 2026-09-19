package pe.parkeo.data.remote.api

import pe.parkeo.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ParkingApi {
    @GET("api/parking")
    suspend fun getParkingLots(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponseDto<List<ParkingLotDto>>>

    @GET("api/parking/{id}")
    suspend fun getParkingLotDetail(@Path("id") id: Long): Response<ApiResponseDto<ParkingLotDetailDto>>

    @GET("api/parking/nearby")
    suspend fun getNearbyParkingLots(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("radius") radius: Double = 5.0
    ): Response<ApiResponseDto<List<ParkingLotDto>>>

    @GET("api/parking/search")
    suspend fun searchParkingLots(
        @Query("q") query: String
    ): Response<ApiResponseDto<List<ParkingLotDto>>>

    @GET("api/parking/{id}/availability")
    suspend fun getAvailability(
        @Path("id") id: Long,
        @Query("date") date: String? = null
    ): Response<ApiResponseDto<AvailabilityDto>>

    @GET("api/parking/{id}/spaces/available")
    suspend fun getAvailableSpaces(
        @Path("id") id: Long,
        @Query("startTime") startTime: String,
        @Query("endTime") endTime: String,
        @Query("vehicleTypeId") vehicleTypeId: Long? = null
    ): Response<ApiResponseDto<List<ParkingSpaceDto>>>
}
