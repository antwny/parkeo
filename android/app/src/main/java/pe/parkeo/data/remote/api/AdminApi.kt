package pe.parkeo.data.remote.api

import pe.parkeo.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface AdminApi {
    @GET("api/admin/statistics")
    suspend fun getStatistics(): Response<ApiResponseDto<AdminStatisticsDto>>

    @GET("api/admin/parking-lots")
    suspend fun getParkingLots(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<ApiResponseDto<PageDto<ParkingLotDto>>>

    @PUT("api/admin/parking-lots/{id}/status")
    suspend fun updateParkingLotStatus(
        @Path("id") id: Long,
        @Body body: UpdateParkingLotStatusRequestDto
    ): Response<ApiResponseDto<ParkingLotDto>>

    @PUT("api/admin/parking-lots/{id}/operator")
    suspend fun assignOperator(
        @Path("id") id: Long,
        @Body body: AssignOperatorRequestDto
    ): Response<ApiResponseDto<ParkingLotDto>>

    @DELETE("api/admin/parking-lots/{id}")
    suspend fun deactivateParkingLot(
        @Path("id") id: Long
    ): Response<ApiResponseDto<Unit>>

    @GET("api/admin/users")
    suspend fun getUsers(): Response<ApiResponseDto<List<UserDto>>>

    @PUT("api/admin/users/{id}/status")
    suspend fun updateUserStatus(
        @Path("id") id: Long,
        @Body body: UpdateUserStatusRequestDto
    ): Response<ApiResponseDto<UserDto>>
}
