// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Contrato Retrofit; declara las operaciones HTTP y los tipos de datos intercambiados con la API.
package pe.parkeo.data.remote.api

import pe.parkeo.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface OperatorApi {
    @GET("api/operator/parking-lots")
    /** Consulta y devuelve: get my parking lots. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun getMyParkingLots(): Response<ApiResponseDto<List<ParkingLotDto>>>

    @GET("api/operator/parking-lots/{id}/spaces")
    /** Consulta y devuelve: get spaces. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun getSpaces(
        @Path("id") id: Long
    ): Response<ApiResponseDto<List<ParkingSpaceDto>>>

    @PATCH("api/operator/spaces/{spaceId}/status")
    /** Actualiza el recurso o estado indicado: update space status. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun updateSpaceStatus(
        @Path("spaceId") spaceId: Long,
        @Body request: UpdateSpaceStatusRequestDto
    ): Response<ApiResponseDto<ParkingSpaceDto>>

    @GET("api/operator/reservations")
    /** Consulta y devuelve: get reservations. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun getReservations(
        @Query("parkingLotId") parkingLotId: Long,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<ApiResponseDto<PageDto<ReservationDto>>>

    @PATCH("api/operator/reservations/{id}/check-in")
    /** Comprueba la condición indicada: check in reservation. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun checkInReservation(
        @Path("id") id: Long
    ): Response<ApiResponseDto<ReservationDto>>

    @PATCH("api/operator/reservations/{id}/check-out")
    /** Comprueba la condición indicada: check out reservation. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun checkOutReservation(
        @Path("id") id: Long
    ): Response<ApiResponseDto<ReservationDto>>
}
