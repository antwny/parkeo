// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Contrato Retrofit; declara las operaciones HTTP y los tipos de datos intercambiados con la API.
package pe.parkeo.data.remote.api

import pe.parkeo.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface VehicleApi {
    @GET("api/vehicles")
    /** Consulta y devuelve: get my vehicles. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun getMyVehicles(): Response<ApiResponseDto<List<VehicleDto>>>

    @POST("api/vehicles")
    /** Crea y devuelve: create vehicle. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun createVehicle(@Body request: CreateVehicleRequestDto): Response<ApiResponseDto<VehicleDto>>

    @PUT("api/vehicles/{id}")
    /** Actualiza el recurso o estado indicado: update vehicle. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun updateVehicle(
        @Path("id") id: Long,
        @Body request: CreateVehicleRequestDto
    ): Response<ApiResponseDto<VehicleDto>>

    @DELETE("api/vehicles/{id}")
    /** Elimina o desactiva el recurso indicado: delete vehicle. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun deleteVehicle(@Path("id") id: Long): Response<ApiResponseDto<Unit>>

    @GET("api/vehicles/types")
    /** Consulta y devuelve: get vehicle types. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun getVehicleTypes(): Response<ApiResponseDto<List<VehicleTypeDto>>>
}
