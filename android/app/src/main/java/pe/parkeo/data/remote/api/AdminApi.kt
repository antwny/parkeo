// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Contrato Retrofit; declara las operaciones HTTP y los tipos de datos intercambiados con la API.
package pe.parkeo.data.remote.api

import pe.parkeo.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface AdminApi {
    @GET("api/admin/statistics")
    /** Consulta y devuelve: get statistics. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun getStatistics(): Response<ApiResponseDto<AdminStatisticsDto>>

    @GET("api/admin/parking-lots")
    /** Consulta y devuelve: get parking lots. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun getParkingLots(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<ApiResponseDto<PageDto<ParkingLotDto>>>

    @PUT("api/admin/parking-lots/{id}/status")
    /** Actualiza el recurso o estado indicado: update parking lot status. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun updateParkingLotStatus(
        @Path("id") id: Long,
        @Body body: UpdateParkingLotStatusRequestDto
    ): Response<ApiResponseDto<ParkingLotDto>>

    @PUT("api/admin/parking-lots/{id}/operator")
    /** Asigna el recurso al destino indicado: assign operator. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun assignOperator(
        @Path("id") id: Long,
        @Body body: AssignOperatorRequestDto
    ): Response<ApiResponseDto<ParkingLotDto>>

    @DELETE("api/admin/parking-lots/{id}")
    /** Ejecuta la operación indicada por el nombre: deactivate parking lot. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun deactivateParkingLot(
        @Path("id") id: Long
    ): Response<ApiResponseDto<Unit>>

    @GET("api/admin/users")
    /** Consulta y devuelve: get users. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun getUsers(): Response<ApiResponseDto<List<UserDto>>>

    @PUT("api/admin/users/{id}/status")
    /** Actualiza el recurso o estado indicado: update user status. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun updateUserStatus(
        @Path("id") id: Long,
        @Body body: UpdateUserStatusRequestDto
    ): Response<ApiResponseDto<UserDto>>
}
