/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/data/repository/AuthRepositoryImpl.kt
 * Capa: datos (implementación de repositorio).
 * Propósito: implementar `AuthRepository` combinando dos fuentes: la API
 * remota de autenticación (`AuthApi`) y el almacenamiento local de sesión
 * (`SessionDataStore`). Traduce respuestas HTTP a `Result` y mantiene los
 * tokens sincronizados con DataStore.
 * Conceptos clave:
 *   - Implementación de contrato: el ViewModel solo conoce la interfaz;
 *     `AppContainer` construye esta clase con sus dependencias.
 *   - `Response` de Retrofit: `isSuccessful` indica HTTP 2xx, pero además se
 *     exige `body().success == true` porque el backend envuelve los datos en
 *     un `ApiResponse` propio.
 *   - `errorBody`: contiene el JSON de error de Spring (validaciones,
 *     credenciales, etc.); `NetworkErrorParser` extrae un mensaje legible.
 *   - Persistencia de sesión: access token, refresh token, id, correo, nombre
 *     y rol se guardan en Preferences DataStore; no es almacenamiento cifrado.
 *   - `Result`: `Success` con datos o `Error` con mensaje y, cuando se conoce,
 *     código HTTP. No se propagan excepciones a la UI.
 *   - DTO frente a modelo: el resultado es `AuthResponseDto`, el mismo tipo del
 *     contrato; no hay un modelo de dominio separado para la sesión.
 * Flujo: login/registro -> `AuthApi` -> backend Spring -> JWT -> guardar en
 * `SessionDataStore`. Logout -> borrar tokens remotos (mejor esfuerzo) y
 * limpiar siempre lo local. Refresh -> rotar tokens y sesión.
 * Relación: `DefaultAppContainer` inyecta `AuthApi` y `SessionDataStore`;
 * `AuthInterceptor` lee el access token para las demás peticiones.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.data.repository

import pe.parkeo.data.local.SessionDataStore
import pe.parkeo.data.remote.api.AuthApi
import pe.parkeo.data.remote.dto.*
import pe.parkeo.domain.repository.AuthRepository
import pe.parkeo.util.Result

/**
 * Implementación concreta de `AuthRepository`.
 *
 * Recibe sus colaboradores por constructor (inyección manual): `authApi` para
 * la red y `sessionDataStore` para la persistencia local. No crea Retrofit ni
 * conoce el `Context`; eso lo resuelve `DefaultAppContainer`.
 *
 * @param authApi Cliente Retrofit con las rutas de autenticación.
 * @param sessionDataStore Almacén local de tokens y datos básicos del usuario.
 */
