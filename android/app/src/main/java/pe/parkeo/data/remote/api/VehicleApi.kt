package pe.parkeo.data.remote.api

import pe.parkeo.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface VehicleApi {
    @GET("api/vehicles")
    suspend fun getMyVehicles(): Response<ApiResponseDto<List<VehicleDto>>>

    @POST("api/vehicles")
    suspend fun createVehicle(@Body request: CreateVehicleRequestDto): Response<ApiResponseDto<VehicleDto>>

    @PUT("api/vehicles/{id}")
    suspend fun updateVehicle(
        @Path("id") id: Long,
        @Body request: CreateVehicleRequestDto
    ): Response<ApiResponseDto<VehicleDto>>

    @DELETE("api/vehicles/{id}")
    suspend fun deleteVehicle(@Path("id") id: Long): Response<ApiResponseDto<Unit>>

    @GET("api/vehicles/types")
    suspend fun getVehicleTypes(): Response<ApiResponseDto<List<VehicleTypeDto>>>
}
