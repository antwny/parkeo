// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Contrato Retrofit; declara las operaciones HTTP y los tipos de datos intercambiados con la API.
package pe.parkeo.data.remote.api

import pe.parkeo.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {
    @POST("api/auth/login")
    /** Autentica las credenciales y actualiza el estado de sesión: login. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun login(@Body request: LoginRequestDto): Response<ApiResponseDto<AuthResponseDto>>

    @POST("api/auth/register")
    /** Registra y devuelve: register. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun register(@Body request: RegisterRequestDto): Response<ApiResponseDto<AuthResponseDto>>

    @POST("api/auth/refresh")
    /** Ejecuta la operación indicada por el nombre: refresh. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun refresh(@Body request: RefreshTokenRequestDto): Response<ApiResponseDto<AuthResponseDto>>

    @POST("api/auth/logout")
    /** Cierra la sesión y limpia el estado local/remoto correspondiente: logout. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun logout(@Body request: RefreshTokenRequestDto): Response<ApiResponseDto<Unit>>
}
