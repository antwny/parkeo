package pe.parkeo.data.remote.api

import pe.parkeo.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface OperatorApi {
    @GET("api/operator/parking-lots")
    suspend fun getMyParkingLots(): Response<ApiResponseDto<List<ParkingLotDto>>>

    @GET("api/operator/parking-lots/{id}/spaces")
    suspend fun getSpaces(
        @Path("id") id: Long
    ): Response<ApiResponseDto<List<ParkingSpaceDto>>>

    @PATCH("api/operator/spaces/{spaceId}/status")
    suspend fun updateSpaceStatus(
        @Path("spaceId") spaceId: Long,
        @Body request: UpdateSpaceStatusRequestDto
    ): Response<ApiResponseDto<ParkingSpaceDto>>

    @GET("api/operator/reservations")
    suspend fun getReservations(
        @Query("parkingLotId") parkingLotId: Long,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<ApiResponseDto<PageDto<ReservationDto>>>

    @PATCH("api/operator/reservations/{id}/check-in")
    suspend fun checkInReservation(
        @Path("id") id: Long
    ): Response<ApiResponseDto<ReservationDto>>

    @PATCH("api/operator/reservations/{id}/check-out")
    suspend fun checkOutReservation(
        @Path("id") id: Long
    ): Response<ApiResponseDto<ReservationDto>>
}
