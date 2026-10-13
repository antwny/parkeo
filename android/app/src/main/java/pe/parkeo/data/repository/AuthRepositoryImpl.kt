// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Acceso/abstracción de datos para el área indicada; conecta operaciones de negocio con sus fuentes.
package pe.parkeo.data.repository

import pe.parkeo.data.local.SessionDataStore
import pe.parkeo.data.remote.api.AuthApi
import pe.parkeo.data.remote.dto.*
import pe.parkeo.domain.repository.AuthRepository
import pe.parkeo.util.Result

class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val sessionDataStore: SessionDataStore
) : AuthRepository {

    /** Autentica las credenciales y actualiza el estado de sesión: login. Consulte el catálogo de métodos y conceptos en la guía maestra. */
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

    /** Registra y devuelve: register. Consulte el catálogo de métodos y conceptos en la guía maestra. */
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

    /** Cierra la sesión y limpia el estado local/remoto correspondiente: logout. Consulte el catálogo de métodos y conceptos en la guía maestra. */
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

    /** Ejecuta la operación indicada por el nombre: refresh token. Consulte el catálogo de métodos y conceptos en la guía maestra. */
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

    /** Indica si se cumple la condición indicada: is logged in. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    override suspend fun isLoggedIn(): Boolean {
        return !sessionDataStore.getAccessToken().isNullOrBlank()
    }
}
