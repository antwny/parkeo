package pe.parkeo.data.remote.api

import pe.parkeo.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<ApiResponseDto<AuthResponseDto>>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<ApiResponseDto<AuthResponseDto>>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequestDto): Response<ApiResponseDto<AuthResponseDto>>

    @POST("api/auth/logout")
    suspend fun logout(@Body request: RefreshTokenRequestDto): Response<ApiResponseDto<Unit>>
}
