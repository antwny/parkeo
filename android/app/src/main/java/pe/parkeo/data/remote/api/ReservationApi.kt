// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Contrato Retrofit; declara las operaciones HTTP y los tipos de datos intercambiados con la API.
package pe.parkeo.data.remote.api

import pe.parkeo.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ReservationApi {
    @GET("api/reservations")
    /** Consulta y devuelve: get my reservations. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun getMyReservations(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponseDto<PageDto<ReservationDto>>>

    @GET("api/reservations/{id}")
    /** Consulta y devuelve: get reservation detail. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun getReservationDetail(@Path("id") id: Long): Response<ApiResponseDto<ReservationDto>>

    @POST("api/reservations")
    /** Crea y devuelve: create reservation. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun createReservation(@Body request: CreateReservationRequestDto): Response<ApiResponseDto<ReservationDto>>

    @PATCH("api/reservations/{id}/cancel")
    /** Valida y cancela la reserva indicada: cancel reservation. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun cancelReservation(
        @Path("id") id: Long,
        @Body body: CancelReservationRequestDto = CancelReservationRequestDto()
    ): Response<ApiResponseDto<ReservationDto>>
}