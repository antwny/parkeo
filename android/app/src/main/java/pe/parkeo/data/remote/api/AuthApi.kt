/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/data/remote/api/AuthApi.kt
 * Capa: datos/red (contrato Retrofit de autenticación).
 * Propósito: declarar las operaciones HTTP de registro, login, renovación y cierre
 * de sesión. Retrofit genera la implementación en tiempo de ejecución.
 * Conceptos clave: Retrofit, `@POST`, `@Body`, `Response<T>`, `ApiResponseDto<T>`,
 * DTO de petición/respuesta, JSON, endpoints públicos, tokens JWT.
 * Flujo resumido: AuthRepositoryImpl llama a `login` o `register`; si el cuerpo trae
 * `success = true` guarda la sesión en SessionDataStore. `refresh` renueva tokens y
 * `logout` revoca el refresh token remoto.
 * Relación: la crea AppContainer con `retrofit.create(AuthApi::class.java)`; la consume
 * AuthRepositoryImpl. La URL base proviene de `BuildConfig.BASE_URL` y las rutas se
 * concatenan a ella. Todos los endpoints bajo el prefijo `/api/auth/` son públicos en el backend.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.data.remote.api

import pe.parkeo.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Contrato REST del recurso de autenticación.
 *
 * Cada método es `suspend`: Retrofit ejecuta la llamada fuera del hilo principal. El
 * resultado siempre es `Response<ApiResponseDto<T>>`, lo que permite distinguir el
 * código HTTP (`response.code()`, `isSuccessful`) del sobre lógico (`success`,
 * `message`, `data`). Los errores 4xx/5xx llegan en `errorBody()` y los parsea la capa
 * de repositorio; no lanzan excepción por sí solos.
 */
interface AuthApi {
    /**
     * Autentica un usuario con correo y contraseña.
     *
     * `POST /api/auth/login` (público). El backend responde `200 OK` con
     * `success = true` y `data` que contiene los tokens y el usuario. Devuelve `400`
     * si el cuerpo no cumple la validación y `401` si las credenciales son inválidas.
     * El repositorio guarda la sesión solo cuando el HTTP es exitoso y `success` es
     * verdadero.
     *
     * @param request credenciales en JSON: `email` y `password`.
     * @return sobre con `AuthResponseDto` (tokens + usuario) o mensaje de error.
     */
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<ApiResponseDto<AuthResponseDto>>

    /**
     * Registra una cuenta nueva y devuelve una sesión iniciada.
     *
     * `POST /api/auth/register` (público). El backend responde `201 Created` con
     * `AuthResponseDto`; `400` si los datos no cumplen las validaciones y `409` si el
     * correo ya está registrado. El usuario creado recibe el rol `ROLE_USER` salvo que
     * el backend aplique otra regla.
     *
     * @param request datos de alta: correo, contraseña, nombre, apellido y teléfono
     *   opcional.
     * @return sobre con la sesión recién creada o el error de validación/conflicto.
     */
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<ApiResponseDto<AuthResponseDto>>

    /**
     * Renueva la sesión a partir del refresh token.
     *
     * `POST /api/auth/refresh` (público). El backend valida que el token exista, no
     * esté revocado y no haya expirado; responde `200 OK` con un par de tokens nuevo
     * y el usuario. Devuelve `401` si el token es inválido o expiró. El backend revoca
     * el refresh token anterior (rotación), por lo que el repositorio debe reemplazar
     * ambos tokens.
     *
     * @param request cuerpo con el `refreshToken` actual.
     * @return sobre con `AuthResponseDto` renovado o error de autenticación.
     */
    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequestDto): Response<ApiResponseDto<AuthResponseDto>>

    /**
     * Cierra la sesión revocando el refresh token en el servidor.
     *
     * `POST /api/auth/logout` (público). Responde `200 OK`; si el token no existe, el
     * backend no falla (operación idempotente). `data` es `null` porque el backend usa
     * `ApiResponse<Void>`, representado aquí como `ApiResponseDto<Unit>`. El
     * repositorio limpia la sesión local aunque esta llamada falle por red.
     *
     * @param request cuerpo con el `refreshToken` que se desea revocar.
     * @return sobre sin datos útiles; el código HTTP y `success` indican el resultado.
     */
    @POST("api/auth/logout")
    suspend fun logout(@Body request: RefreshTokenRequestDto): Response<ApiResponseDto<Unit>>
}
