// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Contrato Retrofit; declara las operaciones HTTP y los tipos de datos intercambiados con la API.
package pe.parkeo.data.remote.api

import pe.parkeo.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ParkingApi {
    @GET("api/parking")
    /** Consulta y devuelve: get parking lots. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun getParkingLots(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponseDto<PageDto<ParkingLotDto>>>

    @GET("api/parking/{id}")
    /** Consulta y devuelve: get parking lot detail. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun getParkingLotDetail(@Path("id") id: Long): Response<ApiResponseDto<ParkingLotDetailDto>>

    @GET("api/parking/nearby")
    /** Consulta y devuelve: get nearby parking lots. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun getNearbyParkingLots(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("radius") radius: Double = 5.0
    ): Response<ApiResponseDto<List<ParkingLotDto>>>

    @GET("api/parking/search")
    /** Ejecuta la operación indicada por el nombre: search parking lots. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun searchParkingLots(
        @Query("q") query: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponseDto<PageDto<ParkingLotDto>>>

    @GET("api/parking/{id}/availability")
    /** Consulta y devuelve: get availability. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun getAvailability(
        @Path("id") id: Long,
        @Query("vehicleTypeId") vehicleTypeId: Long? = null
    ): Response<ApiResponseDto<List<ParkingSpaceDto>>>

    @GET("api/parking/{id}/spaces/available")
    /** Consulta y devuelve: get available spaces. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun getAvailableSpaces(
        @Path("id") id: Long,
        @Query("vehicleTypeId") vehicleTypeId: Long? = null
    ): Response<ApiResponseDto<List<ParkingSpaceDto>>>
}
