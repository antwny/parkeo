package pe.parkeo.data.remote.api

import pe.parkeo.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ReservationApi {
    @GET("api/reservations")
    suspend fun getMyReservations(
        @Query("status") status: String? = null
    ): Response<ApiResponseDto<List<ReservationDto>>>

    @GET("api/reservations/{id}")
    suspend fun getReservationDetail(@Path("id") id: Long): Response<ApiResponseDto<ReservationDto>>

    @POST("api/reservations")
    suspend fun createReservation(@Body request: CreateReservationRequestDto): Response<ApiResponseDto<ReservationDto>>

    @PATCH("api/reservations/{id}/cancel")
    suspend fun cancelReservation(@Path("id") id: Long): Response<ApiResponseDto<ReservationDto>>
}