class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val sessionDataStore: SessionDataStore
) : AuthRepository {

    /**
     * Inicia sesión contra el backend y persiste la sesión si es válida.
     *
     * Secuencia real:
     * 1. Construye `LoginRequestDto` y llama a `authApi.login` (POST
     *    `api/auth/login`).
     * 2. Comprueba dos condiciones: HTTP exitoso (`isSuccessful`) y la
     *    bandera lógica `success` del cuerpo. Una respuesta 200 con
     *    `success = false` también es un fallo.
     * 3. Extrae `data` y guarda access token, refresh token, id, correo,
     *    nombre completo y rol en `SessionDataStore`.
     * 4. Devuelve `Success` con `AuthResponseDto`.
     *
     * Si la respuesta no es válida, intenta extraer el mensaje del
     * `errorBody` (errores de validación de Spring o mensaje superior) con
     * `NetworkErrorParser`, usa el mensaje del cuerpo como respaldo y devuelve
     * `Error` junto con el código HTTP. Los fallos de red, timeout,
     * deserialización o un cuerpo inesperado se capturan y se convierten en
     * `Error` sin código.
     *
     * @param email Correo del usuario.
     * @param password Contraseña enviada solo en el cuerpo de la petición.
     * @return `Result.Success` con tokens y usuario, o `Result.Error` con un
     *   mensaje comprensible. No lanza excepciones al llamador.
     */
    override suspend fun login(email: String, password: String): Result<AuthResponseDto> {
        return try {
            val response = authApi.login(LoginRequestDto(email, password))
            if (response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()!!.data!!
                sessionDataStore.saveSession(
                    accessToken = authData.accessToken,
                    refreshToken = authData.refreshToken,
                    userId = authData.user.id,
                    email = authData.user.email,
                    name = "${authData.user.firstName} ${authData.user.lastName}",
                    role = authData.user.role
                )
                Result.Success(authData)
            } else {
                val errorMsg = pe.parkeo.util.NetworkErrorParser.parseErrorMessage(
                    response.errorBody(),
                    response.body()?.message ?: "Error de autenticación"
                )
                Result.Error(errorMsg, response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Registra una cuenta y, si el backend inicia sesión, la persiste.
     *
     * Sigue la misma secuencia que `login`: construye `RegisterRequestDto`
     * (con `phone` opcional), llama a POST `api/auth/register`, exige HTTP
     * exitoso y `success = true`, guarda la sesión y devuelve el DTO. Los
     * errores de validación del backend (correo duplicado, campos inválidos)
     * se extraen del `errorBody`; los fallos de red se capturan.
     *
     * @param email Correo único del nuevo usuario.
     * @param password Contraseña elegida.
     * @param firstName Nombres.
     * @param lastName Apellidos.
     * @param phone Teléfono opcional; puede ser `null`.
     * @return `Result.Success` con tokens y usuario, o `Result.Error` con el
     *   mensaje del backend o de conexión.
     */
    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String?
    ): Result<AuthResponseDto> {
        return try {
            val response = authApi.register(
                RegisterRequestDto(email, password, firstName, lastName, phone)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()!!.data!!
                sessionDataStore.saveSession(
                    accessToken = authData.accessToken,
                    refreshToken = authData.refreshToken,
                    userId = authData.user.id,
                    email = authData.user.email,
                    name = "${authData.user.firstName} ${authData.user.lastName}",
                    role = authData.user.role
                )
                Result.Success(authData)
            } else {
                val errorMsg = pe.parkeo.util.NetworkErrorParser.parseErrorMessage(
                    response.errorBody(),
                    response.body()?.message ?: "Error al registrarse"
                )
                Result.Error(errorMsg, response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Cierra la sesión local y solicita la revocación remota del refresh token.
     *
     * Secuencia real:
     * 1. Lee el refresh token de DataStore.
     * 2. Si existe y no está en blanco, llama a `authApi.logout` (POST
     *    `api/auth/logout`) con `RefreshTokenRequestDto`. No revisa el estado
     *    de la respuesta: es una operación de "mejor esfuerzo".
     * 3. Cualquier excepción de red se ignora deliberadamente para que el
     *    usuario pueda salir incluso sin conexión.
     * 4. En `finally` limpia siempre la sesión local (tokens y datos de
     *    usuario), por lo que el resultado final es que no hay sesión.
     *
     * No devuelve `Result`; la UI puede asumir que el cierre local ocurrió.
     */
    override suspend fun logout() {
        try {
            val refreshToken = sessionDataStore.getRefreshToken()
            if (!refreshToken.isNullOrBlank()) {
                authApi.logout(RefreshTokenRequestDto(refreshToken))
            }
        } catch (e: Exception) {
            // Ignorar errores de logout en red, siempre limpiar sesión local
        } finally {
            sessionDataStore.clearSession()
        }
    }

    /**
     * Renueva la sesión usando el refresh token almacenado.
     *
     * Secuencia real:
     * 1. Lee el refresh token; si no existe, devuelve `Error("No hay refresh
     *    token")` sin hacer red y sin limpiar la sesión local.
     * 2. Llama a POST `api/auth/refresh` con `RefreshTokenRequestDto`.
     * 3. Si HTTP es exitoso y `success = true`, guarda la nueva sesión
     *    (tokens y datos de usuario) y devuelve `Success`.
     * 4. Si el backend rechaza la solicitud, limpia la sesión local y
     *    devuelve `Error("Sesión expirada")`.
     * 5. Si falla la red o la deserialización, devuelve el mensaje de la
     *    excepción o "Error de conexión"; en ese caso no limpia la sesión,
     *    porque el fallo puede ser temporal.
     *
     * @return `Result.Success` con la sesión renovada o `Result.Error` con un
     *   mensaje; no propaga excepciones.
     */
    override suspend fun refreshToken(): Result<AuthResponseDto> {
        return try {
            val refreshToken = sessionDataStore.getRefreshToken()
                ?: return Result.Error("No hay refresh token")
            val response = authApi.refresh(RefreshTokenRequestDto(refreshToken))
            if (response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()!!.data!!
                sessionDataStore.saveSession(
                    accessToken = authData.accessToken,
                    refreshToken = authData.refreshToken,
                    userId = authData.user.id,
                    email = authData.user.email,
                    name = "${authData.user.firstName} ${authData.user.lastName}",
                    role = authData.user.role
                )
                Result.Success(authData)
            } else {
                sessionDataStore.clearSession()
                Result.Error("Sesión expirada")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Comprueba si hay un access token local no vacío.
     *
     * Es una lectura de DataStore, sin red ni validación de vencimiento. Puede
     * devolver `true` para un token expirado; las peticiones autenticadas
     * posteriores fallarán con 401 y deberán disparar el refresh o el logout.
     * A diferencia de las operaciones de red, aquí no hay `try/catch`: si la
     * lectura de DataStore falla, la excepción se propaga al llamador.
     *
     * @return `true` si existe un token no vacío; `false` en caso contrario.
     */
    override suspend fun isLoggedIn(): Boolean {
        return !sessionDataStore.getAccessToken().isNullOrBlank()
    }
}
